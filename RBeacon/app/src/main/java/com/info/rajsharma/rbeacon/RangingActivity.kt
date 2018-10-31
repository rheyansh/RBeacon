package com.info.rajsharma.rbeacon

import adapter.MessageAdapter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import org.altbeacon.beacon.*
import adapter.StaticData

class RangingActivity : AppCompatActivity(), BeaconConsumer, RangeNotifier, MonitorNotifier {

    private val beaconManager = BeaconManager.getInstanceForApplication(this)

    private lateinit var messageList: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var clearLogsBtn: Button
    private lateinit var stop_ranging_logs_button: Button
    private lateinit var start_ranging_logs_button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranging)

        clearLogsBtn = findViewById<View>(R.id.clearLogsBtn) as Button
        stop_ranging_logs_button = findViewById<View>(R.id.stop_ranging_logs_button) as Button
        start_ranging_logs_button = findViewById<View>(R.id.start_ranging_logs_button) as Button

        clearLogsBtn.setOnClickListener {
            adapter.removeAllMessages()
        }

        stop_ranging_logs_button.setOnClickListener {

            for (region in StaticData.getSriRegion()) {
                beaconManager.stopRangingBeaconsInRegion(region)
            }
        }

        start_ranging_logs_button.setOnClickListener {

            for (region in StaticData.getSriRegion()) {
                beaconManager.startRangingBeaconsInRegion(region)
            }
        }

        messageList = findViewById<RecyclerView>(R.id.logList)
        messageList.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(this)
        messageList.adapter = adapter

        beaconManager.bind(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }

    override fun onPause() {
        super.onPause()
        if (beaconManager.isBound(this)) beaconManager.backgroundMode = true
    }

    override fun onResume() {
        super.onResume()
        if (beaconManager.isBound(this)) beaconManager.backgroundMode = false
    }

    override fun onBeaconServiceConnect() {

        beaconManager!!.addRangeNotifier(this)
        try {
            for (region in StaticData.getSriRegion()) {
                beaconManager.startRangingBeaconsInRegion(region)
            }
        } catch (e: RemoteException) {
        }
    }

    override fun didEnterRegion(region: Region) {

        val sriBeacon = StaticData.getSriBeaconForRegion(region)
        addLog("BEACON ENTER: "+sriBeacon?.id2 + ": "+sriBeacon?.bMessage)
    }

    override fun didExitRegion(region: Region) {

        val sriBeacon = StaticData.getSriBeaconForRegion(region)
        addLog("BEACON EXIT: "+sriBeacon?.id2 + ": "+sriBeacon?.bMessage)
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {

        val sriBeacon = StaticData.getSriBeaconForRegion(region)
        addLog("BEACON STATE DETERMINE: ${state}: "+sriBeacon?.id2 + ": "+sriBeacon?.bMessage)
    }

    override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {

        for (beacon in beacons) {
            //if (beacon.serviceUuid == 0xfeaa && beacon.beaconTypeCode == 0x00) {
            // This is a Eddystone-UID frame
            val namespaceId = beacon.id1
            val instanceId = beacon.id2

            addLog("Beacon transmitting namespace id: " + namespaceId +
                    " and instance id: " + instanceId +
                    " approximately " + beacon.distance + " meters away.")

            // Do we have telemetry data?
            if (beacon.extraDataFields.size > 0) {
                val telemetryVersion = beacon.extraDataFields[0]
                val batteryMilliVolts = beacon.extraDataFields[1]
                val pduCount = beacon.extraDataFields[3]
                val uptime = beacon.extraDataFields[4]

                addLog("The above beacon is sending telemetry version " + telemetryVersion +
                        ", has been up for : " + uptime + " seconds" +
                        ", has a battery level of " + batteryMilliVolts + " mV" +
                        ", and has transmitted " + pduCount + " advertisements.")
            }
            //}
        }
    }

    fun addLog(text: String) {
        adapter.addMessage(text)
        scrollToBottom()
    }

    fun scrollToBottom() {
        runOnUiThread {
            messageList.scrollToPosition(adapter?.itemCount!! - 1)
        }
    }

    companion object {
        protected val TAG = "RangingActivity"
    }
}
