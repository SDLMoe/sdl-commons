package moe.sdl.commons.event

/**
 * `AbstractEvent` includes interception method,
 * which can intercept the event broadcast to lower priority event listener.
 *
 * @property [isIntercepted] whether this event has been intercepted
 *
 * @see [EventManager.broadcast]
 * @see [EventPriority]
 */
abstract class Event {
  @Volatile
  var isIntercepted: Boolean = false
    private set

  /**
   * Intercept and stop this event broadcasting
   * It can intercept events broadcast to lower priority event listener.
   *
   * @see [EventManager.broadcast]
   * @see [EventPriority]
   */
  fun intercept() {
    isIntercepted = true
  }
}

/**
 * `AbstractCancellableEvent` is subclass of `AbstractEvent`,
 * but can be cancelled, the final cancellation result will be
 * returned to the caller site.
 *
 * @property [isCancelled] whether this event has been cancelled
 * @see [Event]
 */
abstract class CancellableEvent : Event() {
  @Volatile
  var isCancelled: Boolean = false
    private set

  /**
   * Cancel this event and the state of event
   * will return to the call site of `broadcastEvent()` method.
   *
   * You must make sure you are cancelling a cancellable event,
   * otherwise, it will throw an exception
   *
   * @see [EventManager.broadcast]
   */
  fun cancel() {
    isCancelled = true
    onCancel()
  }

  /**
   * Will be called when a cancellation occurs.
   */
  open fun onCancel() {}
}

/**
 * Event broadcast priority
 * Decreasing priority from left to right
 * Same priority event listeners will be called in parallel.
 *
 * @see [EventManager.broadcast]
 */
enum class EventPriority {
  HIGHEST, HIGH, NORMAL, LOW, LOWEST
}
