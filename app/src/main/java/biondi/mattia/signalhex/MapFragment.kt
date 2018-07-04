package biondi.mattia.signalhex

import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.geometry.Point
import kotlinx.android.synthetic.main.content_layout.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

import kotlin.math.abs

// Lista delle coordinate salvate
var locationList = arrayListOf<LatLng>()
var networkList = arrayListOf<String>()
var intensityList = arrayListOf<Int>()

// Lista dei poligoni disegnati sulla mappa
var edgePolygon = mutableListOf<Polygon>()
var umtsPolygon = mutableListOf<Polygon>()
var ltePolygon = mutableListOf<Polygon>()
var wifiPolygon = mutableListOf<Polygon>()

// Lista degli esagoni disegnati sulla mappa
var edgeHexagon = mutableListOf<Hexagon>()
var umtsHexagon = mutableListOf<Hexagon>()
var lteHexagon = mutableListOf<Hexagon>()
var wifiHexagon = mutableListOf<Hexagon>()

// Il primo esagono da cui iniziare a disegnare gli altri
var firstHexagon: HexagonLayout? = null

class MapFragment: Fragment(), OnMapReadyCallback {

    // Costrutto del FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Posizione attuale
    private var currentLocation: Location? = null
    // Chiavi per memorizzare lo stato dell'activity
    val CURRENT_LOCATION_KEY = "current-location"

    // Richiesta di posizione
    private lateinit var locationRequest: LocationRequest

    // Intervalli di tempo in cui si aggiorna la posizione
    private val INTERVAL = 1000L
    private val FASTEST_INTERVAL = 1000L

    // Comandi da eseguire dopo aver ottenuto la posizione
    private lateinit var locationCallback: LocationCallback

    // La mappa
    private var map: GoogleMap? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.map_layout, container, false)
    }

    override fun onStart() {
        super.onStart()
        activity!!.invalidateOptionsMenu()

        // Inizializzazione del FusedLocationProvider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity as Activity)
        // Recupera il Fragment in cui mostrare la mappa
        // Utilizza childFragmentManager perchè il Map Fragment è un Fragment nel Fragment
        // Nota: è necessario il cast a MapFragment in quanto la funzione ritorna un Fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        // Acquisisce la mappa e la inizializza quando l'istanza GoogleMap è pronta per essere utilizzata
        mapFragment.getMapAsync(this)
        // Crea la richiesta di aggiornamenti continui sulla posizione
        createLocationRequest()
        // Crea l'oggetto che si occuperà di eseguire i comandi dopo aver ottenuto la posizione
        createLocationCallback()
    }

    override fun onResume() {
        super.onResume()
        loadLists()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        saveLists()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(CURRENT_LOCATION_KEY, currentLocation)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            currentLocation = savedInstanceState.getParcelable(CURRENT_LOCATION_KEY)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // TODO funzione per cambiare tipo di mappa (magari nelle impostazioni)
        map!!.mapType = GoogleMap.MAP_TYPE_HYBRID

        // Ottiene la posizione attuale
        getLocation()
        // Aggiorna l'interfaccia della mappa (mostra o nasconde i comandi)
        updateLocationUI()
        // Carica i poligoni dalla memoria
        loadMap()
    }

    private fun getLocation() {
        // Ottiene la miglior e più recente posizione posizione del dispositivo, che può anche essere nulla nei casi in cui la posizione non sia disponibile
        try {
            fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location : Location? ->
                        currentLocation = location
                        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(currentLocation?.latitude as Double, currentLocation?.longitude as Double), map!!.maxZoomLevel))
                    }
        } catch (e: SecurityException) {
        }
    }

    private fun updateLocationUI() {
        map ?: return
        try {
            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
        } catch (e: SecurityException) {
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest().apply {
            interval = INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    // Aggiorna la posizione attuale
                    currentLocation = location
                    activity!!.coordinatesText.text = (location.latitude).toString() + ", " + (location.longitude).toString()

                    if (startBoolean) saveLocation(LatLng(location.latitude, location.longitude), currentNetwork, currentIntensity, false)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null // Looper
            )
        } catch (e: SecurityException) {
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun saveLocation(location: LatLng, network: String, intensity: Int, loading: Boolean) {
        val hexagon = createHexagon(location)

        if (!loading) {
            locationList.add(location)
            networkList.add(network)
            intensityList.add(intensity)
        }

        when (network) {
            "2G" -> {
                addOrUpdateHexagon(edgeHexagon, edgePolygon, hexagon, intensity, edgeBoolean)
            }
            "3G" -> {
                addOrUpdateHexagon(umtsHexagon, umtsPolygon, hexagon, intensity, umtsBoolean)
            }
            "4G" -> {
                addOrUpdateHexagon(lteHexagon, ltePolygon, hexagon, intensity, lteBoolean)
            }
            "Wi-Fi" -> {
                addOrUpdateHexagon(wifiHexagon, wifiPolygon, hexagon, intensity, wifiBoolean)
            }
        }
    }

    private fun createHexagon(location: LatLng): Hexagon {
        val orientation = layout_flat
        val ratio = Point(0.7, 1.0) // Latitudine e longitudine non hanno un aspect ratio regolare
        // TODO funzione per modificare la dimensione dell'esagono nelle impostazioni
        val scale = 0.0000075
        val size = Point(ratio.x * scale, ratio.y * scale)

        lateinit var hexagon: Hexagon
        if (firstHexagon == null) {
            firstHexagon = HexagonLayout(orientation, size, location)
            hexagon = Hexagon(0.0, 0.0)
        } else {
            hexagon = firstHexagon!!.nearestHexagon(location)
        }
        return hexagon
    }

    private fun addOrUpdateHexagon(hexagonList: MutableList<Hexagon>, polygonList: MutableList<Polygon>, hexagon: Hexagon, intensity: Int, boolean: Boolean) {
        val index = hexagonExists(polygonList, hexagon)

        var firstPolygon = false
        if (index == 0) {
            if (polygonList.isEmpty())
                firstPolygon = true
        }

        if(index != -1 && !firstPolygon) {
            updateHexagon(polygonList[index], intensity)
        }
        else {
            addHexagon(polygonList, hexagonList, hexagon, intensity, boolean)
        }
    }

    private fun hexagonExists(list: List<Polygon>, hexagon: Hexagon): Int {
        val x = abs(hexagon.x)
        val y = abs(hexagon.y)
        val abs = "$x $y"
        if (abs == "0.0 0.0") return 0

        for ((index, polygon) in list.withIndex()) {
            if (polygon.tag == hexagon.hexagonToString()) {
                return index
            }
        }
        return -1
    }

    private fun addHexagon(polygonList: MutableList<Polygon>, hexagonList: MutableList<Hexagon>, hexagon: Hexagon, intensity: Int, boolean: Boolean) {
        val points = firstHexagon!!.getCorners(hexagon)

        var color = getColor(intensity)

        //TODO funzione alpha
        val alpha = 255 //getAlpha()
        color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

        val polygon = PolygonOptions()
                .addAll(points)
                .fillColor(color)
                .strokeWidth(2.5F)
                .strokeJointType(2)
                .visible(boolean)
                .zIndex(intensity.toFloat())

        polygonList.add(map!!.addPolygon(polygon))
        polygonList.last().tag = hexagon.hexagonToString()
        hexagonList.add(hexagon)
    }

    private fun updateHexagon(polygon: Polygon, intensity: Int) {
        if (polygon.zIndex != intensity.toFloat()) {
            polygon.zIndex = intensity.toFloat()
            polygon.fillColor = getColor(intensity)
            return
        }
    }

    private fun getColor(intensity: Int): Int {
        return when(intensity) {
            0 -> ContextCompat.getColor(activity as Activity, R.color.none)
            1 -> ContextCompat.getColor(activity as Activity, R.color.poor)
            2 -> ContextCompat.getColor(activity as Activity, R.color.moderate)
            3 -> ContextCompat.getColor(activity as Activity, R.color.good)
            4 -> ContextCompat.getColor(activity as Activity, R.color.great)
            else -> Color.TRANSPARENT
        }
    }

    private fun saveLists() {
        val gson = Gson()
        for (i in 0 until 3) {
            val name = when(i) {
                0 -> "location.json"
                1 -> "network.json"
                2 -> "intensity.json"
                else -> null
            }

            val list = when(i) {
                0 -> locationList
                1 -> networkList
                2 -> intensityList
                else -> null
            }

            if (exists(name!!)) delete(name)
            context!!.openFileOutput(name, Context.MODE_PRIVATE).use {
                it.write(gson.toJson(list).toByteArray())
            }
        }
    }

    private fun exists(fileName: String): Boolean {
        val path = context!!.filesDir.absolutePath + "/" + fileName
        val file = File(path)
        return file.exists()
    }

    private fun delete(fileName: String) {
        val path = context!!.filesDir.absolutePath + "/" + fileName
        val file = File(path)
        file.delete()
    }

    private fun loadLists() {
        val gson = Gson()
        for (i in 0 until 3) {
            val name = when(i) {
                0 -> "location.json"
                1 -> "network.json"
                2 -> "intensity.json"
                else -> null
            }

            if (exists(name!!)) {
                val fileInputStream = context!!.openFileInput(name)
                val inputStreamReader = InputStreamReader(fileInputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val stringBuilder = StringBuilder()
                var line =  bufferedReader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line =  bufferedReader.readLine()
                }
                when(i) {
                    0 -> locationList = gson.fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<LatLng>>(){}.type)
                    1 -> networkList = gson.fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<String>>(){}.type)
                    2 -> intensityList = gson.fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<Int>>(){}.type)
                }
            } else return
        }
    }

    private fun loadMap() {
        // Ferma gli aggiornamenti sulla posizione nel caso siano in esecuzione
        var startBooleanState = false
        if (startBoolean) {
            startBooleanState = startBoolean
            startBoolean = false
        }
        // Controlla che le liste siano tutte della stessa dimensione per evitare problemi
        if (locationList.size == networkList.size && locationList.size == intensityList.size) {
            // Svuota le liste attuali per ri-riempirle con i dati salvati in memoria
            clearLists(true)
            // Cicla le tre liste ricreando la situazione precedente sulla mappa
            for (i in 0 until locationList.size) {
                saveLocation(locationList[i], networkList[i], intensityList[i], true)
            }
        }
        // Fa ripartire gli aggiornamenti nel caso fossero in esecuzione
        if (startBooleanState) startBoolean = true
    }


}

fun clearLists(loading: Boolean) {
    if (!loading) {
        locationList.clear()
        networkList.clear()
        intensityList.clear()
    }

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