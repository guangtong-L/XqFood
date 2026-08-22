package ai.xiaodudou.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RecommendRequest {
    @NotEmpty @Size(max = 20)
    private List<@Valid RecommendIngredientRequest> ingredients;
    @NotNull @Min(1) @Max(5)
    private Integer count = 3;
    @NotNull @Min(5) @Max(180)
    private Integer maxCookMinutes = 60;

    public List<RecommendIngredientRequest> getIngredients() { return ingredients; }
    public void setIngredients(List<RecommendIngredientRequest> ingredients) { this.ingredients = ingredients; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public Integer getMaxCookMinutes() { return maxCookMinutes; }
    public void setMaxCookMinutes(Integer maxCookMinutes) { this.maxCookMinutes = maxCookMinutes; }

    @JsonAnySetter
    public void rejectUnknown(String field, Object ignored) {
        throw new IllegalArgumentException("不支持请求字段: " + field);
    }
}
