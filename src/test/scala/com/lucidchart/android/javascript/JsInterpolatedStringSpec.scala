package com.lucidchart.android.javascript

import org.specs2.mutable.Specification

class JsInterpolatedStringSpec extends Specification {

  "JsInterpolatedString" should {

    "create a js string with a string variable" in {
      val testString = "Hello"
      js"call($testString)" mustEqual "call(\"Hello\")"
    }

    "create a js string with number variables" in {
      val intValue: Int = 1
      val longValue: Long = 2L
      val floatValue: Float = 3.5f
      val doubleValue: Double = 4.5
      js"test($intValue, $longValue, $floatValue, $doubleValue)" mustEqual "test(1, 2, 3.5, 4.5)"
    }

    "create a js string with a boolean variable" in {
      val booleanValue = true
      js"test($booleanValue)" mustEqual "test(true)"
    }

    "create a js string with a big int and big double" in {
      val bigIntValue = BigInt(1000000)
      val bigDecimal = BigDecimal(1000000.5)
      js"test($bigIntValue, $bigDecimal)" mustEqual "test(1000000, 1000000.5)"
    }

    "create a js string with an empty iterable" in {
      val iterable: Iterable[Int] = List()
      js"iterable($iterable)" mustEqual "iterable([])"
    }

    "create a js string with an iterable with one item" in {
      val iterable: Iterable[Int] = List(4)
      js"iterable($iterable)" mustEqual "iterable([4])"
    }

    "create a js string with an iterable with two items" in {
      val iterable: Iterable[Int] = List(4,3)
      js"iterable($iterable)" mustEqual "iterable([4,3])"
    }


    "create a js string with an iterable with multiple items" in {
      val iterable: Iterable[Int] = List(4, 3, 2, 1)
      js"iterable($iterable)" mustEqual "iterable([4,3,2,1])"
    }

    "create a js string with an array" in {
      val arrayOfStrings: Array[String] = Array("hello", "world", "testing")
      js"testStrings($arrayOfStrings)" mustEqual "testStrings([\"hello\",\"world\",\"testing\"])"
    }

    "create a js string with a custom string representation" in {
      import CustomStringRepresentation._
      val tuple2 = (5, 4)
      js"testTuple($tuple2)" mustEqual "testTuple(9)"
    }

  }

}

case class CustomStringRepresentation(num1: Int, num2: Int) extends StringRepresentation {
  private val resultString = (num1 + num2).toString
  override def length: Int = resultString.length
  override def appendToBuilder(stringBuilder: StringBuilder): Unit = {
    stringBuilder ++= resultString
  }
}

object CustomStringRepresentation {
  implicit val jsParam: JsParameter[(Int, Int)] = new JsParameter[(Int, Int)] {
    override def asStringRepresentation(numbers: (Int, Int)): StringRepresentation = CustomStringRepresentation(numbers._1, numbers._2)
  }
}


