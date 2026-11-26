# Recipe Hub

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary: Vietnamese home cooks making everyday family meals. They open the site on a phone, standing in the kitchen or at the market, holding a few ingredients and asking "what can I cook with these?". Sessions are short, one-handed, often with wet or busy hands.

Secondary: members (MEMBER role) who post their own family recipes, and moderators/admins (MODERATOR, ADMIN) who review submissions and maintain the ingredient catalog, mostly from a desktop.

Guests (Khách) can browse and search without an account.

## Product Purpose

A localhost recipe site for Vietnamese home cooking. Members publish recipes (title, cover, ingredients with quantity and unit, numbered steps with optional photos). Moderators approve, reject with a reason, or hide them. Success means a cook finds a dish they can make with what they already have, then follows it to the end without losing their place.

## Positioning

Search starts from the fridge, not from a dish name: the cook picks the ingredients on hand (each with its emoji, grouped by kind) and gets recipes that use all of them ("có gì nấu nấy"). A planned "Cái bát AI" page will extend the same idea. Generic recipe sites lead with a title search box and a photo grid; Recipe Hub leads with the ingredient pick.

## Operating Context

- Phone-first: kitchen counter, market stall, bright daylight or dim kitchen light.
- Reading a recipe happens mid-cooking: steps must stay legible at arm's length and the ingredient list must be quick to scan.
- Posting a recipe happens at leisure, often on desktop; the editor saves drafts and submits for moderation.
- Admin work (moderation queue, catalog, roles, users) happens on desktop.

## Capabilities and Constraints

- Vue 3 + Vite + TypeScript frontend, Spring Boot backend, Postgres. Runs on localhost.
- Public pages use hand-written CSS; `/admin/*` uses PrimeVue 5 (Aura preset), restyled to match, not replaced.
- Search filters: title text (accent-aware), category, tag, ingredients (recipe must contain all chosen ingredients), pagination.
- Recipe states: Bản nháp, Chờ duyệt, Đã đăng, Bị từ chối, Đã ẩn.
- Ingredient catalog has emoji and groups; members can propose new ingredients (pending review).
- Planned, not built: ratings, comments, favorites, "Cái bát AI".
- UI language is Vietnamese only.

## Brand Commitments

- Name: Recipe Hub. Keep it.
- The 🍲 emoji logo is not binding; the mark may be redrawn.
- Voice: plain, warm, everyday Vietnamese; no marketing hype.

## Evidence on Hand

- Recipe content and cover photos come from members via upload; the seed data has few or no photos, so every surface must look complete without images.
- Ingredient emoji from the catalog.
- No testimonials, statistics, or press exist; do not invent them.

## Product Principles

1. Start from what the cook has: the ingredient pick is the front door, not a filter hidden behind a dropdown.
2. Usable with one hand mid-cooking: big targets, legible steps, nothing that hides the current task.
3. Complete without photos: a recipe with no cover still looks deliberate.
4. Moderation is visible and fair: authors always see their recipe's status and the reason for a rejection.

## Accessibility & Inclusion

WCAG 2.1 AA contrast and keyboard access; touch targets at least 44px on public pages; respect reduced motion; Vietnamese diacritics must render correctly in every chosen typeface.
