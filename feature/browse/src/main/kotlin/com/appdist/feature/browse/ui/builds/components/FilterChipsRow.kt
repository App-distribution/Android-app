package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdist.core.ui.theme.AppTheme
import com.appdist.feature.browse.domain.BuildFilterOptions
import com.appdist.feature.browse.ui.builds.BuildFilters

/**
 * Быстрые фильтры журнала: ряд видов сборки и ряд стендов. Значения приходят
 * с сервера — это то, что реально есть у сборок проекта, — поэтому новый
 * стенд появится здесь сам. Ряд с одним значением бесполезен и не показывается.
 */
@Composable
fun FilterChipsRow(
    filters: BuildFilters,
    options: BuildFilterOptions,
    onFlavorSelected: (String?) -> Unit,
    onStandSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (options.flavors.size > 1) {
            ChipGroup(values = options.flavors, selected = filters.flavor, onSelected = onFlavorSelected)
        }
        if (options.stands.size > 1) {
            ChipGroup(values = options.stands, selected = filters.stand, onSelected = onStandSelected)
        }
    }
}

@Composable
private fun ChipGroup(values: List<String>, selected: String?, onSelected: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterPill(label = "Все", selected = selected == null, onClick = { onSelected(null) })
        values.forEach { value ->
            FilterPill(
                label = value,
                selected = selected == value,
                onClick = { onSelected(if (selected == value) null else value) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 32.dp)
            .background(
                if (selected) AppTheme.colors.statusLatestContainer else AppTheme.colors.chip,
                shape,
            )
            .border(
                1.dp,
                if (selected) AppTheme.colors.panelStrokeStrong else AppTheme.colors.panelStroke,
                shape,
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
            ),
            color = if (selected) AppTheme.colors.statusLatest else AppTheme.colors.ink,
        )
    }
}
