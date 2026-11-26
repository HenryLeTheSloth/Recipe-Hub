---
version: 1
slug: "frontend-src-pages-homepage-vue"
primary_target: "frontend/src/pages/HomePage.vue"
related_targets: ["frontend/src/layouts/PublicLayout.vue","frontend/src/pages/RecipeDetailPage.vue","frontend/src/pages/RecipeListPage.vue","frontend/src/styles/base.css"]
---

# Recipe Hub frontend (public pages, admin restyled)

Scope: whole Vue frontend. Home is Operate (pick ingredients, get dishes); recipe detail is Read (follow steps mid-cooking); editor, my recipes, auth are Operate; /admin keeps PrimeVue, restyled with this palette and type.
Audience: Vietnamese home cooks on a phone in the kitchen or at the market, daylight or bright kitchen light, hence a light paper world. Avoid: childish/cartoon, cluttered or hard to use while cooking.

## Direction contract

THESIS: Recipe Hub is the tear-off wall calendar (lịch bloc) every Vietnamese kitchen has: one sheet per day, one giant red numeral. Here the numeral is how many dishes you can cook with what you have. It refuses the category default of a title search box over a photo-card grid.

OWN-WORLD: Thin grey-white printing paper (#F4F3EE) stacked as sheets, ink black (#1D1A17), calendar red (#D2281D) for numerals, Sundays and the primary action, ink blue (#23408E) for links and secondary marks. Flat colour only, no gradients or soft glows; depth comes from stacked sheet edges and the red glued spine. Ruled cells like the giờ hoàng đạo box. Condensed tall display numerals (Big Shoulders Display) over a Vietnamese workhorse sans (Be Vietnam Pro). States are rubber stamps and line forms (dashed = pending, struck = hidden), never colour alone.

STORY: The cook sees today's sheet, taps what is in the kitchen, watches the numeral change, and opens the dishes. On a recipe, steps read like ruled lines and get stamped done one by one.

FIRST VIEWPORT: Mobile: red glued spine at top, then the sheet header with today's real solar and lunar date, the giant red count numeral (about 30% of viewport height, capped at 34svh so the first ingredient rows still show) with its one-line meaning bottom-aligned beside it (wrapping below when three digits fill the width), then the ruled ingredient table (group rows, tap cells), then a full-width red "Xem N món" action pinned at the sheet's foot. Desktop: numeral left, ruled table right, same sheet.

FORM: Lịch bloc xé, position 7 of the ordered list, seed key 1f7cf651. Signature interaction: when the count changes, the numeral flips like a sheet torn off (one orchestrated motion, reduced-motion respected).

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
