package com.info.rajsharma.rbeacon

import adapter.MessageListAdapter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Button
import org.altbeacon.beacon.*
import java.util.*
import kotlin.concurrent.schedule
import adapter.*

class MessageList : AppCompatActivity() {

    private lateinit var messageList: RecyclerView
    private lateinit var adapter: MessageListAdapter
    private lateinit var beaconTransmitter: BeaconTransmitter
    private lateinit var clearLogsBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_list)

        messageList = findViewById<RecyclerView>(R.id.logList)
        clearLogsBtn = findViewById<View>(R.id.clearLogsBtn) as Button

        messageList.layoutManager = LinearLayoutManager(this)
        adapter = MessageListAdapter(this)
        messageList.adapter = adapter

        adapter.items = StaticData.getBeaconList().toMutableList()

        adapter.onItemClick = {
            broadcastBeacon(it)
        }

        clearLogsBtn.setOnClickListener {
            adapter.removeAllMessages()
            adapter.addAllBeacons()
        }
    }

    private fun broadcastBeacon(sriBeacon: RBeacon) {
        val arr = arrayOf(0L)

        val beacon = Beacon.Builder()
                .setId1(sriBeacon.bId)
                .setId2(sriBeacon.id2)
                .setId3(sriBeacon.id3)
                .setManufacturer(0x0118)
                .setTxPower(-59)
                .setDataFields(arr.toMutableList())
                .build()
        val beaconParser = BeaconParser()
                .setBeaconLayout(TEST_BEACON_LAYOUT)

        //If you want to broadcast perticular beacon, than set your layout like below 2 lines
//        val beaconParser = BeaconParser()
//         .setBeaconLayout(ESTIMOTE_BEACON_LAYOUT)

        beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)
        beaconTransmitter.startAdvertising(beacon)

        addLog("TRANSMISSTING BEACON : "+sriBeacon?.id2 + ": "+sriBeacon?.bMessage)
    }

    private fun stopBroadcast() {
        beaconTransmitter.stopAdvertising()
        addLog("stopBroadcast")
    }

    fun addLog(text: String) {
        adapter?.addMessage(text)

        Timer().schedule(5) {
            scrollToBottom()
        }

    }

    fun scrollToBottom() {
        runOnUiThread {
            messageList.scrollToPosition(adapter?.itemCount!! - 1)
        }
    }

}
