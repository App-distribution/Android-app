package com.appdist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.theme.AppTheme
import com.appdist.core.ui.theme.MonoLabel

private val LedgerShape = RoundedCornerShape(6.dp)

@Composable
fun LedgerScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides AppTheme.colors.ink) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppTheme.colors.ground)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(bottom = bottomGapAfterInsets()),
            content = content,
        )
    }
}

@Composable
fun LedgerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingDescription: String? = null,
    onLeadingClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null && onLeadingClick != null) {
            LedgerIconButton(leadingIcon, leadingDescription, onLeadingClick)
        } else {
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = if (leadingIcon == null) 12.dp else 2.dp),
        )
        if (trailingIcon != null && onTrailingClick != null) {
            LedgerIconButton(trailingIcon, trailingDescription, onTrailingClick)
        } else {
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
fun LedgerIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.inkSoft,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = if (highlighted) {
        Modifier.background(
            Brush.linearGradient(
                listOf(AppTheme.colors.action, AppTheme.colors.accentBright)
            ),
            LedgerShape,
        )
    } else {
        Modifier.background(AppTheme.colors.panel, LedgerShape)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(background)
            .border(
                width = 1.dp,
                color = if (highlighted) AppTheme.colors.highlightStroke else AppTheme.colors.panelStroke,
                shape = LedgerShape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

@Composable
fun LedgerPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                color = if (enabled) AppTheme.colors.actionTile else AppTheme.colors.surfaceRaised,
                shape = LedgerShape,
            )
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) AppTheme.colors.onActionTile else AppTheme.colors.muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) AppTheme.colors.onActionTile else AppTheme.colors.muted,
        )
    }
}

@Composable
fun LedgerOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(AppTheme.colors.surface, LedgerShape)
            .border(1.dp, AppTheme.colors.panelStroke, LedgerShape)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.inkSoft,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.ink)
    }
}

@Composable
fun LedgerSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MonoLabel,
        color = AppTheme.colors.muted,
        modifier = modifier,
    )
}

@Composable
fun LedgerBadge(
    label: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .background(
                if (selected) AppTheme.colors.statusLatestContainer else AppTheme.colors.surface,
                LedgerShape,
            )
            .border(
                1.dp,
                if (selected) AppTheme.colors.panelStrokeStrong else AppTheme.colors.panelStroke,
                LedgerShape,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MonoLabel,
            color = if (selected) AppTheme.colors.statusLatest else AppTheme.colors.inkSoft,
        )
    }
}
