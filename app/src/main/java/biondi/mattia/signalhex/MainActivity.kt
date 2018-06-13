package biondi.mattia.signalhex

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.telephony.*
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.main_layout.*
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.content_layout.*
import kotlinx.android.synthetic.main.edge_switch_layout.*
import kotlinx.android.synthetic.main.edge_switch_layout.view.*
import kotlinx.android.synthetic.main.lte_switch_layout.*
import kotlinx.android.synthetic.main.lte_switch_layout.view.*
import kotlinx.android.synthetic.main.umts_switch_layout.*
import kotlinx.android.synthetic.main.umts_switch_layout.view.*
import kotlinx.android.synthetic.main.wifi_switch_layout.*
import kotlinx.android.synthetic.main.wifi_switch_layout.view.*

class MainActivity :
        AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener {

    // Costrutto del Connectivity Manager
    private var connectivityManager: ConnectivityManager? = null
    private lateinit var wifiManager: WifiManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var phoneStateListener: PhoneStateListener

    private lateinit var networkRequest: NetworkRequest
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private var requestingNetworkUpdates = false

    private var networkIntensity = 0 // TODO evita variabile globale

    private val PRECISION = 5

    override fun onCreate(savedInstanceState: Bundle?) { //todo fai partire tutti gli switch su true
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
        createPhoneStateListener()
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
        outState?.putBoolean(EDGE_BOOLEAN_KEY, edgeBoolean)
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
            edgeBoolean = savedInstanceState.getBoolean(EDGE_BOOLEAN_KEY)
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
            }

            // Indica che abbiamo perso la rete
            override fun onLost(network: Network) {
                networkLost()
            }

            private fun networkChanged(network: Network) {
                val networkCapabilities = connectivityManager!!.getNetworkCapabilities(network)
                val networkInfo = connectivityManager!!.getNetworkInfo(network)
                var intensity: Int

                if (networkInfo != null && networkInfo.isConnected) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        runOnUiThread({
                            nameText1.text = getWifiName()
                            currentNetwork = getString(R.string.wifi)
                            typeText1.text = currentNetwork
                            intensity = getWifiIntensity()
                            currentIntensity = intensity
                            intensityText1.text = getString(R.string.intensity1, intensity, PRECISION-1)
                            getQuality(intensity)
                        })
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        // Registra il listener qua per evitare di avere continui aggiornamenti se si utilizza solo il Wi-Fi
                        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                        runOnUiThread({
                            nameText1.text = getCarrierName()
                            currentNetwork = getNetworkType()
                            typeText1.text = currentNetwork
                            intensity = networkIntensity
                            currentIntensity = intensity
                            intensityText1.text = getString(R.string.intensity1,  intensity, PRECISION-1)
                            getQuality(intensity)
                        })
                    }
                }
            }

            private fun networkLost() {
                // La funzione viene chiamata riferendosi alla rete mobile quando ci si connette al Wi-Fi,
                // mostrando informazioni sbagliate a schermo. Si assicura quindi che realmente non ci sia nessuna rete
                // attiva
                val networkInfo = connectivityManager!!.activeNetworkInfo
                if (networkInfo==null || !networkInfo.isConnected) {
                    runOnUiThread({
                        nameText1.text = getString(R.string.not_connected)
                        currentNetwork = getString(R.string.none)
                        typeText1.text = getString(R.string.not_connected)
                        currentIntensity = 0
                        intensityText1.text = getString(R.string.not_connected)
                        getQuality(0)
                    })
                }
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
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }
    }

    private fun updateIcons() {
        val edge_item = nav_view.menu.findItem(R.id.edge_item)
        val umts_item = nav_view.menu.findItem(R.id.umts_item)
        val lte_item = nav_view.menu.findItem(R.id.lte_item)
        val wifi_item = nav_view.menu.findItem(R.id.wifi_item)

        if (locationPermission()) {
            edge_item.isEnabled = true
            edge_item.actionView.edge_switch.isEnabled = true
            if(edgeBoolean) edge_item.setIcon(R.drawable.ic_cellular_on)
            else edge_item.setIcon(R.drawable.ic_cellular_off)

            umts_item.isEnabled = true
            umts_item.actionView.umts_switch.isEnabled = true
            if(umtsBoolean) umts_item.setIcon(R.drawable.ic_cellular_on)
            else umts_item.setIcon(R.drawable.ic_cellular_off)

            lte_item.isEnabled = true
            lte_item.actionView.lte_switch.isEnabled = true
            if(lteBoolean) lte_item.setIcon(R.drawable.ic_cellular_on)
            else lte_item.setIcon(R.drawable.ic_cellular_off)

            wifi_item.isEnabled = true
            wifi_item.actionView.wifi_switch.isEnabled = true
            if(wifiBoolean) wifi_item.setIcon(R.drawable.ic_wifi_on)
            else wifi_item.setIcon(R.drawable.ic_wifi_off)
        } else {
            edgeBoolean = false
            edge_item.setIcon(R.drawable.ic_cellular_off)
            edge_item.isEnabled = false
            edge_item.actionView.edge_switch.isChecked = false
            edge_item.actionView.edge_switch.isEnabled = false

            umtsBoolean = false
            umts_item.setIcon(R.drawable.ic_cellular_off)
            umts_item.isEnabled = false
            umts_item.actionView.umts_switch.isChecked = false
            umts_item.actionView.umts_switch.isEnabled = false

            lteBoolean = false
            lte_item.setIcon(R.drawable.ic_cellular_off)
            lte_item.isEnabled = false
            lte_item.actionView.lte_switch.isChecked = false
            lte_item.actionView.lte_switch.isEnabled = false

            wifiBoolean = false
            wifi_item.setIcon(R.drawable.ic_wifi_off)
            wifi_item.isEnabled = false
            wifi_item.actionView.wifi_switch.isChecked = false
            wifi_item.actionView.wifi_switch.isEnabled = false
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (locationPermission()) {
            menu.findItem(R.id.start_item).isEnabled = true
            menu.findItem(R.id.refresh_item).isEnabled = true
            if (startBoolean) menu.findItem(R.id.start_item).setIcon(R.drawable.ic_pause)
            else menu.findItem(R.id.start_item).setIcon(R.drawable.ic_play_enabled)
        } else {
            startBoolean = false
            menu.findItem(R.id.start_item).setIcon(R.drawable.ic_play_disabled)
            menu.findItem(R.id.start_item).isEnabled = false

            menu.findItem(R.id.refresh_item).setIcon(R.drawable.ic_refresh_disabled)
            menu.findItem(R.id.refresh_item).isEnabled = false
        }
        updateIcons()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.start_item -> {
                startBoolean = !startBoolean
                invalidateOptionsMenu()
            }
            R.id.refresh_item -> {
                val alert = AlertDialog.Builder(this)
                alert.setMessage(R.string.alert_dialog_message)
                        .setTitle(R.string.alert_dialog_title)
                alert.setPositiveButton(R.string.alert_dialog_positive, {
                    _, _ ->
                    startBoolean = false
                    invalidateOptionsMenu()
                    // Svuota le liste
                    clearLists()
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
            R.id.edge_item -> {
                edgeBoolean = !edgeBoolean
                edge_switch.isChecked = edgeBoolean
                setVisibility(edgePolygon, edgeBoolean)
            }
            R.id.umts_item -> {
                umtsBoolean = !umtsBoolean
                umts_switch.isChecked = umtsBoolean
                setVisibility(umtsPolygon, umtsBoolean)
            }
            R.id.lte_item -> {
                lteBoolean = !lteBoolean
                lte_switch.isChecked = lteBoolean
                setVisibility(ltePolygon, lteBoolean)
            }
            R.id.wifi_item -> {
                wifiBoolean = !wifiBoolean
                wifi_switch.isChecked = wifiBoolean
                setVisibility(wifiPolygon, wifiBoolean)
            }
            R.id.settings -> {
                // TODO: implementa impostazioni
            }
        }
        invalidateOptionsMenu()
        // Chiude il Navigation Drawer
        //drawer_layout.closeDrawer(GravityCompat.START)
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

    private fun createPhoneStateListener() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                networkIntensity = signalStrength.level
            }
        }
    }

    private fun getCarrierName(): String {
        return telephonyManager.networkOperatorName
    }

    private fun getNetworkType(): String {
        when (telephonyManager.networkType) {
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

    private fun getQuality(int: Int) {
        lateinit var string: String
        val color: Int
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
            else -> {
                string = "-1"
                color = Color.CYAN
            }
        }

        qualityText.text = string
        qualityText.setTextColor(color)
    }
}