package com.recipehub.community;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.recipe.Recipe;
import com.recipehub.recipe.RecipeQueryService;
import com.recipehub.recipe.RecipeRepository;
import com.recipehub.recipe.RecipeStatus;
import com.recipehub.user.AppUserDetails;
import com.recipehub.user.User;
import com.recipehub.user.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock FavoriteRepository favorites;
    @Mock RecipeRepository recipes;
    @Mock UserRepository users;
    @Mock RecipeQueryService query;
    FavoriteService service;

    final User cook = new User("a@x.com", "cook", "HASH", "Cook");
    final User member = new User("m@x.com", "member", "HASH", "Member");
    final Recipe recipe = new Recipe(cook, "Cà chua xào trứng", "ca-chua-xao-trung");
    final AppUserDetails actor = new AppUserDetails(7L, "m@x.com", "HASH", true, false, Set.of());

    @BeforeEach
    void setUp() {
        service = new FavoriteService(favorites, recipes, users, query);
        ReflectionTestUtils.setField(cook, "id", 1L);
        ReflectionTestUtils.setField(member, "id", 7L);
        ReflectionTestUtils.setField(recipe, "id", 100L);
        ReflectionTestUtils.setField(recipe, "status", RecipeStatus.PUBLISHED);
    }

    @Test
    void addTwiceIsIdempotent() {
        when(recipes.findById(100L)).thenReturn(Optional.of(recipe));
        when(users.getReferenceById(7L)).thenReturn(member);
        when(favorites.existsById(new FavoriteId(7L, 100L))).thenReturn(false, true);
        service.add(actor, 100L);
        service.add(actor, 100L);
        verify(favorites, times(1)).save(any());
    }

    @Test
    void cannotFavoriteUnpublished() {
        ReflectionTestUtils.setField(recipe, "status", RecipeStatus.PENDING);
        when(recipes.findById(100L)).thenReturn(Optional.of(recipe));
        assertThatThrownBy(() -> service.add(actor, 100L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(favorites, never()).save(any());
    }

    @Test
    void removeDeletesById() {
        service.remove(actor, 100L);
        verify(favorites).deleteById(new FavoriteId(7L, 100L));
    }
}
