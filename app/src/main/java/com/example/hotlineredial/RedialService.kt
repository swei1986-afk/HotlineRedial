package com.example.hotlineredial

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.*
import android.telecom.DisconnectCause
import android.telecom.TelecomManager

class RedialService: Service() {
 companion object { const val START="redial.start"; const val STOP="redial.stop"; const val EVENT="redial.event"; const val CHANNEL="redial_channel" }
 private val handler=Handler(Looper.getMainLooper())
 private var number=""; private var seconds=5; private var max=100; private var attempts=0
 private var running=false; private var connected=false; private var waiting=false; private var dialPending=false
 private var task:Runnable?=null
 override fun onBind(intent:Intent?)=null
 override fun onCreate() { super.onCreate(); getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"自动重拨状态",NotificationManager.IMPORTANCE_LOW)) }
 override fun onStartCommand(intent:Intent?, flags:Int, startId:Int):Int {
  when(intent?.action) {
   STOP -> finish("已停止重拨")
   START -> {
    number=intent.getStringExtra("number")?:""; seconds=intent.getIntExtra("seconds",5).coerceIn(5,3600); max=intent.getIntExtra("max",100).coerceIn(1,100)
    running=true; connected=false; waiting=false; dialPending=false; attempts=0
    val notification=notice("准备拨号")
    if(Build.VERSION.SDK_INT>=29) startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL) else startForeground(1,notification)
    handler.post { dial() }
   }
   EVENT -> {
    if(!running) return START_NOT_STICKY
    val state=intent.getStringExtra("state")?:""
    when(state) {
     "active" -> { connected=true; finish("已接通，停止后续重拨") }
     "dialing" -> { dialPending=false; update("第 $attempts 次呼叫中") }
     "disconnected" -> {
      dialPending=false
      val reason=intent.getIntExtra("reason",-1)
      if(!connected && (reason==DisconnectCause.BUSY || reason==DisconnectCause.CONGESTION || reason==DisconnectCause.ERROR || reason==DisconnectCause.OTHER)) retry()
      else finish("呼叫结束（原因码 $reason），已停止")
     }
    }
   }
  }
  return START_NOT_STICKY
 }
 private fun dial() {
  if(!running || waiting) return
  if(attempts>=max) { finish("已达到最大拨号次数 $max"); return }
  if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED) { finish("缺少拨号权限"); return }
  val telecom=getSystemService(TelecomManager::class.java)
  if(telecom.isInCall) { finish("检测到其他通话，已停止"); return }
  attempts++; dialPending=true; update("正在进行第 $attempts / $max 次拨号")
  try { telecom.placeCall(Uri.fromParts("tel",number,null),Bundle()) } catch(e:Exception) { finish("系统拒绝后台拨号：${e.message}"); return }
  task=Runnable { if(running && dialPending) finish("未收到系统呼叫回调，请检查默认拨号权限") }
  handler.postDelayed(task!!,20000)
 }
 private fun retry() {
  if(!running || waiting) return
  task?.let { handler.removeCallbacks(it) }; dialPending=false
  if(attempts>=max) { finish("已达到最大拨号次数 $max"); return }
  waiting=true; update("占线或呼叫失败，$seconds 秒后重拨（已尝试 $attempts 次）")
  task=Runnable { waiting=false; dial() }; handler.postDelayed(task!!,seconds*1000L)
 }
 private fun notice(msg:String):Notification {
  val stop=PendingIntent.getService(this,0,Intent(this,RedialService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val open=PendingIntent.getActivity(this,1,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  return Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.sym_action_call).setContentTitle("热线重拨助手").setContentText(msg).setContentIntent(open).addAction(android.R.drawable.ic_menu_close_clear_cancel,"停止",stop).setOngoing(true).build()
 }
 private fun update(msg:String) { getSystemService(NotificationManager::class.java).notify(1,notice(msg)) }
 private fun finish(msg:String) { running=false; waiting=false; dialPending=false; task?.let { handler.removeCallbacks(it) }; task=null; getSystemService(NotificationManager::class.java).notify(2,Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.sym_action_call).setContentTitle("热线重拨助手").setContentText(msg).setAutoCancel(true).build()); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
 override fun onDestroy() { task?.let { handler.removeCallbacks(it) }; super.onDestroy() }
}
