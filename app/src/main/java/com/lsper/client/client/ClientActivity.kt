package com.lsper.client.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.lsper.client.QRCodeUtil
import com.lsper.client.R
import com.lsper.client.bean.Content
import com.lsper.client.bean.Sms
import com.lsper.client.socketClient
import com.lsper.client.websocket.SocketClient
import kotlin.concurrent.thread

class ClientActivity : AppCompatActivity() {


    lateinit var sendSmsReceiver:SendSmsReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        val socket = socketClient as SocketClient


        //使用UUID作为唯一标识符
        val sp = getSharedPreferences("UUID", MODE_PRIVATE)
        val uuid = sp.getString("localUUID",null)

        val text = findViewById<TextView>(R.id.uuid)
        text.text = uuid

        val QRCode = findViewById<ImageView>(R.id.QRCode)
        val mBitmap = QRCodeUtil.createQRCodeBitmap(uuid,880,880)
        QRCode.setImageBitmap(mBitmap)

        //生成Json
        val content = Content()
        content.senderUUID = uuid
        content.recipientUUID = null
        content.content = null
        content.origin = "client"
        content.type = "connection"



        val json = Gson().toJson(content)

        //运行客户端
        thread {
            socket.send(json)
        }


        val intentFilter = IntentFilter()
        intentFilter.addAction("com.smsManager.broadcast.SEND_SMS")
        sendSmsReceiver = SendSmsReceiver()
        registerReceiver(sendSmsReceiver,intentFilter)



    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(sendSmsReceiver)
    }

    inner class SendSmsReceiver: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val smsManager = SmsManager.getDefault()
            val smsJson = intent?.getStringExtra("sms")
            val gson = Gson()
            val sms = gson.fromJson(smsJson,Sms::class.java)
            if (sms.phoneNumber!=null){
                if (sms.lateTime!=null){
                    Thread.sleep(sms.lateTime)
                    smsManager.sendTextMessage(sms.phoneNumber,null,sms.smsContent,null,null)
                }else{
                    smsManager.sendTextMessage(sms.phoneNumber,null,sms.smsContent,null,null)
                }
            }
        }

    }
}