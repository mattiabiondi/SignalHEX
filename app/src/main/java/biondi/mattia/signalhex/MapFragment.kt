package biondi.mattia.signalhex

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
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

// La mappa
var map: GoogleMap? = null

// Dimensione degli esagoni
var hexagonsDimension = 1
// Colore degli esagoni
var hexagonsColors = 0
// Trasparenza degli esagoni
var hexagonsAlpha = 0

class MapFragment : Fragment(), OnMapReadyCallback {

    // Costrutto del FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Posizione attuale
    private var currentLocation: Location? = null
    // Chiavi per memorizzare lo stato dell'activity
    private val currentLocationKey = "current-location"

    // Richiesta di posizione
    private lateinit var locationRequest: LocationRequest

    // Intervalli di tempo in cui si aggiorna la posizione
    private val _interval = 1000L
    private val _fastestInterval = 1000L

    // Comandi da eseguire dopo aver ottenuto la posizione
    private lateinit var locationCallback: LocationCallback

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
        // Nota: è necessario il cast a SupportMapFragment in quanto la funzione ritorna un Fragment
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

        // Carica le impostazioni
        loadPreferences()

        // Carica le liste degli esagoni
        loadLists()

        // Avvia le richieste di aggiornamenti di posizione
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()

        // Ferma le richieste di aggiornamenti di posizione
        stopLocationUpdates()

        // Salva le liste degli esagoni
        saveLists()

        // Salva le impostazioni
        savePreferences()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(currentLocationKey, currentLocation)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            currentLocation = savedInstanceState.getParcelable(currentLocationKey)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Imposta il tipo di mappa (normale o satellitare)
        setMapType()
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
                    .addOnSuccessListener { location: Location? ->
                        currentLocation = location
                        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(currentLocation?.latitude as Double, currentLocation?.longitude as Double), map!!.maxZoomLevel))
                    }
        } catch (e: SecurityException) {
        }
    }

    // Aggiorna l'interfaccia utente (i controlli sulla mappa)
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
            interval = _interval
            fastestInterval = _fastestInterval
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
                    activity!!.coordinatesText.text = (location.latitude).toString() + ", " + (location.longitude).toString() // Qui devo concatenare per forza le due stringhe per avere come separatore il punto al posto della virgola

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

    // Salva la posizione ottenuta
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

    // Crea l'esagono sulla mappa
    private fun createHexagon(location: LatLng): Hexagon {
        val orientation = layout_flat
        val ratio = Point(0.7, 1.0) // Latitudine e longitudine non hanno un aspect ratio regolare
        val scale = (hexagonsDimension + 1) * 0.00000375
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

    // Decide se deve creare un nuovo esagono o se è dentro ad un esagono già esistente
    private fun addOrUpdateHexagon(hexagonList: MutableList<Hexagon>, polygonList: MutableList<Polygon>, hexagon: Hexagon, intensity: Int, boolean: Boolean) {
        val index = hexagonExists(polygonList, hexagon)

        var firstPolygon = false
        if (index == 0) {
            if (polygonList.isEmpty())
                firstPolygon = true
        }

        if (index != -1 && !firstPolygon) {
            updateHexagon(polygonList[index], intensity)
        } else {
            addHexagon(polygonList, hexagonList, hexagon, intensity, boolean)
        }
    }

    // Controlla se si è già dentro ad un esagono
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

    // Aggiunge un esagono sulla mappa
    private fun addHexagon(polygonList: MutableList<Polygon>, hexagonList: MutableList<Hexagon>, hexagon: Hexagon, intensity: Int, boolean: Boolean) {
        val points = firstHexagon!!.getCorners(hexagon)

        var color = getColor(intensity)
        val alpha = 255 / (hexagonsAlpha + 1)
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

    // Aggiorna l'esagono in cui siamo
    private fun updateHexagon(polygon: Polygon, intensity: Int) {
        if (polygon.zIndex != intensity.toFloat()) {
            var color = getColor(intensity)
            val alpha = 255 / (hexagonsAlpha + 1)
            color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            polygon.zIndex = intensity.toFloat()
            polygon.fillColor = color
            return
        }
    }

    // Ottiene il colore dell'esagono
    private fun getColor(intensity: Int): Int {
        return when (intensity) {
            0 -> when (hexagonsColors) {
                0 -> ContextCompat.getColor(activity as Activity, R.color.none0)
                1 -> ContextCompat.getColor(activity as Activity, R.color.none1)
                2 -> ContextCompat.getColor(activity as Activity, R.color.none2)
                else -> Color.TRANSPARENT
            }
            1 -> when (hexagonsColors) {
                0 -> ContextCompat.getColor(activity as Activity, R.color.poor0)
                1 -> ContextCompat.getColor(activity as Activity, R.color.poor1)
                2 -> ContextCompat.getColor(activity as Activity, R.color.poor2)
                else -> Color.TRANSPARENT
            }
            2 -> when (hexagonsColors) {
                0 -> ContextCompat.getColor(activity as Activity, R.color.moderate0)
                1 -> ContextCompat.getColor(activity as Activity, R.color.moderate1)
                2 -> ContextCompat.getColor(activity as Activity, R.color.moderate2)
                else -> Color.TRANSPARENT
            }
            3 -> when (hexagonsColors) {
                0 -> ContextCompat.getColor(activity as Activity, R.color.good0)
                1 -> ContextCompat.getColor(activity as Activity, R.color.good1)
                2 -> ContextCompat.getColor(activity as Activity, R.color.good2)
                else -> Color.TRANSPARENT
            }
            4 -> when (hexagonsColors) {
                0 -> ContextCompat.getColor(activity as Activity, R.color.great0)
                1 -> ContextCompat.getColor(activity as Activity, R.color.great1)
                2 -> ContextCompat.getColor(activity as Activity, R.color.great2)
                else -> Color.TRANSPARENT
            }
            else -> Color.TRANSPARENT
        }
    }

    // Salva le liste in memoria
    private fun saveLists() {
        val gson = Gson()
        for (i in 0 until 3) {
            val name = when (i) {
                0 -> "location.json"
                1 -> "network.json"
                2 -> "intensity.json"
                else -> null
            }

            val list = when (i) {
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

    // Controlla se il File esiste già
    private fun exists(fileName: String): Boolean {
        val path = context!!.filesDir.absolutePath + "/" + fileName
        val file = File(path)
        return file.exists()
    }

    // Cancella il File in memoria
    private fun delete(fileName: String) {
        val path = context!!.filesDir.absolutePath + "/" + fileName
        val file = File(path)
        file.delete()
    }

    // Carica le liste dalla memoria
    private fun loadLists() {
        val gson = Gson()
        for (i in 0 until 3) {
            val name = when (i) {
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
                var line = bufferedReader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = bufferedReader.readLine()
                }
                when (i) {
                    0 -> locationList = gson.fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<LatLng>>() {}.type)
                    1 -> networkList = gson.fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<String>>() {}.type)
                    2 -> intensityList = gson.fromJson(stringBuilder.toString(), object : TypeToken<ArrayList<Int>>() {}.type)
                }
            } else return
        }
    }

    // Carica la mappa
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

    // Salva le preferenze in memoria
    private fun savePreferences() {
        val sharedPref = activity!!.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.edge), edgeBoolean)
            putBoolean(getString(R.string.umts), umtsBoolean)
            putBoolean(getString(R.string.lte), lteBoolean)
            putBoolean(getString(R.string.wifi), wifiBoolean)

            putBoolean(getString(R.string.map_satellite), satelliteBoolean)

            putInt(getString(R.string.hexagons_dimension), hexagonsDimension)
            putInt(getString(R.string.hexagons_colors), hexagonsColors)
            putInt(getString(R.string.hexagons_transparency), hexagonsAlpha)

            apply()
        }
        //activity!!.invalidateOptionsMenu()
    }

    // Carica le preferenze dalla memoria
    private fun loadPreferences() {
        val sharedPref = activity!!.getPreferences(Context.MODE_PRIVATE) ?: return

        edgeBoolean = sharedPref.getBoolean(getString(R.string.edge), true)
        umtsBoolean = sharedPref.getBoolean(getString(R.string.umts), true)
        lteBoolean = sharedPref.getBoolean(getString(R.string.lte), true)
        wifiBoolean = sharedPref.getBoolean(getString(R.string.wifi), true)

        satelliteBoolean = sharedPref.getBoolean(getString(R.string.map_satellite), false)

        hexagonsDimension = sharedPref.getInt(getString(R.string.hexagons_dimension), 1)
        hexagonsColors = sharedPref.getInt(getString(R.string.hexagons_colors), 0)
        hexagonsAlpha = sharedPref.getInt(getString(R.string.hexagons_transparency), 0)
    }
}

fun setMapType() {
    map ?: return
    if (satelliteBoolean)
        map!!.mapType = GoogleMap.MAP_TYPE_HYBRID
    else
        map!!.mapType = GoogleMap.MAP_TYPE_NORMAL
}

// Svuota tutte le liste
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

// Rimuove tutti gli esagoni
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