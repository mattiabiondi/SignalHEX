package biondi.mattia.signalstrengthheatmap

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Switch
import android.widget.Toast
import android.widget.ToggleButton
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.main_layout.*
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.content_layout.*

class MainActivity :
        AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback {

    private lateinit var umtsSwitch: Switch
    private lateinit var lteSwitch: Switch
    private lateinit var wifiSwitch: Switch

    // Inizializzazione del FusedLocationProvider
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Codice di richiesta
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    // Autorizzazione ad utilizzare la posizione
    private var locationPermission = false

    // Posizione attuale
    private var currentLocation: Location? = null

    // Richiesta di posizione
    private var locationRequest: LocationRequest? = null

    // Intervalli di tempo in cui si aggiorna la posizione
    private val INTERVAL = 50L
    private val FASTEST_INTERVAL = 50L

    // Comandi da eseguire dopo aver ottenuto la posizione
    private lateinit var locationCallback: LocationCallback

    // Booleana per mettere in pausa le richieste di posizione durante onPause()
    private var requestingLocationUpdates = false

    // Posizione di default se non viene concessa l'autorizzazione ad utilizzare la posizione (DISI)
    private val defaultLocation = LatLng(44.497264, 11.356047)
    private val defaultZoom: Float = 20f

    // La mappa
    private var map: GoogleMap? = null

    // La visuale della mappa
    private lateinit var cameraPosition: CameraPosition

    // Chiavi per memorizzare gli stati dell'activity
    private val REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key"
    private val KEY_LOCATION = "location"
    private val KEY_CAMERA_POSITION = "camera_position"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        // Imposta una SupportActionBar per avere a disposizione maggiori controlli al posto di una normale ActionBar
        setSupportActionBar(toolbar)

        // Icona menu per aprire il Navigation Drawer
        // L'icona ruota in base a quanto il Navigation Drawer sia aperto
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // Imposta un Listener sulla barra di navigazione
        nav_view.setNavigationItemSelectedListener(this)

        umtsSwitch = findViewById(R.id.umts_switch)
        lteSwitch = findViewById(R.id.lte_switch)
        wifiSwitch = findViewById(R.id.wifi_switch)

        // Recupera il Fragment in cui mostrare la mappa
        // Nota: è necessario il cast a MapFragment in quanto la funzione ritorna un Fragment
        val mapFragment = fragmentManager.findFragmentById(R.id.main_map) as MapFragment

        // Acquisisce la mappa e la inizializza quando l'istanza GoogleMap è pronta per essere utilizzata
        mapFragment.getMapAsync(this)

        // Costrutto del FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Ottiene i permessi per utilizzare la posizione
        getLocationPermission()
        // Crea la richiesta di aggiornamenti continui sulla posizione
        createLocationRequest()
        // Crea l'oggetto che si occuperà di eseguire i comandi dopo aver ottenuto la posizione
        createLocationCallback()
        // Chiama il metodo per riprendere i valori salvati dalla precedente istanza dell'activity (se esistono)
        updateValuesFromBundle(savedInstanceState)
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
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    private fun getLocationPermission () {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permessi non ottenuti
            // Fornisce una spiegazione all'utente se quest'ultimo nega più volte i permessi per accedere alla posizione //TODO articola
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Location permission needed.", Toast.LENGTH_SHORT).show()
            } else {
                // Nessuna spiegazione necessaria, possiamo chiedere i permessi
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            }
        } else {
            // Permessi già ottenuti
            locationPermission = true
        }
    }

    // Si occupa del risultato delle richieste
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // Se la richiesta viene cancellata gli array risultanti sono vuoti
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permessi ottenuti
                } else {
                    // Permessi negati
                }
                return
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // Ottiene la posizione attuale
        getLocation()
        updateLocationUI()
    }

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
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom))
                map?.uiSettings?.isMyLocationButtonEnabled = false
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
                    textLat.text = "Latitude: " + location.latitude.toString()
                    textLon.text = "Longitude: " + location.longitude.toString()
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

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return
            // Aggiorna i valori dal Bundle
            if (savedInstanceState.keySet()?.contains(REQUESTING_LOCATION_UPDATES_KEY) as Boolean) {
                requestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY)
            }
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                currentLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            }
            if (savedInstanceState.keySet().contains(KEY_CAMERA_POSITION)) {
                cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
            }

        // Aggiorna la UI per riprendere lo stato precedente
        updateLocationUI()
    }

    private fun updateLocationUI() {
        // Elvis operator TODO magari spiega
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

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // TODO: implementa impostazioni
        when (item.itemId) {
            R.id.play -> {

            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        //TODO: argomenta il "when"
        // Kotlin mette a disposizione il "when", un costrutto molto simile allo "switch"

        when (item.itemId) {
            R.id.settings -> {

            }
        }
        // Chiude il Navigation Drawer
        //drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}