package biondi.mattia.signalstrengthheatmap

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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
        NavigationView.OnNavigationItemSelectedListener{

    // Costrutto del Connectivity Manager
    private var connectivityManager: ConnectivityManager? = null
    private lateinit var wifiManager: WifiManager
    private lateinit var telephonyManager: TelephonyManager

    private lateinit var networkRequest: NetworkRequest

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private var requestingNetworkUpdates = false

    // Booleani che controllano quale mappa visualizzare
    private var umtsBoolean = false
    private val UMTS_BOOLEAN_KEY = "umts-boolean"
    private var lteBoolean = false
    private val LTE_BOOLEAN_KEY = "lte-boolean"
    private var wifiBoolean = false
    private val WIFI_BOOLEAN_KEY = "wifi-boolean"

    // Referenze ai pulsanti del menu per modificarne le icone a runtime
    private lateinit var umtsItem: MenuItem
    private lateinit var lteItem: MenuItem
    private lateinit var wifiItem: MenuItem

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

        // Inizializzazione del ConnectivityManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        createNetworkRequest()
        createNetworkCallback()
    }

    override fun onResume() {
        super.onResume()
        addFragment()
        startNetworkUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopNetworkUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(UMTS_BOOLEAN_KEY, umtsBoolean)
        outState?.putBoolean(LTE_BOOLEAN_KEY, lteBoolean)
        outState?.putBoolean(WIFI_BOOLEAN_KEY, wifiBoolean)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        // Chiama la superclasse in modo da recuperare la gerarchia della view
        super.onRestoreInstanceState(savedInstanceState)
        // Recupera lo stato delle variabili dall'istanza salvata
        if (savedInstanceState != null) {
            umtsBoolean = savedInstanceState.getBoolean(UMTS_BOOLEAN_KEY)
            lteBoolean = savedInstanceState.getBoolean(LTE_BOOLEAN_KEY)
            wifiBoolean = savedInstanceState.getBoolean(WIFI_BOOLEAN_KEY)
        }
    }

    private fun locationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    } //todo make it global

    private fun addFragment() {
        if (locationPermission()){
            fragmentManager.beginTransaction().replace(R.id.fragment_frame, MapFragment()).commit()
        } else {
            fragmentManager.beginTransaction().replace(R.id.fragment_frame, LocationPermissionFragment()).commit()
        }
    } //todo make it global

    // Si occupa del risultato delle richieste
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // Se la richiesta viene cancellata gli array risultanti sono vuoti
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permessi ottenuti
                    addFragment()
                } else {
                    // Permessi negati
                    Toast.makeText(this, "¯\\_(ツ)_/¯", Toast.LENGTH_LONG).show()
                }
                return
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
                networkChanged(network)
            }

            // Un cambio di capacità può indicare che è cambiato il tipo di connessione
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                networkChanged(network)
            }

            // Un cambio di connessione può indicare che è cambiato l'indirizzo IP
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                networkChanged(network)
            }

            // Indica che perderemo la connessione tra maxMsToLive millisecondi
            override fun onLosing(network: Network, maxMsToLive: Int) {
                networkLosing(maxMsToLive)
            }

            // Indica che abbiamo perso la rete
            override fun onLost(network: Network) {
                networkLost()
            }

            private fun networkChanged(network: Network) {
                val networkCapabilities = connectivityManager!!.getNetworkCapabilities(network)
                val networkInfo = connectivityManager!!.getNetworkInfo(network)
                var intensity: Int

                if (networkInfo.isConnected) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        runOnUiThread({
                            nameText1.text = getWifiName()
                            typeText1.text = getString(R.string.wifi)
                            intensity = getWifiIntensity()
                            intensityText1.text = getString(R.string.intensity1, intensity + 1, PRECISION)
                            getQuality(intensity)
                        })
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        runOnUiThread({
                            nameText1.text = getCarrierName()
                            val networkType = getNetworkType(networkInfo)
                            typeText1.text = networkType
                            intensity = getNetworkIntensity(networkType)
                            intensityText1.text = getString(R.string.intensity1,  intensity + 1, PRECISION)
                            getQuality(intensity)
                        })
                    }
                } else if (!networkInfo.isConnected) {
                    runOnUiThread({
                        nameText1.text = getString(R.string.not_connected)
                        typeText1.text = getString(R.string.not_connected)
                        intensityText1.text = getString(R.string.not_connected)
                        getQuality(0)
                    })
                }
            }

            //todo testala
            private fun networkLosing(int: Int) {
                runOnUiThread({
                    val seconds = int * 1000 // milliseconds to seconds
                    intensityText1.text = resources.getQuantityString(R.plurals.losing_connection_in, seconds, seconds)
                })
            }

            private fun networkLost() {
                runOnUiThread({
                    nameText1.text = getString(R.string.not_available)
                    typeText1.text = getString(R.string.not_available)
                    intensityText1.text = getString(R.string.intensity1,  0 + 1, PRECISION)
                    getQuality(0)
                })
            }
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

    // TODO: handle screen rotate
    private fun updateIcons() {
        if(umtsBoolean) umtsItem.setIcon(R.drawable.ic_cellular_on)
        else umtsItem.setIcon(R.drawable.ic_cellular_off)

        if(lteBoolean) lteItem.setIcon(R.drawable.ic_cellular_on)
        else lteItem.setIcon(R.drawable.ic_cellular_off)

        if(wifiBoolean) wifiItem.setIcon(R.drawable.ic_wifi_on)
        else wifiItem.setIcon(R.drawable.ic_wifi_off)
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
                    //wifiList.clear()
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

    private fun getQuality(int: Int) {
        lateinit var string: String
        var color: Int = R.string.none
        when (int) {
            0 -> {
                string = resources.getString(R.string.none)
                color = ContextCompat.getColor(this, R.color.none)
            }
            1 -> {
                string = resources.getString(R.string.poor)
                color = ContextCompat.getColor(this, R.color.poor)
            }
            2 -> {
                string = resources.getString(R.string.moderate)
                color = ContextCompat.getColor(this, R.color.moderate)
            }
            3 -> {
                string = resources.getString(R.string.good)
                color = ContextCompat.getColor(this, R.color.good)
            }
            4 -> {
                string = resources.getString(R.string.great)
                color = ContextCompat.getColor(this, R.color.great)
            }
        }
        qualityText.text = string
        qualityText.setTextColor(color)
    }
}

//todo controlli is enabled sugli interruttori, mostrare la mappa solo dopo aver ottenuto i permessi