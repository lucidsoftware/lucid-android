package com.lucidchart.lifecycle

trait MockLogging {
  def debug(msg: String, e: Throwable = null): Unit = {}
  def error(msg: String, e: Throwable = null): Unit = {}
  def info(msg: String, e: Throwable = null): Unit = {}
  def verbose(msg: String, e: Throwable = null): Unit = {}
  def warn(msg: String, e: Throwable = null): Unit = {}
  def wtf(msg: String, e: Throwable = null): Unit = {}
}
