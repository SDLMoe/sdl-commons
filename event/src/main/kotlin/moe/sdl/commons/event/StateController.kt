@file:Suppress("unused")

package moe.sdl.commons.event

import kotlinx.coroutines.launch
import moe.sdl.commons.coroutines.ModuleScope
import moe.sdl.commons.event.StateController.ListenerState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Interface for different state class
 *
 * You should implement it with your custom state interface
 *
 * ```
 * interface SomeClassWithState : WithState<SomeClassWithState.State> {
 *
 *  // This is a method you wanted to be differential implementation with different states
 *  fun foobar(): String
 *
 *  // An enum includes all your states
 *  enum class State {
 *      START,
 *      DOING,
 *      END
 *  }
 * }
 * ```
 * @property state indicated state of this class
 * @see StateController
 */
interface WithState<out T> {
  val state: T

  /**
   * This method will be called when this state starts
   *
   * @see StateController.setState
   */
  suspend fun startState() {}

  /**
   * This method will be called when this state ends
   *
   * @see StateController.setState
   */
  suspend fun endState() {}
}

/**
 * A simple state controller
 *
 * Generic [S] is an enum included all states of this controller,
 * [I] is your custom implementation of [WithState] interface,
 * [C] is your parent state that will be a receiver in state observer,
 * and interceptor lambda.
 *
 * You should call [StateController.init] to make sure
 * your first state has been correctly start with [WithState.startState].
 *
 * @param scope [ModuleScope] will provide a coroutine scope during the state transferring
 * @param parentStateClass is your parent state that will
 * be a receiver in state observer, and interceptor lambda
 * @see WithState
 */
open class StateController<S, I : WithState<S>, C>(
  protected var scope: ModuleScope,
  protected var parentStateClass: C,
  firstState: I,
) {

  protected val currentState = AtomicReference(firstState)

  private var observers = ConcurrentHashMap<suspend C.(S, S) -> Unit, ListenerState>()
  private var interceptors = ConcurrentHashMap<suspend C.(S, S) -> Boolean, ListenerState>()

  /**
   * Init state controller, to call the first state [WithState.startState]
   */
  suspend fun init() {
    currentState.get().startState()
  }

  /**
   * Get current enum state
   */
  fun getCurrentState(): S = currentState.get().state

  /**
   * Get current instance state
   */
  fun getStateInstance(): I {
    return currentState.get()
  }

  /**
   * Transfer state from current state to specified state
   *
   * It will call all observers in parallel, all interceptors in serial during transferring,
   * if there is an interceptor with [ListenerState.BEFORE_UPDATE] priority,
   * it can intercept and cancel this transferring.
   *
   * @see ListenerState
   * @see observeStateChange
   * @see interceptStateChange
   * @param after transfer to this state, instance type [I]
   */
  suspend fun setState(after: I): I {
    val before = currentState.get()
    val beforeState = before.state
    val afterState = after.state
    if (invokeChange(beforeState, afterState, ListenerState.BEFORE_UPDATE, parentStateClass)) return before
    before.endState()
    currentState.set(after)
    after.startState()
    invokeChange(beforeState, afterState, ListenerState.AFTER_UPDATE, parentStateClass)
    return before
  }

  private suspend fun invokeChange(
    beforeState: S,
    afterState: S,
    listenerState: ListenerState,
    listenerCaller: C,
  ): Boolean {
    val isIntercepted = AtomicBoolean(false)
    observers
      .asSequence()
      .filter { it.value == listenerState }
      .forEach { (observer, _) ->
        scope.launch {
          listenerCaller.observer(beforeState, afterState)
        }
      }
    interceptors
      .asSequence()
      .filter { it.value == listenerState }
      .map { (interceptor, _) ->
        scope.launch {
          val interceptorResult = listenerCaller.interceptor(beforeState, afterState)
          // if it is false, update it, i.e. isIntercepted || interceptorResult
          isIntercepted.compareAndSet(false, interceptorResult)
        }
      }
      .forEach { it.join() }

    return isIntercepted.get()
  }

  /**
   * Observe a state change
   *
   * [observer] will be called in parallel
   *
   * @param listenerState a [ListenerState] to indicate invocation priority
   * @param observer observation lambda block with [C] context
   * @see observe
   * @see setState
   */
  fun observeStateChange(
    listenerState: ListenerState = ListenerState.BEFORE_UPDATE,
    observer: suspend C.(S, S) -> Unit,
  ) {
    observers[observer] = listenerState
  }

  /**
   * Intercept a state change
   *
   * [interceptor] will be called in serial
   *
   * @param listenerState a [ListenerState] to indicate invocation priority
   * @param interceptor interception lambda block with [C] context
   * @see block
   * @see intercept
   * @see setState
   */
  fun interceptStateChange(
    listenerState: ListenerState = ListenerState.BEFORE_UPDATE,
    interceptor: suspend C.(S, S) -> Boolean,
  ) {
    interceptors[interceptor] = listenerState
  }

  fun clearObservers() {
    observers.clear()
  }

  fun clearInterceptors() {
    interceptors.clear()
  }

  /**
   * Observer or interceptor's priority
   *
   * [setState] will call all observers in parallel, all interceptors in serial during transferring,
   * if there is an interceptor with [ListenerState.BEFORE_UPDATE] priority,
   * it can intercept and cancel the transferring.
   *
   * @see setState
   */
  enum class ListenerState {
    BEFORE_UPDATE,
    AFTER_UPDATE,
  }
}

/**
 * [S] is state enum, [I] is state interface, [C] is the class with state
 * @param stateInstances all instances of your different state classes
 */
class InitStateController<S : Enum<*>, I : WithState<S>, C>(
  scope: ModuleScope,
  parentStateClass: C,
  vararg stateInstances: I,
) : StateController<S, I, C>(scope, parentStateClass, stateInstances.first()) {

  private val states = listOf(*stateInstances)

  /**
   * Transfer state from current state to specified state
   *
   * It will call all observers in parallel, all interceptors in serial during transferring,
   * if there is an interceptor with [ListenerState.BEFORE_UPDATE] priority,
   * it can intercept and cancel this transferring.
   *
   * @see ListenerState
   * @see observeStateChange
   * @see interceptStateChange
   * @param afterState transfer to this state, enum type [S]
   */
  suspend fun setState(afterState: S): I = setState(states.first { it.state == afterState })
}

/**
 * Quick way of [StateController.observeStateChange]
 * [S] is state enum, [I] is state interface, [C] is the class with state
 *
 * @param listenerState a [ListenerState] to indicate invocation priority
 * @param observer observation lambda block with [C] context,
 * and without any input parameter
 * @see StateController.observeStateChange
 * @see StateController.setState
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <S : Enum<*>, I : WithState<S>, C> StateController<S, I, C>.observe(
  listenerState: ListenerState = ListenerState.BEFORE_UPDATE,
  noinline observer: suspend C.() -> Unit,
): Unit = observeStateChange(listenerState) { _, _ -> this.observer() }

/**
 * Quick way of [StateController.interceptStateChange]
 * [S] is state enum, [I] is state interface, [C] is the class with state
 *
 * @param listenerState a [ListenerState] to indicate invocation priority
 * @param interceptor interception lambda block with [C] context
 * and without any input parameter
 * @see StateController.interceptStateChange
 * @see StateController.setState
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <S : Enum<*>, I : WithState<S>, C> StateController<S, I, C>.intercept(
  listenerState: ListenerState = ListenerState.BEFORE_UPDATE,
  noinline interceptor: suspend C.() -> Boolean,
): Unit = interceptStateChange(listenerState) { _, _ -> this.interceptor() }

/**
 * Quick way of [StateController.interceptStateChange]
 * [S] is state enum, [I] is state interface, [C] is the class with state
 *
 * Observe changes and call [block] in serial
 *
 * @param listenerState a [ListenerState] to indicate invocation priority
 * @param block lambda block with [C] context
 * and without any input parameter, and final return
 * @see StateController.interceptStateChange
 * @see StateController.setState
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <S : Enum<*>, I : WithState<S>, C> StateController<S, I, C>.block(
  listenerState: ListenerState = ListenerState.BEFORE_UPDATE,
  noinline block: suspend C.() -> Unit,
): Unit = interceptStateChange(listenerState) { _, _ ->
  block()
  false
}
