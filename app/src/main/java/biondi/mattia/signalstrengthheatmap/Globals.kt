package biondi.mattia.signalstrengthheatmap

import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.heatmaps.WeightedLatLng

// Codice di richiesta
const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

// Boolean che controlla se deve ottenere o meno i dati
var startBoolean = false

// Booleani che controllano quale mappa visualizzare
var edgeBoolean = false
var umtsBoolean = false
var lteBoolean = false
var wifiBoolean = false

var currentNetwork = R.string.none.toString()
var currentIntensity = 0

// Lista delle coordinate ottenute dal dispositivo
var edgeList = mutableListOf<WeightedLatLng>()
var umtsList = mutableListOf<WeightedLatLng>()
var lteList = mutableListOf<WeightedLatLng>()
var wifiList = mutableListOf<WeightedLatLng>()

// Lista dei cerchi disegnati a schermo
var edgeCircle = mutableListOf<Circle>()
var umtsCircle = mutableListOf<Circle>()
var lteCircle = mutableListOf<Circle>()
var wifiCircle = mutableListOf<Circle>()

// Lista degli esagoni disegnati sulla mappa
var wifiHexagon = mutableListOf<Polygon>()

fun clearLists() {
    edgeList.clear()
    umtsList.clear()
    lteList.clear()
    wifiList.clear()
    removeAllCircles()
}

fun setVisibility(list: MutableList<Circle>, boolean: Boolean) {
    for (i in 0 until list.size) {
        list[i].isVisible = boolean
    }
}

fun removeAllCircles() {
    for (i in 0 until 4) {
        val list = when (i) {
            0 -> edgeCircle
            1 -> umtsCircle
            2 -> lteCircle
            3 -> wifiCircle
            else -> null
        }

        for (k in 0 until list!!.size) {
            list[k].remove()
        }
    }
}

// Chiavi per memorizzare lo stato dell'activity
const val START_KEY = "start"
const val EDGE_BOOLEAN_KEY = "edge-boolean"
const val UMTS_BOOLEAN_KEY = "umts-boolean"
const val LTE_BOOLEAN_KEY = "lte-boolean"
const val WIFI_BOOLEAN_KEY = "wifi-boolean"
const val CURRENT_LOCATION_KEY = "current-location"
const val PREVIOUS_LOCATION_KEY = "previous-location"
const val REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key"






