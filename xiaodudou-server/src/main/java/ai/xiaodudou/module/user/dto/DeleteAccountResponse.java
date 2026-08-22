package ai.xiaodudou.module.user.dto;

/** 注销结果不返回任何原账号信息。 */
public record DeleteAccountResponse(boolean deleted) {
}
