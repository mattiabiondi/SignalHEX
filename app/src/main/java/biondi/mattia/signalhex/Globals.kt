package biondi.mattia.signalhex

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon

// Codice di richiesta
const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

// Boolean che controlla se deve ottenere o meno i dati
var startBoolean = false

// Booleani che controllano quale mappa visualizzare
var edgeBoolean = true
var umtsBoolean = true
var lteBoolean = true
var wifiBoolean = true

var currentNetwork = R.string.none.toString()
var currentIntensity = 0

// Lista delle coordinate salvate
var locationList = arrayListOf<LatLng>()
var networkList = arrayListOf<String>()
var intensityList = arrayListOf<Int>()

// Lista degli esagoni disegnati sulla mappa
var edgePolygon = mutableListOf<Polygon>()
var umtsPolygon = mutableListOf<Polygon>()
var ltePolygon = mutableListOf<Polygon>()
var wifiPolygon = mutableListOf<Polygon>()

var edgeHexagon = mutableListOf<Hexagon>()
var umtsHexagon = mutableListOf<Hexagon>()
var lteHexagon = mutableListOf<Hexagon>()
var wifiHexagon = mutableListOf<Hexagon>()

var firstHexagon: HexagonLayout? = null

fun clearLists() {
    removeAllHexagons()

    edgePolygon.clear()
    umtsPolygon.clear()
    ltePolygon.clear()
    wifiPolygon.clear()

    edgeHexagon.clear()
    umtsHexagon.clear()
    lteHexagon.clear()
    wifiHexagon.clear()
}

fun setVisibility(list: MutableList<Polygon>, boolean: Boolean) {
    for (i in 0 until list.size) {
        list[i].isVisible = boolean
    }
}

fun removeAllHexagons() {
    for (i in 0 until 4) {
        val polygonList = when (i) {
            0 -> edgePolygon
            1 -> umtsPolygon
            2 -> ltePolygon
            3 -> wifiPolygon
            else -> null
        }

        for (k in 0 until polygonList!!.size) {
            polygonList[k].remove()
        }
    }
    firstHexagon = null
}

// Chiavi per memorizzare lo stato dell'activity
const val START_KEY = "start"
const val EDGE_BOOLEAN_KEY = "edge-boolean"
const val UMTS_BOOLEAN_KEY = "umts-boolean"
const val LTE_BOOLEAN_KEY = "lte-boolean"
const val WIFI_BOOLEAN_KEY = "wifi-boolean"
const val CURRENT_NETWORK = "current-network"
const val CURRENT_INTENSITY = "current-intensity"
const val CURRENT_LOCATION_KEY = "current-location"
const val LOCATION_LIST = "location-list"
const val NETWORK_LIST = "network-list"
const val INTENSITY_LIST = "intensity-list"