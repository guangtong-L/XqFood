package ai.xiaodudou.module.user;

import ai.xiaodudou.module.user.dto.ProfileResponse;
import ai.xiaodudou.module.user.dto.SaveProfileRequest;
import ai.xiaodudou.module.user.dto.UserMeResponse;
import ai.xiaodudou.module.user.dto.WxLoginRequest;
import ai.xiaodudou.module.user.controller.UserController;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.entity.UserProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void meResponseSerializationMustContainOnlyApprovedWhitelist() throws Exception {
        UserMeResponse response = new UserMeResponse(1L, "昵称", "https://example.com/a.png", 0,
                null, LocalDateTime.of(2026, 1, 1, 0, 0),
                new ProfileResponse("PREPARE", null, null, null, null, null, List.of(), List.of(), null));

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(response);
        java.util.Set<String> keys = new java.util.HashSet<>();
        json.fieldNames().forEachRemaining(keys::add);

        assertThat(keys)
                .isEqualTo(Set.of("id", "nickname", "avatarUrl", "vipLevel", "vipExpireAt", "createdAt", "profile"));
        assertThat(json.toString()).doesNotContain("wxOpenid", "wxUnionid", "phone", "status", "deleted",
                "encryptedPayload", "encryptionKeyVersion", "userId");
    }

    @Test
    void loginRequestRequiresBoundedCodeAndValidAvatarUrl() {
        WxLoginRequest request = new WxLoginRequest("", "n".repeat(65), "http://unsafe.example/a.png");
        assertThat(validator.validate(request)).hasSize(3);
    }

    @Test
    void profileConsentAndCollectionLimitsAreValidatedBeforeService() {
        SaveProfileRequest request = new SaveProfileRequest();
        request.setStageType("PREPARE");
        request.setSensitiveInfoConsent(false);
        request.setAllergies(java.util.Collections.nCopies(11, "egg"));
        request.setHealthNotes("x".repeat(501));

        assertThat(validator.validate(request).stream().map(v -> v.getPropertyPath().toString()))
                .contains("sensitiveInfoConsent", "allergies", "healthNotes");
    }

    @Test
    void userControllerNeverUsesDatabaseEntitiesAsRequestOrResponseContract() {
        for (java.lang.reflect.Method method : UserController.class.getDeclaredMethods()) {
            assertThat(method.getGenericReturnType().getTypeName())
                    .doesNotContain(User.class.getName(), UserProfile.class.getName());
            assertThat(method.getParameterTypes()).doesNotContain(User.class, UserProfile.class);
        }
    }
}
