package ai.xiaodudou.module.user.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.user.dto.ProfileData;
import ai.xiaodudou.module.user.dto.ProfileResponse;
import ai.xiaodudou.module.user.dto.SaveProfileRequest;
import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 画像唯一访问边界：校验、跨阶段清理、加解密与历史明文迁移均在此完成。 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    public static final Set<String> STAGES = Set.of("PREPARE", "PREGNANCY", "POSTPARTUM", "WEANING", "CHILD");
    public static final Set<String> DELIVERY_TYPES = Set.of("natural", "cesarean");
    public static final Set<String> FEEDING_TYPES = Set.of("breast", "mixed", "formula");
    public static final Set<String> ALLERGY_CODES = Set.of(
            "egg", "milk", "seafood", "peanut", "nut", "soy", "wheat", "mango");
    public static final Set<String> DISLIKE_CODES = Set.of(
            "coriander", "offal", "bitter_melon", "celery", "ginger", "garlic",
            "onion", "mushroom", "carrot", "eggplant", "fat_meat", "spicy");

    private final UserProfileMapper profileMapper;
    private final ProfileEncryptionService encryptionService;

    public ProfileResponse getResponse(Long userId) {
        return ProfileResponse.from(getData(userId));
    }

    public ProfileData getData(Long userId) {
        UserProfile profile = profileMapper.selectActiveByUserId(userId);
        if (profile == null) return null;
        if (profile.getEncryptedPayload() != null && !profile.getEncryptedPayload().isBlank()) {
            return encryptionService.decrypt(userId, profile.getEncryptedPayload());
        }
        return fromLegacy(profile);
    }

    @Transactional
    public ProfileResponse save(Long userId, SaveProfileRequest request) {
        ProfileData data = normalizeAndValidate(request);
        String encrypted = encryptionService.encrypt(userId, data);
        UserProfile existing = profileMapper.selectActiveByUserId(userId);
        if (existing == null) {
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setEncryptedPayload(encrypted);
            profile.setEncryptionKeyVersion(encryptionService.keyVersion());
            profileMapper.insert(profile);
        } else if (profileMapper.updateEncryptedAndClearLegacy(
                existing.getId(), encrypted, encryptionService.keyVersion()) != 1) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "画像保存失败");
        }
        return ProfileResponse.from(data);
    }

    @Transactional
    public void migrateLegacy(UserProfile profile) {
        if (profile == null || profile.getEncryptedPayload() != null) return;
        String encrypted = encryptionService.encrypt(profile.getUserId(), fromLegacy(profile));
        if (profileMapper.updateEncryptedAndClearLegacy(
                profile.getId(), encrypted, encryptionService.keyVersion()) != 1) {
            throw new IllegalStateException("历史画像迁移写入失败，profileId=" + profile.getId());
        }
    }

    ProfileData normalizeAndValidate(SaveProfileRequest request) {
        String stage = request.getStageType() == null ? null : request.getStageType().trim().toUpperCase(Locale.ROOT);
        if (!STAGES.contains(stage)) bad("stageType 必须是五种合法阶段之一");

        Integer pregnancyWeek = null;
        Integer postpartumDay = null;
        String deliveryType = null;
        String feedingType = null;
        LocalDate babyBirthDate = null;

        if ("PREGNANCY".equals(stage)) {
            if (request.getPregnancyWeek() == null || request.getPregnancyWeek() < 1 || request.getPregnancyWeek() > 42) {
                bad("孕期必须填写 1 到 42 的孕周");
            }
            pregnancyWeek = request.getPregnancyWeek();
        } else if ("POSTPARTUM".equals(stage)) {
            if (request.getPostpartumDay() == null || request.getPostpartumDay() < 0 || request.getPostpartumDay() > 730) {
                bad("产后必须填写 0 到 730 的天数");
            }
            if (!DELIVERY_TYPES.contains(request.getDeliveryType())) bad("请选择合法分娩方式");
            if (!FEEDING_TYPES.contains(request.getFeedingType())) bad("请选择合法喂养方式");
            postpartumDay = request.getPostpartumDay();
            deliveryType = request.getDeliveryType();
            feedingType = request.getFeedingType();
        } else if ("WEANING".equals(stage) || "CHILD".equals(stage)) {
            LocalDate date = request.getBabyBirthDate();
            LocalDate today = LocalDate.now();
            if (date == null) bad("请填写宝宝出生日期");
            if (date.isAfter(today)) bad("宝宝出生日期不能晚于今天");
            if (date.isBefore(today.minusYears(18))) bad("宝宝年龄必须在 0 到 18 周岁范围内");
            babyBirthDate = date;
        }

        List<String> allergies = normalizeCodes(request.getAllergies(), ALLERGY_CODES, 10, "过敏源");
        List<String> dislikes = normalizeCodes(request.getDislikes(), DISLIKE_CODES, 12, "忌口");
        String healthNotes = trimToNull(request.getHealthNotes());
        if (healthNotes != null && healthNotes.length() > 500) bad("健康备注不能超过 500 字");

        return ProfileData.builder()
                .stageType(stage)
                .pregnancyWeek(pregnancyWeek)
                .postpartumDay(postpartumDay)
                .deliveryType(deliveryType)
                .feedingType(feedingType)
                .babyBirthDate(babyBirthDate)
                .allergies(allergies)
                .dislikes(dislikes)
                .healthNotes(healthNotes)
                .build();
    }

    private ProfileData fromLegacy(UserProfile profile) {
        return ProfileData.builder()
                .stageType(profile.getStageType())
                .pregnancyWeek(profile.getPregnancyWeek())
                .postpartumDay(profile.getPostpartumDay())
                .deliveryType(profile.getDeliveryType())
                .feedingType(profile.getFeedingType())
                .babyBirthDate(profile.getBabyBirthDate())
                .allergies(profile.getAllergies() == null ? List.of() : List.copyOf(profile.getAllergies()))
                .dislikes(profile.getDislikes() == null ? List.of() : List.copyOf(profile.getDislikes()))
                .healthNotes(profile.getHealthNotes())
                .build();
    }

    private List<String> normalizeCodes(List<String> source, Set<String> whitelist, int max, String label) {
        if (source == null || source.isEmpty()) return List.of();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String item : source) {
            String value = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty() || value.length() > 32 || !whitelist.contains(value)) {
                bad(label + "包含不支持的选项");
            }
            unique.add(value);
        }
        if (unique.size() > max) bad(label + "数量超过上限");
        return List.copyOf(new ArrayList<>(unique));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void bad(String message) {
        throw new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
