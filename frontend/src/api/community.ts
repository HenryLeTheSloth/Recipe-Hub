import { api } from './http'
import type { Page, RecipeCard } from './recipes'

export interface Author { id: number; username: string; displayName: string; avatarUrl: string | null }
export interface Rating { id: number; stars: number; review: string | null; author: Author; hidden: boolean; updatedAt: string }
export interface Comment { id: number; content: string; author: Author; hidden: boolean; createdAt: string; replies: Comment[] }

export const listRatings = (recipeId: number, page = 0) => api<Page<Rating>>(`/recipes/${recipeId}/ratings?page=${page}&size=20`)
export const putMyRating = (recipeId: number, stars: number, review: string) =>
  api<Rating>(`/recipes/${recipeId}/my-rating`, { method: 'PUT', json: { stars, review } })
export const deleteMyRating = (recipeId: number) => api<void>(`/recipes/${recipeId}/my-rating`, { method: 'DELETE' })
export const hideRating = (id: number, hidden: boolean) => api<Rating>(`/admin/ratings/${id}`, { method: 'PATCH', json: { hidden } })

export const listComments = (recipeId: number) => api<Comment[]>(`/recipes/${recipeId}/comments`)
export const postComment = (recipeId: number, content: string, parentId: number | null) =>
  api<Comment>(`/recipes/${recipeId}/comments`, { method: 'POST', json: { content, parentId } })
export const deleteComment = (id: number) => api<void>(`/comments/${id}`, { method: 'DELETE' })
export const hideComment = (id: number, hidden: boolean) => api<Comment>(`/admin/comments/${id}`, { method: 'PATCH', json: { hidden } })

export const addFavorite = (recipeId: number) => api<void>(`/recipes/${recipeId}/favorite`, { method: 'PUT' })
export const removeFavorite = (recipeId: number) => api<void>(`/recipes/${recipeId}/favorite`, { method: 'DELETE' })
export const listFavorites = (page: number) => api<Page<RecipeCard>>(`/me/favorites?page=${page}&size=12`)
