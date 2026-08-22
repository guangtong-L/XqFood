package ai.xiaodudou.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 画像密文中的最小业务数据，不包含用户 ID 或数据库字段。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileData {
    private String stageType;
    private Integer pregnancyWeek;
    private Integer postpartumDay;
    private String deliveryType;
    private String feedingType;
    private LocalDate babyBirthDate;
    private List<String> allergies;
    private List<String> dislikes;
    private String healthNotes;
}
