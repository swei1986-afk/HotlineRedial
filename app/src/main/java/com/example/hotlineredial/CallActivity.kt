package com.example.hotlineredial

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import android.telecom.Call

class CallActivity:Activity() {
 private lateinit var label:TextView
 private val callback=object:Call.Callback() {
  override fun onStateChanged(call:Call,state:Int) { runOnUiThread { label.text=when(state) { Call.STATE_ACTIVE->"通话已接通"; Call.STATE_DIALING->"正在拨号"; Call.STATE_CONNECTING->"正在连接"; Call.STATE_DISCONNECTED->"通话已结束"; else->"通话状态：$state" }; if(state==Call.STATE_DISCONNECTED) finish() } }
 }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER; setPadding(40,40,40,40) }
  label=TextView(this).apply { text="正在呼叫"; textSize=26f; gravity=Gravity.CENTER }; root.addView(label)
  root.addView(Button(this).apply { text="挂断电话"; setOnClickListener { CallUiRegistry.current?.disconnect() } })
  setContentView(root)
 }
 override fun onResume() { super.onResume(); CallUiRegistry.current?.let { it.registerCallback(callback); callback.onStateChanged(it,it.state) } }
 override fun onPause() { CallUiRegistry.current?.unregisterCallback(callback); super.onPause() }
}
