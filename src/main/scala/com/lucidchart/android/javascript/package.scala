package com.lucidchart.android

import io.estatico.newtype._
import scala.language.implicitConversions

package object javascript {

  type JsInterpolatedString = JsInterpolatedString.Type
  object JsInterpolatedString extends NewSubType.Default[String]

  implicit class JsParameterStringContext(val stringContext: StringContext) extends AnyVal {
    def js(args: JsParameterValue*): JsInterpolatedString = {
      val parts = stringContext.parts
      val stringArgs = args.map(_.toString)
      val partsLength = parts.foldLeft(0) { (combined, current) =>
        combined + current.length
      }
      val argsLength = stringArgs.foldLeft(0) { (combined, current) =>
        combined + current.length
      }
      val builder = new StringBuilder(partsLength + argsLength)
      parts.zip(stringArgs).foreach { case (part, arg) =>
        builder ++= part
        builder ++= arg
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
