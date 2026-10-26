package com.jibi.ai.services

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

class JibiAccessibilityService : AccessibilityService() {
    companion object { var instance: JibiAccessibilityService? = null }
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return root.findAccessibilityNodeInfosByText(text).any { node ->
            var cur: AccessibilityNodeInfo? = node
            while (cur != null) { if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return@any true; cur = cur.parent }
            false
        }
    }
    fun typeText(text: String): Boolean {
        val node = findEditable(rootInActiveWindow ?: return false) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        })
    }
    private fun findEditable(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (n.isEditable) return n
        for (i in 0 until n.childCount) n.getChild(i)?.let { child -> findEditable(child)?.let { return it } }
        return null
    }
    fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val all = mutableListOf<AccessibilityNodeInfo>(); collect(root, all)
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return all.any { it.isScrollable && it.performAction(action) }
    }
    private fun collect(n: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) { out += n; for (i in 0 until n.childCount) n.getChild(i)?.let { collect(it, out) } }
    fun home() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun back() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun recents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun notifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
}
