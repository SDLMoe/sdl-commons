package moe.sdl.commons.coroutines

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal val logger = LoggerFactory.getLogger(ModuleScope::class.java)

typealias ExceptionHandler = (ctx: CoroutineContext, e: Throwable, logger: Logger, moduleName: String) -> Unit

/**
 * Provide a common [CoroutineScope] for module
 * with common [CoroutineExceptionHandler] and [Dispatchers.Default]
 *
 * In general, you should add a [ModuleScope] as a class member field
 * or object member field with some `init(parentCoroutineContext)` method.
 * And launch or dispatch jobs coroutines by [ModuleScope]
 *
 * @property parentJob specified [Job] with parent coroutine context [Job]
 *
 * @param moduleName coroutine name
 * @param parentContext parent scope [CoroutineContext]
 * @param dispatcher custom [CoroutineDispatcher]
 * @param exceptionHandler custom exception handler lambda
 * with [CoroutineContext], [Throwable], [KLogger] the specified logger, [String] module name
 */
open class ModuleScope(
  private val moduleName: String = "UnnamedModule",
  parentContext: CoroutineContext = EmptyCoroutineContext,
  dispatcher: CoroutineDispatcher? = Dispatchers.Default,
  exceptionHandler: ExceptionHandler? = { _, e, _, _ ->
    logger.atError().setCause(e).log { "Caught Exception on $moduleName" }
  },
) : CoroutineScope {

  val parentJob = SupervisorJob(parentContext[Job])

  override val coroutineContext: CoroutineContext = run {
    var ctx = parentContext + parentJob + CoroutineName(moduleName)
    dispatcher?.let { ctx = ctx.plus(it) }
    exceptionHandler?.let { handler ->
      ctx = ctx.plus(
        CoroutineExceptionHandler { context, e ->
          handler(context, e, logger, moduleName)
        },
      )
    }
    ctx
  }

  fun dispose() {
    parentJob.cancel()
    onClosed()
  }

  fun subscope(name: String): ModuleScope = ModuleScope("$moduleName.$name", coroutineContext, null, null)

  open fun onClosed() {
  }
}
