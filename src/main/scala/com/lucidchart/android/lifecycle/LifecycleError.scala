package com.lucidchart.android.lifecycle

class LifecycleError(lifecycle: Lifecycles.Value) extends Exception(s"Value not valid until $lifecycle")
