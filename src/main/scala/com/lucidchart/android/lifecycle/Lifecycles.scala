package com.lucidchart.android.lifecycle

import cats.Eq
import cats.instances.string._
import cats.syntax.eq._

object Lifecycles extends Enumeration {
  // Fragments & Activities
  val OnCreate = Value("onCreate")
  val OnCreateOptionsMenu = Value("onCreateOptionsMenu")
  val OnStart = Value("onStart")
  val OnResume = Value("onResume")

  // Fragments
  val OnAttach = Value("onAttach")
  val OnActivityCreated = Value("onActivityCreated")
  val OnCreateView = Value("onCreateView")
  val OnViewStateRestored = Value("onViewStateRestored")

  // Dialog Fragment
  val OnCreateDialog = Value("onCreateDialog")

  def exists(name: String): Boolean = values.exists(_.toString === name)

  implicit val eq: Eq[Value] = Eq.fromUniversalEquals[Value]
}
