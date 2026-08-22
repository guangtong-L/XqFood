package ai.xiaodudou.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 配置 - 跨域、静态资源等
 *
 * @author xiaodudou
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Environment environment;

    public WebMvcConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * 仅允许配置的精确来源。空白名单代表浏览器跨域访问全部拒绝。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = Binder.get(environment)
                .bind("xiaodudou.cors.allowed-origins", Bindable.listOf(String.class))
                .orElse(List.of());
        boolean allowCredentials = environment.getProperty(
                "xiaodudou.cors.allow-credentials", Boolean.class, true);
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.stream()
                        .filter(origin -> origin != null && !origin.isBlank())
                        .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("x-token", RequestIdFilter.HEADER)
                .allowCredentials(allowCredentials)
                .maxAge(3600);
    }
}
