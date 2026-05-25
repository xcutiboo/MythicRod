package io.xcutiboo.mythicrod.api;

/// Small success/failure container used by MythicRod's public API.
///
/// A successful result carries a value. A failed result carries a human-readable
/// error string. Check `isSuccess()` or `isFailure()` before dereferencing the
/// value.
///
/// @param <T> wrapped success value type
public final class Result<T> {
    private final boolean success;
    private final T value;
    private final String error;

    private Result(boolean success, T value, String error) {
        this.success = success;
        this.value = value;
        this.error = error;
    }

    /// Creates a successful result.
    ///
    /// @param value success value
    /// @param <T> value type
    /// @return successful result carrying the supplied value
    public static <T> Result<T> success(T value) {
        return new Result<>(true, value, null);
    }

    /// Creates a failed result.
    ///
    /// @param error human-readable failure reason
    /// @param <T> value type
    /// @return failed result carrying the supplied error
    public static <T> Result<T> failure(String error) {
        return new Result<>(false, null, error);
    }

    /// Returns whether this result contains a success value.
    ///
    /// @return `true` when this result contains a value
    public boolean isSuccess() {
        return success;
    }

    /// Returns whether this result contains an error instead of a value.
    ///
    /// @return `true` when this result contains an error instead of a value
    public boolean isFailure() {
        return !success;
    }

    /// Returns the wrapped success value.
    ///
    /// @return success value, or `null` when the result is a failure
    public T getValue() {
        return value;
    }

    /// Returns the wrapped error message.
    ///
    /// @return failure reason, or `null` when the result is successful
    public String getError() {
        return error;
    }

    /// Returns the success value when present, otherwise the supplied fallback.
    ///
    /// @param fallbackValue value to return when this result is a failure
    /// @return success value or fallback
    public T orElse(T fallbackValue) {
        return success ? value : fallbackValue;
    }

    /// Returns the success value or throws with the stored error message.
    ///
    /// @return wrapped success value
    /// @throws IllegalStateException when the result is a failure
    public T orElseThrow() {
        if (success) {
            return value;
        }
        throw new IllegalStateException(error != null && !error.isBlank()
            ? error
            : "Operation failed without an error message");
    }
}
