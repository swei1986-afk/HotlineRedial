package com.example.hotlineredial

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
 private lateinit var number: EditText
 private lateinit var interval: EditText
 private lateinit var limit: EditText
 private lateinit var status: TextView
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply { orientation=1; setPadding(36,42,36,30) }
  fun label(s:String) { root.addView(TextView(this).apply { text=s; textSize=17f; setPadding(0,20,0,6) }) }
  root.addView(TextView(this).apply { text="热线重拨助手 V0.2.1"; textSize=26f; setTextColor(Color.rgb(17,100,65)); gravity=Gravity.CENTER })
  label("热线号码")
  number=EditText(this).apply { hint="例如 12345"; inputType=3; setText(intent?.data?.schemeSpecificPart ?: "") }; root.addView(number)
  label("失败后等待秒数（5–3600）")
  interval=EditText(this).apply { inputType=2; setText("5") }; root.addView(interval)
  label("最大尝试次数（1–100）")
  limit=EditText(this).apply { inputType=2; setText("100") }; root.addView(limit)
  status=TextView(this).apply { text="等待开始"; textSize=17f; setPadding(0,22,0,22) }; root.addView(status)
  fun button(t:String, click:()->Unit) { root.addView(Button(this).apply { text=t; setOnClickListener { click() } },LinearLayout.LayoutParams(-1,-2)) }
  button("设为默认拨号应用") {
   val rm = getSystemService(RoleManager::class.java)
   when {
    rm.isRoleHeld(RoleManager.ROLE_DIALER) -> status.text = "已设为默认拨号应用"
    rm.isRoleAvailable(RoleManager.ROLE_DIALER) -> try {
     startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), 42)
    } catch (e: android.content.ActivityNotFoundException) { showSettings(false) }
    else -> showSettings(false)
   }
  }
  button("② 开始自动重拨") { begin() }
  button("停止后续重拨") { startService(Intent(this,RedialService::class.java).setAction(RedialService.STOP)); status.text="已请求停止" }
  root.addView(TextView(this).apply { text="后台拨号依赖默认拨号角色及系统允许。接通后不再重拨；不会自动挂断通话。请在澎湃OS设置中允许自启动、关闭电池优化。"; setPadding(0,24,0,0) })
  setContentView(ScrollView(this).apply { addView(root) })
 }
 private fun showSettings(phone: Boolean) {
  val hint = if (phone) "在应用权限里允许“电话”，然后返回并点击开始。" else "在系统默认应用里选择“电话应用”，再选择“热线重拨助手”。"
  status.text = hint
  android.app.AlertDialog.Builder(this).setMessage(hint).setNegativeButton("稍后", null)
   .setPositiveButton("打开设置") { _, _ ->
    val page = if (phone) Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
     android.net.Uri.fromParts("package", packageName, null)) else Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    try { startActivity(page) } catch (e: android.content.ActivityNotFoundException) { status.text = "请手动打开手机设置。" + hint }
   }.show()
 }
 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  super.onActivityResult(requestCode, resultCode, data)
  if (requestCode == 42) {
   if (getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)) status.text = "已设为默认拨号应用"
   else showSettings(false)
  }
 }
 override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
  super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  if (requestCode == 12) {
   if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) status.text = "电话权限已允许，请点击开始"
   else showSettings(true)
  }
 }
 private fun begin() {
  val n=number.text.toString().trim()
  if(!n.matches(Regex("[0-9+*#]{2,20}"))) { status.text="号码格式不正确"; return }
  val rm=getSystemService(RoleManager::class.java)
  if(!rm.isRoleHeld(RoleManager.ROLE_DIALER)) { status.text="请先设置为默认拨号应用"; return }
  if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.CALL_PHONE),12); status.text="授权后重新点击开始"; return }
  val secs=(interval.text.toString().toIntOrNull()?:5).coerceIn(5,3600)
  val max=(limit.text.toString().toIntOrNull()?:100).coerceIn(1,100)
  val i=Intent(this,RedialService::class.java).setAction(RedialService.START).putExtra("number",n).putExtra("seconds",secs).putExtra("max",max)
  try { startForegroundService(i); status.text="已启动；可切换其他应用。请查看常驻通知。" } catch(e:Exception) { status.text="启动失败：${e.message}" }
 }
}
