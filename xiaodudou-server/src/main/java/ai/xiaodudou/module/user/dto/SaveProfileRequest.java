package ai.xiaodudou.module.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 保存画像请求。跨字段和白名单规则由 ProfileService 统一校验。 */
@Data
public class SaveProfileRequest {

    @NotBlank
    @Size(max = 16)
    private String stageType;

    @Min(1)
    @Max(42)
    private Integer pregnancyWeek;

    @Min(0)
    @Max(730)
    private Integer postpartumDay;

    @Size(max = 16)
    private String deliveryType;

    @Size(max = 16)
    private String feedingType;

    private LocalDate babyBirthDate;

    @Size(max = 10)
    private List<@NotBlank @Size(max = 32) String> allergies;

    @Size(max = 12)
    private List<@NotBlank @Size(max = 32) String> dislikes;

    @Size(max = 500)
    private String healthNotes;

    @AssertTrue(message = "请先确认敏感信息用途说明")
    private boolean sensitiveInfoConsent;
}
