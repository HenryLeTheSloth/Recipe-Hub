---
name: Recipe Hub
description: "Bếp xanh": a cream kitchen counter with white recipe cards, forest green as the brand and young-lime as the one bright fill, built around picking what is in the kitchen and seeing what you can cook.
colors:
  bg: "#f7f4ec"
  surface: "#ffffff"
  accent: "#1e4d3a"
  accent-deep: "#143528"
  accent-text: "#ffffff"
  lime: "#c9e86b"
  lime-soft: "#eef6d3"
  sage: "#e6efe6"
  text: "#1c2420"
  muted: "#5b635e"
  border: "#e3ded2"
  field: "#767c75"
  star: "#a35908"
  danger: "#b3261e"
  ok: "#1e6b3a"
typography:
  headline:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "clamp(1.75rem, 4vw, 2.5rem)"
    fontWeight: 800
    lineHeight: 1.15
    letterSpacing: "-0.02em"
  headline-recipe:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "clamp(2rem, 5vw, 3rem)"
    fontWeight: 800
    lineHeight: 1.1
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 800
    lineHeight: 1.2
  figure:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 800
    lineHeight: 1
    fontFeature: "tnum"
  card-title:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "1.1rem"
    fontWeight: 700
    lineHeight: 1.3
  body:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.55
  body-reading:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "1.1rem"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "0.85rem"
    fontWeight: 600
    lineHeight: 1.4
rounded:
  control: "8px"
  card: "16px"
  hero: "24px"
  pill: "999px"
elevation:
  card: "0 1px 2px rgb(20 53 40 / 0.06), 0 2px 8px rgb(20 53 40 / 0.06)"
  lift: "0 8px 24px rgb(20 53 40 / 0.12)"
components:
  button:
    backgroundColor: "transparent"
    textColor: "{colors.accent}"
    borderColor: "{colors.accent}"
    rounded: "{rounded.pill}"
    height: "44px"
  button-hover:
    backgroundColor: "{colors.sage}"
    textColor: "{colors.accent-deep}"
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.accent-text}"
    rounded: "{rounded.pill}"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.accent-deep}"
  button-lime:
    backgroundColor: "{colors.lime}"
    textColor: "{colors.accent-deep}"
    rounded: "{rounded.pill}"
    height: "48px"
  button-lime-hover:
    backgroundColor: "#b8dc4f"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    borderColor: "{colors.field}"
    rounded: "{rounded.control}"
    height: "44px"
  hero:
    backgroundColor: "{colors.accent}"
    textColor: "#ffffff"
    rounded: "{rounded.hero}"
    padding: "2.5rem"
  picked-bar:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.pill}"
  group-tab:
    backgroundColor: "transparent"
    textColor: "#ffffff"
    borderColor: "rgb(255 255 255 / 0.35)"
    rounded: "{rounded.pill}"
    height: "44px"
  group-tab-active:
    backgroundColor: "#ffffff"
    textColor: "{colors.accent-deep}"
  chip:
    backgroundColor: "rgb(255 255 255 / 0.12)"
    textColor: "#ffffff"
    rounded: "{rounded.pill}"
    height: "44px"
  chip-pressed:
    backgroundColor: "{colors.lime}"
    textColor: "{colors.accent-deep}"
  recipe-card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.card}"
    elevation: "{elevation.card}"
  badge-difficulty:
    backgroundColor: "{colors.lime-soft}"
    textColor: "{colors.accent-deep}"
    rounded: "{rounded.pill}"
  badge-category:
    backgroundColor: "{colors.sage}"
    textColor: "{colors.accent-deep}"
    rounded: "{rounded.pill}"
  time-pill:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.accent-deep}"
    rounded: "{rounded.pill}"
  stamp:
    textColor: "{colors.text}"
    rounded: "{rounded.control}"
  stamp-draft:
    textColor: "{colors.muted}"
  stamp-pending:
    textColor: "{colors.star}"
  stamp-published:
    textColor: "{colors.ok}"
  stamp-rejected:
    textColor: "{colors.danger}"
  stamp-hidden:
    textColor: "{colors.muted}"
  nav-link-active:
    textColor: "{colors.accent}"
---

# Design System: Recipe Hub

## Overview

**Creative North Star: "Bếp xanh"**

Recipe Hub is a clean cream kitchen counter with white recipe cards laid out on it. Forest green is the brand: the header mark, the home hero, the primary actions. One young-lime fill marks the single thing to press next and the ingredients you have picked. Everything else is quiet. Cream is the background, white is the cards, and dark green-black is the text.

The home page is built around one job: pick what is in the kitchen, then see what you can cook. A compact green hero holds the whole picker. It has the picked tags in a white bar, the lime "Xem N món" action, ingredient groups as tabs and the ingredients as chips. Results follow straight after as a grid of photo cards. The page does not show a giant count. The number of matches is plain text inside the heading and the button.

Depth is soft and green-tinted: cards sit on a two-layer shadow and lift 2px on hover. Shapes are rounded on a three-step scale: 8px for controls, 16px for cards, 24px for the hero, and pills for anything tappable that is not a field. The admin area keeps PrimeVue (Aura), re-tinted with the same green and warm-neutral ramps in `main.ts`, so it reads as the same product.

**Key characteristics:**
- Cream background, white rounded cards, a forest green brand colour, lime used only as a fill.
- One typeface, Be Vietnam Pro, at weights 400 to 800. Hierarchy comes from weight and size.
- Photo-first cards at 4:3, with a time pill, a 2-line title, an author line, badges and a rating row.
- Pills for buttons, chips, tabs and badges. Fields keep an 8px corner so they read as fields.
- Soft green-tinted shadows. No gradients and no glows.
- Status is a stamp whose border style carries the meaning, so it reads without colour.

## Colors

### Primary
- **Forest** (`accent` #1e4d3a): the brand. It is used for the header brand, the home hero fill, primary buttons, outline button strokes and text, links, the active nav link, step numbers, fact figures and the focus ring. 9.64:1 with white, so white text on it is always legal.
- **Deep forest** (`accent-deep` #143528): hover for primary actions, and text on lime, lime-soft and sage fills (9.71:1 on lime).

### Secondary
- **Young lime** (`lime` #c9e86b): a fill only, never text. Used for the one "go" action (`.btn.lime`), pressed ingredient chips, and count badges on group tabs. Always paired with deep-forest text.
- **Lime soft** (`lime-soft` #eef6d3): picked-ingredient tags in the bar, the difficulty badge, and the pressed favourite button.
- **Sage** (`sage` #e6efe6): category badges, recipe fact tiles, "Cách dùng" tiles, the no-photo card face and button hover.

### Status
- **Star amber** (`star` #a35908): rating stars and the pending stamp (5.26:1 on white).
- **Danger** (`danger` #b3261e): field errors, destructive actions and the rejected stamp (6.54:1 on white).
- **Ok** (`ok` #1e6b3a): the published stamp, the "Xong" step stamp and success messages (6.52:1 on white).

### Neutral
- **Counter cream** (`bg` #f7f4ec): the page background.
- **White** (`surface` #ffffff): cards, the header bar, toolbars, fields and the picked bar.
- **Text** (`text` #1c2420): all body text (15.88:1 on white, 14.45:1 on cream).
- **Muted** (`muted` #5b635e): author lines, counts next to headings, hints and placeholders (6.19:1 on white, 5.63:1 on cream).
- **Border** (`border` #e3ded2): 1px dividers and card or toolbar edges. It separates surfaces and never carries meaning.
- **Field** (`field` #767c75): the 1px stroke on inputs and selects (4.28:1 on white), so controls read as controls.
- On the green hero, secondary text uses #dfe9e2 (7.76:1).

### Named rules
**The One Lime Rule.** Each view gets at most one lime action. Lime also marks what is selected (pressed chips, picked tags). It is never used for text or decoration.

**The Green Carries the Brand Rule.** Forest is the only saturated brand hue. Amber, danger and ok exist for status only. Do not add a new hue for decoration.

## Typography

**Font:** Be Vietnam Pro, weights 400/500/600/700/800 (falls back to system-ui, sans-serif). It loads from Google Fonts with `display=swap` and has full Vietnamese diacritics. `--font-display` is kept as an alias of `--font-body` so older components resolve to the same face.

**Character:** A sturdy, friendly Vietnamese sans. Headlines are tight (800 weight, -0.02em tracking) and the reading text stays legible at arm's length in kitchen light.

### Hierarchy
- **Headline** (800, clamp(1.75rem, 4vw, 2.5rem), -0.02em): page h1 ("Trong bếp có gì?", "Công thức"). Balanced wrapping.
- **Headline, recipe** (800, clamp(2rem, 5vw, 3rem)): the dish title on recipe detail.
- **Title** (800, 1.5rem): section heads (Nguyên liệu, Cách làm, Đánh giá, Bình luận, the result heading on home).
- **Figure** (800, 1.75rem, forest, tabular): recipe fact numbers. Step numbers are 800-weight white numbers in a 2.25rem forest circle.
- **Card title** (700, 1.1rem, clamped to 2 lines).
- **Body** (400, 1rem, 1.55): default text.
- **Body, reading** (400, 1.1rem, 1.6, pre-line): recipe steps, which are read mid-cooking.
- **Label** (600, 0.85 to 0.95rem): badges, pills, form labels and stamps. Sentence case.

### Named rules
**The Sentence Case Rule.** No all-caps labels and no letter-spaced eyebrows above headings.

**The Quiet Number Rule.** Counts are information, not decoration. They go in running text ("12 món nấu được với…"), in a button ("Xem 12 món") or in a small badge. Never use a display-size numeral.

## Layout

Public pages sit in a centred 1200px column with 1rem gutters (1.5rem from 860px). The header is a full-width white bar with a 1px bottom border, and its content is aligned to the same column. On mobile the nav scrolls sideways under the brand and search.

**Home, top to bottom:**
1. Green hero (edge to edge on mobile, 24px corners and 2.5rem padding from 720px). It holds the h1, a one-line lede and the picked bar. The bar is a white pill on desktop and a 16px card on mobile; the "Xem N món" lime button goes full width below 520px. Below the bar come the group tabs and the chips of the active group.
2. Results: an `aria-live` h2 with the count in words, a "Xem tất cả" link, and the card grid (first 8 matches, or the newest when nothing is picked).
3. "Cách dùng": three sage tiles with numbered steps. This is a real sequence, so the numbers are earned.
4. Category pills.

On mobile, the group tabs scroll sideways. The chips form a 2-row grid that scrolls sideways with scroll-snap, so the picker never pushes results far down.

**Other pages.** The recipe list puts its filters in a white 16px toolbar (desktop grid `9rem minmax(0,2fr) repeat(3, minmax(0,1fr)) auto`) above the card grid. Recipe detail puts ingredients and steps in a 2fr / 3fr grid from 820px. Recipe facts are sage tiles in 2 columns, then 4 from 640px. Card grids use `repeat(auto-fill, minmax(min(100%, 16rem), 1fr))` with 1.5rem / 1.25rem gaps.

Home sections are 3rem apart. Breakpoints in use: 520, 640, 720, 820, 860px.

## Elevation & Depth

Surfaces separate by colour first (white on cream) and by a soft, green-tinted shadow second.

- **Card** (`--shadow-card`: `0 1px 2px rgb(20 53 40 / 0.06), 0 2px 8px rgb(20 53 40 / 0.06)`): recipe cards, sheets, the auth card.
- **Lift** (`--shadow-lift`: `0 8px 24px rgb(20 53 40 / 0.12)`): a card on hover, together with a 2px translate. There is no translate under reduced motion.

The green hero needs no shadow; its fill is its elevation.

## Shapes

Three radii plus a pill:
- **8px** (`--radius`): inputs, selects, textareas and stamps.
- **16px** (`--radius-lg`): cards, sheets, toolbars, photos on detail, tiles, and the picked bar on mobile.
- **24px** (`--radius-xl`): the home hero from 720px.
- **Pill** (999px): every button, chip, tab, badge, tag, the time pill and the header search.

## Components

### Buttons
- **Shape:** pill, min-height 44px, 600 weight.
- **Default (outline):** 1px forest stroke and forest text; hover fills sage with deep-forest text.
- **Primary:** forest fill and white text; hover deep-forest.
- **Lime:** lime fill, deep-forest 700 text, 48px tall on home; hover #b8dc4f. Used only for the one "go" action in a view. It is hidden when there is nothing to go to (count 0).
- **Link button:** forest underlined text, no box, still 44px tall; the danger variant uses `danger`.
- **Icon button:** 44 × 44px.
- **Disabled:** 0.55 opacity, no hover change.

### Hero and picker (signature)
- **Hero:** forest fill, white h1, #dfe9e2 lede; focus rings inside it are lime.
- **Picked bar:** white. Each picked ingredient is a lime-soft pill tag with "×" (36px), followed by a muted "Bỏ chọn" link. With nothing picked it shows the placeholder "Chưa chọn nguyên liệu nào".
- **Group tabs:** pill buttons with `aria-pressed` and `aria-controls`. Inactive tabs have white text and a 35%-white stroke; the active tab is white with deep-forest text. A lime badge shows how many items are picked in that group.
- **Chips:** 44px pills with a 12% white fill, showing the emoji and then the name. Pressed chips (`aria-pressed`) turn lime with deep-forest 700 text and a ✓.
- The selection lives in the URL, so Back keeps it.

### Recipe card
- White card, 16px corners, card shadow, and a lift on hover.
- **Media:** 4:3 with a sage background; the photo is `object-fit: cover`. With no photo, the brand bowl mark sits centred at 56px in forest at 50% opacity.
- **Time pill:** white, top-left over the photo, total minutes.
- **Body:** a 2-line title, the muted line "bởi {tác giả}", then badges (difficulty on lime-soft, category on sage).
- **Rating row:** a border-top divider, then "★ 4.8 (6 đánh giá)" with the star in amber.

### Inputs / Fields
- White field, 1px `field` stroke, 8px corners, 44px min-height, 1rem text.
- The header search is a pill on cream with a transparent stroke.
- **Focus:** 2px forest outline with a 2px offset.
- **Error:** `danger` text at 0.875rem and 500 weight, under the field.

### Navigation
- **Public:** text-coloured 600 links; hover and active links are forest. "Đăng ký" is a primary button.
- **Admin:** PrimeVue sidebar on the Kitchen preset (forest primary ramp, warm-neutral surface ramp), with the brand in forest.

### Stamps (status)
A 2px border in the text colour, 8px corners, 700 weight at 0.8rem, never wraps. The border style carries the state, so it reads without colour:
- **Bản nháp** (draft): dotted, muted.
- **Chờ duyệt** (pending): dashed, amber.
- **Đã đăng** (published): solid, ok green.
- **Bị từ chối** (rejected): solid, danger, rotated -2deg.
- **Đã ẩn** (hidden): solid, muted, struck through.
- **Xong** (step done): ok green with a check icon; the text of a done step turns muted.

## Do's and Don'ts

### Do:
- **Do** keep every text pair at 4.5:1 or better.
- **Do** make every public tap target at least 44px tall (48px for the home "go" button).
- **Do** keep lime to one action per view plus selected states, always with deep-forest text.
- **Do** put counts in words, buttons or badges.
- **Do** give every new state a stamp border style plus a word, never colour alone.
- **Do** make every surface complete without photos, as the bowl-mark card face does.
- **Do** respect `prefers-reduced-motion`: the global rule collapses transitions and the card lift drops its translate.

### Don't:
- **Don't** use all-caps or letter-spaced labels, or eyebrows above headings.
- **Don't** join meta with middle dots ("25 phút · Dễ · Món chính"); use badges and pills.
- **Don't** use "→" arrows in link or button text.
- **Don't** use lime for text, borders or decoration.
- **Don't** add gradients, glows or grey shadows; shadows are green-tinted and soft.
- **Don't** bring back a display-size count numeral.
- **Don't** add motion that ignores `prefers-reduced-motion`.
