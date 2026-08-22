package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.user.dto.WxLoginRequest;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 并发安全的账号查建与资料更新事务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAccountService {

    private final UserMapper userMapper;

    // MySQL 默认 REPEATABLE_READ 会让首次空查询建立旧快照：并发 INSERT 冲突后，
    // 同一事务中的再次查询仍可能看不到已提交记录。READ_COMMITTED 保证冲突核验可见。
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public User findOrCreate(String openid, String unionid, WxLoginRequest request, boolean realMode) {
        User user = userMapper.selectByOpenidIncludingDeleted(openid);
        if (user == null) {
            User candidate = new User();
            candidate.setWxOpenid(openid);
            candidate.setWxUnionid(unionid);
            candidate.setNickname(StrUtil.isNotBlank(request.nickname()) ? request.nickname() : "小肚兜用户");
            candidate.setAvatarUrl(request.avatarUrl());
            candidate.setStatus(1);
            candidate.setVipLevel(0);
            candidate.setDeleted(0);
            try {
                userMapper.insert(candidate);
                user = candidate;
                log.info("new_user_registered id={} mode={}", user.getId(), realMode ? "real" : "mock");
            } catch (DuplicateKeyException duplicate) {
                // 只把确实由同 openid 并发创建出的记录当作幂等成功。
                user = userMapper.selectByOpenidIncludingDeleted(openid);
                if (user == null) {
                    throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "账号创建失败，请稍后重试");
                }
            }
        }

        requireActive(user);
        boolean changed = false;
        if (StrUtil.isNotBlank(request.nickname()) && !request.nickname().equals(user.getNickname())) {
            user.setNickname(request.nickname());
            changed = true;
        }
        if (StrUtil.isNotBlank(request.avatarUrl()) && !request.avatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(request.avatarUrl());
            changed = true;
        }
        if (StrUtil.isNotBlank(unionid) && !unionid.equals(user.getWxUnionid())) {
            user.setWxUnionid(unionid);
            changed = true;
        }
        if (changed) userMapper.updateById(user);
        return user;
    }

    private void requireActive(User user) {
        if (!Integer.valueOf(1).equals(user.getStatus()) || !Integer.valueOf(0).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已停用或注销");
        }
    }
}
