package ai.xiaodudou.module.action.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.module.action.dto.CheckinRequest;
import ai.xiaodudou.module.action.dto.CheckinResponse;
import ai.xiaodudou.module.action.entity.UserRecipeAction;
import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserActionServiceTest {
    private UserRecipeActionMapper actionMapper;
    private RecipeMapper recipeMapper;
    private UserActionService service;

    @BeforeEach
    void setUp() {
        actionMapper = mock(UserRecipeActionMapper.class);
        recipeMapper = mock(RecipeMapper.class);
        service = new UserActionService(actionMapper, recipeMapper);
    }

    @Test
    void firstFavoriteUsesStrictInsertAndReturnsCreated() {
        when(recipeMapper.selectActiveById(9L)).thenReturn(activeRecipe(9L));
        when(actionMapper.insertAction(anyLong(), eq(3L), eq(9L), eq("favorite"),
                eq(null), eq(null), eq(null), eq("favorite:9"), any())).thenReturn(1);

        assertThat(service.addFavorite(3L, 9L)).isTrue();
        verify(actionMapper, never()).countOwnFavorite(anyLong(), anyLong());
    }

    @Test
    void realDuplicateReturnsAlreadyExistsOnlyAfterExactOwnedKeyIsVerified() {
        when(recipeMapper.selectActiveById(9L)).thenReturn(activeRecipe(9L));
        when(actionMapper.insertAction(anyLong(), eq(3L), eq(9L), eq("favorite"),
                eq(null), eq(null), eq(null), eq("favorite:9"), any()))
                .thenThrow(new DuplicateKeyException("duplicate"));
        UserRecipeAction existing = new UserRecipeAction();
        existing.setUserId(3L);
        existing.setRecipeId(9L);
        existing.setAction("favorite");
        existing.setIdempotencyKey("favorite:9");
        when(actionMapper.selectOwnByIdempotency(3L, "favorite", "favorite:9")).thenReturn(existing);

        assertThat(service.addFavorite(3L, 9L)).isFalse();
        verify(actionMapper).selectOwnByIdempotency(3L, "favorite", "favorite:9");
    }

    @Test
    void duplicateOnAnotherConstraintMustNotBeHiddenWhenOwnedKeyDoesNotExist() {
        when(recipeMapper.selectActiveById(9L)).thenReturn(activeRecipe(9L));
        DuplicateKeyException duplicate = new DuplicateKeyException("different unique constraint");
        when(actionMapper.insertAction(anyLong(), eq(3L), eq(9L), eq("favorite"),
                eq(null), eq(null), eq(null), eq("favorite:9"), any())).thenThrow(duplicate);
        when(actionMapper.selectOwnByIdempotency(3L, "favorite", "favorite:9")).thenReturn(null);

        assertThatThrownBy(() -> service.addFavorite(3L, 9L)).isSameAs(duplicate);
    }

    @Test
    void zeroAffectedRowsWithoutDuplicateMustFailExplicitly() {
        when(recipeMapper.selectActiveById(9L)).thenReturn(activeRecipe(9L));
        when(actionMapper.insertAction(anyLong(), eq(3L), eq(9L), eq("favorite"),
                eq(null), eq(null), eq(null), eq("favorite:9"), any())).thenReturn(0);

        assertThatThrownBy(() -> service.addFavorite(3L, 9L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写入状态异常");
        verify(actionMapper, never()).selectOwnByIdempotency(3L, "favorite", "favorite:9");
    }

    @Test
    void nonDuplicateDatabaseFailureMustPropagateWithoutIdempotencyLookup() {
        when(recipeMapper.selectActiveById(9L)).thenReturn(activeRecipe(9L));
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
        when(actionMapper.insertAction(anyLong(), eq(3L), eq(9L), eq("favorite"),
                eq(null), eq(null), eq(null), eq("favorite:9"), any())).thenThrow(failure);

        assertThatThrownBy(() -> service.addFavorite(3L, 9L)).isSameAs(failure);
        verify(actionMapper, never()).selectOwnByIdempotency(3L, "favorite", "favorite:9");
    }

    @Test
    void inactiveOrMissingRecipeCannotBeFavoritedOrCheckedIn() {
        when(recipeMapper.selectActiveById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.addFavorite(3L, 9L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("下架");
        assertThatThrownBy(() -> service.checkin(3L, validRequest(LocalDate.now())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("下架");
        verify(actionMapper, never()).insertIgnoreAction(anyLong(), anyLong(), anyLong(), any(),
                any(), any(), any(), any(), any());
        verify(actionMapper, never()).insertAction(anyLong(), anyLong(), anyLong(), any(),
                any(), any(), any(), any(), any());
    }

    @Test
    void duplicateCheckinReturnsExistingRecordAndDeterministicBusinessKey() {
        LocalDate date = LocalDate.now().minusDays(1);
        when(recipeMapper.selectActiveById(9L)).thenReturn(activeRecipe(9L));
        when(actionMapper.insertIgnoreAction(anyLong(), eq(3L), eq(9L), eq("cook"), eq(date),
                eq("lunch"), eq(new BigDecimal("1.25")), eq("cook:" + date + ":lunch:9"), any()))
                .thenReturn(0);
        UserRecipeAction existing = new UserRecipeAction();
        existing.setId(88L);
        when(actionMapper.selectOwnByIdempotency(3L, "cook", "cook:" + date + ":lunch:9"))
                .thenReturn(existing);

        CheckinResponse response = service.checkin(3L, validRequest(date));

        assertThat(response.actionId()).isEqualTo(88L);
        assertThat(response.created()).isFalse();
        assertThat(response.alreadyExists()).isTrue();
    }

    @Test
    void checkinDateAllowsTodayAndSevenDaysButRejectsFutureAndOlder() {
        service.validateActionDate(LocalDate.now());
        service.validateActionDate(LocalDate.now().minusDays(7));
        assertThatThrownBy(() -> service.validateActionDate(LocalDate.now().plusDays(1)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("未来");
        assertThatThrownBy(() -> service.validateActionDate(LocalDate.now().minusDays(8)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("7 天");
    }

    @Test
    void requestValidatesMealRecipeAndServingBoundaries() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(new CheckinRequest(null, "brunch", new BigDecimal("0.24"), null)))
                .hasSize(3);
        assertThat(validator.validate(new CheckinRequest(1L, "breakfast", new BigDecimal("10.001"), null)))
                .isNotEmpty();
        assertThat(validator.validate(new CheckinRequest(1L, "snack", new BigDecimal("0.25"), null)))
                .isEmpty();
    }

    @Test
    void deleteIsAlwaysScopedToCurrentUserAndCannotDeleteOthersRecord() {
        when(actionMapper.deleteOwnCook(3L, 88L)).thenReturn(0);

        assertThatThrownBy(() -> service.deleteCheckin(3L, 88L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不存在");
        verify(actionMapper).deleteOwnCook(3L, 88L);
    }

    @Test
    void invalidMonthDoesNotSilentlyFallBackToCurrentMonth() {
        assertThatThrownBy(() -> service.parseMonth("2026-13"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM");
        assertThatThrownBy(() -> service.parseMonth(" "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM");
        assertThatThrownBy(() -> service.parseMonth("1999-12"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("范围");
    }

    private CheckinRequest validRequest(LocalDate date) {
        return new CheckinRequest(9L, "lunch", new BigDecimal("1.25"), date);
    }

    private Recipe activeRecipe(Long id) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setStatus(1);
        recipe.setDeleted(0);
        return recipe;
    }
}
