package com.jarvis.table

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccess : AccessibilityService() {
    companion object { var instance: JarvisAccess? = null }

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(i: Intent?): Boolean { instance = null; return super.onUnbind(i) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun typeText(t: String) {
        val n = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        n.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, t) }
        )
    }

    fun enter() {
        findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
    }

    fun clickText(t: String) {
        var n: AccessibilityNodeInfo? = rootInActiveWindow?.findAccessibilityNodeInfosByText(t)?.firstOrNull()
        while (n != null && !n.isClickable) n = n.parent
        n?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun tap(x: Float, y: Float) {
        val d = resources.displayMetrics
        val p = Path().apply { moveTo(x * d.widthPixels, y * d.heightPixels) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 80)).build(), null, null)
    }

    fun scrollDown() {
        val d = resources.displayMetrics
        val p = Path().apply {
            moveTo(d.widthPixels / 2f, d.heightPixels * 0.7f)
            lineTo(d.widthPixels / 2f, d.heightPixels * 0.3f)
        }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 300)).build(), null, null)
    }
}
