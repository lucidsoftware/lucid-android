package com.lucidchart.android

import io.estatico.newtype._
import scala.language.implicitConversions

package object javascript {

  type JsInterpolatedString = JsInterpolatedString.Type
  object JsInterpolatedString extends NewSubType.Default[String]

  implicit class JsParameterStringContext(val stringContext: StringContext) extends AnyVal {
    def js(args: JsParameterValue*): JsInterpolatedString = {
      val parts = stringContext.parts
      val partsLength = parts.foldLeft(0) { (combined, current) =>
        combined + current.length
      }
      val argsLength = args.foldLeft(0) { (combined, current) =>
        combined + current.stringRepresentation.length
      }
      val builder = new StringBuilder(partsLength + argsLength)
      parts.zip(args).foreach { case (part, arg) =>
        builder ++= part
        arg.stringRepresentation.appendToBuilder(builder)
      }
      builder ++= parts.last
      JsInterpolatedString(builder.toString)
    }
  }

  implicit def toJsParameterValue[A: JsParameter](a: A): JsParameterValue = {
    new JsParameterValue {
      val stringRepresentation: StringRepresentation = implicitly[JsParameter[A]].asStringRepresentation(a)
    }
  }

}
