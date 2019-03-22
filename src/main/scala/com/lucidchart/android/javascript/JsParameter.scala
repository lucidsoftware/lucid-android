package com.lucidchart.android.javascript

trait JsParameterValue {
  def stringRepresentation: StringRepresentation
}

trait JsParameter[A] { self =>
  def asStringRepresentation(a: A): StringRepresentation

  def contramap[B](f: B => A): JsParameter[B] = new JsParameter[B] {
    def asStringRepresentation(a: B): StringRepresentation = self.asStringRepresentation(f(a))
  }
}

object JsParameter {

  def apply[A](f: A => String): JsParameter[A] = {
    new JsParameter[A] {
      def asStringRepresentation(a: A): StringRepresentation = DefaultStringRepresentation(f(a))
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
    new JsParameter[Iterable[A]] {
      def asStringRepresentation(iter: Iterable[A]): StringRepresentation = {
        IterableStringRepresentation(iter.map(paramA.asStringRepresentation))
      }
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

case class IterableStringRepresentation(stringRepresentations: Iterable[StringRepresentation]) extends StringRepresentation {
  override def length: Int = {
    if (stringRepresentations.isEmpty) {
      2
    } else {
      stringRepresentations.foldLeft(1) {
        case (acc, strRep) => acc + 1 + strRep.length
      }
    }
  }

  override def appendToBuilder(stringBuilder: StringBuilder): Unit = {
    stringBuilder.append("[")
    stringRepresentations.headOption.foreach(_.appendToBuilder(stringBuilder))
    stringRepresentations.drop(1).foreach { param =>
      stringBuilder.append(",")
      param.appendToBuilder(stringBuilder)
    }
    stringBuilder.append("]")
  }

}
