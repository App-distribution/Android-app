package com.appdist.core.ui.theme

import androidx.compose.ui.graphics.Color

val VolcanicGround = Color(0xFF080B0C)
val VolcanicSurface = Color(0xFF111718)
val VolcanicSurfaceRaised = Color(0xFF182123)
val VolcanicAction = Color(0xFF063F47)
val VolcanicAccent = Color(0xFF9DDDD2)
val VolcanicInk = Color(0xFFECF1EF)
val VolcanicMuted = Color(0xFFA9BCB8)
val VolcanicOutline = Color(0xFF30433F)
val VolcanicGraph = Color(0x387FA09A)

// Volcanic Split revision: translucent basalt panels over the ground, so the
// navigation graph keeps reading through the content.
val VolcanicPanel = Color(0xCC10191A)
val VolcanicPanelStroke = Color(0x55FFFFFF)
val VolcanicPanelStrokeStrong = Color(0x66FFFFFF)
val VolcanicField = Color(0x99050808)
val VolcanicChip = Color(0xAA050808)

// Action zone surfaces: the seam itself is VolcanicAction, secondary controls
// inside it sit one step deeper so the ink action tiles stay the loudest thing.
val VolcanicRailTarget = Color(0xFF052F35)
val VolcanicRailTargetStroke = Color(0x33FFFFFF)
val VolcanicRailDivider = Color(0x66FFFFFF)
val VolcanicActionPreview = Color(0xFF0C6974)

val VolcanicAccentBright = Color(0xFFC9FFF5)
val VolcanicNodeRelated = Color(0xFF376C67)
val VolcanicNodeOutline = Color(0x777FA09A)

val VolcanicInkSoft = Color(0xFFD5E0DD)
val VolcanicMutedDim = Color(0xFF8FA7A3)
val VolcanicGhost = Color(0x08FFFFFF)
val VolcanicGhostLabel = Color(0x33AFC5C0)
val VolcanicTrack = Color(0xFF263637)

val AppPrimary = VolcanicAccent
val AppOnPrimary = VolcanicGround
val AppPrimaryContainer = VolcanicAction
val AppOnPrimaryContainer = VolcanicInk

val AppBackground = VolcanicGround
val AppSurface = VolcanicSurface
val AppSurfaceVariant = VolcanicSurfaceRaised
val AppOnSurface = VolcanicInk
val AppOnSurfaceVariant = VolcanicMuted
val AppOutline = VolcanicOutline
val AppOutlineVariant = VolcanicOutline

val AppDarkPrimary = VolcanicAccent
val AppDarkBackground = VolcanicGround
val AppDarkSurface = VolcanicSurface
val AppDarkSurfaceVariant = VolcanicSurfaceRaised
val AppDarkOnSurface = VolcanicInk
val AppDarkOnSurfaceVariant = VolcanicMuted
val AppDarkOutline = VolcanicOutline

// Semantic status colors (theme-independent)
val StatusMandatory = Color(0xFFFFB4AB)
val StatusMandatoryContainer = Color(0xFF4A1D1B)
val StatusDeprecated = Color(0xFFCAD4D1)
val StatusDeprecatedContainer = Color(0xFF29302F)
val StatusExpiring = Color(0xFFFFD18A)
val StatusExpiringContainer = Color(0xFF4A3210)
val StatusLatest = VolcanicAccent
val StatusLatestContainer = VolcanicAction
