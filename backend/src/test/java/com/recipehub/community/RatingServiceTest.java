package com.recipehub.community;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.community.dto.RatingRequest;
import com.recipehub.community.dto.RatingView;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeMapper;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.recipe.RecipeStatus;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock RatingRepository ratings;
    @Mock RecipeRepository recipes;
    @Mock UserRepository users;
    RatingService service;

    final User author = withId(new User("a@x.com", "author", "HASH", "Tác giả"), 1L);
    final User rater = withId(new User("r@x.com", "rater", "HASH", "Người chấm"), 7L);
    final AppUserDetails raterActor = actor(7L);
    final Recipe recipe = withId(new Recipe(author, "Cà chua xào trứng", "ca-chua-xao-trung"), 100L);

    @BeforeEach
    void setUp() {
        service = new RatingService(ratings, recipes, users, Mappers.getMapper(RecipeMapper.class));
        ReflectionTestUtils.setField(recipe, "status", RecipeStatus.PUBLISHED);
        lenient().when(recipes.findById(100L)).thenReturn(Optional.of(recipe));
        lenient().when(users.getReferenceById(7L)).thenReturn(rater);
        lenient().when(ratings.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    static AppUserDetails actor(long id, String... perms) {
        return new AppUserDetails(id, "u" + id + "@x.com", "HASH", true, false, Set.of(perms));
    }

    @Test
    void authorCannotRateOwnRecipe() {
        assertThatThrownBy(() -> service.upsert(actor(1L), 100L, new RatingRequest(5, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(ratings, never()).save(any());
    }

    @Test
    void cannotRateUnpublished() {
        ReflectionTestUtils.setField(recipe, "status", RecipeStatus.HIDDEN);
        assertThatThrownBy(() -> service.upsert(raterActor, 100L, new RatingRequest(5, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void firstRatingCreates() {
        RatingView v = service.upsert(raterActor, 100L, new RatingRequest(4, "Ngon"));
        assertThat(v.stars()).isEqualTo(4);
        assertThat(v.author().username()).isEqualTo("rater");
        assertThat(v.hidden()).isFalse();
    }

    @Test
    void secondRatingUpdatesExisting() {
        Rating old = withId(new Rating(rater, recipe, 2, "Tạm"), 55L);
        when(ratings.findByUser_IdAndRecipe_Id(7L, 100L)).thenReturn(Optional.of(old));
        RatingView v = service.upsert(raterActor, 100L, new RatingRequest(5, "Tuyệt"));
        var saved = ArgumentCaptor.forClass(Rating.class);
        verify(ratings).save(saved.capture());
        assertThat(saved.getValue()).isSameAs(old);
        assertThat(saved.getValue().getId()).isEqualTo(55L);
        assertThat(v.stars()).isEqualTo(5);
        assertThat(v.review()).isEqualTo("Tuyệt");
    }

    @Test
    void hiddenRatingsOnlyForModerators() {
        Rating r = withId(new Rating(rater, recipe, 3, null), 55L);
        when(ratings.findByRecipe_IdAndHiddenFalse(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(ratings.findByRecipe_Id(any(), any())).thenReturn(new PageImpl<>(List.of(r)));
        assertThat(service.list(100L, null, PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(service.list(100L, actor(9L, "CONTENT_MODERATE"), PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test
    void listOfUnpublishedIs404() {
        ReflectionTestUtils.setField(recipe, "status", RecipeStatus.DRAFT);
        assertThatThrownBy(() -> service.list(100L, actor(9L, "CONTENT_MODERATE"), PageRequest.of(0, 10)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void moderatorCanHide() {
        Rating r = withId(new Rating(rater, recipe, 3, null), 55L);
        when(ratings.findById(55L)).thenReturn(Optional.of(r));
        assertThat(service.setHidden(55L, true).hidden()).isTrue();
    }
}
