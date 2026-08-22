package ai.xiaodudou.module.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 只返回基础设施是否可用，不向调用方泄露地址、凭据或异常消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfrastructureReadinessService {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public boolean isReady() {
        return databaseReady() && redisReady();
    }

    boolean databaseReady() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.warn("readiness_dependency_down dependency=db errorType={}", e.getClass().getSimpleName());
            return false;
        }
    }

    boolean redisReady() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return pong != null && "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            log.warn("readiness_dependency_down dependency=redis errorType={}", e.getClass().getSimpleName());
            return false;
        }
    }
}
