package com.lucidchart.android.logging

import android.util.Log

trait DefaultLogTag {
  implicit protected val logTag = LogTag(this.getClass.getName)
}

trait DefaultAndroidLogging extends DefaultLogTag {
  implicit protected val logger = new DefaultAndroidLogger()
}

trait Logger extends Any {
  def debug(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit
  def error(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit
  def info(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit
  def verbose(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit
  def warn(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit
  def wtf(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit
}

class DefaultAndroidLogger extends Logger {

  override def debug(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit = {
    Log.d(logTag, msg, e)
  }

  def error(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit = {
    Log.e(logTag, msg, e)
  }

  def info(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit = {
    Log.i(logTag, msg, e)
  }

  def verbose(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit = {
    Log.v(logTag, msg, e)
  }

  def warn(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit = {
    Log.w(logTag, msg, e)
  }

  def wtf(msg: String, e: Throwable = null)(implicit logTag: LogTag): Unit = {
    Log.wtf(logTag, msg, e)
  }

}

