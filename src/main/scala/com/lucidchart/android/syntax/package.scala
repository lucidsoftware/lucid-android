package com.lucidchart.android

import android.view.View

package object syntax {

  implicit class ExtendedBoolean(val b: Boolean) extends AnyVal {
    def option[A](trueValue: => A): Option[A] = {
      if (b) {
        Some(trueValue)
      } else {
        None
      }
    }
  }

  implicit class ExtendedView[V <: View](val view: V) extends AnyVal with ViewSyntax[V]
}
