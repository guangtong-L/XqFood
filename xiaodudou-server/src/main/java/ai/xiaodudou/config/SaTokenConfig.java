package ai.xiaodudou.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import ai.xiaodudou.module.user.service.AccountStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权配置
 *
 * @author xiaodudou
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final AccountStatusService accountStatusService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 放行浏览器预检请求，否则前端会被误判为跨域失败
                    if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                        return;
                    }
                    StpUtil.checkLogin();
                    try {
                        accountStatusService.requireActive(StpUtil.getLoginIdAsLong());
                    } catch (RuntimeException e) {
                        StpUtil.logout();
                        throw e;
                    }
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/health",
                        "/api/v1/readiness",
                        "/api/v1/auth/login/**",
                        "/api/v1/auth/wx-login",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                );
    }
}
