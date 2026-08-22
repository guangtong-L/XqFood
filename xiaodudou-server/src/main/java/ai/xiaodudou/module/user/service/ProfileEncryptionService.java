package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.user.dto.ProfileData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** 使用 256 位密钥的 AES-GCM 画像加密。每次加密使用独立随机 12 字节 IV，认证标签由 JCE 追加。 */
@Service
public class ProfileEncryptionService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec key;
    private final String keyVersion;

    public ProfileEncryptionService(
            ObjectMapper objectMapper,
            @Value("${xiaodudou.security.data-encryption-key:}") String base64Key,
            @Value("${xiaodudou.security.data-encryption-key-version:v1}") String keyVersion) {
        this.objectMapper = objectMapper;
        this.keyVersion = keyVersion == null || keyVersion.isBlank() ? "v1" : keyVersion.trim();
        this.key = decodeKey(base64Key);
    }

    public boolean isReady() {
        return key != null;
    }

    public String keyVersion() {
        return keyVersion;
    }

    public String encrypt(Long userId, ProfileData data) {
        requireKey();
        requireUserId(userId);
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad(userId));
            byte[] plaintext = objectMapper.writeValueAsBytes(data);
            byte[] ciphertextAndTag = cipher.doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(iv.length + ciphertextAndTag.length)
                    .put(iv).put(ciphertextAndTag).array();
            return keyVersion + ":" + Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException | JsonProcessingException e) {
            throw unavailable("敏感画像加密失败", e);
        }
    }

    public ProfileData decrypt(Long userId, String encryptedPayload) {
        requireKey();
        requireUserId(userId);
        if (encryptedPayload == null || encryptedPayload.isBlank()) return null;
        int delimiter = encryptedPayload.indexOf(':');
        if (delimiter <= 0 || !keyVersion.equals(encryptedPayload.substring(0, delimiter))) {
            throw unavailable("敏感画像密钥版本不可用", null);
        }
        try {
            byte[] envelope = Base64.getDecoder().decode(encryptedPayload.substring(delimiter + 1));
            if (envelope.length <= IV_LENGTH + 16) {
                throw new GeneralSecurityException("invalid envelope");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertextAndTag = new byte[envelope.length - IV_LENGTH];
            System.arraycopy(envelope, 0, iv, 0, IV_LENGTH);
            System.arraycopy(envelope, IV_LENGTH, ciphertextAndTag, 0, ciphertextAndTag.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad(userId));
            byte[] plaintext = cipher.doFinal(ciphertextAndTag);
            return objectMapper.readValue(plaintext, ProfileData.class);
        } catch (AEADBadTagException e) {
            throw unavailable("敏感画像完整性校验失败", e);
        } catch (GeneralSecurityException | IllegalArgumentException | IOException e) {
            throw unavailable("敏感画像解密失败", e);
        }
    }

    private SecretKeySpec decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Key.trim());
            if (bytes.length != 32) {
                throw new IllegalStateException("XDD_DATA_ENCRYPTION_KEY 必须是 32 字节密钥的 Base64 编码");
            }
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("XDD_DATA_ENCRYPTION_KEY 不是合法 Base64 密钥", e);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "敏感画像服务未配置");
        }
    }

    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户标识无效");
        }
    }

    /** 将密文绑定到所属用户，避免数据库中的画像密文被跨用户整行替换。 */
    private byte[] aad(Long userId) {
        return ("user-profile:" + userId + ":" + keyVersion).getBytes(StandardCharsets.UTF_8);
    }

    private BusinessException unavailable(String message, Exception cause) {
        BusinessException exception = new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        if (cause != null) exception.initCause(cause);
        return exception;
    }
}
