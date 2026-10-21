---
name: AppDist Volcanic Split
description: A build journal navigated as a living release graph.
colors:
  volcanic-ground: "#080B0C"
  volcanic-surface: "#111718"
  volcanic-surface-raised: "#182123"
  volcanic-panel: "#10191ACC"
  volcanic-panel-stroke: "#FFFFFF55"
  volcanic-field: "#05080899"
  action-teal: "#063F47"
  action-teal-deep: "#052F35"
  route-mint: "#9DDDD2"
  route-mint-bright: "#C9FFF5"
  node-related: "#376C67"
  primary-ink: "#ECF1EF"
  muted-ink: "#A9BCB8"
  muted-ink-dim: "#8FA7A3"
  structural-outline: "#30433F"
  navigation-graph: "#7FA09A38"
  ghost-numeral: "#FFFFFF08"
  track: "#263637"
typography:
  display-build:
    fontSize: "46sp"
    fontWeight: 700
    lineHeight: "50sp"
    letterSpacing: "-1.4sp"
  display:
    fontSize: "38sp"
    fontWeight: 700
    lineHeight: "42sp"
    letterSpacing: "-0.7sp"
  headline:
    fontSize: "28sp"
    fontWeight: 700
    lineHeight: "33sp"
    letterSpacing: "-0.35sp"
  title:
    fontSize: "22sp"
    fontWeight: 700
    lineHeight: "28sp"
  body:
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  label:
    fontSize: "11sp"
    fontWeight: 600
    lineHeight: "16sp"
  mono-label:
    fontSize: "10sp"
    fontWeight: 700
    lineHeight: "14sp"
    letterSpacing: "0.9sp"
  mono-action:
    fontSize: "9sp"
    fontWeight: 700
    lineHeight: "13sp"
    letterSpacing: "0.3sp"
rounded:
  control: "12dp"
  card: "18dp"
  featured: "24dp"
  action: "22dp"
  split-seam: "40dp"
spacing:
  compact: "6dp"
  small: "10dp"
  card-gap: "12dp"
  card-inset: "16dp"
  content-edge: "20dp"
  section: "24dp"
  large: "30dp"
components:
  action-zone:
    backgroundColor: "{colors.action-teal}"
    textColor: "{colors.primary-ink}"
    rounded: "{rounded.split-seam}"
    width: "92dp"
  action-tile:
    backgroundColor: "{colors.primary-ink}"
    textColor: "{colors.action-teal}"
    rounded: "{rounded.action}"
    width: "76dp"
    minHeight: "112dp"
  rail-target:
    backgroundColor: "{colors.action-teal-deep}"
    textColor: "{colors.primary-ink}"
    rounded: "{rounded.control}"
    size: "48dp"
  card:
    backgroundColor: "{colors.volcanic-panel}"
    borderColor: "{colors.volcanic-panel-stroke}"
    textColor: "{colors.primary-ink}"
    rounded: "{rounded.card}"
    padding: "16dp"
  card-featured:
    backgroundColor: "{colors.volcanic-panel}"
    borderColor: "{colors.volcanic-panel-stroke}"
    textColor: "{colors.primary-ink}"
    rounded: "{rounded.featured}"
    padding: "16dp"
  field:
    backgroundColor: "{colors.volcanic-field}"
    borderColor: "{colors.volcanic-panel-stroke}"
    textColor: "{colors.primary-ink}"
    rounded: "18dp"
    height: "48dp"
---

# Design System: AppDist Volcanic Split

## Overview

**Creative North Star: "The Volcanic Release Graph"**

AppDist presents the build journal as a dark, navigable system rather than a generic file browser. A basalt-black workspace carries a quiet planar graph, while a persistent deep-teal seam owns either top-level navigation or the current screen's primary action. Mint light marks the active route, current state, and decisive control.

The world is technical but not terminal-themed: hierarchy comes from scale, weight, tonal layering, and smooth geometry. Metadata uses mono only where its fixed rhythm improves scanning.

**Key Characteristics:**

- Dark volcanic ground with one cool teal action surface.
- A planar, non-crossing navigation graph that changes its active subgraph per destination.
- Thumb-reachable actions on a user-selectable left or right rail.
- Large newest-build treatment followed by comfortably inset history rows.
- Translucent basalt panels, so the graph keeps reading underneath the content.
- A ghost numeral of the screen's own subject — build number, journal count, handedness — as the second reading.
- Graph-plus-build-log identity shared by the background and launcher icon.

## Colors

The palette uses near-black depth, mineral teal structure, and a rare pale-mint signal.

### Primary

- **Route Mint:** active paths, selected rail controls, progress, and important state text.
- **Action Teal:** the persistent action seam, featured build surfaces, and brand mark backing.

### Neutral

- **Volcanic Ground:** full-screen base and primary text on mint controls.
- **Volcanic Surface:** list rows, detail blocks, settings groups, and fields.
- **Raised Basalt:** secondary Material surfaces and selected supporting controls.
- **Primary Ink:** headings and primary body copy.
- **Muted Mineral Ink:** metadata and secondary explanation.
- **Structural Outline:** focused or outlined controls only; cards are tonal, not double-treated.

**The Active Route Rule.** Mint belongs to the active route, primary action, or meaningful state; it is not general decoration.

**The Tinted Secondary Rule.** Secondary text on colored surfaces derives from that surface's foreground, never unrelated gray.

## Typography

The current implementation uses Android's platform sans and mono fallback. Their sizes and role hierarchy are recorded; the platform sans is an implementation fallback, not a canonical display identity.

### Hierarchy

- **Display** (700, 38sp/42sp): build version and the single strongest value on a detail or featured card.
- **Headline** (700, 28sp/33sp): screen title.
- **Title** (700, 22sp/28sp): section or strong list hierarchy.
- **Body** (400, 14sp/20sp): descriptions and release notes.
- **Label** (600, 11sp/16sp): compact state and section labels.
- **Mono metadata:** package names, versions, branches, dates, byte counts, hashes, and technical keys only.

**The Data-Only Mono Rule.** Mono is reserved for identifiers and measurements; navigation and descriptive prose stay proportional.

## Layout

Every primary screen is a two-part composition: flexible content plus a 92dp action zone. The zone may sit on either side according to the persisted handedness setting. Content uses a 20dp outer edge and a 16dp inner edge toward the seam, 12dp list gaps, and 16dp card insets. Both halves apply system-bar insets independently so the color seam remains edge-to-edge.

The action zone reads top to bottom as context then action. Context — a 48dp secondary target (settings, back) and the current object's compact metadata — is pinned to the top. The ДЕЙСТВИЕ caption and the primary tile sit centered in the space left below it, so the action lands in the same place whether the screen carries one line of metadata or six. Tiles use 76dp width and scalable minimum heights; every control stays at least 48dp tall. Content scrolls when vertical space or font scaling requires it.

**The Single Action Anchor.** A screen has one action block, and the zone places it — screens supply `zoneTop` and `zoneAction`, never their own weighted spacers. Moving the anchor is a one-line change in the scaffold rather than an edit in every screen.

Bottom-anchored controls in content clear the system navigation by a floor, not by an offset on top of the inset: the gap is `max(56dp, systemInset + 16dp)`, because some builds of Android report a smaller bottom inset than the navigation bar they actually draw, and a control that only trusts `navigationBarsPadding()` ends up under the buttons. The action zone applies the same rule with a 64dp floor.

The background graph is planar: one trunk feeds ordered branches without crossings, in the layout space of the content half. Apps feeds a junction; the junction feeds the builds branch (Builds then Build detail), the Downloads branch, and the Settings branch. The active destination lights its continuous path and terminal node; disconnected decorative fragments are not allowed. The whole graph mirrors with the action zone so the trunk always points at the seam.

## Elevation & Depth

The system uses no resting shadows. Depth comes from three dark tonal levels, scale, and the teal split surface. Dialogs use standard Material modal elevation only when installation is blocked and protected focus is appropriate.

**The Tonal Depth Rule.** A container uses either a tonal shift or a border for separation, never a border under a broad shadow.

## Shapes

Cards use smooth 18dp corners; featured build surfaces expand to 24dp. Primary action tiles use 22dp corners. The outer action seam uses a single 40dp inward-facing curve, creating one continuous silhouette instead of a stack of unrelated rounded cards.

Compact chips may be pill-like because they are filters or statuses. Large containers are never pills.

## Components

### Buttons

- **Action tile:** ink surface, teal icon and mono caption, 76dp wide, 112dp minimum height, 24dp corners, vertically centered in the zone. One per screen; download progress and cancel live inside the same tile rather than spawning a second control.
- **Rail target:** 48dp, 16dp corners, one tone deeper than the seam with a hairline outline, so it never competes with the tile.
- **Blocked install:** disabled tile; signature and downgrade blockers open a persistent explanatory dialog with a recovery action.

### Chips

- **Style:** dark tonal fill with a hairline outline, 16dp corners, 48dp touch height for filters.
- **State:** the selected filter moves to the ink surface with teal text — the same contrast as the primary action, so state reads without relying on hue. Status chips always include text.

### Cards / Containers

- **Corner Style:** 18dp standard, 20–24dp featured.
- **Background:** translucent basalt with a hairline light outline; the graph reads through it.
- **Shadow Strategy:** none at rest.
- **Internal Padding:** 16dp.

### Inputs / Fields

- **Style:** near-black translucent fill, 18dp corners, hairline outline, 48dp height.
- **Focus:** route-mint cursor and indicator.

### Navigation

The action zone replaces the app-level bottom bar and owns the screen's primary action. Switching between the top-level routes — Сборки and Скачанные — is a mint-underlined switcher in the content header, not a control in the zone. Selected destinations expose visual and accessibility selected state. Nested screens preserve native system Back and show a labeled Back control in the content top bar.

### Navigation Graph

The graph is both context and identity. It is quiet behind content, bright only along the active subgraph, and appears in simplified form in the launcher icon. Sequential dots on an active branch provide the second reading: a chronological build log.

## Do's and Don'ts

### Do:

- **Do** keep the primary screen action in the action zone whenever the action is singular and decisive.
- **Do** display version, environment, branch, and build number before users open technical details.
- **Do** preserve 48dp touch targets, selected semantics, system Back, and edge-to-edge insets.
- **Do** keep graph edges planar and connect every terminal to the common trunk.
- **Do** let the ghost numeral name the screen's own subject, and keep it under 10% opacity.

### Don't:

- **Don't** reintroduce a bottom navigation bar on Volcanic screens.
- **Don't** put route switching in the action zone; that zone belongs to the screen's action.
- **Don't** put a second competing ink surface on a screen — one primary tile at a time.
- **Don't** use imagery unrelated to software builds, testing, releases, or artifacts.
- **Don't** use mono as a technical costume for ordinary prose.
- **Don't** add shadows, gradients, or decorative texture to compensate for weak hierarchy.
- **Don't** let list content touch card edges; history cards retain their established insets.

