package ai.xiaodudou.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 跨域、静态资源等
 *
 * @author xiaodudou
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 开发期允许全部跨域，生产环境务必收紧到指定域名
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("x-token")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
