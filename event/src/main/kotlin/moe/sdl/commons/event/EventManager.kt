@file:Suppress("unused")

package moe.sdl.commons.event

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import moe.sdl.commons.coroutines.ModuleScope
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

private val logger = LoggerFactory.getLogger(EventManager::class.java)

/**
 * Thread-safe EventManager
 */
class EventManager(
  private var eventScope: ModuleScope = ModuleScope("EventManager"),
) {
  private class PriorityEntry(
    val priority: EventPriority,
    val listeners: ConcurrentLinkedQueue<suspend (Event) -> Unit>,
  )

  private class PriorityChannelEntry(
    val priority: EventPriority,
    val channel: Channel<Event>,
  )

  private val listeners = List(EventPriority.entries.size) {
    PriorityEntry(EventPriority.entries[it], ConcurrentLinkedQueue())
  }
  private val blockingListeners = List(EventPriority.entries.size) {
    PriorityEntry(EventPriority.entries[it], ConcurrentLinkedQueue())
  }
  private val flowListeners = List(EventPriority.entries.size) {
    PriorityChannelEntry(EventPriority.entries[it], Channel(64))
  }

  private val blockingTimeout =
    System.getProperty("moe.sdl.commons.event.timeout.blocking")?.toLongOrNull()
      ?: (1000 * 30L)

  private val eventTimeout =
    System.getProperty("moe.sdl.commons.event.timeout.event")?.toLongOrNull()
      ?: (blockingTimeout * 3)

  /**
   * Get original event flow from broadcasting
   * Flow will receive the event data in parallel
   *
   * You can **NOT CANCEL** or **INTERCEPT** any event in parallel listener.
   *
   * @param priority optional, set the priority of this listener
   * @return [Flow]
   */
  fun flow(priority: EventPriority = EventPriority.NORMAL): Flow<Event> {
    return flowListeners.first { it.priority == priority }.channel.receiveAsFlow()
  }

  /**
   * Register a parallel listener.
   *
   * All registered listeners will be called in parallel.
   *
   * You can **NOT CANCEL** or **INTERCEPT** any event in parallel listener.
   *
   * @param priority optional, set the priority of this listener
   * @param listener lambda block of your listener with the all event of parameter
   */
  fun register(priority: EventPriority = EventPriority.NORMAL, listener: suspend (Event) -> Unit) {
    listeners.first { it.priority == priority }.listeners.add(listener)
  }

  /**
   * Register a blocking listener.
   *
   * All registered block listeners will be called in serial.
   *
   * You can cancel or intercept listened event in serial listener.
   *
   * @param priority optional, set the priority of this listener
   * @param listener lambda block of your listener with all type of event of parameter
   */
  fun registerBlocking(priority: EventPriority = EventPriority.NORMAL, listener: suspend (Event) -> Unit) {
    blockingListeners.first { it.priority == priority }.listeners.add(listener)
  }

  /**
   * Broadcast event, and return the cancel state of this event
   *
   * @param event the event will be broadcast
   * @return [Boolean] that represents the cancel state of this event
   */
  suspend fun broadcast(event: Event): Boolean {
    val isCancelled = AtomicBoolean(false)
    val isIntercepted = AtomicBoolean(false)
    logger.atTrace().log { "Try to broadcast event ${event::class.simpleName}" }
    listeners.forEach { ele ->
      val priority = ele.priority
      val pListener = ele.listeners
      pListener.forEach { listener ->
        eventScope.launch {
          listener(event)
        }
      }
      eventScope.launch {
        flowListeners
          .first { it.priority == priority }.channel
          .send(event)
      }
      val blockListeners = blockingListeners.first { it.priority == priority }.listeners
      if (blockListeners.size > 0) {
        withTimeout(eventTimeout) {
          blockListeners.forEach { listener ->
            eventScope.launch {
              withTimeout(blockingTimeout) {
                listener(event)
              }
              // if is false, update it, i.e. isIntercepted || event.isIntercepted
              isIntercepted.compareAndSet(false, event.isIntercepted)
              // if is false, update it
              isCancelled.compareAndSet(false, event is CancellableEvent && event.isCancelled)
            }.join()
          }
        }
      }
      if (isIntercepted.get()) {
        logger.atDebug().log { "Event ${event::class.simpleName} has been intercepted" }
        return isCancelled.get()
      }
    }
    val cancelled = isCancelled.get()
    if (cancelled) logger.atDebug().log { "Broadcast event ${event::class.simpleName}, has been cancelled" }
    return cancelled
  }

  fun cancel() {
    eventScope.parentJob.cancel()
  }
}

/**
 * Register a parallel listener to capture
 * the event that conform with the requirement of [T] only once.
 *
 * You should **NOT CANCEL** or **INTERCEPT** any event in parallel listener.
 *
 * @param T event class to listen
 * @param priority the priority of this listener
 * @return [Event] return a captured event
 */
suspend inline fun <reified T : Event> EventManager.nextEvent(priority: EventPriority = EventPriority.NORMAL): Event? =
  flow(priority).firstOrNull { it is T }

/**
 * Register a parallel listener to capture
 * the event that conform with the requirement of `filter` and [T] only once.
 *
 * You should **NOT CANCEL** or **INTERCEPT** any event in parallel listener.
 *
 * Use `inline` with `listener` will lose precise stacktrace for exception
 *
 * @param T event class to listen
 * @param priority the priority of this listener
 * @param filter lambda block of your filter for specific event detail
 * @return [Event] return a captured event
 */
suspend inline fun <reified T : Event> EventManager.nextEvent(
  priority: EventPriority = EventPriority.NORMAL,
  noinline filter: (T) -> Boolean,
): Event? = flow(priority).firstOrNull { it is T && filter(it) }

/**
 * Register a parallel listener. All registered listeners will be called in parallel.
 *
 * You should **NOT CANCEL** or **INTERCEPT** any event in parallel listener.
 *
 * Use `inline` with `listener` will lose precise stacktrace for exception
 *
 * @param T event class to listen
 * @param priority optional, set the priority of this listener
 * @param listener lambda block of your listener with the specific event of parameter
 */
inline fun <reified T : Event> EventManager.register(
  priority: EventPriority = EventPriority.NORMAL,
  noinline listener: suspend (T) -> Unit,
): Unit = register(priority) {
  if (it is T) listener(it)
}

/**
 * Register a block listener. All registered block listeners will be called in serial.
 *
 * You can cancel or intercept listened event in serial listener.
 *
 * Use `inline` with `listener` will lose precise stacktrace for exception.
 *
 * @param T event class to listen
 * @param priority optional, set the priority of this listener
 * @param listener lambda block of your listener with the specific event of parameter
 */
inline fun <reified T : Event> EventManager.registerBlocking(
  priority: EventPriority = EventPriority.NORMAL,
  noinline listener: suspend (T) -> Unit,
): Unit = registerBlocking(priority) {
  if (it is T) listener(it)
}

/**
 * Broadcast event in a quick way
 *
 * Use `inline` will lose precise stacktrace for exception
 *
 * @param ifNotCancel lambda block with the action if broadcast event has **NOT** been cancelled
 */
suspend inline fun <T : Event, R> T.broadcast(manager: EventManager, noinline ifNotCancel: suspend (T) -> R): R? =
  if (!manager.broadcast(this)) ifNotCancel(this) else null

/**
 * Broadcast event in a quick way
 *
 * Use `inline` for lambda function will lose precise stacktrace for exception
 *
 * @param ifCancelled lambda block with the action if broadcast event has been cancelled
 */
suspend inline fun <T : Event, R> T.broadcastIfCancelled(
  manager: EventManager,
  noinline ifCancelled: suspend (T) -> R,
): R? = if (manager.broadcast(this)) ifCancelled(this) else null

/**
 * Broadcast event in a quick way
 *
 * Use `inline` for lambda function will lose precise stacktrace for exception
 *
 * @param ifNotCancel lambda block with the action if broadcast event has **NOT** been cancelled
 */
suspend inline fun <T : Event, R> T.broadcast(
  manager: EventManager,
  noinline ifNotCancel: suspend (T) -> R,
  noinline ifCancelled: suspend (T) -> R,
): R = if (!manager.broadcast(this)) {
  ifNotCancel(this)
} else {
  ifCancelled(this)
}

/**
 * Broadcast event in quick way
 */
suspend inline fun <T : Event> T.broadcast(manager: EventManager): Boolean = manager.broadcast(this)
