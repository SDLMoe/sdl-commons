package moe.sdl.commons.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class ModuleScopeTest {

  @Test
  fun `scope test`(): Unit = runTest {
    val parentScope = ModuleScope("TestParent")

    val moduleScope = ModuleScope("TestModule", parentScope.coroutineContext)
    val moduleScope2 = ModuleScope("TestModule2", parentScope.coroutineContext)

    parentScope.launch {
      logger.atInfo().log { "Running... Parent Scope ${this.coroutineContext}" }
      logger.atInfo().log { "Parent Children: ${parentScope.parentJob.children.joinToString(" | ")}" }
      delay(500)
    }

    moduleScope.launch {
      delay(200)
    }

    moduleScope2.launch {
      delay(200)
    }

    assertEquals(3, parentScope.parentJob.children.toList().size)
  }
}
