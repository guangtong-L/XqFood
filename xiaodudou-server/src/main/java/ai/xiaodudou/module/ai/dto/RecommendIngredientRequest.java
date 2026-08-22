package ai.xiaodudou.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecommendIngredientRequest(
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "^[\\p{L}\\p{N}（）()·\\- ]+$", message = "食材名称只能包含文字、数字、空格和常用连接符")
        String name,
        @Size(max = 20)
        @Pattern(regexp = "^[\\p{L}\\p{N}（）()·.\\-/ ]*$", message = "数量估算包含不支持的字符")
        String quantityEstimate
) {}
