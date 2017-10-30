package com.lucidchart.android.lifecycle

import cats.Eq
import cats.instances.string._
import cats.syntax.eq._

object Lifecycles extends Enumeration {
  val OnCreate = Value("onCreate")
  val OnCreateOptionsMenu = Value("onCreateOptionsMenu")

  def exists(name: String): Boolean = values.exists(_.toString === name)

  implicit val eq: Eq[Value] = Eq.fromUniversalEquals[Value]
}
