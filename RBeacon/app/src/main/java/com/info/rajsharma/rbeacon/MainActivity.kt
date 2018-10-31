package com.info.rajsharma.rbeacon

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.os.RemoteException
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView

import org.altbeacon.beacon.powersave.BackgroundPowerSaver
import org.altbeacon.beacon.startup.RegionBootstrap
import org.altbeacon.beacon.startup.BootstrapNotifier
import adapter.MessageAdapter
import adapter.RBeacon
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import org.altbeacon.beacon.*
import adapter.StaticData
import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.graphics.Bitmap
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.widget.SeekBar


class MainActivity : AppCompatActivity(), BootstrapNotifier, BeaconConsumer, RangeNotifier {

    private lateinit var clearLogsBtn: Button
    private lateinit var messageList: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var becomeBroadcasterButton: Button
    private lateinit var check_ranging_logs_button: Button
    private lateinit var beaconManager: BeaconManager
    private lateinit var stop_ranging_logs_button: Button
    private lateinit var messageTextView: TextView
    private lateinit var messageImageView: ImageView
    private lateinit var tempTextView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var distanceTextView: TextView

    private var regionBootstrap: RegionBootstrap? = null
    private var backgroundPowerSaver: BackgroundPowerSaver? = null
    private var haveDetectedBeaconsSinceBoot = false

    var notificationDistance: Double = 1.0
    private lateinit var sriBeaconArrayList: ArrayList<RBeacon>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clearLogsBtn = findViewById<View>(R.id.clearLogsBtn) as Button
        messageList = findViewById<RecyclerView>(R.id.logList)
        becomeBroadcasterButton = findViewById<View>(R.id.becomeBroadcasterButton) as Button
        check_ranging_logs_button = findViewById<View>(R.id.check_ranging_logs_button) as Button
        stop_ranging_logs_button = findViewById<View>(R.id.stop_ranging_logs_button) as Button
        messageTextView = findViewById<View>(R.id.messageTextView) as TextView
        messageImageView = findViewById<View>(R.id.messageImageView) as ImageView
        tempTextView = findViewById<View>(R.id.tempTextView) as TextView
        seekBar = findViewById<View>(R.id.seekBar) as SeekBar
        distanceTextView = findViewById<View>(R.id.distanceTextView) as TextView

        seekBar.max = 100

        seekBar.setProgress(notificationDistance.toInt()*4)

        // Set a SeekBar change listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                val roundOff = seekBar.progress / 8.0

                if (roundOff < 0.5) {
                    distanceTextView.text = "Moving to 0.5"
                } else {
                    distanceTextView.text = "Moving to: ${roundOff}"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
                val roundOff = seekBar.progress / 8.0

                if (roundOff < 0.5) {
                    notificationDistance = 0.5
                } else {
                    val roundOff = seekBar.progress / 8.0
                    notificationDistance = roundOff
                }
                showDistance()
            }
        })

        messageList.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(this)
        messageList.adapter = adapter

        showDistance()

        clearLogsBtn.setOnClickListener {
            adapter.removeAllMessages()
        }

        becomeBroadcasterButton.setOnClickListener {
            val intent = Intent(this, MessageList::class.java)
            startActivity(intent)
        }

        check_ranging_logs_button.setOnClickListener {
            val intent = Intent(this, RangingActivity::class.java)
            startActivity(intent)
        }

        sriBeaconArrayList = StaticData.getBeaconList()
        stop_ranging_logs_button.setOnClickListener {

            for (region in StaticData.getSriRegion()) {
                beaconManager.stopRangingBeaconsInRegion(region)
            }
        }

        verifyBluetooth()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("This app needs location access")
                builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                            PERMISSION_REQUEST_COARSE_LOCATION)
                }
                builder.show()
            }
        }

        setupBeacon()
    }

    private fun showDistance() {
        distanceTextView.text = "Distance: ${notificationDistance}"
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }

    public override fun onResume() {
        super.onResume()
        beaconManager = BeaconManager.getInstanceForApplication(this.applicationContext)
        //beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(ESTIMOTE_BEACON_LAYOUT))

        // Detect the main Eddystone-UID frame:
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))
        beaconManager.bind(this)

        if (beaconManager.isBound(this)) beaconManager.backgroundMode = false

    }

    public override fun onPause() {
        super.onPause()
        beaconManager.unbind(this)
        if (beaconManager.isBound(this)) beaconManager.backgroundMode = true

    }

    private fun setupBeacon() {

        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this)
        beaconManager = BeaconManager.getInstanceForApplication(this.applicationContext)
        // Detect the main Eddystone-UID frame:
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))

        //beaconManager.getBeaconParsers().clear();
        //beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(ESTIMOTE_BEACON_LAYOUT))

        beaconManager.bind(this)

        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //
        //beaconManager.getBeaconParsers().clear();
        //beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));


        // Uncomment the code below to use a foreground service to scan for beacons. This unlocks
        // the ability to continually scan for long periods of time in the background on Andorid 8+
        // in exchange for showing an icon at the top of the screen and a always-on notification to
        // communicate to users that your app is using resources in the background.
        //


        //Notification.Builder builder = new Notification.Builder(this);
        //builder.setSmallIcon(R.drawable.ic_launcher);
        //builder.setContentTitle("Scanning for Beacons");
        //Intent intent = new Intent(this, MonitoringActivity.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(
        //        this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        //);
        //builder.setContentIntent(pendingIntent);
        // beaconManager.enableForegroundServiceScanning(builder.build(), 456);

        // For the above foreground scanning service to be useful, you need to disable
        // JobScheduler-based scans (used on Android 8+) and set a fast background scan
        // cycle that would otherwise be disallowed by the operating system.
        //
//        beaconManager.setEnableScheduledScanJobs(false);
//        beaconManager.setBackgroundBetweenScanPeriod(0);
//        beaconManager.setBackgroundScanPeriod(1100);

        beaconManager.setBackgroundBetweenScanPeriod(500);
        beaconManager.setBackgroundScanPeriod(500);
        beaconManager.setForegroundBetweenScanPeriod(500);
        beaconManager.setForegroundScanPeriod(500);

        //beaconManager.setRegionStatePeristenceEnabled(false);

        Log.d(TAG, "setting up background monitoring for beacons and power saving")
        // wake up the app when a beacon is seen

        regionBootstrap = RegionBootstrap(this, StaticData.getSriRegion())

        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
        backgroundPowerSaver = BackgroundPowerSaver(this)

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted")
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { }
                    builder.show()
                }
                return
            }
        }
    }

    override fun didEnterRegion(region: Region) {
        // In this example, this class sends a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        if (!haveDetectedBeaconsSinceBoot) {
            addLog("Auto launching MainActivity")

            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
            //val intent = Intent(this, MonitoringActivity::class.java)
            //  intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            //this.startActivity(intent)
            haveDetectedBeaconsSinceBoot = true
        }
    }

    override fun didExitRegion(region: Region) {

        val sriBeacon = StaticData.getSriBeaconForRegion(region)
        addLog("BEACON EXIT: "+sriBeacon?.id2 + ": "+sriBeacon?.bMessage)

        messageTextView.text = "No beacon available"
        messageImageView.setImageResource(R.drawable.rbeacon_logo_trbg)
    }

    override fun didDetermineStateForRegion(state: Int, region: Region) {
        val sriBeacon = StaticData.getSriBeaconForRegion(region)

        addLog("BEACON Switched from seeing/not seeing beacons: $state"+sriBeacon?.id2 + ": "+sriBeacon?.bMessage)
    }

    override fun onBeaconServiceConnect() {

        beaconManager.addRangeNotifier(this)

        if (sriBeaconArrayList == null) {
            sriBeaconArrayList = StaticData.getBeaconList()
        }

        try {
            for (region in StaticData.getSriRegion()) {
                beaconManager.startRangingBeaconsInRegion(region)
            }
        } catch (e: RemoteException) {
        }
    }

    private fun checkForNotification(beacon: Beacon) {

        val namespaceId = beacon.id1

        val sriBeaconList = sriBeaconArrayList.filter { it.bId == namespaceId.toString()}
        var sriBeacon = sriBeaconList.firstOrNull()

        if (sriBeacon != null) {

            if (beacon.distance < notificationDistance) {

                if (!sriBeacon.isActive) {
                    sriBeacon.isActive = true
                    tempTextView.text = "Force ENTER: ${sriBeacon.id2}"
                    sendNotification(sriBeacon)
                }
            } else {
                sriBeacon.isActive = false
                tempTextView.text = "Force EXIT: ${sriBeacon.id2}"
            }
        }
    }

    //RangeNotifier
    override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {

        for (beacon in beacons) {

            val namespaceId = beacon.id1
            val instanceId = beacon.id2

            addLog("Beacon transmitting namespace id: " + namespaceId +
                    " and instance id: " + instanceId +
                    " approximately " + beacon.distance + " meters away.")

            checkForNotification(beacon)

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
        }
    }

    private fun sendNotification(sriBeacon: RBeacon? = null) {

        var message = "A beacon is nearby"

        var bitmap: Bitmap?

        if (sriBeacon != null) {
            message = sriBeacon.bMessage
            messageTextView.text = message
            messageImageView.setImageResource(sriBeacon.imageResource)
            bitmap = BitmapFactory.decodeResource(getResources(),
                    sriBeacon.imageResource)
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.rbeacon_logo_trbg)
        }


        val bigPictureStyle = NotificationCompat.BigPictureStyle()
                .setSummaryText(message)
                .bigPicture(bitmap)

        val builder = NotificationCompat.Builder(this)
                .setContentTitle("RBeacon")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(bitmap)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(bigPictureStyle)

        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addNextIntent(Intent(this, MainActivity::class.java))

        val resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(resultPendingIntent)
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    fun isBlueToothEnabled(): Boolean {

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) { addLog("Device does not support Bluetooth")
            return false
        }

        if (!mBluetoothAdapter.isEnabled()) {
            addLog("Bluetooth is not Enabled")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return false
        }

        return true
    }

    private fun verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Bluetooth not enabled")
                builder.setMessage("Please enable bluetooth in settings and restart this application.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    finish()
                    System.exit(0)
                }
                builder.show()
            }
        } catch (e: RuntimeException) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Bluetooth LE not available")
            builder.setMessage("Sorry, this device does not support Bluetooth LE.")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setOnDismissListener {
                finish()
                System.exit(0)
            }
            builder.show()
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
        protected val TAG = "MainActivity"
        private val PERMISSION_REQUEST_COARSE_LOCATION = 1
        private val REQUEST_ENABLE_BT = 2
    }
}

