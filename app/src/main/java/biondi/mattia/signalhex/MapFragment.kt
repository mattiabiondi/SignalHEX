package biondi.mattia.signalhex

import android.app.Activity
import android.support.v4.app.Fragment
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.geometry.Point
import kotlinx.android.synthetic.main.content_layout.*

import kotlin.math.abs

class MapFragment: Fragment(), OnMapReadyCallback {

    // Costrutto del FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Posizione attuale
    private var currentLocation: Location? = null

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
        // Imposta il titolo dell'Activity
        //activity.setTitle(R.string.)

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
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(CURRENT_LOCATION_KEY, currentLocation)
        outState.putBoolean(START_KEY, startBoolean)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        retainInstance = true
        if (savedInstanceState != null) {
            currentLocation = savedInstanceState.getParcelable(CURRENT_LOCATION_KEY)
            startBoolean = savedInstanceState.getBoolean(START_KEY)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // TODO funzione per cambiare tipo di mappa (magari nelle impostazioni)
        //map!!.mapType = GoogleMap.MAP_TYPE_HYBRID

        // Ottiene la posizione attuale
        getLocation()
        // Aggiorna l'interfaccia della mappa (mostra o nasconde i comandi)
        updateLocationUI()
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

    //TODO non funzionaaaaaa
    private fun load(locationList: ArrayList<LatLng>, networkList: ArrayList<String>, intensityList: ArrayList<Int>) {
        if (locationList.size == networkList.size && locationList.size == intensityList.size) {
            for (i in 0 until locationList.size) {
                saveLocation(locationList[i], networkList[i], intensityList[i], true)
            }
        }
    }
}