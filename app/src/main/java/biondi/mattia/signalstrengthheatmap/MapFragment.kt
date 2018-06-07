package biondi.mattia.signalstrengthheatmap

import android.app.Fragment
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.maps.android.geometry.Point
import com.google.maps.android.heatmaps.WeightedLatLng
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
        // Controlla che la posizione attuale sia diversa da quella precedente
        // (Se si è fermi sul posto non continua a salvare le posizioni)
        // TODO da migliorare, miglior controllo sulle coordinate entro un certo range
        if (currentLocation != previousLocation) {
            val location = WeightedLatLng(
                    LatLng(currentLocation!!.latitude, currentLocation!!.longitude),
                    currentIntensity.toDouble())

            // todo dato che i point non corrispondono alle coordinate, probabilmente posso fare a meno di una WeightedLatLng
            when (currentNetwork) {
                "2G" -> {
                    edgeList.add(location)
                    //edgeCircle.add(addCircle(location, edgeBoolean))
                }
                "3G" -> {
                    umtsList.add(location)
                    //umtsCircle.add(addCircle(location, umtsBoolean))
                }
                "4G" -> {
                    lteList.add(location)
                    //lteCircle.add(addCircle(location, lteBoolean))
                }
                "Wi-Fi" -> {
                    wifiList.add(location)
                    //wifiCircle.add(addCircle(location, wifiBoolean))

                    /**aggiunge un esagono per ogni coordinata*/

                    val points: List<LatLng> = Layout(
                            layout_pointy,
                            Point(0.00035, 0.0005), // Size
                            Point(currentLocation!!.latitude, currentLocation!!.longitude)) // Origin
                            .polygonCorners(Hexagon(0.0, 0.0))

                    map!!.addPolygon(PolygonOptions()
                            .addAll(points)
                            .fillColor(Color.CYAN))

                    /* aggiunge più esagoni ma lagga ovviamente
                    var i = (currentLocation!!.latitude - 0.0005)
                    while (i <= (currentLocation!!.latitude + 0.005)) {
                        var j = (currentLocation!!.longitude - 0.005)
                        while (j <= (currentLocation!!.latitude + 0.005)) {

                            val points: List<LatLng> = Layout(
                                    layout_pointy,
                                    Point(0.00035, 0.0005), // Size
                                    Point(i, j)) // Origin
                                    .polygonCorners(Hexagon(0.0, 0.0))

                            map!!.addPolygon(PolygonOptions()
                                    .addAll(points)
                                    .fillColor(Color.CYAN))

                            j += 0.5
                        }
                        i += 0.5
                    }*/

                }
            }
        }
    }

    private fun circleOptions(): CircleOptions {
        return CircleOptions()
                .clickable(false)
                .radius(2.0)
                .strokeColor(Color.TRANSPARENT)
                .strokeWidth(0F)
    }

    private fun addCircle(location: WeightedLatLng, boolean: Boolean): Circle {
        val color = when(location.intensity.toInt()) {
            0 -> ContextCompat.getColor(activity, R.color.none)
            1 -> ContextCompat.getColor(activity, R.color.poor)
            2 -> ContextCompat.getColor(activity, R.color.moderate)
            3 -> ContextCompat.getColor(activity, R.color.good)
            4 -> ContextCompat.getColor(activity, R.color.great)
            else -> Color.TRANSPARENT
            //todo trasparenza
        }

        return map!!.addCircle(
               circleOptions()
                       .center(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))
                       .fillColor(color)
                       .zIndex(location.intensity.toFloat())
                       .visible(boolean))

        //todo persistenza in onpause
    }
}

//TODO HEXAGON PATTERN