package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.module.user.dto.ProfileData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileEncryptionServiceTest {

    @Test
    void randomIvMustProduceDifferentCiphertextAndBothDecrypt() {
        ProfileEncryptionService service = service("key-one-32-bytes-for-test-only!!!");
        ProfileData data = ProfileData.builder().stageType("PREGNANCY").pregnancyWeek(20)
                .allergies(List.of("egg")).dislikes(List.of()).build();

        String first = service.encrypt(8L, data);
        String second = service.encrypt(8L, data);

        assertThat(first).startsWith("v1:").isNotEqualTo(second);
        assertThat(service.decrypt(8L, first).getPregnancyWeek()).isEqualTo(20);
        assertThat(service.decrypt(8L, second).getAllergies()).containsExactly("egg");
    }

    @Test
    void tamperedCiphertextMustFailAuthentication() {
        ProfileEncryptionService service = service("key-one-32-bytes-for-test-only!!!");
        String encrypted = service.encrypt(8L, ProfileData.builder().stageType("PREPARE").build());
        byte[] envelope = Base64.getDecoder().decode(encrypted.substring(encrypted.indexOf(':') + 1));
        envelope[envelope.length - 1] ^= 1;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(envelope);

        assertThatThrownBy(() -> service.decrypt(8L, tampered))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("完整性校验");
    }

    @Test
    void wrongKeyMustNotDecrypt() {
        String encrypted = service("key-one-32-bytes-for-test-only!!!")
                .encrypt(8L, ProfileData.builder().stageType("CHILD").build());

        assertThatThrownBy(() -> service("key-two-32-bytes-for-test-only!!!").decrypt(8L, encrypted))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ciphertextMustBeBoundToOwningUser() {
        ProfileEncryptionService service = service("key-one-32-bytes-for-test-only!!!");
        String encrypted = service.encrypt(8L, ProfileData.builder().stageType("PREPARE").build());

        assertThatThrownBy(() -> service.decrypt(9L, encrypted))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("完整性校验");
    }

    @Test
    void keyMustDecodeToExactly256Bits() {
        assertThatThrownBy(() -> new ProfileEncryptionService(
                new ObjectMapper().findAndRegisterModules(), Base64.getEncoder().encodeToString(new byte[16]), "v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 字节");
    }

    private ProfileEncryptionService service(String raw) {
        byte[] source = raw.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(source, 0, key, 0, Math.min(source.length, key.length));
        return new ProfileEncryptionService(
                new ObjectMapper().findAndRegisterModules(), Base64.getEncoder().encodeToString(key), "v1");
    }
}
