package com.lucidchart.lifecycle

import com.lucidchart.android.lifecycle.{EmptyLifecycleValue, LifecycleManaged, LifecycleValue, Lifecycles}
import org.specs2.mutable.Specification
import shapeless.test.illTyped

class LifecycleManagedSpec extends Specification {

  "Lifecycle managed" should {

    "Give a compiler error for a lifecycle annotated value that isn't initialized" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         @onStart
         val testValue: EmptyLifecycleValue[Boolean] = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging
        }
      """, "Lifecycle method onStart was never overridden despite annotated empty lifecycle value: testValue")
      ok
    }

    "Give a compiler error for a lifecycle annotated value that isn't initialized but the method it should be initialized in is overridden" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         @onStart
         val testValue:EmptyLifecycleValue[Boolean] = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         override def onStart(): Unit = {}
        }
      """, "Value was never initialized with @initLifecycleValue in onStart")
      ok
    }

    "Give a compiler error for an uninitialized empty lifecycle even when the lifecycle value is not defined directly" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         val x = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         @onStart
         val testValue: EmptyLifecycleValue[Boolean] = x

         override def onStart(): Unit = {}
        }
      """, "Value was never initialized with @initLifecycleValue in onStart")
      ok
    }

    "Give a compiler error for a lifecycle initialization that doesn't specify which lifecycle value should be initialized" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         @onStart
         val testValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         override def onStart(): Unit = {
          @initLifecycleValue
          val overridenValue = true
         }
        }
      """, "Lifecycle variable name not specified")
      ok
    }

    "Give a compiler error for a lifecycle initialization that specifies a non lifecycle value" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         val other = false

         @onStart
         val testValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         override def onStart(): Unit = {
          @initLifecycleValue(other)
          val overridenValue = true
         }
        }
      """, "Lifecycle variable name does not match any uninitialized empty lifecycle values for onStart: testValue")
      ok
    }

    "Give a compiler error for a lifecycle initialization that specifies a lifecycle value from a different lifecycle" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         val other = false

         @onStart
         val testValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         @onResume
         val viewValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         override def onStart(): Unit = {
          @initLifecycleValue(viewValue)
          val overridenValue = true
         }
        }
      """, "Lifecycle variable name does not match any uninitialized empty lifecycle values for onStart: testValue")
      ok
    }

    "Give a compiler error for a lifecycle initialization that specifies multiple values" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         val other = false

         @onStart
         val testValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         @onStart
         val viewValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         override def onStart(): Unit = {
          @initLifecycleValue(testValue, viewValue)
          val overridenValue = true
         }
        }
      """, "More than 1 variable name specified")
      ok
    }

    "Give a compiler error for a lifecycle initialization that is initialized multiple times" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         val other = false

         @onStart
         val testValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true) with MockLogging

         override def onStart(): Unit = {
          @initLifecycleValue(testValue)
          val overridenValue = true

          @initLifecycleValue(testValue)
          val secondOverride = false
         }
        }
      """, "Lifecycle variable name does not match any uninitialized empty lifecycle values for onStart: testValue")
      ok
    }

    "Give a compiler error for a lifecycle annotation applied to something other than a val" in {
      illTyped(
        """
        @LifecycleManaged(true)
        class Test extends LifecycleMethods with MockLogging {
         val other = false

         @onResume
         var testValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnResume, true) with MockLogging
        }
      """, "Lifecycle annotations can only be used on vals")
      ok
    }


    "Correctly initialize an empty lifecycle value where annotated" in {
      @LifecycleManaged(true)
      class Test extends LifecycleMethods with MockLogging {
        @onStart
        val testValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true)

        @onStart
        val otherValue = new EmptyLifecycleValue[Boolean](Lifecycles.OnStart, true)

        @onResume
        val viewValue = new EmptyLifecycleValue[Int](Lifecycles.OnResume, true)

        override def onStart(): Unit = {
          @initLifecycleValue(testValue)
          val test = true

          @initLifecycleValue(otherValue)
          val otherVal = false
        }

        override def onResume(): Unit = {
          @initLifecycleValue(viewValue)
          val view = 33
        }
      }
      val testInstance = new Test()
      testInstance.testValue.option must beNone
      testInstance.otherValue.option must beNone
      testInstance.onStart()
      testInstance.testValue.option must beSome
      testInstance.otherValue.option must beSome
      testInstance.testValue.require(_ must beTrue)
      testInstance.otherValue.require(_ must beFalse)

      testInstance.viewValue.option must beNone
      testInstance.onResume()
      testInstance.viewValue.option must beSome
      testInstance.viewValue.require(_ mustEqual 33)
      ok
    }


    "Correctly add bodies of lifecycle value constructors to lifecycle methods" in {
      @LifecycleManaged(true)
      class Test extends LifecycleMethods with MockLogging {
        @onStart
        val testValue = LifecycleValue[Int] {
          1 + 1
        }

        @onStart
        val otherValue = LifecycleValue[Boolean] {
          true
        }

        @onResume
        val resumeValue = LifecycleValue[Double] {
          12.5
        }

        var testVariable = false

        override def onResume(): Unit = {
          otherValue.require { otherValue =>
            testVariable = otherValue
          }
        }
      }

      val testInstance = new Test()
      testInstance.testValue.option must beNone
      testInstance.otherValue.option must beNone
      testInstance.onStart()
      testInstance.testValue.option must beSome
      testInstance.otherValue.option must beSome
      testInstance.testValue.require(_ mustEqual 2)
      testInstance.otherValue.require(_ must beTrue)

      testInstance.resumeValue.option must beNone
      testInstance.onResume()
      testInstance.resumeValue.option must beSome
      testInstance.resumeValue.require(_ mustEqual 12.5)
      testInstance.testVariable must beTrue
    }

  }

}
