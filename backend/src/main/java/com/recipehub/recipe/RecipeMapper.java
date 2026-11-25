package com.recipehub.recipe;

import com.recipehub.catalog.Category;
import com.recipehub.catalog.Tag;
import com.recipehub.catalog.dto.CategoryResponse;
import com.recipehub.catalog.dto.TagResponse;
import com.recipehub.media.MediaService;
import com.recipehub.recipe.dto.RecipeCard;
import com.recipehub.recipe.dto.RecipeDetail;
import com.recipehub.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity → DTO; gọi trong transaction vì các quan hệ đều LAZY. */
@Mapper(componentModel = "spring", imports = MediaService.class)
public interface RecipeMapper {

    @Mapping(target = "coverUrl", expression = "java(MediaService.url(r.getCover()))")
    @Mapping(target = "coverMediaId", source = "cover.id")
    RecipeDetail detail(Recipe r);

    @Mapping(target = "coverUrl", expression = "java(MediaService.url(r.getCover()))")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "totalMinutes", expression = "java(r.getPrepMinutes() + r.getCookMinutes())")
    @Mapping(target = "authorName", source = "author.displayName")
    RecipeCard card(Recipe r);

    @Mapping(target = "avatarUrl", expression = "java(MediaService.url(u.getAvatar()))")
    RecipeDetail.AuthorView author(User u);

    @Mapping(target = "ingredientId", source = "ingredient.id")
    @Mapping(target = "name", source = "ingredient.name")
    @Mapping(target = "emoji", source = "ingredient.emoji")
    RecipeDetail.IngredientLine line(RecipeIngredient i);

    @Mapping(target = "imageUrl", expression = "java(MediaService.url(s.getMedia()))")
    @Mapping(target = "mediaId", source = "media.id")
    RecipeDetail.StepView step(RecipeStep s);

    CategoryResponse category(Category c);

    TagResponse tag(Tag t);
}
