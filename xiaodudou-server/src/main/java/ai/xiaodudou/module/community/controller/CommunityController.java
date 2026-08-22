package ai.xiaodudou.module.community.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 妈妈圈缺少独立发布和展示同意机制，当前所有环境默认关闭。 */
@Tag(name = "07 - 妈妈圈")
@RestController
@RequestMapping("/api/v1/community")
public class CommunityController {

    private final boolean communityEnabled;

    public CommunityController(@Value("${xiaodudou.features.community-enabled:false}") boolean communityEnabled) {
        this.communityEnabled = communityEnabled;
    }

    @GetMapping("/feed")
    @Operation(summary = "妈妈圈暂未开放")
    public Result<Void> feed() {
        // 即使开发配置误开，也必须先完成独立同意机制后才能恢复历史实现。
        if (!communityEnabled) {
            throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "妈妈圈暂未开放");
        }
        throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "妈妈圈发布同意机制尚未完成");
    }
}
