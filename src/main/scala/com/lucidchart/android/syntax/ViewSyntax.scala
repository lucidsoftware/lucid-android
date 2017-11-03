package com.lucidchart.android.syntax

import android.view._

trait ViewSyntax[V <: View] extends Any {

  def view: V

  // requires API 23
  // def onContextClick(f: View => Boolean): V
  // def onScrollChange(f: (Vewi, Int, Int, Int, Int) => Unit): V

  // requires API 26
  // def onCapturedPointer(f: (View, MotionEvent) => Boolean): V

  def onClick(f: View => Unit): V = {
    view.setOnClickListener(new View.OnClickListener {
      def onClick(view: View): Unit = f(view)
    })
    view
  }

  def onCreateContextMenu(f: (ContextMenu, View, ContextMenu.ContextMenuInfo) => Unit): V = {
    view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener {
      def onCreateContextMenu(
        menu: ContextMenu,
        view: View,
        menuInfo: ContextMenu.ContextMenuInfo
      ): Unit = {
        f(menu, view, menuInfo)
      }
    })
    view
  }

  def onDrag(f: (View, DragEvent) => Boolean): V = {
    view.setOnDragListener(new View.OnDragListener {
      def onDrag(view: View, event: DragEvent): Boolean = f(view, event)
    })
    view
  }

  def onFocusChange(f: (View, Boolean) => Unit): V = {
    view.setOnFocusChangeListener(new View.OnFocusChangeListener {
      def onFocusChange(view: View, hasFocus: Boolean): Unit = f(view, hasFocus)
    })
    view
  }

  def onGenericMotion(f: (View, MotionEvent) => Boolean): V = {
    view.setOnGenericMotionListener(new View.OnGenericMotionListener {
      def onGenericMotion(view: View, event: MotionEvent): Boolean = f(view, event)
    })
    view
  }

  def onHover(f: (View, MotionEvent) => Boolean): V = {
    view.setOnHoverListener(new View.OnHoverListener {
      def onHover(view: View, event: MotionEvent): Boolean = f(view, event)
    })
    view
  }

  def onKey(f: (View, Int, KeyEvent) => Boolean): V = {
    view.setOnKeyListener(new View.OnKeyListener {
      def onKey(view: View, keyCode: Int, event: KeyEvent): Boolean = f(view, keyCode, event)
    })
    view
  }

  def onLongClick(f: View => Boolean): V = {
    view.setOnLongClickListener(new View.OnLongClickListener {
      def onLongClick(view: View): Boolean = f(view)
    })
    view
  }

  def onSystemUiVisibilityChange(f: Int => Unit): V = {
    view.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener {
      def onSystemUiVisibilityChange(visibility: Int): Unit = f(visibility)
    })
    view
  }

  def onTouch(f: (View, MotionEvent) => Boolean): V = {
    view.setOnTouchListener(new View.OnTouchListener {
      def onTouch(view: View, event: MotionEvent): Boolean = f(view, event)
    })
    view
  }

}