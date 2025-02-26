package moe.sdl.commons.coroutines

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

/**
 * Use ReentrantLock in coroutine with different threads
 *
 * It will store mutex lock in [CoroutineContext] with [ReentrantMutexContextElement]
 */
@OptIn(ExperimentalContracts::class)
suspend inline fun <T> Mutex.withReentrantLock(owner: Any? = null, crossinline action: suspend () -> T): T {
  contract {
    callsInPlace(action, InvocationKind.EXACTLY_ONCE)
  }
  val key = ReentrantMutexContextKey(this)
  // call block directly when this mutex is already locked in the context
  if (currentCoroutineContext()[key] != null) return action()
  // otherwise add it to the context and lock the mutex
  return withContext(ReentrantMutexContextElement(key)) {
    lock(owner)
    try {
      return@withContext action()
    } finally {
      unlock(owner)
    }
  }
}

@JvmInline
value class ReentrantMutexContextElement(override val key: ReentrantMutexContextKey) : CoroutineContext.Element

@JvmInline
value class ReentrantMutexContextKey(val mutex: Mutex) : CoroutineContext.Key<ReentrantMutexContextElement>
