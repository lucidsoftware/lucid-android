# lucid-android

[![Build Status](https://travis-ci.org/lucidsoftware/lucid-android.svg)](https://travis-ci.org/lucidsoftware/lucid-android)

Lucid Android is designed to consolidate scala helpers for developing Android applications.

Right now there are two features of the library.

1. A logging trait
2. A lifecycle management macro

## Logging

In Android when you want to log something like an error, you have to provide a tag for the log statement. Lucid Android provides `com.lucidchart.android.logging.DefaultLogging` to simplify this process. It is fairly common to use the current class name when tagging logs. If you extends `DefaultLogging` this is done automatically for you. For example:

    class MainActivity extends Activity with DefaultLogging {
      override def onCreate(state: Bundle): Unit = {
        debug("a simple debug statement")

        ...
      }
    }

New methods are introduced to facilitate this feature. The mapping from Android's `Log` methods to `DefaultLogging` is as follows:

`Log.d` -> `debug`

`Log.e` -> `error`

`Log.i` -> `info`

`Log.v` -> `verbose`

`Log.w` -> `warn`

`Log.wtf` -> `wtf`

Luicd Android also provides `DefaultUniversalLogging` for instances where you want to use these methods with [value classes and universal traits](https://docs.scala-lang.org/overviews/core/value-classes.html).

You can extend either of these traits to provide custom logging behavior (like sending logs to a server).

The `Logging` trait is also available if you want to provide custom implementations of all the logging methods.

## Lifecycle Mangement

A common pattern in Android is to create vars that get initialized during a particular lifecycle method:

    class MainActivity extends Activity {

      var prefs: SharedPreferences = null

      override def onCreate(state: Bundle): Unit = {
        prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)

        ...
      }
    }

The downside here is that if you access `prefs` at the wrong time, you will get a `NullPointerException`. For `onCreate` that might not be so much of an issue, but for other lifecycle methods like `onActivityCreated` (for fragments) or `onResume` you have to be even more careful about when you access the value. You are also introducing mutable data. This is generally discouraged in scala.

The `@com.lucidchart.android.lifecycle.LifecycleManaged` annotation helps address this by allowing you to annotate top-level members with the lifecycle method they should be initialized in. A new `com.lucidchart.android.lifecycle.LifecycleValue` typed is introduced to help with debugging and provide some guarantees around access lifecycle managed members. The above example becomes:

    @LifecycleManaged
    class MainActivity extends Activity {

      @onCreate
      val prefs = LifecycleValue[SharedPreferences](getSharedPreferences("prefs", Context.MODE_PRIVATE))

    }

Since the type of `prefs` is `LifecycleValue[SharedPreferences]` you have to "unwrap" the value to access it in a way that isn't too different from an `Option`:

    prefs.require { p =>
      // p is the SharedPreferences here
    }

`LifecycleValue.require` is analogous to `Option.map`. By default, when the value hasn't been initialized an error (with a stacktrace) is logged and the body of the `require` call is skipped.

When creating a lifecycle value, sometimes you will have to reference other lifecycle values. To facilitate this kind of composition, a `cats.Monad` instance is provided for `LifecycleValue`. Operations like `map` and `flatMap` demonstrate the same behavior as `require` (`map` is identical, in fact) but have the type semantics that you would except from those methods.

Values can be composed in a few different ways:

    @onCreate
    val prefs = LifecycleValue[SharedPreferences](getSharedPreferences("prefs", Context.MODE_PRIVATE))

    // import cats.syntax.functor._
    @onCreate
    val settings: LifecycleValue[CustomSettings] = pref.map(new CustomSettings(_))

    // import cats.syntax.apply._
    @onCreate
    val unrelatedDeps: LifecycleValue[HasDependencies] = (prefs, settings).mapN((p, s) => new HasDependencies(p, s))

    // import cats.syntax.functor._
    // import cats.syntax.flatMap._
    @onCreate
    val multipleDeps2: LifecycleValue[HasOtherDeps] = for {
      prefs <- prefs
      settings <- LifecycleValue(new CustomSettings(prefs))
    } yield new HasDependencies(prefs, settings)

It's important to take care when mixing lifecycles that you only depend on data from lifecycles that are the same as or happen earlier than the current lifecycle. You wouldn't want an `@onCreate` value depending on an `@onStart` value but the other way around is okay.

You can also customize some behavior of `@LifecycleManaged`. It currently supports two options. You can specify a custom implementation of the `Logging` trait with the `loggerTrait` parameter and you can specify the `debug` parameter:

    @LifecycleManaged(
      loggerTrait = "com.sample.CustomLogging",
      debug = true
    )

When `debug` is set to true (default is false), the app will crash when a `LifecycleValue` is accessed before that lifecycle has been hit. It is common to set this value to `BuildConfig.DEBUG`:

    @LifecycleManaged(
      debug = BuildConfig.DEBUG
    )

A crash is often much easier to notice in development than an error log but you wouldn't necessarily want the user to experience the crash in cases where it accidentally leaks into production.

## Usage

    // build.sbt
    libraryDependencies += "com.lucidchart" %% "lucid-android" % "0.1.0"

    // if you are using @LifecycleManaged
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)