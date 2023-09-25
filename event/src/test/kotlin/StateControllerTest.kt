package moe.sdl.commons.event

import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import moe.sdl.commons.coroutines.ModuleScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StateControllerTest {

  interface ClassA : WithState<ClassA.State> {

    fun foobar(): String

    enum class State {
      START,
      DOING,
      END,
    }
  }

  interface ClassB : WithState<ClassB.State> {

    fun barfoo(): String

    enum class State {
      HAPPY,
      BAD,
      CRY,
    }
  }

  class Impl {

    val count = AtomicInteger()

    val stateController = InitStateController(
      scope = ModuleScope("TestScopeWithState"),
      parentStateClass = this,
      Start(),
      Doing(),
      End(),
    )

    val stateController2 = InitStateController(
      scope = ModuleScope("TestScopeWithState2"),
      parentStateClass = this,
      Happy,
      Bad,
      Cry,
    )

    fun foobar(): String {
      return stateController.getStateInstance().foobar()
    }

    fun barfoo(): String {
      return stateController2.getStateInstance().barfoo()
    }

    object Happy : ClassB {

      override val state: ClassB.State = ClassB.State.HAPPY

      override suspend fun startState() {
//        println("Start Happy!")
      }

      override fun barfoo(): String = "Happy"
    }

    object Bad : ClassB {

      override val state: ClassB.State = ClassB.State.BAD

      override fun barfoo(): String = "Bad"
    }

    object Cry : ClassB {

      override val state: ClassB.State = ClassB.State.CRY

      override fun barfoo(): String = "Cry"
    }

    inner class Start : ClassA {

      override val state: ClassA.State = ClassA.State.START

      override fun foobar(): String {
        return "Start!"
      }

      override suspend fun endState() {
        count.getAndIncrement()
        println("Start!")
      }
    }

    inner class Doing : ClassA {

      override val state: ClassA.State = ClassA.State.DOING

      override fun foobar(): String {
        return "Doing!"
      }

      override suspend fun startState() {
        count.getAndIncrement()
        println("Doing!")
      }

      override suspend fun endState() {
        count.getAndIncrement()
        println("Done!")
      }
    }

    inner class End : ClassA {

      override val state: ClassA.State = ClassA.State.END

      override fun foobar(): String {
        return "End!"
      }

      override suspend fun startState() {
        count.getAndIncrement()
        println("End!")
      }
    }
  }

  @Test
  fun `state controller test`() = runTest {
    val sc = Impl()

    sc.stateController.init()
    sc.stateController2.init()

    sc.stateController.observeStateChange(StateController.ListenerState.AFTER_UPDATE) { before, after ->
      println("Count: ${count.get()}, before: $before, after: $after")
    }

    sc.stateController.intercept(StateController.ListenerState.BEFORE_UPDATE) {
      count.get() >= 4
    }

    sc.stateController.block {
      throw NullPointerException()
      // throw
    }

    assertEquals("Start!", sc.foobar())
    sc.stateController.setState(ClassA.State.DOING)
    assertEquals("Doing!", sc.foobar())
    sc.stateController.setState(ClassA.State.END)
    assertEquals("End!", sc.foobar())
    sc.stateController.setState(ClassA.State.START) // Fail, will be intercepted
    assertEquals("End!", sc.foobar())

    assertEquals("Happy", sc.barfoo())
    sc.stateController2.setState(ClassB.State.BAD)
    assertEquals("Bad", sc.barfoo())
    sc.stateController2.setState(ClassB.State.CRY)
    assertEquals("Cry", sc.barfoo())
  }

  @Test
  fun `high volume test`() = runTest {
    val testCount = 1000

    val sc = Impl()
    sc.stateController.init()
    (1..testCount).map {
      launch {
        sc.stateController.setState(ClassA.State.START)
        assertEquals("Start!", sc.foobar())
        sc.stateController.setState(ClassA.State.DOING)
        assertEquals("Doing!", sc.foobar())
        sc.stateController.setState(ClassA.State.END)
        assertEquals("End!", sc.foobar())
      }
    }.joinAll()

    assertEquals(testCount * 4 + 1, sc.count.get())

    val sc2 = Impl()
    sc2.stateController2.init()
    sc2.stateController.block {
      delay((0..200L).random())
    }
    (1..testCount).map {
      launch {
        sc2.stateController.setState(ClassA.State.START)
        assertEquals("Start!", sc2.foobar())
        sc2.stateController.setState(ClassA.State.DOING)
        assertEquals("Doing!", sc2.foobar())
        sc2.stateController.setState(ClassA.State.END)
        assertEquals("End!", sc2.foobar())
      }
    }.joinAll()
  }
}
