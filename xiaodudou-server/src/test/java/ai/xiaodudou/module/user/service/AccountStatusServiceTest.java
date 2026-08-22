package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountStatusServiceTest {

    @Test
    void disabledAccountCannotUseProtectedApis() {
        UserMapper mapper = mock(UserMapper.class);
        User user = user(0, 0);
        when(mapper.selectByIdIncludingDeleted(9L)).thenReturn(user);

        assertThatThrownBy(() -> new AccountStatusService(mapper).requireActive(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停用或注销");
    }

    @Test
    void deletedAccountCannotUseProtectedApis() {
        UserMapper mapper = mock(UserMapper.class);
        User user = user(1, 1);
        when(mapper.selectByIdIncludingDeleted(9L)).thenReturn(user);

        assertThatThrownBy(() -> new AccountStatusService(mapper).requireActive(9L))
                .isInstanceOf(BusinessException.class);
    }

    private User user(int status, int deleted) {
        User user = new User();
        user.setStatus(status);
        user.setDeleted(deleted);
        return user;
    }
}
