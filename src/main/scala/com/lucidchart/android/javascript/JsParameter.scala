package com.lucidchart.android.javascript

trait JsParameterValue {
  def stringRepresentation: StringRepresentation
}

trait JsParameter[A] { self =>
  def asJsString(a: A): StringRepresentation

  def contramap[B](f: B => A): JsParameter[B] = new JsParameter[B] {
    def asJsString(a: B): StringRepresentation = self.asJsString(f(a))
  }
}

object JsParameter {

  def apply[A](f: A => String): JsParameter[A] = {
    new JsParameter[A] {
      def asJsString(a: A): StringRepresentation = DefaultStringRepresentation(f(a))
    }
  }

  implicit val jsInterpolatedString: JsParameter[JsInterpolatedString] = JsParameter(identity)

  implicit val string: JsParameter[String] = JsParameter { str =>
    "\"" + str + "\""
  }

  implicit val int: JsParameter[Int] = JsParameter(_.toString)

  implicit val long: JsParameter[Long] = JsParameter(_.toString)

  implicit val float: JsParameter[Float] = JsParameter(_.toString)

  implicit val double: JsParameter[Double] = JsParameter(_.toString)

  implicit val bool: JsParameter[Boolean] = JsParameter(_.toString)

  implicit val bigInt: JsParameter[BigInt] = JsParameter(_.toString)

  implicit val bigDecimal: JsParameter[BigDecimal] = JsParameter(_.toString)

  implicit def iterable[A](implicit paramA: JsParameter[A]): JsParameter[Iterable[A]] = {
    JsParameter { iter =>
      iter.map(paramA.asJsString).mkString("[", ",", "]")
    }
  }

  implicit def array[A](implicit paramA: JsParameter[A]): JsParameter[Array[A]] =
    iterable[A](paramA).contramap(_.toIterable)
}

trait StringRepresentation {
  def length: Int
  def appendToBuilder(stringBuilder: StringBuilder): Unit
}

case class DefaultStringRepresentation(str: String) extends StringRepresentation {
  override def length: Int = str.length
  override def appendToBuilder(stringBuilder: StringBuilder): Unit = {
    stringBuilder ++= str
  }
}
