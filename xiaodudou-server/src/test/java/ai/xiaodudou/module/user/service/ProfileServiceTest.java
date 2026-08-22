package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.module.user.dto.ProfileData;
import ai.xiaodudou.module.user.dto.SaveProfileRequest;
import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.apache.ibatis.annotations.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class ProfileServiceTest {

    private UserProfileMapper mapper;
    private ProfileEncryptionService encryption;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        mapper = mock(UserProfileMapper.class);
        encryption = new ProfileEncryptionService(new ObjectMapper().findAndRegisterModules(),
                Base64.getEncoder().encodeToString(new byte[32]), "v1");
        service = new ProfileService(mapper, encryption);
    }

    @Test
    void pregnancyMustClearFieldsFromOtherStagesAndDeduplicateCodes() {
        SaveProfileRequest request = base("PREGNANCY");
        request.setPregnancyWeek(18);
        request.setPostpartumDay(12);
        request.setDeliveryType("natural");
        request.setFeedingType("breast");
        request.setBabyBirthDate(LocalDate.now().minusYears(1));
        request.setAllergies(List.of("egg", "egg", "milk"));

        ProfileData data = service.normalizeAndValidate(request);

        assertThat(data.getPregnancyWeek()).isEqualTo(18);
        assertThat(data.getPostpartumDay()).isNull();
        assertThat(data.getDeliveryType()).isNull();
        assertThat(data.getFeedingType()).isNull();
        assertThat(data.getBabyBirthDate()).isNull();
        assertThat(data.getAllergies()).containsExactly("egg", "milk");
    }

    @Test
    void postpartumRequiresBoundedDayAndExplicitDeliveryAndFeeding() {
        SaveProfileRequest request = base("POSTPARTUM");
        request.setPostpartumDay(731);
        request.setDeliveryType("unknown");
        request.setFeedingType("breast");

        assertThatThrownBy(() -> service.normalizeAndValidate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 到 730");
    }

    @Test
    void babyBirthDateMustNotBeFutureOrOlderThanReasonableRange() {
        SaveProfileRequest future = base("CHILD");
        future.setBabyBirthDate(LocalDate.now().plusDays(1));
        SaveProfileRequest tooOld = base("WEANING");
        tooOld.setBabyBirthDate(LocalDate.now().minusYears(19));

        assertThatThrownBy(() -> service.normalizeAndValidate(future)).hasMessageContaining("不能晚于今天");
        assertThatThrownBy(() -> service.normalizeAndValidate(tooOld)).hasMessageContaining("18 周岁");
    }

    @Test
    void allergyAndDislikeMustUseWhitelists() {
        SaveProfileRequest request = base("PREPARE");
        request.setAllergies(List.of("not-supported"));

        assertThatThrownBy(() -> service.normalizeAndValidate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持");
    }

    @Test
    void newProfileWritesOnlyEncryptedPayload() {
        when(mapper.selectActiveByUserId(7L)).thenReturn(null);
        SaveProfileRequest request = base("POSTPARTUM");
        request.setPostpartumDay(0);
        request.setDeliveryType("cesarean");
        request.setFeedingType("mixed");

        service.save(7L, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(mapper).insert(captor.capture());
        UserProfile stored = captor.getValue();
        assertThat(stored.getEncryptedPayload()).startsWith("v1:");
        assertThat(stored.getEncryptionKeyVersion()).isEqualTo("v1");
        assertThat(stored.getStageType()).isNull();
        assertThat(stored.getPregnancyWeek()).isNull();
        assertThat(stored.getPostpartumDay()).isNull();
        assertThat(encryption.decrypt(7L, stored.getEncryptedPayload()).getPostpartumDay()).isZero();
    }

    @Test
    void legacyPlaintextIsEncryptedAndClearedBySingleMapperUpdate() {
        UserProfile legacy = new UserProfile();
        legacy.setId(99L);
        legacy.setUserId(8L);
        legacy.setStageType("POSTPARTUM");
        legacy.setPostpartumDay(5);
        legacy.setDeliveryType("natural");
        legacy.setFeedingType("breast");
        legacy.setAllergies(List.of("egg"));
        when(mapper.updateEncryptedAndClearLegacy(eq(99L), anyString(), eq("v1"))).thenReturn(1);

        service.migrateLegacy(legacy);

        ArgumentCaptor<String> encrypted = ArgumentCaptor.forClass(String.class);
        verify(mapper).updateEncryptedAndClearLegacy(eq(99L), encrypted.capture(), eq("v1"));
        assertThat(encryption.decrypt(8L, encrypted.getValue()).getAllergies()).containsExactly("egg");
    }

    @Test
    void alreadyEncryptedLegacyRowIsSkippedIdempotently() {
        UserProfile encrypted = new UserProfile();
        encrypted.setId(100L);
        encrypted.setEncryptedPayload("v1:existing");

        service.migrateLegacy(encrypted);

        verify(mapper, never()).updateEncryptedAndClearLegacy(eq(100L), anyString(), anyString());
    }

    @Test
    void encryptedUpdateSqlMustClearEveryLegacySensitiveColumn() throws Exception {
        Update annotation = UserProfileMapper.class
                .getMethod("updateEncryptedAndClearLegacy", Long.class, String.class, String.class)
                .getAnnotation(Update.class);
        String sql = String.join(" ", annotation.value());

        assertThat(sql).contains(
                "stage_type = NULL", "pregnancy_week = NULL", "postpartum_day = NULL",
                "delivery_type = NULL", "feeding_type = NULL", "baby_birth_date = NULL",
                "allergies = NULL", "dislikes = NULL", "health_notes = NULL");
    }

    private SaveProfileRequest base(String stage) {
        SaveProfileRequest request = new SaveProfileRequest();
        request.setStageType(stage);
        request.setSensitiveInfoConsent(true);
        request.setAllergies(List.of());
        request.setDislikes(List.of());
        return request;
    }
}
