package com.example.sampleapp22

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.NodeApi.GetConnectedNodesResult
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.wearable.Node


class HeartBeatService : Service(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private var currentValue = 0
    private val binder: IBinder = HeartbeatServiceBinder()
    private var onChangeListener: OnChangeListener? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    // interface to pass a heartbeat value to the implementing class
    interface OnChangeListener {
        fun onValueChanged(newValue: Int)
    }

    /**
     * Binder for this service. The binding activity passes a listener we send the heartbeat to.
     */
    inner class HeartbeatServiceBinder : Binder() {
        fun setChangeListener(listener: OnChangeListener) {
            onChangeListener = listener
            // return currently known value
            listener.onValueChanged(currentValue)

        }
    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        // register us as a sensor listener
        isRunning = true
        mSensorManager =
            getSystemService(SENSOR_SERVICE) as SensorManager?
        val mHeartRateSensor: Sensor = mSensorManager!!.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        // delay SENSOR_DELAY_UI is sufficiant
        val res =
            mSensorManager!!.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_UI)
        Log.d(
            LOG_TAG,
            " sensor registered: " + if (res) "yes" else "no"
        )
        mGoogleApiClient = GoogleApiClient.Builder(this).addApi(Wearable.API).build()
        mGoogleApiClient?.let{
            it.connect()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager!!.unregisterListener(this)
        Log.d(LOG_TAG, " sensor unregistered")
        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        return super.onStartCommand(intent, flags, startId)

    }
    override fun onSensorChanged(sensorEvent: SensorEvent) {

        // is this a heartbeat event and does it have data?
        if (sensorEvent.sensor.type == Sensor.TYPE_HEART_RATE && sensorEvent.values.size > 0) {
            val newValue = Math.round(sensorEvent.values[0])
            //            int newValue = 60;
            //Log.d(LOG_TAG,sensorEvent.sensor.getName() + " changed to: " + newValue);
            // only do something if the value differs from the value before and the value is not 0.
            if (currentValue != newValue && newValue != 0) {
                // save the new value
                currentValue = newValue
                // send the value to the listener
                if (onChangeListener != null) {
                    Log.d(
                        LOG_TAG,
                        "sending new value to listener: $newValue"
                    )
                    onChangeListener!!.onValueChanged(newValue)


                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, i: Int) {}

    /**
     * sends a string message to the connected handheld using the google api client (if available)
     * @param message
     */
    private fun sendMessageToHandheld(message: String) {
        if (mGoogleApiClient == null) return
        Log.d(LOG_TAG, "sending a message to handheld: $message")

        // use the api client to send the heartbeat value to our handheld
        val nodes: PendingResult<GetConnectedNodesResult> =
            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
        nodes.setResultCallback { result ->
            val nodes: MutableList<Node>? = result.nodes
            if (nodes != null) {
                for (i in nodes.indices) {
                    val node: Node = nodes[i]
                    Wearable.MessageApi.sendMessage(
                        mGoogleApiClient,
                        node.getId(),
                        message,
                        null
                    )
                }
            }
        }
    }

    companion object {
        var isRunning  = false
        private const val LOG_TAG = "MyHeart"
        fun stopService(){

        }
        fun startService(){

        }
    }
}