package ai.xiaodudou.module.user.service;

import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.ai.mapper.AiCallLogMapper;
import ai.xiaodudou.module.feedback.mapper.FeedbackMapper;
import ai.xiaodudou.module.order.mapper.OrderMapper;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import org.apache.ibatis.annotations.Delete;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    @Test
    void successfulDeletionCleansPersonalDataAnonymizesAccountAndInvalidatesSessions() {
        Fixture f = fixture(activeUser());
        when(f.userMapper.anonymizeAndDelete(org.mockito.ArgumentMatchers.eq(8L), anyString(), anyString())).thenReturn(1);

        assertThat(f.service.deleteAccount(8L)).isTrue();

        verify(f.profileMapper).physicallyDeleteByUserId(8L);
        verify(f.actionMapper).physicallyDeleteByUserId(8L);
        verify(f.feedbackMapper).physicallyDeleteByUserId(8L);
        verify(f.aiLogMapper).physicallyDeleteByUserId(8L);
        verify(f.orderMapper).deleteNonFinancialByUserId(8L);
        ArgumentCaptor<String> openid = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unionid = ArgumentCaptor.forClass(String.class);
        verify(f.userMapper).anonymizeAndDelete(org.mockito.ArgumentMatchers.eq(8L), openid.capture(), unionid.capture());
        assertThat(openid.getValue()).startsWith("deleted:8:").hasSizeLessThanOrEqualTo(64);
        assertThat(unionid.getValue()).startsWith("deleted:8:").hasSizeLessThanOrEqualTo(64);
        assertThat(openid.getValue()).isNotEqualTo("original-openid");
        verify(f.sessionService).logoutAll(8L);
    }

    @Test
    void alreadyDeletedAccountIsIdempotentAndDoesNotRepeatDataMutation() {
        User deleted = activeUser();
        deleted.setDeleted(1);
        Fixture f = fixture(deleted);

        assertThat(f.service.deleteAccount(8L)).isTrue();

        verifyNoInteractions(f.profileMapper, f.actionMapper, f.feedbackMapper, f.aiLogMapper, f.orderMapper);
        verify(f.userMapper, never()).anonymizeAndDelete(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
        verify(f.sessionService).logoutAll(8L);
    }

    @Test
    void cleanupFailurePropagatesAndMustNotAnonymizeOrInvalidateSessionBeforeRollback() {
        Fixture f = fixture(activeUser());
        when(f.feedbackMapper.physicallyDeleteByUserId(8L)).thenThrow(new IllegalStateException("db failure"));

        assertThatThrownBy(() -> f.service.deleteAccount(8L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db failure");

        verify(f.userMapper, never()).anonymizeAndDelete(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
        verifyNoInteractions(f.sessionService);
    }

    @Test
    void cleanupFailureThroughSpringTransactionProxyTriggersRollback() {
        Fixture f = fixture(activeUser());
        when(f.feedbackMapper.physicallyDeleteByUserId(8L)).thenThrow(new IllegalStateException("db failure"));
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        ProxyFactory factory = new ProxyFactory(f.service);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        interceptor.afterPropertiesSet();
        factory.addAdvice(interceptor);
        AccountDeletionService transactionalService = (AccountDeletionService) factory.getProxy();

        assertThatThrownBy(() -> transactionalService.deleteAccount(8L))
                .isInstanceOf(IllegalStateException.class);

        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
        verify(f.userMapper, never()).anonymizeAndDelete(org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
        verifyNoInteractions(f.sessionService);
    }

    @Test
    void transactionAndOrderSqlProtectFinancialRecords() throws Exception {
        Transactional transactional = AccountDeletionService.class
                .getMethod("deleteAccount", Long.class).getAnnotation(Transactional.class);
        Delete deleteSql = OrderMapper.class.getMethod("deleteNonFinancialByUserId", Long.class).getAnnotation(Delete.class);
        String sql = String.join(" ", deleteSql.value());

        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).contains(Exception.class);
        assertThat(sql).contains("PENDING", "CANCELLED", "EXPIRED").doesNotContain("PAID", "REFUNDED");
    }

    private Fixture fixture(User user) {
        UserMapper userMapper = mock(UserMapper.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        UserRecipeActionMapper actionMapper = mock(UserRecipeActionMapper.class);
        FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
        AiCallLogMapper aiLogMapper = mock(AiCallLogMapper.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        AccountSessionService sessionService = mock(AccountSessionService.class);
        when(userMapper.selectByIdForUpdateIncludingDeleted(8L)).thenReturn(user);
        AccountDeletionService service = new AccountDeletionService(userMapper, profileMapper, actionMapper,
                feedbackMapper, aiLogMapper, orderMapper, sessionService);
        return new Fixture(service, userMapper, profileMapper, actionMapper, feedbackMapper, aiLogMapper, orderMapper, sessionService);
    }

    private User activeUser() {
        User user = new User();
        user.setId(8L);
        user.setWxOpenid("original-openid");
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }

    private record Fixture(AccountDeletionService service, UserMapper userMapper,
                           UserProfileMapper profileMapper, UserRecipeActionMapper actionMapper,
                           FeedbackMapper feedbackMapper, AiCallLogMapper aiLogMapper,
                           OrderMapper orderMapper, AccountSessionService sessionService) {
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int commits;
        private int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // no-op：仅验证 Spring 事务代理按注解执行回滚路径。
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
