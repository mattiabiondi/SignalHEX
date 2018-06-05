package biondi.mattia.signalstrengthheatmap

import com.google.android.gms.maps.model.LatLng

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
var edgeList = arrayOf(
        mutableListOf<LatLng>(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf())
var umtsList = arrayOf(
        mutableListOf<LatLng>(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf())
var lteList = arrayOf(
        mutableListOf<LatLng>(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf())
var wifiList = arrayOf(
        mutableListOf<LatLng>(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf())

var intensityList = arrayOf(
        mutableListOf<LatLng>(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf())

fun setIntensityList() {
    for (i in 0..4) {
        if (edgeBoolean)
            if (!intensityList[i].containsAll(edgeList[i]))
                intensityList[i].addAll(edgeList[i])
            else intensityList[i].removeAll(edgeList[i])

        if (umtsBoolean)
            if (!intensityList[i].containsAll(umtsList[i]))
                intensityList[i].addAll(umtsList[i])
            else intensityList[i].removeAll(umtsList[i])

        if (lteBoolean)
            if (!intensityList[i].containsAll(lteList[i]))
                intensityList[i].addAll(lteList[i])
            else intensityList[i].removeAll(lteList[i])

        if (wifiBoolean)
            if (!intensityList[i].containsAll(wifiList[i]))
                intensityList[i].addAll(wifiList[i])
            else intensityList[i].removeAll(wifiList[i])
    }
}

fun clearLists() {
    for (i in 0..4) {
        edgeList[i].clear()
        umtsList[i].clear()
        lteList[i].clear()
        wifiList[i].clear()
        intensityList[i]
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






