package com.lucidchart.android

import io.estatico.newtype._
import scala.language.implicitConversions

package object javascript {

  type JsInterpolatedString = JsInterpolatedString.Type
  object JsInterpolatedString extends NewSubType.Default[String]

  implicit class JsParameterStringContext(val stringContext: StringContext) extends AnyVal {
    def js(args: JsParameterValue*): JsInterpolatedString = {
      val parts = stringContext.parts
      val builder = new StringBuilder()
      parts.zip(args).foreach { case (part, arg) =>
        builder ++= part
        builder ++= arg.toString
      }
      builder ++= parts.last
      JsInterpolatedString(builder.toString)
    }
  }

  implicit def toJsParameterValue[A: JsParameter](a: A): JsParameterValue = {
    new JsParameterValue {
      override def toString: String = implicitly[JsParameter[A]].asJsString(a)
    }
  }

}
