package com.appdist.core.common

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface AppError {
    data class Network(val code: Int?, val message: String) : AppError
    data object Unauthorized : AppError
    data object NoInternet : AppError
    data class ChecksumMismatch(val expected: String, val actual: String) : AppError
    data class IncompatibleDevice(val reason: String) : AppError
    data class SignatureMismatch(val installed: String, val available: String) : AppError
    data class Storage(val message: String) : AppError
    data class Unknown(val message: String) : AppError
}

class BackendException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)

fun Throwable.toAppError(): AppError = when (this) {
    is BackendException -> when (statusCode) {
        401, 403 -> AppError.Unauthorized
        else -> AppError.Network(statusCode, message)
    }
    is UnknownHostException, is SocketException, is SocketTimeoutException -> AppError.NoInternet
    else -> AppError.Unknown(message ?: "Unknown error")
}
