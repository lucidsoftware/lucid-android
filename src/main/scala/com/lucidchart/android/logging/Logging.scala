package com.lucidchart.android.logging

import android.util.Log

trait DefaultLogging extends DefaultUniversalLogging {

  override protected val logTag = this.getClass.getName

}

trait DefaultUniversalLogging extends Any {

  protected def logTag: String

  def debug(msg: String, e: Throwable = null): Unit = {
    Log.d(logTag, msg, e)
  }

  def error(msg: String, e: Throwable = null): Unit = {
    Log.e(logTag, msg, e)
  }

  def info(msg: String, e: Throwable = null): Unit = {
    Log.i(logTag, msg, e)
  }

  def verbose(msg: String, e: Throwable = null): Unit = {
    Log.v(logTag, msg, e)
  }

  def warn(msg: String, e: Throwable = null): Unit = {
    Log.w(logTag, msg, e)
  }

  def wtf(msg: String, e: Throwable = null): Unit = {
    Log.wtf(logTag, msg, e)
  }

}

trait Logging extends Any {
  def debug(msg: String, e: Throwable = null): Unit
  def error(msg: String, e: Throwable = null): Unit
  def info(msg: String, e: Throwable = null): Unit
  def verbose(msg: String, e: Throwable = null): Unit
  def warn(msg: String, e: Throwable = null): Unit
  def wtf(msg: String, e: Throwable = null): Unit
}
