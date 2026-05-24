package ai.xiaodudou.module.community.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.community.mapper.CommunityMapper;
import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "07 - 妈妈圈")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityMapper communityMapper;
    private final UserProfileMapper userProfileMapper;

    @GetMapping("/feed")
    @Operation(summary = "同阶段妈妈最近 24h 打卡流")
    public Result<Map<String, Object>> feed(
            @RequestParam(required = false) String stageType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 默认用当前用户阶段
        if (stageType == null || stageType.isBlank()) {
            UserProfile p = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
            stageType = p == null ? "POSTPARTUM" : p.getStageType();
        }

        if (size > 50) size = 50;
        int offset = Math.max(0, (page - 1) * size);

        List<Map<String, Object>> rows = communityMapper.sameStageRecentFeed(userId, stageType, size, offset);
        int activeUsers = communityMapper.sameStageActiveUserCount(userId, stageType);

        // 脱敏：昵称只显示第 1 个字 + 「{阶段}{身份}」如"小* (月子第12天)"
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> r = new LinkedHashMap<>(row);
            String nickname = (String) row.get("nickname");
            r.put("displayName", maskNickname(nickname));
            r.put("stageDesc", buildStageDesc(
                    (String) row.get("stageType"),
                    (Integer) row.get("postpartumDay"),
                    (Integer) row.get("pregnancyWeek")));
            // 去掉原始 nickname 避免泄露
            r.remove("nickname");
            r.remove("userId");  // 用户 ID 也脱掉，前端用 displayName 即可
            records.add(r);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stageType", stageType);
        data.put("activeUsers", activeUsers);
        data.put("records", records);
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    private String maskNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) return "匿名妈妈";
        // 中文：取第 1 字 + *；英文：第 1 字符 + *
        String first = nickname.substring(0, 1);
        return first + "**";
    }

    private String buildStageDesc(String stageType, Integer postpartumDay, Integer pregnancyWeek) {
        if (stageType == null) return "通用";
        return switch (stageType) {
            case "POSTPARTUM" -> "月子第 " + (postpartumDay == null ? "?" : postpartumDay) + " 天";
            case "PREGNANCY" -> "孕 " + (pregnancyWeek == null ? "?" : pregnancyWeek) + " 周";
            case "PREPARE" -> "备孕中";
            case "WEANING" -> "辅食期";
            case "CHILD" -> "儿童期";
            default -> stageType;
        };
    }
}
