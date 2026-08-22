package ai.xiaodudou.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Base64;

/**
 * 生产启动安全闸门，在业务 Bean 和数据库连接初始化前集中拒绝危险配置。
 */
@Component
public class StartupSafetyValidator implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        validate(environment);
    }

    public static void validate(Environment environment) {
        List<String> origins = Binder.get(environment)
                .bind("xiaodudou.cors.allowed-origins", Bindable.listOf(String.class))
                .orElse(List.of());
        boolean allowCredentials = environment.getProperty("xiaodudou.cors.allow-credentials", Boolean.class, true);
        if (allowCredentials && origins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("CORS 安全校验失败：携带凭据时禁止使用通配来源");
        }

        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        if (!profiles.contains("prod")) {
            return;
        }

        List<String> errors = new ArrayList<>();
        if (profiles.contains("dev") || profiles.contains("local")) {
            errors.add("prod 不得与 dev/local profile 同时激活");
        }
        requireFalse(environment, errors, "xiaodudou.mock.login-enabled", "生产环境禁止 Mock 登录");
        requireFalse(environment, errors, "xiaodudou.mock.ai-enabled", "生产环境禁止 Mock AI");
        requireFalse(environment, errors, "xiaodudou.wechat.pay.enabled", "真实微信支付尚未实现，生产环境禁止开启支付");
        requireFalse(environment, errors, "xiaodudou.commercial.enabled", "商业化功能尚未完成，生产环境必须关闭");
        requireFalse(environment, errors, "xiaodudou.features.community-enabled", "妈妈圈缺少发布同意机制，生产环境必须关闭");
        requireFalse(environment, errors, "xiaodudou.features.nutrition-report-enabled", "营养目标百分比与N日报告尚未完成验证，生产环境必须关闭");
        requireFalse(environment, errors, "xiaodudou.features.ai-enabled", "真实内容安全审核供应商尚未完成接入验收，生产环境必须关闭 AI");
        requireFalse(environment, errors, "springdoc.api-docs.enabled", "生产环境禁止暴露 OpenAPI JSON");
        requireFalse(environment, errors, "springdoc.swagger-ui.enabled", "生产环境禁止暴露 API 文档 UI");

        requireTrue(environment, errors, "xiaodudou.wechat.miniapp.enabled", "生产环境必须启用真实微信登录");
        requireValue(environment, errors, "xiaodudou.wechat.miniapp.appid");
        requireValue(environment, errors, "xiaodudou.wechat.miniapp.secret");
        requireValue(environment, errors, "spring.datasource.url");
        requireValue(environment, errors, "spring.datasource.username");
        requireValue(environment, errors, "spring.datasource.password");
        requireValue(environment, errors, "spring.data.redis.host");
        requireValue(environment, errors, "spring.data.redis.password");
        validateEncryptionKey(environment, errors);
        Integer retentionDays = environment.getProperty("xiaodudou.ai.log-retention-days", Integer.class, 0);
        if (retentionDays < 1 || retentionDays > 365) {
            errors.add("生产环境必须显式配置 1..365 天的 AI 日志保留期：xiaodudou.ai.log-retention-days");
        }
        requireRange(environment, errors, "sa-token.timeout", 3600, 604800,
                "生产会话最长有效期必须在 1 小时到 7 天之间");
        requireRange(environment, errors, "sa-token.active-timeout", 300, 43200,
                "生产会话无操作有效期必须在 5 分钟到 12 小时之间");
        requireRange(environment, errors, "xiaodudou.rate-limit.login-attempts", 1, 1000,
                "登录限流次数必须在 1..1000");
        requireRange(environment, errors, "xiaodudou.rate-limit.login-window-seconds", 1, 3600,
                "登录限流窗口必须在 1..3600 秒");

        if (origins.isEmpty()) {
            errors.add("缺少生产 CORS 白名单：xiaodudou.cors.allowed-origins");
        } else {
            for (String origin : origins) {
                if (!isSecureOrigin(origin)) {
                    errors.add("生产 CORS 来源必须是 HTTPS 且禁止通配符/localhost：xiaodudou.cors.allowed-origins");
                    break;
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("小肚兜生产安全闸门拒绝启动：\n - " + String.join("\n - ", errors));
        }
    }

    private static void requireTrue(Environment env, List<String> errors, String key, String message) {
        if (!env.getProperty(key, Boolean.class, false)) errors.add(message + "（" + key + "=true）");
    }

    private static void requireFalse(Environment env, List<String> errors, String key, String message) {
        if (env.getProperty(key, Boolean.class, false)) errors.add(message + "（" + key + "=false）");
    }

    private static void requireRange(Environment env, List<String> errors, String key,
                                     int minimum, int maximum, String message) {
        Integer value = env.getProperty(key, Integer.class);
        if (value == null || value < minimum || value > maximum) {
            errors.add(message + "（" + key + "）");
        }
    }

    private static void requireValue(Environment env, List<String> errors, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("your-") || value.startsWith("TODO_")) {
            errors.add("缺少生产必要配置：" + key);
        }
    }

    private static boolean isSecureOrigin(String origin) {
        try {
            if (origin == null || origin.contains("*")) return false;
            URI uri = URI.create(origin);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && !"localhost".equalsIgnoreCase(host)
                    && !"127.0.0.1".equals(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void validateEncryptionKey(Environment env, List<String> errors) {
        String value = env.getProperty("xiaodudou.security.data-encryption-key");
        if (value == null || value.isBlank()) {
            errors.add("缺少生产敏感画像加密密钥：XDD_DATA_ENCRYPTION_KEY");
            return;
        }
        try {
            if (Base64.getDecoder().decode(value.trim()).length != 32) {
                errors.add("XDD_DATA_ENCRYPTION_KEY 必须是 32 字节密钥的 Base64 编码");
            }
        } catch (IllegalArgumentException e) {
            errors.add("XDD_DATA_ENCRYPTION_KEY 必须是合法 Base64 编码");
        }
    }
}
