package com.example.hotlineredial

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class CallMonitorService: InCallService() {
 private val callbacks=mutableMapOf<Call,Call.Callback>()
 override fun onCallAdded(call:Call) {
  super.onCallAdded(call)
  val callback=object:Call.Callback() {
   override fun onStateChanged(c:Call,state:Int) {
    when(state) {
     Call.STATE_ACTIVE -> send("active")
     Call.STATE_DIALING,Call.STATE_CONNECTING -> send("dialing")
     Call.STATE_DISCONNECTED -> send("disconnected",c.details.disconnectCause.code)
    }
   }
  }
  callbacks[call]=callback; call.registerCallback(callback)
  when(call.state) {
   Call.STATE_ACTIVE -> send("active")
   Call.STATE_DIALING,Call.STATE_CONNECTING -> send("dialing")
   Call.STATE_DISCONNECTED -> send("disconnected",call.details.disconnectCause.code)
  }
  CallUiRegistry.current=call
  try { startActivity(Intent(this,CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) } catch(_:Exception) { }
 }
 override fun onCallRemoved(call:Call) { callbacks.remove(call)?.let { call.unregisterCallback(it) }; if(CallUiRegistry.current===call) CallUiRegistry.current=null; super.onCallRemoved(call) }
 private fun send(state:String,reason:Int=-1) { startService(Intent(this,RedialService::class.java).setAction(RedialService.EVENT).putExtra("state",state).putExtra("reason",reason)) }
}
object CallUiRegistry { var current:Call?=null }
