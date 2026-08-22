package ai.xiaodudou.module.action.service;

import ai.xiaodudou.common.dto.PageResponse;
import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.action.dto.CalendarResponse;
import ai.xiaodudou.module.action.dto.CheckinItemResponse;
import ai.xiaodudou.module.action.dto.CheckinRequest;
import ai.xiaodudou.module.action.dto.CheckinResponse;
import ai.xiaodudou.module.action.entity.UserRecipeAction;
import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.recipe.dto.RecipeResponse;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserActionService {
    private final UserRecipeActionMapper actionMapper;
    private final RecipeMapper recipeMapper;

    @Transactional
    public boolean addFavorite(Long userId, Long recipeId) {
        requireActiveRecipe(recipeId);
        String key = "favorite:" + recipeId;
        try {
            int inserted = actionMapper.insertAction(IdWorker.getId(), userId, recipeId,
                    UserRecipeAction.FAVORITE, null, null, null, key, LocalDateTime.now());
            if (inserted != 1) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "收藏写入状态异常");
            }
            return true;
        } catch (DuplicateKeyException duplicate) {
            UserRecipeAction existing = actionMapper.selectOwnByIdempotency(
                    userId, UserRecipeAction.FAVORITE, key);
            if (existing != null
                    && userId.equals(existing.getUserId())
                    && UserRecipeAction.FAVORITE.equals(existing.getAction())
                    && key.equals(existing.getIdempotencyKey())
                    && recipeId.equals(existing.getRecipeId())) {
                return false;
            }
            // 可能是主键等其他唯一约束冲突，不能伪装成“已收藏”。
            throw duplicate;
        }
    }

    public void removeFavorite(Long userId, Long recipeId) {
        actionMapper.deleteOwnFavorite(userId, recipeId);
    }

    public boolean isFavorited(Long userId, Long recipeId) {
        return actionMapper.countOwnFavorite(userId, recipeId) > 0;
    }

    public PageResponse<RecipeResponse> favorites(Long userId, int page, int size) {
        Page<Recipe> result = actionMapper.selectOwnFavoriteRecipes(new Page<>(page, size), userId);
        return PageResponse.of(result.getRecords().stream().map(RecipeResponse::from).toList(),
                result.getTotal(), page, size);
    }

    @Transactional
    public CheckinResponse checkin(Long userId, CheckinRequest request) {
        requireActiveRecipe(request.recipeId());
        LocalDate date = request.actionDate() == null ? LocalDate.now() : request.actionDate();
        validateActionDate(date);
        String key = "cook:" + date + ":" + request.mealType() + ":" + request.recipeId();
        Long candidateId = IdWorker.getId();
        int inserted = actionMapper.insertIgnoreAction(candidateId, userId, request.recipeId(),
                UserRecipeAction.COOK, date, request.mealType(), request.servings(), key, LocalDateTime.now());
        if (inserted == 1) return new CheckinResponse(candidateId, true, false);
        UserRecipeAction existing = actionMapper.selectOwnByIdempotency(userId, UserRecipeAction.COOK, key);
        if (existing == null) throw new BusinessException(ResultCode.SERVER_ERROR, "打卡幂等状态异常");
        return new CheckinResponse(existing.getId(), false, true);
    }

    public List<CheckinItemResponse> today(Long userId) {
        List<UserRecipeAction> actions = actionMapper.selectOwnCookByDate(userId, LocalDate.now());
        if (actions.isEmpty()) return List.of();
        List<Long> ids = actions.stream().map(UserRecipeAction::getRecipeId).distinct().toList();
        Map<Long, Recipe> recipes = new HashMap<>();
        recipeMapper.selectBatchIds(ids).forEach(recipe -> recipes.put(recipe.getId(), recipe));
        List<CheckinItemResponse> response = new ArrayList<>();
        for (UserRecipeAction action : actions) {
            Recipe recipe = recipes.get(action.getRecipeId());
            response.add(new CheckinItemResponse(action.getId(), action.getRecipeId(),
                    recipe == null ? "已下架菜谱" : recipe.getTitle(), recipe == null ? null : recipe.getCoverUrl(),
                    action.getMealType(), action.getServings(), action.getActionDate(), action.getCreatedAt()));
        }
        return response;
    }

    public boolean deleteCheckin(Long userId, Long actionId) {
        if (actionMapper.deleteOwnCook(userId, actionId) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "打卡记录不存在");
        }
        return true;
    }

    public CalendarResponse calendar(Long userId, String month) {
        YearMonth yearMonth = parseMonth(month);
        List<Map<String, Object>> rows = actionMapper.aggregateCookByDay(
                userId, yearMonth.atDay(1), yearMonth.plusMonths(1).atDay(1));
        Map<String, Integer> dayCount = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            Object count = row.get("count");
            if (day != null && count instanceof Number number) dayCount.put(day.toString(), number.intValue());
        }
        int total = dayCount.values().stream().mapToInt(Integer::intValue).sum();
        return new CalendarResponse(yearMonth.toString(), yearMonth.lengthOfMonth(), dayCount.size(), total, dayCount);
    }

    YearMonth parseMonth(String month) {
        if (month == null) return YearMonth.now();
        if (month.isBlank()) bad("month 必须是 yyyy-MM");
        try {
            YearMonth value = YearMonth.parse(month);
            if (value.isBefore(YearMonth.of(2000, 1)) || value.isAfter(YearMonth.now())) {
                bad("month 超出允许范围");
            }
            return value;
        } catch (DateTimeParseException e) {
            bad("month 必须是 yyyy-MM");
            return null;
        }
    }

    void validateActionDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) bad("actionDate 不能是未来日期");
        if (date.isBefore(today.minusDays(7))) bad("actionDate 最多补录过去 7 天");
    }

    private Recipe requireActiveRecipe(Long recipeId) {
        Recipe recipe = recipeMapper.selectActiveById(recipeId);
        if (recipe == null) throw new BusinessException(ResultCode.RECIPE_NOT_FOUND, "菜谱不存在或已下架");
        return recipe;
    }

    private void bad(String message) {
        throw new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
