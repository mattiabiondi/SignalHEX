package biondi.mattia.signalstrengthheatmap

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

// Lista degli esagoni disegnati sulla mappa
var edgePolygon = mutableListOf<Polygon>()
var umtsPolygon = mutableListOf<Polygon>()
var ltePolygon = mutableListOf<Polygon>()
var wifiPolygon = mutableListOf<Polygon>()

var edgeHexagon = mutableListOf<Hexagon>()
var umtsHexagon = mutableListOf<Hexagon>()
var lteHexagon = mutableListOf<Hexagon>()
var wifiHexagon = mutableListOf<Hexagon>()


fun clearLists() {
    edgeList.clear()
    umtsList.clear()
    lteList.clear()
    wifiList.clear()
    removeAllHexagons()
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

        val hexagonList = when (i) {
            0 -> edgeHexagon
            1 -> umtsHexagon
            2 -> lteHexagon
            3 -> wifiHexagon
            else -> null
        }

        for (k in 0 until polygonList!!.size) {
            polygonList[k].remove()
        }
    }

    edgeHexagon.clear()
    umtsHexagon.clear()
    lteHexagon.clear()
    wifiHexagon.clear()

    firstHexagon = null
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






