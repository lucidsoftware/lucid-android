package com.lucidchart.android.javascript

import org.specs2.mutable.Specification

class IterableStringRepresentationSpec extends Specification {

  "IterableStringRepresentation" should {

    "calculate the length correctly when there are no strings" in {
      val stringList = List()
      val iterableStringRepresentation = IterableStringRepresentation(stringList)
      iterableStringRepresentation.length mustEqual "[]".length
    }

    "calculate the length correctly when there is one string" in {
      val stringList = List(DefaultStringRepresentation("test"))
      val iterableStringRepresentation = IterableStringRepresentation(stringList)
      iterableStringRepresentation.length mustEqual "[test]".length
    }

    "calculate the length correctly when there are two strings" in {
      val stringList = List(DefaultStringRepresentation("test"), DefaultStringRepresentation("a"))
      val iterableStringRepresentation = IterableStringRepresentation(stringList)
      iterableStringRepresentation.length mustEqual "[test,a]".length
    }

    "calculate the length correctly when there are multiple strings" in {
      val stringList =
        List(DefaultStringRepresentation("a"), DefaultStringRepresentation("b"), DefaultStringRepresentation("c"))
      val iterableStringRepresentation = IterableStringRepresentation(stringList)
      iterableStringRepresentation.length mustEqual "[a,b,c]".length
    }
  }

}
