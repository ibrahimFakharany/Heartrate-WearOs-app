package com.example.sampleapp22


import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.support.wearable.activity.WearableActivity
import android.support.wearable.view.WatchViewStub
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.database.FirebaseDatabase

class MainActivity : WearableActivity(), HeartBeatService.OnChangeListener {
    private var mTextView: TextView? = null
    private var alertText: TextView? = null
    var sc: ServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val stub =
            findViewById<View>(R.id.watch_view_stub) as WatchViewStub

        // inflate layout depending on watch type (round or square)
        // inflate layout depending on watch type (round or square)
        stub.setOnLayoutInflatedListener { stub -> // as soon as layout is there...
            mTextView = stub.findViewById<View>(R.id.heartbeat) as TextView

            // bind to our service.
            //                ComponentName heartService = startService(new Intent(WearActivity.this, HeartbeatService.class));
            sc = object : ServiceConnection {
                override fun onServiceConnected(
                    componentName: ComponentName,
                    binder: IBinder
                ) {
                    val LOG_TAG = "MsinActivity"
                    Log.d(LOG_TAG, "connected to service.")

                    // set our change listener to get change events
                    (binder as HeartBeatService.HeartbeatServiceBinder).setChangeListener(this@MainActivity)
                }

                override fun onServiceDisconnected(componentName: ComponentName) {}
            }
            val intent = Intent(this@MainActivity, HeartBeatService::class.java)
            var btn = stub.findViewById<Button>(R.id.start_pause_measure)
            if (HeartBeatService.isRunning) {
                btn.text = "stop"
            } else {
                btn.text = "start"

            }

            btn.setOnClickListener {
                 if(HeartBeatService.isRunning){
                     sc?.let { unbindService(it) }
                     btn.text = "start"
                     stopService(Intent(this@MainActivity, HeartBeatService::class.java))
                     updateFirebase("Stopped")
                 }else {
                     updateFirebase("Started")
                     sc?.let { bindService(intent, it, Service.BIND_AUTO_CREATE) }
                     btn.text = "stop"
                 }

            }
        }
    }


    override fun onValueChanged(newValue: Int) {
        // will be called by the service whenever the heartbeat value changes.
        mTextView!!.text = Integer.toString(newValue)
        updateFirebase(newValue.toString())
    }
    fun updateFirebase(msg:String){
        var database = FirebaseDatabase.getInstance()
        var ref = database.getReference("rates").child("user");
        ref.setValue(msg)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(sc!!)
    }

    override fun onEnterAmbient(ambientDetails: Bundle?) {
        super.onEnterAmbient(ambientDetails)
        mTextView!!.setBackgroundColor(Color.BLACK)
        mTextView!!.paint.isAntiAlias = false
        mTextView!!.setTextColor(Color.WHITE)
    }

    override fun onExitAmbient() {
        //mTextView.setBackgroundColor(Color.CYAN);
        mTextView!!.paint.isAntiAlias = true
        //mTextView.setTextColor(Color.BLACK);
        super.onExitAmbient()
    }
}