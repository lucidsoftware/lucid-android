package com.lucidchart.android.concurrent

import android.app.Activity
import com.lucidchart.android.logging.DefaultAndroidLogging
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

trait UiThreadExecutionContext { self: Activity with DefaultAndroidLogging =>

  val uiExecutionContext: ExecutionContext = new ExecutionContext {

    def execute(runnable: Runnable): Unit = self.runOnUiThread(runnable)

    def reportFailure(t: Throwable): Unit = t match {
      case NonFatal(e) => logger.error("There was an error on the UI thread", e)
      case _ => throw t
    }

  }

}
