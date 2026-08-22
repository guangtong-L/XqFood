package ai.xiaodudou.module.user.client;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信小程序 API 客户端
 *
 * 文档：https://developers.weixin.qq.com/miniprogram/dev/api-backend/open-api/login/auth.code2Session.html
 *
 * ⚠️ AppSecret 必须由部署环境安全注入
 * ⚠️ session_key 必须留服务端，不可返回前端
 */
@Slf4j
@Component
public class WechatClient {

    @Value("${xiaodudou.wechat.miniapp.appid:}")
    private String appid;

    @Value("${xiaodudou.wechat.miniapp.secret:}")
    private String secret;

    @Value("${xiaodudou.wechat.miniapp.enabled:false}")
    private boolean enabled;

    @Value("${xiaodudou.wechat.miniapp.timeout-ms:5000}")
    private int timeoutMs;

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";

    /** 配置是否就绪（AppID + Secret 都有且 enabled=true） */
    public boolean isReady() {
        return enabled
                && StrUtil.isNotBlank(appid) && !appid.startsWith("your-")
                && StrUtil.isNotBlank(secret) && !secret.startsWith("your-");
    }

    /**
     * 调用 jscode2session
     * @param jsCode 前端 wx.login() 返回的 code
     * @return openid + session_key + unionid
     */
    public Code2SessionResult code2Session(String jsCode) {
        if (StrUtil.isBlank(jsCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "code 不能为空");
        }

        long start = System.currentTimeMillis();
        try (HttpResponse resp = HttpRequest.get(CODE2SESSION_URL)
                .form("appid", appid)
                .form("secret", secret)
                .form("js_code", jsCode)
                .form("grant_type", "authorization_code")
                .timeout(timeoutMs)
                .execute()) {

            String body = resp.body();
            long cost = System.currentTimeMillis() - start;
            log.info("[Wechat] code2Session cost={}ms status={}", cost, resp.getStatus());

            if (!resp.isOk()) {
                log.error("[Wechat] HTTP 异常 status={}", resp.getStatus());
                throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务异常");
            }

            JSONObject json = JSONUtil.parseObj(body);
            Integer errcode = json.getInt("errcode");

            // 微信成功时 errcode 可能不存在或为 0
            if (errcode != null && errcode != 0) {
                log.warn("wechat_business_error errcode={}", errcode);
                handleErrcode(errcode);
            }

            Code2SessionResult r = new Code2SessionResult();
            r.setOpenid(json.getStr("openid"));
            r.setSessionKey(json.getStr("session_key"));
            r.setUnionid(json.getStr("unionid"));

            if (StrUtil.isBlank(r.getOpenid())) {
                throw new BusinessException(ResultCode.LOGIN_FAILED, "微信未返回 openid");
            }
            return r;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Wechat] 调用异常 type={}", e.getClass().getSimpleName());
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务暂时不可用");
        }
    }

    /** 微信错误码到业务错误的映射 */
    private void handleErrcode(int errcode) {
        // 文档: https://developers.weixin.qq.com/miniprogram/dev/framework/server-ability/backend-api.html
        switch (errcode) {
            case -1 -> throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信服务繁忙，请稍后重试");
            case 40029 -> throw new BusinessException(ResultCode.LOGIN_FAILED, "登录凭证无效，请重新登录");
            case 45011 -> throw new BusinessException(ResultCode.RATE_LIMIT, "微信接口频率限制，请稍后重试");
            case 40226 -> throw new BusinessException(ResultCode.FORBIDDEN, "账号风险拦截");
            default -> throw new BusinessException(ResultCode.LOGIN_FAILED, "微信登录失败，请重新登录");
        }
    }

    @Data
    public static class Code2SessionResult {
        private String openid;
        private String sessionKey;
        private String unionid;
    }
}
