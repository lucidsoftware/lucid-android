package com.lucidchart.android.lifecycle

import cats.kernel.Eq
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
        val LifecycleValidationResult(membersWithExpandedActions, initAnnotationActions, lifecycleActions) = validateActions(members, parameters)
        val ModifyLifecycleMethodsResult(finalMembers) = modifyLifecycleMethods(membersWithExpandedActions, initAnnotationActions, lifecycleActions)

        q"""
          $mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns  } with ..$parents { $self =>
            ..$finalMembers
          }
        """ :: maybeCompanion

      case q"""
        $mods trait $tpname[..$tparams] extends { ..$earlydefns  } with ..$parents { $self =>
          ..$members
        }
      """ :: maybeCompanion =>
        val LifecycleValidationResult(membersWithExpandedActions, initAnnotationActions, lifecycleActions) = validateActions(members, parameters)
        val ModifyLifecycleMethodsResult(finalMembers) = modifyLifecycleMethods(membersWithExpandedActions, initAnnotationActions, lifecycleActions)

        q"""
          $mods trait $tpname[..$tparams] extends { ..$earlydefns  } with ..$parents { $self =>
            ..$finalMembers
          }
        """ :: maybeCompanion

      case unchanged =>
        c.error(c.enclosingPosition, "@LifecycleManaged must be used on a class or trait")
        unchanged
    }

    c.Expr[Any](Block(results, Literal(Constant(()))))
  }

  private case class Parameters(
    debug: Tree = q"false"
  )

  private def extractParams(paramTree: Tree): Parameters = {
    val params = paramTree match {
      case q"new LifecycleManaged(..$params)" => params
      case q"new com.lucidchart.android.lifecycle.LifecycleManaged(..$params)" => params
      case _ => Nil
    }

    params.foldLeft(Parameters()) {
      case (sofar, q"debug = $debugExpr") => sofar.copy(debug = debugExpr)
      case (sofar, _) => sofar
    }
  }

  private def validateActions(members: List[Tree], params: Parameters): LifecycleValidationResult = {
    members.foldLeft(LifecycleValidationResult(Nil, Nil, Nil)) {
      case (result, member @ q"..$mods val $name: $preliminaryReturnType = $body") if hasLifecycleAnnotation(mods.annotations) =>
        val returnType: Tree = getReturnType(preliminaryReturnType, body)
        returnType match {
          case tq"EmptyLifecycleValue[..$tparams]" =>
            val lifecycleReturnType = tq"$lifecyclePackage.LifecycleValue[..$tparams]"

            val (expandedMember, modifiedLifecycleName) =
              expandMember(member.pos, mods, name.toString, lifecycleReturnType, params)

            result.copy(
              members = result.members ++ expandedMember,
              initAnnotationNames = result.initAnnotationNames :+ modifiedLifecycleName
            )
          case _ =>
            val (expandedMember, ModifiedLifecycleName(lifecycle, _, _, newName)) =
              expandMember(member.pos, mods, name.toString, returnType, params)

            val action = Action(lifecycle, q"$newName = $body")

            result.copy(
              members = result.members ++ expandedMember,
              lifecycleActions = result.lifecycleActions :+ action
            )
        }
      case (result, member) =>
        val memberWithoutLifecycleAnnotation = if (hasLifecycleAnnotation(member)) {
          c.error(member.pos, "Lifecycle annotations can only be used on vals")
          extractLifecycleAnnotationFromMember(member)
        } else {
          member
        }

        result.copy(members = result.members :+ memberWithoutLifecycleAnnotation)
    }
  }

  private def getReturnType(preliminaryReturnType: Tree, body: Tree): Tree  = {
    if (preliminaryReturnType.isEmpty) {
      body match {
        case q"new EmptyLifecycleValue[..$tpt](...$ignoredParams) with ..$parents" =>
          tq"EmptyLifecycleValue[..$tpt]"
        case q"LifecycleValue[..$tpt]($rest)" =>
          tq"$lifecyclePackage.LifecycleValue[..$tpt]"
        case _ =>
          c.error(
            preliminaryReturnType.pos,
            "You must specify the return type as LifecycleValue[T] or EmptyLifecycleValue[T] or use LifecycleValue.apply with a type parameter: LifecycleValue[T] or create a new EmptyLifecycleValue"
          )
          preliminaryReturnType
      }
    } else {
      preliminaryReturnType
    }
  }

  private def getModsFromMember(tree: Tree): Option[Modifiers] = {
    tree match {
      case ValDef(mods, _, _, _)       => Some(mods)
      case ClassDef(mods, _, _, _)     => Some(mods)
      case TypeDef(mods, _, _, _)      => Some(mods)
      case DefDef(mods, _, _, _, _, _) => Some(mods)
      case ModuleDef(mods, _, _)       => Some(mods)
      case _                           => None
    }
  }

  private def hasLifecycleAnnotation(tree: Tree): Boolean = {
    getModsFromMember(tree).exists { mods =>
      hasLifecycleAnnotation(mods.annotations)
    }
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

  private def expandMember(position: Position, mods: Modifiers, name: String, returnType: Tree, params: Parameters): (List[Tree], ModifiedLifecycleName) = {
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

    val unwrappedType = returnType match {
      case tq"$outer[..$inner]" => inner.head
    }

    val accessName = TermName(name)
    val expanded =
      List(
        q"$varMods var $syntheticName: $returnType = new $lifecyclePackage.EmptyLifecycleValue[$unwrappedType]($lifecycleInstance, ${params.debug})",
        q"$defMods def $accessName: $returnType = $syntheticName"
      )

    (expanded, ModifiedLifecycleName(lifecycle, position, accessName, syntheticName))
  }

  private def extractLifecycleAnnotationFromMember(member: Tree): Tree = {
    val mods = getModsFromMember(member)
    mods.map { mods =>
      val (newMods, _) = extractLifecycleAnnotation(mods)

      member match {
        case ValDef(_, x, y, z)       => ValDef(newMods, x, y, z)
        case ClassDef(_, x, y, z)     => ClassDef(newMods, x, y, z)
        case TypeDef(_, x, y, z)      => TypeDef(newMods, x, y, z)
        case DefDef(_, a, b, c, d, e) => DefDef(newMods, a, b, c, d, e)
        case ModuleDef(_, a, b)       => ModuleDef(newMods, a, b)
      }
    }.getOrElse(member)
  }

  private def extractLifecycleAnnotation(mods: Modifiers, isAnnotation: (Tree => Boolean)): (Modifiers, Tree) = {
    val Modifiers(flags, privateWithin, annotations) = mods

    val extractAnnotationTree = annotations
      .find(isAnnotation)
      .getOrElse {
        c.abort(c.enclosingPosition, "Attempted to extract a lifecycle annotation when none was present")
      }

    val newAnnotations = annotations.filterNot(_.equalsStructure(extractAnnotationTree))

    (Modifiers(flags, privateWithin, newAnnotations), extractAnnotationTree)
  }

  private def extractLifecycleAnnotation(mods: Modifiers): (Modifiers, Lifecycles.Value) = {
    val (newModifiers, annotation) = extractLifecycleAnnotation(mods, isLifecycleAnnotation)
    val lifecycle = annotation match {
      case q"new $annotationName(...$params)" => Lifecycles.withName(annotationName.toString)
    }

    (newModifiers, lifecycle)
  }

  private def modifyLifecycleMethods(members: List[Tree], initAnnotationActions: List[ModifiedLifecycleName], lifecycleActions: List[Action]): ModifyLifecycleMethodsResult = {
    val modifiedResult = addLifecycleActions(members, lifecycleActions)
    addLifecycleInitializations(modifiedResult.members, initAnnotationActions)
  }

  private def addLifecycleActions(members: List[Tree], actions: List[Action]): ModifyLifecycleMethodsResult = {
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
          case Lifecycles.OnCreateView =>
            LifecycleMetadata(List(q"inflater: android.view.LayoutInflater", q"container: android.view.ViewGroup", q"state: android.os.Bundle"), TypeName("View"))
          case Lifecycles.OnCreateDialog =>
            LifecycleMetadata(List(q"state: android.os.Bundle"), TypeName("Unit"))
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

  private def addLifecycleInitializations(members: List[Tree], names: List[ModifiedLifecycleName]): ModifyLifecycleMethodsResult = {
    val processedLifecycles = mutable.Set.empty[Lifecycles.Value]

    val modifiedLifecycles = members.map {
      case q"override def $methodName(..$params): $returnType = { ..$body }"
          if Lifecycles.exists(methodName.toString) =>
        val lifecycle = Lifecycles.withName(methodName.toString)
        processedLifecycles += lifecycle
        val lifecycleNames = names.filter(_.lifecycle === lifecycle)
        val updatedBody = body.foldLeft(NameUpdateResult(lifecycleNames, Nil)) { (accumulator, expression) =>
          expression match {
            case q"$mods val $tname: $tpt = $expr" =>
              mods.annotations.find(isInitializationAnnotation).map { initAnnotation =>
                val initParam = getInitializationAnnotationParameter(initAnnotation)
                val (modsWithoutInitAnnotation, _) = extractLifecycleAnnotation(mods, isInitializationAnnotation)
                val updatedVal = q"$modsWithoutInitAnnotation val $tname: $tpt = $expr"

                initParam.flatMap { param =>
                  accumulator.remainingNames.find(_.currentName.toString == param.toString).map { currentName =>
                    val macroExpression = q"${currentName.newValueName} = $lifecyclePackage.LifecycleValue($tname)"
                    accumulator.copy(
                      remainingNames = accumulator.remainingNames.filterNot(_ === currentName),
                      updatedExpressions = accumulator.updatedExpressions ++ List(updatedVal, macroExpression)
                    )
                  }.orElse {
                    val lifecycleValueNames = lifecycleNames.map(_.currentName).mkString(",")
                    c.error(
                      param.pos,
                      s"Lifecycle variable name does not match any uninitialized empty lifecycle values for $lifecycle: $lifecycleValueNames"
                    )
                    None
                  }

                }.getOrElse {
                  accumulator.copy(updatedExpressions = accumulator.updatedExpressions :+ updatedVal)
                }

              }.getOrElse {
                accumulator.copy(updatedExpressions = accumulator.updatedExpressions :+ expression)
              }

            case otherExpression => accumulator.copy(updatedExpressions = accumulator.updatedExpressions :+ expression)
          }
        }
        updatedBody.remainingNames.foreach { name =>
          c.error(name.position, s"Value was never initialized with @initLifecycleValue in $lifecycle")
        }
        q"override def $methodName(..$params): $returnType = { ..${updatedBody.updatedExpressions} }"

      case member => member
    }
    names.foreach { name =>
      if(!processedLifecycles.contains(name.lifecycle)) {
        c.error(name.position, s"Lifecycle method ${name.lifecycle} was never overridden despite annotated empty lifecycle value: ${name.currentName}")
      }
    }
    ModifyLifecycleMethodsResult(modifiedLifecycles)
  }


  private def isInitializationAnnotation(annotation: Tree): Boolean = {
    annotation match {
      case q"new $annotationName(...$params)" =>
        annotationName.toString == "initLifecycleValue"
      case _ =>
        false
    }
  }

  private def getInitializationAnnotationParameter(initAnnotation: Tree): Option[Tree] = {
    val param: Option[Tree] = initAnnotation match {
      case q"new initLifecycleValue(..$params)" =>
        if(params.length > 1) {
          c.error(initAnnotation.pos, "More than 1 variable name specified")
        }
        params.headOption
      case _ => None
    }
    param.orElse {
      c.error(initAnnotation.pos, "Lifecycle variable name not specified")
      param
    }
  }


  case class Action(
    lifecycle: Lifecycles.Value,
    action: Tree
  )

  object Action {
    implicit val liftable: Liftable[Action] = Liftable(_.action)
  }

  case class ModifiedLifecycleName(
    lifecycle: Lifecycles.Value,
    position: Position,
    currentName: TermName,
    newValueName: TermName
  )

  object ModifiedLifecycleName {
    implicit val eq: Eq[ModifiedLifecycleName] = Eq.fromUniversalEquals[ModifiedLifecycleName]
  }

  case class LifecycleValidationResult(
    members: List[Tree],
    initAnnotationNames: List[ModifiedLifecycleName],
    lifecycleActions: List[Action]
  )

  case class ModifyLifecycleMethodsResult(
    members: List[Tree]
  )

  case class NameUpdateResult(
    remainingNames: List[ModifiedLifecycleName],
    updatedExpressions: List[Tree]
  )

}
