package com.lucidchart.android.lifecycle

import com.lucidchart.android.logging.{DefaultLogTag, LogTag, Logger}

trait MockLogging extends DefaultLogTag {
  implicit protected val logger: Logger = MockLogger
}

object MockLogger extends Logger {
  override def debug(msg: String, e: Throwable)(implicit logTag: LogTag): Unit = {}
  override def error(msg: String, e: Throwable)(implicit logTag: LogTag): Unit = {}
  override def info(msg: String, e: Throwable)(implicit logTag: LogTag): Unit = {}
  override def verbose(msg: String, e: Throwable)(implicit logTag: LogTag): Unit = {}
  override def warn(msg: String, e: Throwable)(implicit logTag: LogTag): Unit = {}
  override def wtf(msg: String, e: Throwable)(implicit logTag: LogTag): Unit = {}
}
