package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 每个受保护请求均校验账号未禁用、未注销。 */
@Service
@RequiredArgsConstructor
public class AccountStatusService {

    private final UserMapper userMapper;

    public User requireActive(Long userId) {
        User user = userMapper.selectByIdIncludingDeleted(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (!Integer.valueOf(1).equals(user.getStatus()) || !Integer.valueOf(0).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用或注销");
        }
        return user;
    }
}
