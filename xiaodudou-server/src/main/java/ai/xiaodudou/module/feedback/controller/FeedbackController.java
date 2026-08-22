package ai.xiaodudou.module.feedback.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.feedback.entity.Feedback;
import ai.xiaodudou.module.feedback.mapper.FeedbackMapper;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 用户反馈
 *
 * 防刷：同用户 30s 只能提交 1 条（Redis 锁）
 * 长度：5 ~ 1000 字
 */
@Slf4j
@Tag(name = "09 - 反馈与客服")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackMapper feedbackMapper;
    private final StringRedisTemplate redis;

    private static final Duration SUBMIT_LOCK_TTL = Duration.ofSeconds(30);
    private static final int CONTENT_MIN = 5;
    private static final int CONTENT_MAX = 1000;

    @Data
    public static class FeedbackReq {
        private String content;
        private String contact;
        private String category;
        private Map<String, Object> clientInfo;
    }

    @PostMapping
    @Operation(summary = "提交反馈")
    public Result<Map<String, Object>> submit(@RequestBody FeedbackReq req) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (StrUtil.isBlank(req.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "反馈内容不能为空");
        }
        String content = req.getContent().trim();
        if (content.length() < CONTENT_MIN || content.length() > CONTENT_MAX) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "反馈内容长度需 " + CONTENT_MIN + " ~ " + CONTENT_MAX + " 字");
        }

        // 防刷：30s 内只能提交 1 条
        String lockKey = "lock:feedback:" + userId;
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", SUBMIT_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ResultCode.RATE_LIMIT, "提交太快，请稍后再试");
        }

        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setContent(content);
        fb.setContact(StrUtil.isBlank(req.getContact()) ? null : req.getContact().trim());
        fb.setCategory(StrUtil.isBlank(req.getCategory()) ? "general" : req.getCategory());
        fb.setStatus(Feedback.STATUS_PENDING);
        fb.setClientInfo(req.getClientInfo());
        feedbackMapper.insert(fb);

        log.info("[Feedback] 提交成功 userId={} feedbackId={} category={}",
                userId, fb.getId(), fb.getCategory());

        return Result.ok(Map.of(
                "feedbackId", fb.getId(),
                "status", fb.getStatus(),
                "message", "反馈已收到，处理进度以实际状态为准"
        ));
    }

    @GetMapping("/my")
    @Operation(summary = "我的反馈历史")
    public Result<List<Feedback>> myList() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Feedback> list = feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreatedAt)
                .last("limit 50"));
        return Result.ok(list);
    }
}
