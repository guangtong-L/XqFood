package ai.xiaodudou.module.user.dto;

import java.time.LocalDate;
import java.util.List;

/** 对外画像白名单；不暴露画像主键、用户 ID、密文或密钥版本。 */
public record ProfileResponse(
        String stageType,
        Integer pregnancyWeek,
        Integer postpartumDay,
        String deliveryType,
        String feedingType,
        LocalDate babyBirthDate,
        List<String> allergies,
        List<String> dislikes,
        String healthNotes
) {
    public static ProfileResponse from(ProfileData data) {
        if (data == null) return null;
        return new ProfileResponse(
                data.getStageType(), data.getPregnancyWeek(), data.getPostpartumDay(),
                data.getDeliveryType(), data.getFeedingType(), data.getBabyBirthDate(),
                data.getAllergies() == null ? List.of() : List.copyOf(data.getAllergies()),
                data.getDislikes() == null ? List.of() : List.copyOf(data.getDislikes()),
                data.getHealthNotes());
    }
}
