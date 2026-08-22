package ai.xiaodudou.module.system;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfrastructureReadinessServiceTest {

    @Test
    void readyOnlyWhenDatabaseAndRedisRespond() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection database = mock(Connection.class);
        RedisConnectionFactory redisFactory = mock(RedisConnectionFactory.class);
        RedisConnection redis = mock(RedisConnection.class);
        when(dataSource.getConnection()).thenReturn(database);
        when(database.isValid(2)).thenReturn(true);
        when(redisFactory.getConnection()).thenReturn(redis);
        when(redis.ping()).thenReturn("PONG");

        assertThat(new InfrastructureReadinessService(dataSource, redisFactory).isReady()).isTrue();
    }

    @Test
    void databaseFailureMakesReadinessDownWithoutCheckingRedis() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        RedisConnectionFactory redisFactory = mock(RedisConnectionFactory.class);
        when(dataSource.getConnection()).thenThrow(new IllegalStateException("db unavailable"));

        assertThat(new InfrastructureReadinessService(dataSource, redisFactory).isReady()).isFalse();
    }

    @Test
    void redisFailureMakesReadinessDown() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection database = mock(Connection.class);
        RedisConnectionFactory redisFactory = mock(RedisConnectionFactory.class);
        when(dataSource.getConnection()).thenReturn(database);
        when(database.isValid(2)).thenReturn(true);
        when(redisFactory.getConnection()).thenThrow(new IllegalStateException("redis unavailable"));

        assertThat(new InfrastructureReadinessService(dataSource, redisFactory).isReady()).isFalse();
    }
}
