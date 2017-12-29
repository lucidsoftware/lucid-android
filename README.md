# lucid-android

[![Build Status](https://travis-ci.org/lucidsoftware/lucid-android.svg)](https://travis-ci.org/lucidsoftware/lucid-android)

Lucid Android is designed to consolidate scala helpers for developing Android applications.

The following features are available:

1. A logging trait
2. A lifecycle management macro
3. Android-centric ExecutionContexts
4. View event handlers
5. Javascript execution helpers

## Logging

In Android when you want to log something like an error, you have to provide a tag for the log statement. Lucid Android provides `com.lucidchart.android.logging.DefaultLogging` to simplify this process. It is fairly common to use the current class name when tagging logs. If you extend `DefaultLogging` this is done automatically for you. For example:

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

Lucid Android also provides `DefaultUniversalLogging` for instances where you want to use these methods with [value classes and universal traits](https://docs.scala-lang.org/overviews/core/value-classes.html).

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

The `@com.lucidchart.android.lifecycle.LifecycleManaged` annotation helps address this by allowing you to annotate top-level members with the lifecycle method they should be initialized in. A new `com.lucidchart.android.lifecycle.LifecycleValue` type is introduced to help with debugging and provide some guarantees around accessing lifecycle managed members. The above example becomes:

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

Unfortunately, it's sometimes necessary to return the value you want to store in a `LifecycleValue` in one of the lifecycle methods. For instance, it's common to store a reference to the views from `onCreateView`. However, accessing a `LifecycleValue`'s wrapped value as a return value in that method would essentially require using `option.get`, an anti-pattern and an inconvenience. Additionally, creating the views in `onCreateView` requires the view inflater and container which are passed in as arguments to the method. A `LifecycleValue` defined externally to that method would have no way to access those arguments. For these instances you can use a different version of the `LifecycleManaged` macro.

    @LifecycleManaged
    class ExampleFragment extends Fragment {
      @onCreateView
      val view = new EmptyLifecycleValue[View](Lifecycles.OnCreateView, Build.DEBUG) with Logging

      override def onCreateView(inflater: LayoutInflater, container: ViewGroup, state: Bundle): View = {
        @initLifecycleValue(view)
        val rootView = inflater.inflate(R.id.example_view, container)

        rootView
      }
    }

The `initLifecycleValue` annotation allows the `LifecycleValue` to be initialized in the correct context without unnecessarily wrapping and unwrapping the value. Furthermore, the `LifecycleValue` can only be initialized once in its respective method, and it must be initialized. This allows it to be treated in the same manner as `LifecycleValue`s created directly on the class.

When creating a lifecycle value, sometimes you will have to reference other lifecycle values. To facilitate this kind of composition, a `cats.Monad` instance is provided for `LifecycleValue`. Operations like `map` and `flatMap` demonstrate the same behavior as `require` (`map` is identical, in fact) but have the type semantics that you would except from those methods.

Values can be composed in a few different ways:

    @onCreate
    val prefs = LifecycleValue[SharedPreferences](getSharedPreferences("prefs", Context.MODE_PRIVATE))

    // import cats.syntax.functor._
    @onCreate
    val settings: LifecycleValue[CustomSettings] = prefs.map(new CustomSettings(_))

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

## ExecutionContext

Typically to run something asynchronously in Android it is typical to leverage `AsyncTask`. For simple operations where you don't need fine grained control over reporting progress and want to avoid the boilerplate (and weirdness) around passing data into a call to an `AsyncTask` you probably will have a much better experience using Scala's `Future`.

You will need an `ExecutionContext` to do this successfully. Lucid Android provides two options: `AsyncTaskExecutionContext` and `UiThreadExecutionContext`. `AsyncTaskExecutionContext.ec` will run tasks on the same background thread pool that `AsyncTask` uses. `UiThreadExecutionContext` is a trait that provides an `ExecutionContext` that processes tasks on the main thread. This is useful for switching back to the UI thread when you need to update ui. Only `AsyncTaskExecutionContext.ec` is marked as implicit. This example runs `getUserDataFromNetwork()` on a background thread and then switches to the UI thread in `.onComplete`.

    import com.lucidchart.android.concurrent.UiThreadExeuctionContext
    import com.lucidchart.android.concurrent.AsyncTaskExecutionContext.ec

    class MainActiivty extends Activity with UiThreadExecutionContext with DefaultLogging {
      override def onCreate(state: Bundle): Unit = {
        super.onCreate(state)

        Future(getUserDataFromNetwork()).onComplete {
          case Success(userData) => updateUi(userData)
          case Failure(e) => error("something bad happened", e)
        }(UiThreadExeuctionContext.ec)
      }
    }

Note that `UiThreadExecutionContext` can only be mixed in to an activity that also mixes in `Logging`.

## Event handlers

All `View.setOn*Listener` methods that take an interface with a single method can be called with `View.on*` passing in an anonymous method with the same parameters as the single interface method:

    val button: Button = ???
    button.onClick { view =>
      // on click logic here
    }

## Javascript Execution Helpers

Under `import com.lucidchart.android.javascript._` a new string interpolater is introduced for safely interpolating scala values into javascript function parameters. Typically if you have a javascript function that takes a string like this:

    function jsFunction(str) { /* ... */ }

When an Android application needs to call this function passing in a Scala string you will end up with something like this:

    val value = "hello"
    webView.evaluateJavascript(s"""jsFunction("$value")""", null)

You have to manually add the quotes. Because of some weird parsing issues with Scala you are also required to use the triple quoted strings in the example above (escaping the quotes around an interpolated value doesn't work).

Lucid Android adds the `js` interpolater and the `JsParameter` type class to simplify this a little bit and make it safer at compile time. The above `evaluateJavascript` call can be rewritten as follows:

    val value = "hello"
    webView.evaluateJavascript(js"jsFunction($value)", null)

Because an implicit `JsParameter[String]` is available, the `js` interpolator can translate this call into syntactically valid javascript (adding the quotes in this case). Numbers, booleans, and collections all work correctly as well. You can also provide implict instances of `JsParameter[A]` for custom types you'd like to be able to interpolate. Collections were the original motivation for this:

    val list = List("hello", "world")
    val jsList = list.map(str => "\"" + str + "\"").mkString("[", ",", "]")
    webView.evaluateJavascript(s"jsFunction($jsList)", null)

Gets translated into simply:

    val list = List("hello", "world")
    webView.evaluateJavascript(js"jsFunction($list)", null)

The `js` interpolator returns a `JsInterpolatedString`. At compile time this is a simple wrapper for a (and subclass of) `String`. At runtime, the extra allocation of the wrapper doesn't actually happen so memory allocations are not negatively impacted when using `js`. See [scala-newtype](https://github.com/estatico/scala-newtype) for more details. You do end up with an extra allocation of a `JsParameter` when interpolating collections.

## Usage

    // build.sbt
    libraryDependencies += "com.lucidchart" %% "lucid-android" % "0.7.0"

    // if you are using @LifecycleManaged
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
