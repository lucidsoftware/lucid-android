package com.lucidchart.android.lifecycle

import cats.syntax.eq._
import com.lucidchart.android.syntax._
import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.collection.mutable
import scala.reflect.macros.whitebox.Context

@compileTimeOnly("enable macro paradise to expand macro annotations")
class LifecycleManaged extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LifecycleMacro.impl
}

class LifecycleMacro(val c: Context) {
  import c.universe._

  private val lifecyclePackage = q"com.lucidchart.android.lifecycle"

  def impl(annottees: c.Expr[Any]*): c.Expr[Any] = {

    val parameters = extractParams(c.prefix.tree)

    val inputs = annottees.map(_.tree).toList

    val results: List[Tree] = inputs match {
      case q"""
        $mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns  } with ..$parents { $self =>
          ..$members
        }
      """ :: maybeCompanion =>
        val ActionValidationResult(membersWithExpandedActions, actions) = validateActions(members, parameters)
        val ModifyLifecycleMethodsResult(finalMembers) = modifyLifecycleMethods(membersWithExpandedActions, actions)

        q"""
          $mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns  } with ..$parents { $self =>
            ..$finalMembers
          }
        """ :: maybeCompanion

      case unchanged =>
        c.error(c.enclosingPosition, "@LifecycleManaged must be used on a class")
        unchanged
    }

    c.Expr[Any](Block(results, Literal(Constant(()))))
  }

  private case class Parameters(
    loggerTrait: Option[String] = None,
    debug: Tree = q"false"
  )

  private def extractParams(paramTree: Tree): Parameters = {
    val params = paramTree match {
      case q"new LifecycleManaged(..$params)" => params
      case q"new com.lucidchart.android.lifecycle.LifecycleManaged(..$params)" => params
      case _ => Nil
    }

    params.foldLeft(Parameters()) {
      case (sofar, q"loggerTrait = $traitName") =>
        traitName match {
          case Literal(Constant(value: String)) => sofar.copy(loggerTrait = Some(value))
          case _ =>
            c.error(traitName.pos, s"Logger trait must be a string literal")
            sofar
        }

      case (sofar, q"debug = $debugExpr") => sofar.copy(debug = debugExpr)
      case (sofar, _) => sofar
    }
  }

  private def validateActions(members: List[Tree], params: Parameters): ActionValidationResult = {
    members.foldLeft(ActionValidationResult(Nil, Nil)) {
      case (result, q"..$mods val $name: $preliminaryReturnType = $body") if hasLifecycleAnnotation(mods.annotations) =>
        val returnType: Tree = if (preliminaryReturnType.isEmpty) {
          body match {
            case q"LifecycleValue[..$tpt]($rest)" =>
              tq"$lifecyclePackage.LifecycleValue[..$tpt]"
            case _ =>
              c.error(
                preliminaryReturnType.pos,
                "You must specify the return type as LifecycleValue[T] or use LifecycleValue.apply with a type parameter: LifecycleValue[T] { ... }"
              )
              preliminaryReturnType
          }
        } else {
          preliminaryReturnType
        }

        val (expandedMember, action) =
          expandMember(mods, name.toString, body, returnType, params)

        result.copy(
          members = result.members ++ expandedMember,
          actions = result.actions :+ action
        )

      case (result, member) =>
        if (hasLifecycleAnnotation(member)) {
          c.error(member.pos, "Lifecycle annotations can only be used on vals")
        }

        result.copy(members = result.members :+ member)
    }
  }

  private def hasLifecycleAnnotation(tree: Tree): Boolean = {
    val annotations = tree match {
      case ValDef(mods, _, _, _)       => mods.annotations
      case ClassDef(mods, _, _, _)     => mods.annotations
      case TypeDef(mods, _, _, _)      => mods.annotations
      case DefDef(mods, _, _, _, _, _) => mods.annotations
      case ModuleDef(mods, _, _)       => mods.annotations
      case _                           => Nil
    }

    hasLifecycleAnnotation(annotations)
  }

  private def hasLifecycleAnnotation(annotations: List[Tree]): Boolean = {
    annotations.exists(isLifecycleAnnotation)
  }

  private def isLifecycleAnnotation(annot: Tree): Boolean = {
    annot match {
      case q"new $annotationName(...$params)" =>
        Lifecycles.exists(annotationName.toString)
      case _ =>
        false
    }
  }

  private def expandMember(mods: Modifiers, name: String, body: Tree, returnType: Tree, params: Parameters): (List[Tree], Action) = {
    val syntheticName = TermName(name + "$" + c.freshName)
    val (cleanedMods, lifecycle) = extractLifecycleAnnotation(mods)
    val varMods =
      Modifiers(
        Flag.PRIVATE,
        cleanedMods.privateWithin,
        cleanedMods.annotations
      )

    val defMods = cleanedMods
    val lifecycleInstance = q"$lifecyclePackage.Lifecycles.withName(${Literal(Constant(lifecycle.toString))})"

    val loggerTrait = params.loggerTrait.map { loggerTrait =>
      tq"${TypeName(loggerTrait)}"
    }.getOrElse {
      tq"com.lucidchart.android.logging.DefaultLogging"
    }

    val unwrappedType = returnType match {
      case tq"$outer[..$inner]" => inner.head
    }

    val expanded =
      List(
        q"$varMods var $syntheticName: $returnType = new $lifecyclePackage.EmptyLifecycleValue[$unwrappedType]($lifecycleInstance, ${params.debug}) with $loggerTrait",
        q"$defMods def ${TermName(name)}: $returnType = $syntheticName"
      )

    val action = q"$syntheticName = $body"

    (expanded, Action(lifecycle, action))
  }

  private def extractLifecycleAnnotation(mods: Modifiers): (Modifiers, Lifecycles.Value) = {
    val Modifiers(flags, privateWithin, annotations) = mods

    val lifecycleTree = annotations
      .find(isLifecycleAnnotation)
      .getOrElse {
        c.abort(c.enclosingPosition, "Attempted to extract a lifecycle annotation when none was present")
      }

    val newAnnotations = annotations.filterNot(_.equalsStructure(lifecycleTree))
    val lifecycle = lifecycleTree match {
      case q"new $annotationName(...$params)" => Lifecycles.withName(annotationName.toString)
    }

    (Modifiers(flags, privateWithin, newAnnotations), lifecycle)
  }

  private def modifyLifecycleMethods(members: List[Tree], actions: List[Action]): ModifyLifecycleMethodsResult = {
    val processedLifecycles = mutable.Set.empty[String]

    val membersWithModifiedLifecycles = members.map {
      case member @ q"override def $methodName(..$params): $returnType = { ..$body }"
          if Lifecycles.exists(methodName.toString) =>
        processedLifecycles += methodName.toString
        val lifecycle = Lifecycles.withName(methodName.toString)
        val lifecycleActions = actions.filter(_.lifecycle === lifecycle)

        q"override def $methodName(..$params): $returnType = { ..$lifecycleActions; ..$body }"

      case member => member
    }

    case class LifecycleMetadata(params: List[Tree], returnType: TypeName)

    def generateDefault(lifecycle: Lifecycles.Value): Option[Tree] = {
      val lifecycleActions = actions.filter(_.lifecycle === lifecycle)
      lifecycleActions.nonEmpty.option {
        val LifecycleMetadata(params, returnType) = lifecycle match {
          case Lifecycles.OnCreate | Lifecycles.OnActivityCreated | Lifecycles.OnViewStateRestored =>
            LifecycleMetadata(List(q"state: android.os.Bundle"), TypeName("Unit"))
          case Lifecycles.OnCreateOptionsMenu =>
            LifecycleMetadata(List(q"menu: android.view.Menu"), TypeName("Boolean"))
          case Lifecycles.OnStart | Lifecycles.OnResume =>
            LifecycleMetadata(Nil, TypeName("Unit"))
          case Lifecycles.OnAttach =>
            LifecycleMetadata(List(q"context: android.content.Context"), TypeName("Unit"))
        }

        val term = TermName(lifecycle.toString)
        val passedParams = params.map {
          case q"$name: $tpt" => name
        }
        q"override def $term(..$params): $returnType = { ..$lifecycleActions; super.$term(..$passedParams) }"
      }
    }

    val defaultLifecycles: List[Tree] = Lifecycles.values
      .filterNot(lc => processedLifecycles.contains(lc.toString))
      .flatMap(generateDefault)(collection.breakOut)

    ModifyLifecycleMethodsResult(defaultLifecycles ++ membersWithModifiedLifecycles)
  }

  case class Action(
    lifecycle: Lifecycles.Value,
    action: Tree
  )

  object Action {
    implicit val liftable: Liftable[Action] = Liftable(_.action)
  }

  case class ActionValidationResult(
    members: List[Tree],
    actions: List[Action]
  )

  case class ModifyLifecycleMethodsResult(
    members: List[Tree]
  )

}
