package adapter

import com.info.rajsharma.rbeacon.R
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region

val ESTIMOTE_BEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
val TEST_BEACON_LAYOUT = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"

class RBeacon {

    var bId: String
    var bMessage: String
    var id2: String
    var id3: String
    var isActive: Boolean = false
    var imageResource = R.drawable.rbeacon_logo_trbg

    constructor(bId: String,
                bMessage: String,
                id2: String,
                id3: String = "201") {
        this.bId = bId
        this.bMessage = bMessage
        this.id2 = id2
        this.id3 = id3
    }
}

object StaticData {

    val baseBiD = "2f234454-cf6d-4a0f-adf2-f4911ba9ffa"

    fun getBeaconList(): ArrayList<RBeacon> {

        var beaconList = arrayListOf<RBeacon>()

        val rBeacon101 =  RBeacon(baseBiD+"4", "Hi, I am Beacon 101", "101")
        val rBeacon102 = RBeacon(baseBiD+"5", "Hi, I am Beacon 102", "102")
        val rBeacon103 = RBeacon(baseBiD+"6", "Hi, I am Beacon 103", "103")

        rBeacon101.imageResource = R.drawable.rbeacon_101
        rBeacon102.imageResource = R.drawable.rbeacon_102
        rBeacon103.imageResource = R.drawable.rbeacon_103

        beaconList.add(rBeacon101)
        beaconList.add(rBeacon102)
        beaconList.add(rBeacon103)

        return beaconList
    }

    fun getSriRegion(): ArrayList<Region> {

        var regionList = arrayListOf<Region>()

        var ids1 = mutableListOf<Identifier>()
        ids1.add(Identifier.parse(baseBiD+"4"))
        regionList.add(Region("region1", ids1))

        var ids2 = mutableListOf<Identifier>()
        ids2.add(Identifier.parse(baseBiD+"5"))
        regionList.add(Region("region2", ids2))

        var ids3 = mutableListOf<Identifier>()
        ids3.add(Identifier.parse(baseBiD+"6"))
        regionList.add(Region("region3", ids3))

        return regionList
    }

    fun getSriBeaconForRegion(region: Region) : RBeacon? {
        val beaconsInRegion= StaticData.getBeaconList().filter { it.bId == region.id1.toString() }
        return beaconsInRegion.firstOrNull()
    }

}