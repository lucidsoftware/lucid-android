package com.lucidchart.android

import io.estatico.newtype.NewSubType

package object logging {
  type LogTag = LogTag.Type
  object LogTag extends NewSubType.Default[String]
}
