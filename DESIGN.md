---
name: Recipe Hub
description: A tear-off kitchen wall calendar (lịch bloc) where the giant red numeral counts the dishes you can cook with what you have.
colors:
  wall: "#e4e3dc"
  paper: "#f4f3ee"
  ink: "#1d1a17"
  red: "#d2281d"
  red-deep: "#a81e15"
  blue: "#23408e"
  rule: "#d9d4c9"
  field: "#8c857a"
  muted: "#5f5951"
  ok: "#2e6b3a"
  accent-text: "#ffffff"
typography:
  display:
    fontFamily: "Big Shoulders Display, Be Vietnam Pro, sans-serif"
    fontSize: "min(34svh, calc(205cqi / var(--d, 3)), 26rem)"
    fontWeight: 900
    lineHeight: 0.78
    letterSpacing: "-0.03em"
    fontFeature: "tnum"
  headline:
    fontFamily: "Big Shoulders Display, Be Vietnam Pro, sans-serif"
    fontSize: "clamp(2.25rem, 6vw, 3.5rem)"
    fontWeight: 800
    lineHeight: 1.15
    letterSpacing: "-0.01em"
  headline-recipe:
    fontFamily: "Big Shoulders Display, Be Vietnam Pro, sans-serif"
    fontSize: "clamp(2.5rem, 8vw, 4.5rem)"
    fontWeight: 800
    lineHeight: 0.95
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Big Shoulders Display, Be Vietnam Pro, sans-serif"
    fontSize: "2rem"
    fontWeight: 800
    lineHeight: 1.15
    letterSpacing: "-0.01em"
  figure:
    fontFamily: "Big Shoulders Display, Be Vietnam Pro, sans-serif"
    fontSize: "2.75rem"
    fontWeight: 900
    lineHeight: 0.85
    fontFeature: "tnum"
  title-sans:
    fontFamily: "Be Vietnam Pro, system-ui, sans-serif"
    fontSize: "1.15rem"
    fontWeight: 700
    lineHeight: 1.15
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
    fontWeight: 700
    lineHeight: 1.4
rounded:
  radius: "4px"
  stamp: "3px"
components:
  button-primary:
    backgroundColor: "{colors.red}"
    textColor: "{colors.accent-text}"
    rounded: "{rounded.radius}"
    padding: "0.55rem 1.1rem"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.red-deep}"
    textColor: "{colors.accent-text}"
  button-tear-line:
    backgroundColor: "{colors.red}"
    textColor: "{colors.accent-text}"
    rounded: "{rounded.radius}"
    padding: "0.55rem 1.75rem"
    height: "52px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.radius}"
    padding: "0.55rem 1.1rem"
    height: "44px"
  button-outline-hover:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  link-button:
    textColor: "{colors.blue}"
    padding: "0.25rem 0"
    height: "44px"
  input:
    backgroundColor: "#ffffff"
    textColor: "{colors.ink}"
    rounded: "{rounded.radius}"
    padding: "0.55rem 0.75rem"
    height: "44px"
  sheet:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    padding: "1.25rem 1.25rem 1.5rem"
  sheet-wide:
    padding: "1.75rem 2rem 2rem"
  ingredient-cell:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.radius}"
    padding: "0.3rem 0.7rem"
    height: "44px"
  ingredient-cell-pressed:
    backgroundColor: "{colors.red}"
    textColor: "{colors.accent-text}"
  stamp:
    textColor: "{colors.ink}"
    rounded: "{rounded.stamp}"
    padding: "0.15rem 0.55rem"
  stamp-draft:
    textColor: "{colors.muted}"
  stamp-pending:
    textColor: "{colors.blue}"
  stamp-published:
    textColor: "{colors.ok}"
  stamp-rejected:
    textColor: "{colors.red}"
  stamp-hidden:
    textColor: "{colors.muted}"
  recipe-card:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    padding: "0.85rem 1rem 1rem"
  count-numeral:
    textColor: "{colors.red}"
    backgroundColor: "{colors.paper}"
    typography: "{typography.display}"
  nav-link-active:
    textColor: "{colors.red}"
---

# Design System: Recipe Hub

## Overview

**Creative North Star: "The Tờ Lịch Bloc"**

Recipe Hub is the tear-off wall calendar that hangs in every Vietnamese kitchen: one thin sheet of grey-white printing paper per day, a red glued spine across the top, and one giant red numeral that dominates the sheet. Here the numeral counts the dishes the cook can make with what is in the kitchen. Every public surface is a sheet on that pad; the page behind them is the kitchen wall.

The world is flat, printed and ruled. Colour is spot ink on paper: black for words, calendar red for numbers, Sundays and the one primary action, ink blue for links and secondary marks. Depth comes only from the sheets stacked underneath (hard paper edges, no blur) and from the red spine. Lists and facts sit in ruled cells like the giờ hoàng đạo box printed on a real lịch bloc. Status is a rubber stamp whose line form carries the meaning, so it reads without colour.

Density is generous and one-handed: big targets, legible steps at arm's length, a sticky action pinned at the sheet's torn foot. The one orchestrated motion is the tear: when the count changes, the old numeral is torn off the spine and the new one is already printed beneath. The admin area keeps PrimeVue (Aura) but is re-inked with the same red and paper ramps, so it reads as the same pad.

**Key Characteristics:**
- Paper sheets on a wall, red glued spine (10px) at the top of every sheet, dotted perforation just below it.
- Condensed, tall Big Shoulders Display for numerals and headlines; Be Vietnam Pro for every word you read.
- Numbers are red, words are ink.
- Ruled cells: a 2px ink rule opens a table, 1px warm rules divide it.
- Rubber-stamp states: dotted, dashed, solid, rotated, struck.
- Flat colour, no gradients, no blurred shadows; depth is stacked paper.
- One signature motion: the tear-off numeral, removed under reduced motion.

## Colors

Spot ink on thin grey-white paper: one red, one blue, one black, a green reserved for "done".

### Primary
- **Calendar Red** (red): the numerals, the Sunday date, the glued spine on every sheet and card, the pressed ingredient cell, the primary action and the active nav link. 4.65:1 on paper, so red text is legal at body size but it is spent on numbers and actions, not paragraphs.
- **Dried Seal Red** (red-deep): hover and pressed state of red actions, link hover, and the value behind `--danger` for field and form errors (6.61:1 on paper).

### Secondary
- **Ink Blue** (blue): links, the link-style button, the focus ring (2px outline, 2px offset) and the pending stamp (8.61:1 on paper).

### Tertiary
- **Ledger Green** (ok): only the published stamp, the "Xong" done stamp on recipe steps and success messages (5.76:1 on paper). It never decorates.

### Neutral
- **Printing Paper** (paper): every sheet, card, sticky tear-line and the top search field; also the fill behind the count numeral so a leaving sheet hides the new one.
- **Kitchen Wall** (wall): the page background behind the sheets (`--bg`), and the scrollbar track.
- **Lampblack Ink** (ink): all text (15.59:1 on paper), the 2px rule that opens a ruled table, the outline button stroke.
- **Faded Print** (muted): secondary text such as the lunar date, author lines, group labels, hints (6.23:1 on paper, 5.38:1 on wall).
- **Ruling Line** (rule): 1px cell dividers, the dotted perforation, the stacked sheet edges (1.33:1 on paper, so it divides; it does not carry meaning).
- **Field Edge** (field): 1px stroke on inputs and selects (3.7:1 on white, 3.4:1 on paper), so controls read as controls.
- **White** (accent-text): text on red. Field interiors are also plain white (#ffffff) so inputs read as a slip laid on the sheet.

Legacy aliases kept so every page and the admin share one palette: `--bg` = wall, `--surface` = paper, `--text` = ink, `--border` = rule, `--accent` = red, `--danger` = #a81e15 (same value as red-deep).

### Named Rules
**The Numbers Are Red Rule.** Every Big Shoulders numeral (count, card minutes, step number, fact figure, result count, 403 code) is red; every headline and paragraph is ink. Red words are reserved for the Sunday date, the active nav link, the pressed cell and actions.

**The Four Inks Rule.** The palette is paper, ink, red and blue, plus green only for done/published. No gradients, no tints used as fills, no new hue for decoration.

## Typography

**Display Font:** Big Shoulders Display, weights 700/800/900 (falls back to Be Vietnam Pro, then sans-serif)
**Body Font:** Be Vietnam Pro, weights 400/500/600/700 (falls back to system-ui, sans-serif)

Both load from Google Fonts with `display=swap`, and both carry full Vietnamese diacritics.

**Character:** A condensed, tall poster numeral like the ones printed on a calendar sheet, over a sturdy Vietnamese workhorse sans that stays legible at arm's length in kitchen light.

### Hierarchy
- **Display** (900, the count numeral, line-height 0.78, -0.03em, tabular): the home count only. Sized by the numeral rule below.
- **Headline** (800, clamp(2.25rem, 6vw, 3.5rem), 1.15, -0.01em): page h1 ("Trong bếp có gì?", "Công thức"). Balanced wrapping.
- **Headline, recipe** (800, clamp(2.5rem, 8vw, 4.5rem), 0.95, max 18ch): the dish title on recipe detail.
- **Title** (800, 2rem, -0.01em): display-face section heads (Nguyên liệu, Cách làm, Nấu được ngay, Theo danh mục); the editor uses 1.9rem.
- **Figure** (900, 2.25 to 2.75rem, 0.85 to 1, red, tabular): step numbers, recipe facts, result count. Larger figures (6rem empty-state zero, 6.5rem card minutes, 8rem 403 code) are the same role scaled up.
- **Title, sans** (700, 1.15rem, 1.15): card titles and plain h2.
- **Body** (400, 1rem, 1.55): default text; captions at 1.05rem, max 34ch beside the numeral.
- **Body, reading** (400, 1.1rem, 1.6, pre-line): recipe steps, read mid-cooking.
- **Label** (700, 0.85rem): ingredient group labels and stamps (stamps 0.8rem, +0.02em). Sentence case, never uppercase. Form field labels are 600 at 0.95rem.

### The numeral sizing rule
The count numeral is sized against both the viewport and its own column, divided by its digit count (`--d`, set from the number's length) inside an inline-size container:

- Below 900px: `font-size: min(34svh, calc(205cqi / var(--d, 3)), 26rem)`. One digit grows as tall as possible; three digits still fit the width; the 34svh cap keeps the first ingredient rows visible in the first viewport.
- At 900px and up: `font-size: min(48svh, calc(205cqi / var(--d, 3)), 26rem)`, numeral in a sticky left column.

### Named Rules
**The Two Faces Rule.** Big Shoulders is for numerals and headlines only; anything a cook reads in a sentence is Be Vietnam Pro.

**The Sentence Case Rule.** No all-caps labels and no letter-spaced eyebrows above headings. The only case transform in the build capitalises the first letter of the weekday that `Intl` returns in lower case.

## Layout

Public pages sit in a centred 1200px column with 1rem gutters (1.5rem from 860px). The topbar is a two-row grid on mobile (brand and search, then a horizontally scrolling ruled nav strip) and one row from 860px.

The home sheet is one column on mobile in reading order: date header (solar date, red on Sundays; lunar date in muted), the numeral with its one-line caption bottom-aligned beside it (the caption wraps below when three digits fill the width, `flex: 1 1 9rem`), the ingredient table, then the sticky tear-line footer. From 900px the body becomes a 5fr / 7fr grid: sticky numeral column on the left divided by a 1px rule, ingredient table on the right.

Ruled tables stack their group label above the cells on mobile and put it in a 7.5rem ruled left column from 600px. Recipe detail puts ingredients and steps in a 2fr / 3fr grid from 820px; recipe facts go from 2 to 4 ruled columns at 640px. Card grids use `repeat(auto-fill, minmax(min(100%, 15rem), 1fr))` with 1.5rem / 1.25rem gaps.

Spacing is in rem without a named scale: sheet inset 1.25rem (2rem from 720px), 0.75rem to 1.5rem between rows, 3rem between home sections. Breakpoints in use: 600, 640, 720, 820, 860, 900px.

## Elevation & Depth

Flat. Depth exists only as a stack of paper: each sheet casts two hard, zero-blur paper edges below it, each outlined by the ruling line, so you see the next sheets of the pad peeking out. Cards carry one such edge. Nothing floats, glows or blurs.

### Shadow Vocabulary
- **Sheet stack** (`box-shadow: 0 3px 0 -1px var(--paper), 0 3px 0 0 var(--rule), 0 6px 0 -1px var(--paper), 0 6px 0 0 var(--rule)`): every sheet and the auth card, with 6px bottom margin to clear it.
- **Card edge** (`box-shadow: 0 3px 0 -1px var(--paper), 0 3px 0 0 var(--rule)`): recipe cards.

### Named Rules
**The Stacked Sheet Rule.** Depth is paper under paper: hard edges in paper and rule colours, never a blurred or tinted shadow, never a gradient.

## Shapes

Sheets and cards are square-cornered paper with a red spine (10px on sheets, 5px on cards, 4px on the ingredient picker dropdown) and a 2px dotted perforation 6px below the spine on sheets. Controls use a barely softened 4px corner (`--radius`); stamps use 3px. The tear-line footer and every ruled table are bounded by lines, not boxes: a 2px ink rule to open, 1px rule dividers inside, 2px dotted rule for the tear line.

## Components

### Buttons
Printed and confident; colour fills on hover, no lift.
- **Shape:** gently squared (4px), min-height 44px, 600 weight, 0.4rem icon gap.
- **Primary:** calendar red fill, white text, red border; hover to red-deep.
- **Outline (default):** transparent with a 1px ink stroke and ink text; hover inverts to ink fill with paper text. Background and colour transition over 0.15s.
- **Tear-line action:** the primary button pinned in the sheet's sticky footer, 52px tall, 1.05rem; full width on mobile beside an optional "Bỏ chọn" link button, natural width with 1.75rem inline padding from 720px.
- **Link button:** blue underlined text, no box, still 44px tall; a danger variant uses red-deep.
- **Icon button:** 44 × 44px square.
- **Disabled:** 0.55 opacity, no hover change.

### Chips (selected filters)
- **Style:** red fill, white 600 text, 4px corners, 44px tall, with a small × icon; hover to red-deep. They exist only as "remove this filter" actions on the recipe list.

### Cards / Containers
- **Sheet:** paper, 10px red spine, dotted perforation, sheet-stack edges, square corners, inset 1.25rem (1.75rem 2rem 2rem from 720px). The auth card is a 440px sheet.
- **Recipe card:** paper, 5px red spine, one paper edge, square corners. Cover photo at 16:9; with no photo, the same 16:9 slot prints the total minutes as a 6.5rem red numeral with "phút", ruled off below, so a card without a picture still looks deliberate. Facts (minutes, difficulty, category) sit in a row of ruled cells, never a dotted meta string. Hover turns the title red.

### Inputs / Fields
- **Style:** white field, 1px field stroke (`--field` #8c857a, 3.7:1 on white), 4px corners, 44px min-height, 1rem text. The topbar search sits on paper with the same field stroke and a muted search icon; the ingredient picker uses a 1px dashed ink stroke.
- **Focus:** 2px ink-blue outline and blue border.
- **Error:** red-deep text at 0.875rem, 500 weight, under the field; form errors in red-deep, success in green.

### Navigation
- **Public:** ink links, 500 weight; hover red; the active link turns red with a 2px underline offset 0.45em. On mobile the links become a ruled strip that scrolls sideways without wrapping; "Đăng ký" is a primary button.
- **Admin:** a 232px paper sidebar with a red spine; links are 44px ruled rows, the active one filled red with white text. Below 720px the sidebar becomes a horizontal ruled strip.

### Stamps (status)
A rubber stamp: 2px border in the text colour, 3px corners, 700 weight at 0.8rem, +0.02em, never wraps. The line form carries the state so it reads without colour:
- **Bản nháp** (draft): dotted border, muted.
- **Chờ duyệt** (pending): dashed border, blue.
- **Đã đăng** (published): solid border, green.
- **Bị từ chối** (rejected): solid border, red, rotated -2deg as if stamped in a hurry.
- **Đã ẩn** (hidden): solid border, muted, text struck through.
- **Xong** (step done, recipe detail): green 2px stamp with a check icon, rotated -4deg on paper; done step text turns muted. Prepared ingredients are struck with a 2px red line.

### Ingredient table (signature)
Ruled cells like the giờ hoàng đạo box: a 2px ink rule on top, one row per group divided by 1px rules, the group label in muted 700 0.85rem. Each ingredient is a 44px tap cell (emoji at 1.15em, then the name), transparent until hover shows a rule-coloured border; pressed (`aria-pressed`) fills red with white 600 text. Selection lives in the URL so Back keeps it.

### Count numeral and tear (signature)
The giant red numeral sits on a paper fill, sized by the numeral rule. Its caption beside it is `aria-live`; the numeral itself is `aria-hidden` and the number is repeated for screen readers inside the caption. When the count changes, the new numeral is already printed in the same grid cell underneath; the old one is torn off the spine: over 0.38s it moves up 22% and rotates -9deg about its top-left corner while its lower 55% is clipped away (`cubic-bezier(0.16, 1, 0.3, 1)`), and it fades with ease-in. The numeral is `width: fit-content` so the leaving sheet covers only the digits, never the rule beside it. Under `prefers-reduced-motion: reduce` every transition and animation collapses to 0.01ms, so the number simply changes.

## Do's and Don'ts

### Do:
- **Do** keep every text pair at 4.5:1 or better; the shipped pairs run from 4.65:1 (red on paper) to 15.59:1 (ink on paper).
- **Do** make every public tap target at least 44px tall (52px for the tear-line action, 48px for recipe ingredient rows).
- **Do** print numbers in red Big Shoulders and words in ink Be Vietnam Pro.
- **Do** put meta (minutes, difficulty, category, facts) in ruled cells divided by 1px rules.
- **Do** give every new state a stamp line form (dotted, dashed, solid, rotated, struck) plus a word, never colour alone.
- **Do** start every new surface as a sheet: paper, red spine, dotted perforation, stacked edges.
- **Do** keep the tear as the single orchestrated motion and let the global reduced-motion rule remove it.
- **Do** make every surface complete without photos, as the minutes-numeral card face does.

### Don't:
- **Don't** use all-caps or letter-spaced labels; labels are sentence case.
- **Don't** put eyebrows or kickers above headings.
- **Don't** join meta with middle dots ("25 phút · Dễ · Món chính"); use ruled cells.
- **Don't** use "→" arrows in link or button text.
- **Don't** add gradients, blurred shadows or glows; depth is stacked paper only.
- **Don't** add a hue outside paper, ink, red, blue and done-green.
- **Don't** add motion that ignores `prefers-reduced-motion`.
