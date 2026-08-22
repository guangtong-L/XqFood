package ai.xiaodudou.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 段位规划：
 *   0       成功
 *   1xxxx   通用客户端错误
 *   2xxxx   通用服务端错误
 *   3xxxx   鉴权与权限
 *   4xxxx   用户与档案
 *   5xxxx   食谱与食材
 *   6xxxx   AI 相关
 *   7xxxx   订单支付
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    OK(0, "成功"),

    // 通用
    BAD_REQUEST(10001, "请求参数错误"),
    NOT_FOUND(10004, "资源不存在"),
    PAYLOAD_TOO_LARGE(10013, "上传文件过大"),
    INVALID_IMAGE(10022, "图片格式或内容无效"),
    RATE_LIMIT(10029, "请求过于频繁，请稍后再试"),
    SERVER_ERROR(20000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(20003, "服务暂时不可用，请稍后重试"),
    FEATURE_NOT_AVAILABLE(20010, "该功能暂未开放"),

    // 鉴权
    UNAUTHORIZED(30001, "未登录或登录已失效"),
    FORBIDDEN(30003, "无权访问"),
    LOGIN_FAILED(30100, "登录失败"),

    // 用户
    USER_NOT_FOUND(40001, "用户不存在"),
    PROFILE_INCOMPLETE(40010, "请先完善阶段画像"),

    // 食谱/食材
    RECIPE_NOT_FOUND(50001, "食谱不存在"),

    // AI
    AI_TIMEOUT(60001, "AI 服务超时"),
    AI_QUOTA_USED_UP(60010, "今日 AI 额度已用完"),
    AI_AUDIT_FAILED(60020, "内容审核未通过"),
    AI_INGREDIENT_NOT_RECOGNIZED(60030, "未识别到食材，请重拍或手动输入"),
    AI_SERVICE_UNAVAILABLE(60050, "AI 服务暂时不可用，请稍后重试"),
    AI_INVALID_RESPONSE(60060, "AI 返回结构异常，请稍后重试"),

    // 订单
    ORDER_NOT_FOUND(70001, "订单不存在"),
    PAY_FAILED(70010, "支付失败");

    private final Integer code;
    private final String message;
}
