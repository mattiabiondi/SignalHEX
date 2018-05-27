package biondi.mattia.signalstrengthheatmap

import android.app.Fragment
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng

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

// Posizione di default se non viene concessa l'autorizzazione ad utilizzare la posizione (DISI)
private val defaultLocation = LatLng(44.497264, 11.356047)
private val defaultZoom: Float = 20f

// La mappa
private var map: GoogleMap? = null

// Chiavi per memorizzare gli stati dell'activity
private val REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key"
private val KEY_LOCATION = "location"

// Boolean che controlla se deve ottenere o meno i dati
var startBoolean = false
private val START_KEY = "start"

class MapFragment: Fragment(), OnMapReadyCallback {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater!!.inflate(R.layout.map_layout, container, false)
    }

    override fun onStart() {
        super.onStart()
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
        //createLocationRequest()
        // Crea l'oggetto che si occuperà di eseguire i comandi dopo aver ottenuto la posizione
        //createLocationCallback()
    }

    override fun onResume() {
        super.onResume()
        //startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        //stopLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        outState?.putParcelable(KEY_LOCATION, currentLocation)
        outState?.putBoolean(START_KEY, startBoolean)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            requestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY)
            currentLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            startBoolean = savedInstanceState.getBoolean(START_KEY)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Ottiene la posizione attuale
        //getLocation()
        // Aggiorna l'interfaccia della mappa (mostra o nasconde i comandi)
        //updateLocationUI()
    }

    /*
    private fun getLocation() {
        // Ottiene la miglior e più recente posizione posizione del dispositivo, che può anche essere nulla nei casi in cui la posizione non sia disponibile
        try {
            if (locationPermission) {
                fusedLocationProviderClient.lastLocation
                        .addOnSuccessListener { location : Location? ->
                            currentLocation = location
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    LatLng(currentLocation?.latitude as Double, currentLocation?.longitude as Double), defaultZoom))
                        }
            } else {
                // Se i permessi non sono stati ottenuti
                //map?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom))
                //map?.uiSettings?.isMyLocationButtonEnabled = false
                //todo mostra pulsante per ottenere i permessi
            }
        } catch (e: SecurityException) {
        }
    }

    private fun updateLocationUI() {
        map ?: return

        try {
            if (locationPermission) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            }
            else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                currentLocation = null
            }
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
                    coordinatesText.text = (location.latitude).toString() + ", " + (location.longitude).toString()
                    // Passa la posizione attuale alla funzione che si occupa di generare la Heatmap
                    if (startBoolean) checkLocation()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            if (locationPermission && !requestingLocationUpdates) {
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

    private fun checkLocation() {
        if (currentLocation != previousLocation) { //todo da migliorare
            if (wifiBoolean) {

            }
        }
    }
*/
}