package com.lucidchart.android

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
}