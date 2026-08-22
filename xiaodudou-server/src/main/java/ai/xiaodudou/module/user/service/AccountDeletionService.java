package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.ai.mapper.AiCallLogMapper;
import ai.xiaodudou.module.feedback.mapper.FeedbackMapper;
import ai.xiaodudou.module.order.mapper.OrderMapper;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/** 账号注销事务：先锁账号，再清理数据，最后匿名化账号；任何失败整体回滚。 */
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final UserRecipeActionMapper actionMapper;
    private final FeedbackMapper feedbackMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final OrderMapper orderMapper;
    private final AccountSessionService sessionService;

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAccount(Long userId) {
        User user = userMapper.selectByIdForUpdateIncludingDeleted(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (Integer.valueOf(1).equals(user.getDeleted())) {
            scheduleSessionInvalidation(userId);
            return true;
        }

        profileMapper.physicallyDeleteByUserId(userId);
        actionMapper.physicallyDeleteByUserId(userId);
        feedbackMapper.physicallyDeleteByUserId(userId);
        aiCallLogMapper.physicallyDeleteByUserId(userId);
        orderMapper.deleteNonFinancialByUserId(userId);

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String anonymousOpenid = "deleted:" + userId + ":" + suffix.substring(0, 16);
        String anonymousUnionid = "deleted:" + userId + ":" + suffix.substring(16);
        if (userMapper.anonymizeAndDelete(userId, anonymousOpenid, anonymousUnionid) != 1) {
            throw new IllegalStateException("账号匿名化更新失败");
        }

        scheduleSessionInvalidation(userId);
        return true;
    }

    private void scheduleSessionInvalidation(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sessionService.logoutAll(userId);
                }
            });
        } else {
            sessionService.logoutAll(userId);
        }
    }
}
