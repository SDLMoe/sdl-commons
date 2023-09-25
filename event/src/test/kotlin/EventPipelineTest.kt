package moe.sdl.commons.event

import kotlinx.coroutines.*
import moe.sdl.commons.coroutines.ModuleScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
class EventPipelineTest {
  class TestEvent1 : Event()
  class TestEvent2 : Event()
  class TestEvent3 : CancellableEvent()

  private val list = listOf(TestEvent1(), TestEvent2(), TestEvent3(), TestEvent1(), TestEvent2(), TestEvent3())

  @Test
  fun `simple test`() = runBlocking {
    val events = EventManager()
    val boolean = AtomicBoolean(false)
    events.register {
      boolean.set(true)
    }
    events.broadcast(TestEvent1())
    assert(boolean.get())
  }

  @Test
  fun `event intercept test`() = runBlocking {
    val events = EventManager()
    events.register(EventPriority.LOW) {
      error("unreachable code")
    }
    events.registerBlocking {
      it.intercept()
    }

    list.forEach { events.broadcast(it) }
  }

  @Test
  fun `event cancel test`() = runBlocking {
    val events = EventManager()
    events.registerBlocking {
      if (it is CancellableEvent) {
        it.cancel()
      }
    }

    list.forEach {
      val status = events.broadcast(it)
      if (it is CancellableEvent) {
        assertEquals(true, status)
      }
    }
  }

  @Test
  fun `high volume test`(): Unit = runBlocking {
    val events = EventManager()
    events.register(EventPriority.LOW) {
      error("unreachable code")
    }

    val counter = AtomicInteger(0)

    events.registerBlocking {
      it.intercept()
      counter.getAndIncrement()
    }

    events.registerBlocking(EventPriority.HIGHEST) {
      if (it is CancellableEvent) {
        it.cancel()
        counter.getAndIncrement()
      }
    }

    (1..100).toList().map {
      launch {
        repeat(10) {
          val event = TestEvent3()
          val status = events.broadcast(event)
          counter.getAndIncrement()
          assertEquals(true, status)
        }
      }
    }.joinAll()

    assertEquals(3 * 100 * 10, counter.get())
  }

  @Test
  fun `exception test`(): Unit = runBlocking {
    val runCount = AtomicInteger(0)
    val errCount = AtomicInteger(0)

    val events = EventManager(
      ModuleScope(
        moduleName = "EventManager",
        exceptionHandler = { _, e, logger, moduleName ->
          errCount.getAndIncrement()
          logger.atError().setCause(e).log("Caught Exception on $moduleName")
        },
      ),
    )

    events.registerBlocking {
      delay(10)
      throw IllegalStateException()
    }

    events.register(EventPriority.LOWEST) {
      delay(30)
      println(runCount.incrementAndGet())
    }

    events.registerBlocking(EventPriority.HIGHEST) {
      delay(20)
      throw NullPointerException()
    }

    events.registerBlocking(EventPriority.LOWEST) {
      delay(30)
      println(runCount.incrementAndGet())
    }

    events.broadcast(TestEvent1())

    delay(60)
    assertEquals(2, runCount.get())
    assertEquals(2, errCount.get())
  }

  @Test
  fun `next event test`(): Unit = runBlocking {
    val events = EventManager()
    val job = launch {
      withTimeoutOrNull(50) {
        events.nextEvent<TestEvent2>()
      }
      val event3 = withTimeoutOrNull(50) {
        events.nextEvent<TestEvent3> { it.isCancelled }
      }
      assertEquals(event3, null)
    }

    events.registerBlocking(EventPriority.HIGH) {
      if (it is CancellableEvent) {
        it.cancel()
      }
    }

    events.broadcast(TestEvent3())

    job.join()
  }
}
