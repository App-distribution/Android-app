package com.appdist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appdist.core.common.AppError

@Composable
fun ErrorScreen(
    error: AppError,
    title: String? = null,
    description: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.errorContainer),
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title ?: error.toTitle(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = description ?: error.toDescription(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        error.toBadge()?.let { badge ->
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.height(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text("Повторить", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun AppError.toTitle() = when (this) {
    AppError.NoInternet -> "Нет подключения к сети"
    AppError.Unauthorized -> "Необходима авторизация"
    is AppError.Network -> "Не удалось загрузить данные"
    is AppError.ChecksumMismatch -> "Файл повреждён"
    is AppError.SignatureMismatch -> "Подпись приложения отличается"
    is AppError.IncompatibleDevice -> "Сборка несовместима"
    is AppError.Storage -> "Недостаточно места"
    is AppError.Unknown -> "Что-то пошло не так"
}

private fun AppError.toDescription() = when (this) {
    AppError.NoInternet -> "Проверьте подключение к интернету и попробуйте ещё раз."
    AppError.Unauthorized -> "Войдите снова, чтобы продолжить работу."
    is AppError.Network -> when (code) {
        404 -> "Запрошенные данные не найдены или больше недоступны."
        else -> "Сервис временно недоступен. Попробуйте ещё раз."
    }
    is AppError.ChecksumMismatch -> "Контрольная сумма файла не совпадает. Загрузите сборку повторно."
    is AppError.SignatureMismatch -> "Подпись сборки отличается от уже установленного приложения."
    is AppError.IncompatibleDevice -> reason
    is AppError.Storage -> message
    is AppError.Unknown -> message.ifBlank { "Повторите попытку через некоторое время." }
}

private fun AppError.toBadge(): String? = when (this) {
    is AppError.Network -> code?.let { "Ошибка сервера · $it" }
    else -> null
}
