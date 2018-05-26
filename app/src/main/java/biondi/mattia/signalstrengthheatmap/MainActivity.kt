package biondi.mattia.signalstrengthheatmap

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.telephony.*
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlinx.android.synthetic.main.main_layout.*
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.content_layout.*
import kotlinx.android.synthetic.main.lte_switch_layout.*
import kotlinx.android.synthetic.main.umts_switch_layout.*
import kotlinx.android.synthetic.main.wifi_switch_layout.*

class MainActivity :
        AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback {

    // Costrutto del FusedLocationProviderClient
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Costrutto del Connectivity Manager
    private var connectivityManager: ConnectivityManager? = null
    private lateinit var wifiManager: WifiManager
    private lateinit var telephonyManager: TelephonyManager

    // Codice di richiesta
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    // Autorizzazioni
    private var locationPermission = false

    // Posizione attuale
    private var currentLocation: Location? = null
    private var previousLocation: Location? = null

    // Richiesta di posizione
    private lateinit var locationRequest: LocationRequest
    private lateinit var networkRequest: NetworkRequest

    // Intervalli di tempo in cui si aggiorna la posizione
    private val INTERVAL = 1000L
    private val FASTEST_INTERVAL = 1000L

    // Comandi da eseguire dopo aver ottenuto la posizione
    private lateinit var locationCallback: LocationCallback
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    // Booleana per mettere in pausa le richieste di posizione durante onPause()
    private var requestingLocationUpdates = false
    private var requestingNetworkUpdates = false

    // Posizione di default se non viene concessa l'autorizzazione ad utilizzare la posizione (DISI)
    private val defaultLocation = LatLng(44.497264, 11.356047)
    private val defaultZoom: Float = 20f

    // La mappa
    private var map: GoogleMap? = null

    // Chiavi per memorizzare gli stati dell'activity
    private val REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key"
    private val KEY_LOCATION = "location"

    // Booleani che controllano quale mappa visualizzare
    private var umtsBoolean = false
    private val UMTS_BOOLEAN_KEY = "umts-boolean"
    private var lteBoolean = false
    private val LTE_BOOLEAN_KEY = "lte-boolean"
    private var wifiBoolean = false
    private val WIFI_BOOLEAN_KEY = "wifi-boolean"

    // Boolean che controlla se deve ottenere o meno i dati
    private var startBoolean = false
    private val START_KEY = "start"

    // Referenze ai pulsanti del menu per modificarne le icone a runtime
    private var umtsItem: MenuItem? = null
    private var lteItem: MenuItem? = null
    private var wifiItem: MenuItem? = null

    // Lista delle coordinate ottenute dal dispositivo
    var umtsList = mutableListOf<WeightedLatLng>()
    var lteList = mutableListOf<WeightedLatLng>()
    var wifiList = mutableListOf<WeightedLatLng>()

    val PRECISION = 5

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

        // Inizializzazione del FusedLocationProvider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Recupera il Fragment in cui mostrare la mappa
        // Nota: è necessario il cast a MapFragment in quanto la funzione ritorna un Fragment
        val mapFragment = fragmentManager.findFragmentById(R.id.main_map) as MapFragment
        // Acquisisce la mappa e la inizializza quando l'istanza GoogleMap è pronta per essere utilizzata
        mapFragment.getMapAsync(this)
        // Crea la richiesta di aggiornamenti continui sulla posizione
        createLocationRequest()
        // Crea l'oggetto che si occuperà di eseguire i comandi dopo aver ottenuto la posizione
        createLocationCallback()

        // Inizializzazione del ConnectivityManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        createNetworkRequest()
        createNetworkCallback()

        // Ottiene i permessi per utilizzare la posizione
        getLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        startNetworkUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        stopNetworkUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        outState?.putParcelable(KEY_LOCATION, currentLocation)
        outState?.putBoolean(START_KEY, startBoolean)
        outState?.putBoolean(UMTS_BOOLEAN_KEY, umtsBoolean)
        outState?.putBoolean(LTE_BOOLEAN_KEY, lteBoolean)
        outState?.putBoolean(WIFI_BOOLEAN_KEY, wifiBoolean)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        // Chiama la superclasse in modo da recuperare la gerarchia della view
        super.onRestoreInstanceState(savedInstanceState)
        // Recupera lo stato delle variabili dall'istanza salvata
        requestingLocationUpdates = savedInstanceState?.getBoolean(REQUESTING_LOCATION_UPDATES_KEY) as Boolean
        currentLocation = savedInstanceState.getParcelable(KEY_LOCATION)
        startBoolean = savedInstanceState.getBoolean(START_KEY)
        umtsBoolean = savedInstanceState.getBoolean(UMTS_BOOLEAN_KEY)
        lteBoolean = savedInstanceState.getBoolean(LTE_BOOLEAN_KEY)
        wifiBoolean = savedInstanceState.getBoolean(WIFI_BOOLEAN_KEY)
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permessi non ottenuti
            // Fornisce una spiegazione all'utente se quest'ultimo nega più volte i permessi per accedere alla posizione
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
        // Aggiorna l'interfaccia della mappa (mostra o nasconde i comandi)
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
                //map?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom))
                //map?.uiSettings?.isMyLocationButtonEnabled = false
                //todo mostra pulsante per ottenere i permessi
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

                    // todo non è il posto giusto
                    //connectionText.text = currentNetwork
                }
            }
        }
    }

    private fun createNetworkRequest() {
        networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
    }

    private fun createNetworkCallback() {

        // I metodi in questa classe vengono chiamati quando la connessione di rete cambia
        networkCallback = object : ConnectivityManager.NetworkCallback() {

            // Una rete diventa disponibile
            override fun onAvailable(network: Network) {
                Toast.makeText(this@MainActivity, "onAvailable", Toast.LENGTH_LONG).show()
                networkChanged(network)
            }

            // Un cambio di capacità può indicare che è cambiato il tipo di connessione
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                Toast.makeText(this@MainActivity, "onCapabilitiesChanged", Toast.LENGTH_LONG).show()
                networkChanged(network)
            }

            // Un cambio di connessione può indicare che è cambiato l'indirizzo IP
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                Toast.makeText(this@MainActivity, "onLinkPropertiesChanged", Toast.LENGTH_LONG).show()
                networkChanged(network)
            }

            // Indica che perderemo la connessione tra maxMsToLive millisecondi
            override fun onLosing(network: Network, maxMsToLive: Int) {
                // TODO
                Toast.makeText(this@MainActivity, "onLosing", Toast.LENGTH_LONG).show()
            }

            // Indica che abbiamo perso la rete
            override fun onLost(network: Network) {
                // TODO
                Toast.makeText(this@MainActivity, "onLost", Toast.LENGTH_LONG).show()
            }

            // Nessuna rete è disponibile
            override fun onUnavailable() {
                // TODO
                Toast.makeText(this@MainActivity, "onUnavailable", Toast.LENGTH_LONG).show()
            }

            private fun networkChanged(network: Network) {
                val networkCapabilities = connectivityManager!!.getNetworkCapabilities(network)
                val networkInfo = connectivityManager!!.getNetworkInfo(network)

                if (networkInfo.isConnected) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        runOnUiThread({
                            nameText1.text = getWifiName()
                            typeText1.text = getString(R.string.wifi)
                            intensityText1.text = getString(R.string.intensity1, getWifiIntensity() + 1, PRECISION)
                        })
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        runOnUiThread({
                            nameText1.text = getCarrierName()
                            val networkType = getNetworkType(networkInfo)
                            typeText1.text = networkType
                            intensityText1.text = getString(R.string.intensity1, getNetworkIntensity(networkType) + 1, PRECISION)
                        })
                    }
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

    private fun startNetworkUpdates() {
        if (!requestingNetworkUpdates) {
            requestingNetworkUpdates = true
            connectivityManager?.registerNetworkCallback(
                    networkRequest,
                    networkCallback)
        }
    }

    private fun stopNetworkUpdates() {
        if (requestingNetworkUpdates) {
            requestingNetworkUpdates = false
            connectivityManager?.unregisterNetworkCallback(networkCallback)
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

    // TODO: handle screen rotate
    private fun updateIcons() {
        if(umtsBoolean) umtsItem?.setIcon(R.drawable.ic_cellular_on)
        else umtsItem?.setIcon(R.drawable.ic_cellular_off)

        if(lteBoolean) lteItem?.setIcon(R.drawable.ic_cellular_on)
        else lteItem?.setIcon(R.drawable.ic_cellular_off)

        if(wifiBoolean) wifiItem?.setIcon(R.drawable.ic_wifi_on)
        else wifiItem?.setIcon(R.drawable.ic_wifi_off)
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (startBoolean) menu?.findItem(R.id.start_item)?.setIcon(R.drawable.ic_pause)
        else menu?.findItem(R.id.start_item)?.setIcon(R.drawable.ic_play)
        updateIcons()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.start_item -> {
                startBoolean = !startBoolean
                invalidateOptionsMenu()
            }
            R.id.refresh -> {
                val alert = AlertDialog.Builder(this)
                alert.setMessage(R.string.alert_dialog_message)
                        .setTitle(R.string.alert_dialog_title)
                alert.setPositiveButton(R.string.alert_dialog_positive, {
                    _, _ ->
                    startBoolean = false
                    invalidateOptionsMenu()
                    // Svuota la lista
                    wifiList.clear()
                })
                alert.setNegativeButton(R.string.alert_dialog_negative, {
                    _, _ ->  //niente
                })
                val dialog = alert.create()
                dialog.show()
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Aggiorna le icone del Navigation Drawer quando vengono selezionate
        when (item.itemId) {
            R.id.umts_item -> {
                umtsItem = item
                umtsBoolean = !umtsBoolean
                umts_switch.isChecked = umtsBoolean
            }
            R.id.lte_item -> {
                lteItem = item
                lteBoolean = !lteBoolean
                lte_switch.isChecked = lteBoolean
            }
            R.id.wifi_item -> {
                wifiItem = item
                wifiBoolean = !wifiBoolean
                wifi_switch.isChecked = wifiBoolean
            }
            R.id.settings -> {
                // TODO: implementa impostazioni
            }
        }
        invalidateOptionsMenu()
        // Chiude il Navigation Drawer
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun checkLocation() {
        if (currentLocation != previousLocation) { //todo da migliorare
            if (wifiBoolean) {
                //addUmtsLocation(currentLocation!!)
                //addWifiLocation(currentLocation!!)
            }
        }
    }

    private fun addUmtsLocation(location: Location) {
        val latLng = getUmtsWeight(location)
        // Aggiunge la posizione attuale alla lista
        umtsList.add(latLng)
    }

    /*private fun addWifiLocation(location: Location) {
        val latLng = getWifiWeight(location)
        // Aggiunge la posizione attuale alla lista
        wifiList.add(latLng)

    }*/

    private fun getUmtsWeight(location: Location) : WeightedLatLng {
        var intensity = WeightedLatLng.DEFAULT_INTENSITY

       //@TargetApi(Build.VERSION_CODES.O)
       // if (!telephonyManager.isDataEnabled) Toast.makeText(this, "Turn on data", Toast.LENGTH_SHORT).show()

        try {
                val cellList = telephonyManager.allCellInfo
                if (cellList != null && cellList.isNotEmpty()){
                    val cellInfoWcdma = telephonyManager.allCellInfo[0] as CellInfoWcdma //todo android.telephony.CellInfoLte cannot be cast to android.telephony.CellInfoWcdma
                    intensity = cellInfoWcdma.cellSignalStrength.level.toDouble()
                    //textUMTS1.text = (intensity + 1).toInt().toString()
                }

        } catch (e: SecurityException) {
        }
        return WeightedLatLng(LatLng(location.latitude, location.longitude), intensity)
    }

    /*private fun getWifiWeight(location: Location) : WeightedLatLng {
        if (wifiManager.isWifiEnabled) {


        }
        return WeightedLatLng(LatLng(location.latitude, location.longitude), intensity)
    }*/



    private fun getWifiName(): String {
        var string = wifiManager.connectionInfo.ssid
        string = string.drop(1).dropLast(1) //Rimuove le virgolette dal nome
        return string
    }

    private fun getWifiIntensity(): Int {
        var intensity = 0
        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo != null) {
            intensity = WifiManager.calculateSignalLevel(wifiInfo.rssi, PRECISION)
        }
        return intensity
    }

    private fun getCarrierName(): String {
        return telephonyManager.networkOperatorName
    }

    private fun getNetworkType(networkInfo: NetworkInfo): String {
        when (networkInfo.subtype) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN ->
                return "2G"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP ->
                return "3G"
            TelephonyManager.NETWORK_TYPE_LTE ->
                return "4G"
            else ->
                return "UNKNOWN"
        }
    }

    private fun getNetworkIntensity(string: String): Int {
        var intensity = 0
        try {
            val cellList = telephonyManager.allCellInfo
            if (cellList != null && cellList.isNotEmpty()) {
                val cellInfo = telephonyManager.allCellInfo[0]
                when (string) {
                    "2G" -> intensity = (cellInfo as CellInfoGsm).cellSignalStrength.level
                    "3G" -> intensity = (cellInfo as CellInfoWcdma).cellSignalStrength.level
                    "4G" -> intensity = (cellInfo as CellInfoLte).cellSignalStrength.level
                }
            }
        } catch (e: SecurityException) {
        }
        return intensity
    }
}

//todo controlli is enabled sugli interruttori, mostrare la mappa solo dopo aver ottenuto i permessi