package io.xcutiboo.mythicrod.api.platform;

/// Minimal cancellation handle returned by delayed and repeating scheduler work.
public interface PlatformTask {

    /// Cancels the task. Repeated calls should be harmless.
    void cancel();

    /// Returns whether the underlying scheduler reports this task as cancelled.
    ///
    /// @return `true` when the underlying scheduler reports the task as cancelled
    boolean isCancelled();
}
