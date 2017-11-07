package com.lucidchart.android.javascript

trait JsParameterValue

trait JsParameter[A] { self =>
  def asJsString(a: A): String

  def contramap[B](f: B => A): JsParameter[B] = new JsParameter[B] {
    def asJsString(a: B): String = self.asJsString(f(a))
  }
}

object JsParameter {

  def apply[A](f: A => String): JsParameter[A] = {
    new JsParameter[A] {
      def asJsString(a: A) = f(a)
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

  implicit def iterable[A](implicit paramA: JsParameter[A]): JsParameter[Iterable[A]] = {
    JsParameter { iter =>
      if (iter.isEmpty) {
        "[]"
      } else {
        iter.map(paramA.asJsString).mkString("[", ",", "]")
      }
    }
  }

  implicit def array[A](implicit paramA: JsParameter[A]): JsParameter[Array[A]] =
    iterable[A](paramA).contramap(_.toIterable)
}
