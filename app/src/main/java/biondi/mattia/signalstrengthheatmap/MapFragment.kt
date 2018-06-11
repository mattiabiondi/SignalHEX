package biondi.mattia.signalstrengthheatmap

import android.app.Fragment
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.geometry.Point
import kotlinx.android.synthetic.main.content_layout.*

class MapFragment: Fragment(), OnMapReadyCallback {

    // Costrutto del FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Posizione attuale
    private var currentLocation: Location? = null
    private var previousLocation: Location? = null

    // Richiesta di posizione
    private lateinit var locationRequest: LocationRequest

    // Intervalli di tempo in cui si aggiorna la posizione
    private val INTERVAL = 1000L
    private val FASTEST_INTERVAL = 1000L

    // Comandi da eseguire dopo aver ottenuto la posizione
    private lateinit var locationCallback: LocationCallback

    // Booleana per mettere in pausa le richieste di posizione durante onPause()
    private var requestingLocationUpdates = false

    private val defaultZoom: Float = 20f

    // La mappa
    private var map: GoogleMap? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater!!.inflate(R.layout.map_layout, container, false)
    }

    override fun onStart() {
        super.onStart()
        activity.invalidateOptionsMenu()
        // Imposta il titolo dell'Activity
        //activity.setTitle(R.string.)

        // Inizializzazione del FusedLocationProvider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity.applicationContext)
        // Recupera il Fragment in cui mostrare la mappa
        // Utilizza childFragmentManager perchè il Map Fragment è un Fragment nel Fragment
        // Nota: è necessario il cast a MapFragment in quanto la funzione ritorna un Fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as MapFragment
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        outState?.putParcelable(CURRENT_LOCATION_KEY, currentLocation)
        outState?.putParcelable(PREVIOUS_LOCATION_KEY, previousLocation)
        outState?.putBoolean(START_KEY, startBoolean)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            requestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY)
            currentLocation = savedInstanceState.getParcelable(CURRENT_LOCATION_KEY)
            previousLocation = savedInstanceState.getParcelable(PREVIOUS_LOCATION_KEY)
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
                                LatLng(currentLocation?.latitude as Double, currentLocation?.longitude as Double), defaultZoom))
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
                    previousLocation = currentLocation
                    currentLocation = location
                    activity.coordinatesText.text = (location.latitude).toString() + ", " + (location.longitude).toString()

                    if (startBoolean) saveLocation()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            if (!requestingLocationUpdates) {
                requestingLocationUpdates = true
                fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        null // Looper
                )
            }
        } catch (e: SecurityException) {
        }
    }

    private fun stopLocationUpdates() {
        if (requestingLocationUpdates) {
            requestingLocationUpdates = false
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun saveLocation() {
        // Controlla che esista o meno l'esagono iniziale da cui calcolare gli esagoni seguenti
        // Se la posizione cambia, ma si è già all'interno di un esagono, la funzione non viene eseguita
        // (Se si è fermi sul posto non continua a salvare le posizioni)
        val location = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        // TODO controllo sul cambio di intensità sulla stessa posizione
        if ((firstHexagon == null) || (!PolyUtil.containsLocation(location, currentHexagon!!.points, false))) {
            val intensity = currentIntensity

            when (currentNetwork) {
                "2G" -> {
                    //edgeList.add(location)
                    edgeHexagon.add(addHexagon(location, intensity, edgeBoolean))
                }
                "3G" -> {
                    //umtsList.add(location)
                    umtsHexagon.add(addHexagon(location, intensity, umtsBoolean))
                }
                "4G" -> {
                    //lteList.add(location)
                    lteHexagon.add(addHexagon(location, intensity, lteBoolean))
                }
                "Wi-Fi" -> {
                    //wifiList.add(location)
                    wifiHexagon.add(addHexagon(location, intensity, wifiBoolean))
                }
            }
        }
    }

    private fun addHexagon(location: LatLng, intensity: Int, boolean: Boolean): Polygon {
        val orientation = layout_flat
        val ratio = Point(0.7, 1.0) // Latitudine e longitudine non hanno un aspect ratio regolare
        // TODO funzione per modificare la dimensione dell'esagono nelle impostazioni
        val scale = 0.0000075
        val size = Point(ratio.x * scale, ratio.y * scale)

        lateinit var points: List<LatLng>
        lateinit var hexagon: Hexagon
        if (firstHexagon == null) {
            firstHexagon = HexagonLayout(orientation, size, location)
            hexagon = Hexagon(0.0, 0.0)
        } else {
            hexagon = firstHexagon!!.nearestHexagon(location)
        }
        points = firstHexagon!!.getCorners(hexagon)

        var color = when(intensity) {
            0 -> ContextCompat.getColor(activity, R.color.none)
            1 -> ContextCompat.getColor(activity, R.color.poor)
            2 -> ContextCompat.getColor(activity, R.color.moderate)
            3 -> ContextCompat.getColor(activity, R.color.good)
            4 -> ContextCompat.getColor(activity, R.color.great)
            else -> Color.TRANSPARENT
        }

        //TODO funzione alpha
        val alpha = 127 //getAlpha()
        color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

        val polygon = PolygonOptions()
                .addAll(points)
                .fillColor(color)
                .strokeWidth(2.5F)
                .strokeJointType(2)
                .visible(boolean)

        currentHexagon = map!!.addPolygon(polygon)
        return currentHexagon!!
    }
}

