package com.lucidchart.android.concurrent

import android.os.AsyncTask
import scala.concurrent.ExecutionContext

object AsyncTaskExecutionContext {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

}

trait AsyncTaskExecutionContext {

  implicit val ec: ExecutionContext = AsyncTaskExecutionContext.ec

}
