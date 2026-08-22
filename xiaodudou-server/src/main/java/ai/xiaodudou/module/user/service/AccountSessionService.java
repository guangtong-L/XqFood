package ai.xiaodudou.module.user.service;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Service;

/** 会话操作独立封装，确保注销成功后踢下该账号全部会话。 */
@Service
public class AccountSessionService {
    public void logoutAll(Long userId) {
        StpUtil.logout(userId);
    }
}
