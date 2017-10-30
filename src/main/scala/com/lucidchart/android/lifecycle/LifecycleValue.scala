package com.lucidchart.android.lifecycle

import cats.Monad
import com.lucidchart.android._
import com.lucidchart.android.logging.Logging
import scala.annotation.tailrec

sealed trait LifecycleValue[+A] {
  def require[B](f: A => B): LifecycleValue[B]
  def option: Option[A]
}

object LifecycleValue {
  def apply[A](a: => A): LifecycleValue[A] = new SomeLifecycleValue[A](a)

  def compose[A](lv: LifecycleValue[A]): LifecycleValue[A] = lv

  implicit val monad: Monad[LifecycleValue] = new Monad[LifecycleValue] {
    def pure[A](x: A): LifecycleValue[A] = apply(x)
    def flatMap[A, B](fa: LifecycleValue[A])(f: A => LifecycleValue[B]): LifecycleValue[B] = {
      fa match {
        case some: SomeLifecycleValue[_]   => f(some.value)
        case empty: EmptyLifecycleValue[_] => empty.emptyMap[B]
      }
    }

    @tailrec
    def tailRecM[A, B](init: A)(fn: A => LifecycleValue[Either[A, B]]): LifecycleValue[B] = {
      val result = fn(init)
      result match {
        case some: SomeLifecycleValue[_] =>
          some.value match {
            case Right(b) => new SomeLifecycleValue(b)
            case Left(a)  => tailRecM(a)(fn)
          }
        case empty: EmptyLifecycleValue[_] => empty.emptyMap[B]
      }
    }
  }
}

private class SomeLifecycleValue[A](a: => A) extends LifecycleValue[A] {
  private var finalResult: A = null.asInstanceOf[A]

  private[lifecycle] def value: A =
    if (finalResult == null) {
      finalResult = a
      finalResult
    } else {
      finalResult
    }

  def require[B](f: A => B): LifecycleValue[B] = {
    val result = f(value)
    new SomeLifecycleValue(result)
  }

  def option: Option[A] = Some(value)
}

abstract class EmptyLifecycleValue[+A](lifecycle: Lifecycles.Value, debug: Boolean) extends LifecycleValue[A] with Logging { self =>
  private[lifecycle] def emptyMap[B]: LifecycleValue[B] = {
    val err = new LifecycleError(lifecycle)
    error(s"Attempted to access a lifecycle bound value before $lifecycle ran", err)
    if (debug) {
      throw err
    } else {
      new EmptyLifecycleValue[B](lifecycle, debug) {
        def debug(msg: String, e: Throwable = null): Unit = self.debug(msg, e)
        def error(msg: String, e: Throwable = null): Unit = self.error(msg, e)
        def info(msg: String, e: Throwable = null): Unit = self.info(msg, e)
        def verbose(msg: String, e: Throwable = null): Unit = self.verbose(msg, e)
        def warn(msg: String, e: Throwable = null): Unit = self.warn(msg, e)
        def wtf(msg: String, e: Throwable = null): Unit = self.wtf(msg, e)
      }
    }
  }

  def require[B](f: A => B): LifecycleValue[B] = {
    emptyMap[B]
  }

  def option: Option[A] = None
}
