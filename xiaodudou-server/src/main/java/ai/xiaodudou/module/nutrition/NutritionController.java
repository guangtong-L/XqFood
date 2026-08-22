package ai.xiaodudou.module.nutrition;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "06 - 营养估算")
@RestController
@RequestMapping("/api/v1/nutrition")
public class NutritionController {
    private final NutritionEstimationService estimationService;
    private final boolean reportEnabled;

    public NutritionController(NutritionEstimationService estimationService,
                               @Value("${xiaodudou.features.nutrition-report-enabled:false}") boolean reportEnabled) {
        this.estimationService = estimationService;
        this.reportEnabled = reportEnabled;
    }

    @GetMapping("/today")
    @Operation(summary = "仅基于今日已记录菜谱及份数的未验证营养估算")
    public Result<NutritionTodayResponse> today() {
        return Result.ok(estimationService.today(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/report")
    @Operation(summary = "N 日营养报告（暂未开放）")
    public Result<Void> report() {
        if (!reportEnabled) {
            throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "营养报告暂未开放");
        }
        // 即便开发环境误开开关，也不得恢复未经验证的目标百分比实现。
        throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE, "营养报告数据模型尚未通过验收");
    }
}
