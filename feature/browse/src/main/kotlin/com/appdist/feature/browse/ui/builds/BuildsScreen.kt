package com.appdist.feature.browse.ui.builds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.appdist.core.common.AppError
import com.appdist.core.common.toAppError
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.feature.browse.ui.builds.components.BuildCard
import com.appdist.feature.browse.ui.builds.components.FilterChipsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildsScreen(
    projectId: String,
    projectName: String,
    packageName: String,
    onBack: () -> Unit,
    onBuildClick: (String) -> Unit,
    viewModel: BuildsViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val builds = viewModel.builds.collectAsLazyPagingItems()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Text(
                        text = projectName.ifBlank { "Сборки" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            var searchText by remember { mutableStateOf("") }

            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp)) {
                Text("Сборки", style = MaterialTheme.typography.headlineSmall)
                if (packageName.isNotBlank()) {
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it; viewModel.setSearch(it) },
                    placeholder = { Text("Поиск по версии, ветке...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                FilterChipsRow(
                    filters = filters,
                    onChannelSelected = viewModel::setChannel,
                    onEnvironmentSelected = viewModel::setEnvironment,
                )
            }

            Spacer(Modifier.height(10.dp))

            when {
                builds.loadState.refresh is LoadState.Loading -> LoadingScreen(Modifier.weight(1f))
                builds.loadState.refresh is LoadState.Error -> {
                    val error = (builds.loadState.refresh as LoadState.Error).error
                    val appError = error.toAppError()
                    ErrorScreen(
                        error = appError,
                        title = "Не удалось загрузить сборки",
                        description = if (appError is AppError.Network) {
                            "Сервис временно недоступен. Поиск и фильтры сохранены."
                        } else null,
                        onRetry = { builds.refresh() },
                        modifier = Modifier.weight(1f),
                    )
                }
                builds.itemCount == 0 -> Box(Modifier.weight(1f)) {
                    EmptyScreen("Сборок не найдено")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(builds.itemCount) { index ->
                        builds[index]?.let { build ->
                            if (index == 1) {
                                Text(
                                    text = "Предыдущие сборки",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                                )
                            }
                            BuildCard(
                                build = build,
                                featured = index == 0,
                                onClick = { onBuildClick(build.id) },
                            )
                        }
                    }
                    if (builds.loadState.append is LoadState.Loading) {
                        item { LoadingScreen() }
                    }
                }
            }
        }
    }
}
