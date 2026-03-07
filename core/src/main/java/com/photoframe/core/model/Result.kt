package com.photoframe.core.model

import androidx.compose.runtime.Immutable

/**
 * A generic sealed class that represents the result of an operation.
 * Used for error handling throughout the application.
 *
 * Thread Safety: Immutable sealed class, safe to share across threads.
 *
 * @param T The type of data returned on success
 */
@Immutable
sealed class Result<out T> {
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with an error.
     */
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()

    /**
     * Represents an operation that is still in progress.
     */
    object Loading : Result<Nothing>()

    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if this is a Loading result.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if this is a Success, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the data if this is a Success, or throws the exception if Error.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get data from Loading state")
    }

    /**
     * Returns the data if this is a Success, or the default value otherwise.
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    /**
     * Maps the data if this is a Success, otherwise returns the same Error/Loading.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    /**
     * Flat maps the data if this is a Success, otherwise returns the same Error/Loading.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> this
    }

    /**
     * Executes the given block if this is a Success.
     */
    inline fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) {
            block(data)
        }
        return this
    }

    /**
     * Executes the given block if this is an Error.
     */
    inline fun onError(block: (Throwable) -> Unit): Result<T> {
        if (this is Error) {
            block(exception)
        }
        return this
    }

    companion object {
        /**
         * Creates a Success result.
         */
        fun <T> success(data: T): Result<T> = Success(data)

        /**
         * Creates an Error result.
         */
        fun error(exception: Throwable, message: String? = null): Result<Nothing> =
            Error(exception, message)

        /**
         * Creates a Loading result.
         */
        fun loading(): Result<Nothing> = Loading
    }
}
