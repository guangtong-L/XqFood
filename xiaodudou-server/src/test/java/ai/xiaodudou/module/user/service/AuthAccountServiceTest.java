package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.module.user.dto.WxLoginRequest;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthAccountServiceTest {

    private static final WxLoginRequest REQUEST = new WxLoginRequest("code", "昵称", "");

    @Test
    void accountCreationUsesReadCommittedForPostConflictVisibility() throws Exception {
        Method method = AuthAccountService.class.getMethod("findOrCreate",
                String.class, String.class, WxLoginRequest.class, boolean.class);
        assertThat(method.getAnnotation(Transactional.class).isolation())
                .isEqualTo(Isolation.READ_COMMITTED);
    }

    @Test
    void createsFirstAccount() {
        UserMapper mapper = mock(UserMapper.class);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(mapper).insert(any(User.class));

        User user = new AuthAccountService(mapper).findOrCreate("openid", null, REQUEST, true);

        assertThat(user.getId()).isEqualTo(10L);
        assertThat(user.getStatus()).isEqualTo(1);
        assertThat(user.getDeleted()).isZero();
    }

    @Test
    void concurrentDuplicateReturnsTheSingleExistingOpenidRow() {
        UserMapper mapper = mock(UserMapper.class);
        User winner = active(20L);
        when(mapper.selectByOpenidIncludingDeleted("openid")).thenReturn(null, winner);
        doThrow(new DuplicateKeyException("duplicate")).when(mapper).insert(any(User.class));

        assertThat(new AuthAccountService(mapper).findOrCreate("openid", null, REQUEST, true))
                .isSameAs(winner);
    }

    @Test
    void duplicateWithoutSameOpenidRowFailsExplicitly() {
        UserMapper mapper = mock(UserMapper.class);
        when(mapper.selectByOpenidIncludingDeleted("openid")).thenReturn(null);
        doThrow(new DuplicateKeyException("other unique key")).when(mapper).insert(any(User.class));

        assertThatThrownBy(() -> new AuthAccountService(mapper)
                .findOrCreate("openid", null, REQUEST, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号创建失败");
    }

    @Test
    void nonDuplicateDatabaseFailureIsNeverHidden() {
        UserMapper mapper = mock(UserMapper.class);
        doThrow(new DataAccessResourceFailureException("db down")).when(mapper).insert(any(User.class));

        assertThatThrownBy(() -> new AuthAccountService(mapper)
                .findOrCreate("openid", null, REQUEST, true))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void disabledAccountIsRejected() {
        UserMapper mapper = mock(UserMapper.class);
        User disabled = active(30L);
        disabled.setStatus(0);
        when(mapper.selectByOpenidIncludingDeleted("openid")).thenReturn(disabled);

        assertThatThrownBy(() -> new AuthAccountService(mapper)
                .findOrCreate("openid", null, REQUEST, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停用或注销");
        verify(mapper).selectByOpenidIncludingDeleted("openid");
    }

    private static User active(long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        user.setDeleted(0);
        user.setVipLevel(0);
        return user;
    }
}
