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
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.maps.model.Polygon
import kotlinx.android.synthetic.main.app_bar_layout.*
import kotlinx.android.synthetic.main.content_layout.*
import kotlinx.android.synthetic.main.edge_switch_layout.*
import kotlinx.android.synthetic.main.edge_switch_layout.view.*
import kotlinx.android.synthetic.main.lte_switch_layout.*
import kotlinx.android.synthetic.main.lte_switch_layout.view.*
import kotlinx.android.synthetic.main.main_layout.*
import kotlinx.android.synthetic.main.map_satellite_layout.*
import kotlinx.android.synthetic.main.map_satellite_layout.view.*
import kotlinx.android.synthetic.main.umts_switch_layout.*
import kotlinx.android.synthetic.main.umts_switch_layout.view.*
import kotlinx.android.synthetic.main.wifi_switch_layout.*
import kotlinx.android.synthetic.main.wifi_switch_layout.view.*

// Codice di richiesta
const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

// Boolean che controlla se deve ottenere o meno i dati
var startBoolean = false

// Boolean che controllano quale mappa visualizzare
var edgeBoolean = true
var umtsBoolean = true
var lteBoolean = true
var wifiBoolean = true

// Boolean per visualizzare o meno la mappa satellitare
var satelliteBoolean = false

// La rete attuale a cui si è collegati
var currentNetwork = R.string.none.toString()
// L'intensità del segnale della rete attuale
var currentIntensity = 0

class MainActivity :
        AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener {

    // Costrutto dei Manager
    private var connectivityManager: ConnectivityManager? = null
    private lateinit var wifiManager: WifiManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var phoneStateListener: PhoneStateListener

    private lateinit var networkRequest: NetworkRequest
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    // Boolean per sapere se stiamo richiedendo o meno aggiornamenti dalla rete
    private var requestingNetworkUpdates = false

    // L'intensità del segnale della rete attuale
    private var networkIntensity = 0

    // Con quanta precisione l'intensità del segnale viene analizzata
    private val precision = 5

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

        // Inizializzazione dei Manager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        createPhoneStateListener()
        createNetworkRequest()
        createNetworkCallback()
    }

    override fun onResume() {
        super.onResume()

        // Aggiunge il fragment corretto
        addFragment()

        // Avvia la richiesta di aggiornamenti del segnale
        startNetworkUpdates()
    }

    override fun onPause() {
        super.onPause()

        // Ferma gli aggiornamenti del segnale
        stopNetworkUpdates()
    }

    // Controlla se abbiamo o no i permessi
    private fun locationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun addFragment() {
        // Se si possiedono i permessi di posizione allora si visualizza la mappa, altrimenti si visualizza il Fragment che richiede i permessi
        if (locationPermission()) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_frame, MapFragment()).commit()
        } else {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_frame, LocationPermissionFragment()).commit()
        }
    }

    // Si occupa del risultato delle richieste
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // Se la richiesta viene cancellata gli array risultanti sono vuoti
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    // Crea la richiesta di rete
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
                        runOnUiThread {
                            nameText1.text = getWifiName()
                            currentNetwork = getString(R.string.wifi)
                            typeText1.text = currentNetwork
                            intensity = getWifiIntensity()
                            currentIntensity = intensity
                            intensityText1.text = getString(R.string.intensity1, intensity, precision - 1)
                            getQuality(intensity)
                        }
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        // Registra il listener qua per evitare di avere continui aggiornamenti se si utilizza solo il Wi-Fi
                        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                        runOnUiThread {
                            nameText1.text = getCarrierName()
                            currentNetwork = getNetworkType()
                            typeText1.text = currentNetwork
                            intensity = networkIntensity
                            currentIntensity = intensity
                            intensityText1.text = getString(R.string.intensity1, intensity, precision - 1)
                            getQuality(intensity)
                        }
                    }
                }
            }

            private fun networkLost() {
                // La funzione viene chiamata quando da rete mobile ci si connette al Wi-Fi, "perdendo" quindi la rete mobile,
                // mostrando informazioni sbagliate a schermo. Si assicura quindi che realmente non ci sia nessuna rete
                // attiva
                val networkInfo = connectivityManager!!.activeNetworkInfo
                if (networkInfo == null || !networkInfo.isConnected) {
                    runOnUiThread {
                        nameText1.text = getString(R.string.not_connected)
                        currentNetwork = getString(R.string.none)
                        typeText1.text = getString(R.string.not_connected)
                        currentIntensity = 0
                        intensityText1.text = getString(R.string.not_connected)
                        getQuality(0)
                    }
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

    // Quando si preme il tasto back
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

    // Funzione chiamata quando si creano i tasti del menù
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

    // Funzione per aggiornare le icone del menù
    private fun updateIcons() {
        val edgeItem = nav_view.menu.findItem(R.id.edge_item)
        val umtsItem = nav_view.menu.findItem(R.id.umts_item)
        val lteItem = nav_view.menu.findItem(R.id.lte_item)
        val wifiItem = nav_view.menu.findItem(R.id.wifi_item)

        val satelliteItem = nav_view.menu.findItem(R.id.map_satellite)

        val dimensionItem = nav_view.menu.findItem(R.id.hexagons_dimension)
        val colorsItem = nav_view.menu.findItem(R.id.hexagons_colors)
        val transparencyItem = nav_view.menu.findItem(R.id.hexagons_transparency)

        edgeItem.actionView.edge_switch.isChecked = edgeBoolean
        umtsItem.actionView.umts_switch.isChecked = umtsBoolean
        lteItem.actionView.lte_switch.isChecked = lteBoolean
        wifiItem.actionView.wifi_switch.isChecked = wifiBoolean
        satelliteItem.actionView.map_satellite_checkbox.isChecked = satelliteBoolean

        if (locationPermission()) {
            edgeItem.isEnabled = true
            edgeItem.actionView.edge_switch.isEnabled = true
            if (edgeBoolean) edgeItem.setIcon(R.drawable.ic_cellular_on)
            else edgeItem.setIcon(R.drawable.ic_cellular_off)

            umtsItem.isEnabled = true
            umtsItem.actionView.umts_switch.isEnabled = true
            if (umtsBoolean) umtsItem.setIcon(R.drawable.ic_cellular_on)
            else umtsItem.setIcon(R.drawable.ic_cellular_off)

            lteItem.isEnabled = true
            lteItem.actionView.lte_switch.isEnabled = true
            if (lteBoolean) lteItem.setIcon(R.drawable.ic_cellular_on)
            else lteItem.setIcon(R.drawable.ic_cellular_off)

            wifiItem.isEnabled = true
            wifiItem.actionView.wifi_switch.isEnabled = true
            if (wifiBoolean) wifiItem.setIcon(R.drawable.ic_wifi_on)
            else wifiItem.setIcon(R.drawable.ic_wifi_off)

            satelliteItem.isEnabled = true
            satelliteItem.actionView.map_satellite_checkbox.isEnabled = true

            dimensionItem.isEnabled = true
            colorsItem.isEnabled = true
            transparencyItem.isEnabled = true
        } else {
            edgeItem.isEnabled = false
            edgeItem.actionView.edge_switch.isEnabled = false
            if (edgeBoolean) edgeItem.setIcon(R.drawable.ic_cellular_on)
            else edgeItem.setIcon(R.drawable.ic_cellular_off1)

            umtsItem.isEnabled = false
            umtsItem.actionView.umts_switch.isEnabled = false
            if (umtsBoolean) umtsItem.setIcon(R.drawable.ic_cellular_on)
            else umtsItem.setIcon(R.drawable.ic_cellular_off1)

            lteItem.isEnabled = false
            lteItem.actionView.lte_switch.isEnabled = false
            if (lteBoolean) lteItem.setIcon(R.drawable.ic_cellular_on)
            else lteItem.setIcon(R.drawable.ic_cellular_off1)

            wifiItem.isEnabled = false
            wifiItem.actionView.wifi_switch.isEnabled = false
            if (wifiBoolean) wifiItem.setIcon(R.drawable.ic_wifi_on)
            else wifiItem.setIcon(R.drawable.ic_wifi_off1)

            satelliteItem.isEnabled = false
            satelliteItem.actionView.map_satellite_checkbox.isEnabled = false

            dimensionItem.isEnabled = false
            colorsItem.isEnabled = false
            transparencyItem.isEnabled = false
        }
    }

    // Funzione chiamata quando si preme un oggetto del menù in alto
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
                alert.setPositiveButton(R.string.alert_dialog_positive) { _, _ ->
                    startBoolean = false
                    invalidateOptionsMenu()
                    // Svuota le liste
                    clearLists(false)
                }
                alert.setNegativeButton(R.string.alert_dialog_negative) { _, _ ->
                    //niente
                }
                val dialog = alert.create()
                dialog.show()
            }
        }
        return true
    }

    // Funzione chiamata quando si preme un oggetto del menù laterale
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
            R.id.map_satellite -> {
                satelliteBoolean = !satelliteBoolean
                map_satellite_checkbox.isChecked = satelliteBoolean
                setMapType()
            }
            R.id.hexagons_dimension -> {
                HexagonsSize().show(fragmentManager, "hexagons size")
            }
            R.id.hexagons_colors -> {
                HexagonsColors().show(fragmentManager, "hexagons colors")
            }
            R.id.hexagons_transparency -> {
                HexagonsTransparency().show(fragmentManager, "hexagons transparency")
            }
        }
        invalidateOptionsMenu() // Richiama onPrepareOptionsMenu

        // Chiude il Navigation Drawer
        //drawer_layout.closeDrawer(GravityCompat.START)

        return true
    }

    // Si occupa di rendere visibili o meno i poligoni sulla mappa
    private fun setVisibility(list: MutableList<Polygon>, boolean: Boolean) {
        for (i in 0 until list.size) {
            list[i].isVisible = boolean
        }
    }

    // Ottiene il nome della rete Wi-Fi
    private fun getWifiName(): String {
        var string = wifiManager.connectionInfo.ssid
        string = string.drop(1).dropLast(1) //Rimuove le virgolette dal nome
        return string
    }

    //Ottiene l'intensità della rete Wi-Fi
    private fun getWifiIntensity(): Int {
        var intensity = 0
        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo != null) {
            intensity = WifiManager.calculateSignalLevel(wifiInfo.rssi, precision)
        }
        return intensity
    }

    // Crea il Phone State Listener
    private fun createPhoneStateListener() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                networkIntensity = signalStrength.level // API level 23
            }
        }
    }

    // Ottiene il nome dell'operatore di rete
    private fun getCarrierName(): String {
        return telephonyManager.networkOperatorName
    }

    // Ottiene il tipo di rete mobile a cui si è collegati
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

    // Mostra a schermo la qualità del segnale
    private fun getQuality(int: Int) {
        lateinit var string: String
        val color: Int
        when (int) {
            0 -> {
                string = resources.getString(R.string.none)
                color = when (hexagonsColors) {
                    0 -> ContextCompat.getColor(this, R.color.none0)
                    1 -> ContextCompat.getColor(this, R.color.none1)
                    2 -> ContextCompat.getColor(this, R.color.none2)
                    else -> Color.TRANSPARENT
                }
            }
            1 -> {
                string = resources.getString(R.string.poor)
                color = when (hexagonsColors) {
                    0 -> ContextCompat.getColor(this, R.color.poor0)
                    1 -> ContextCompat.getColor(this, R.color.poor1)
                    2 -> ContextCompat.getColor(this, R.color.poor2)
                    else -> Color.TRANSPARENT
                }
            }
            2 -> {
                string = resources.getString(R.string.moderate)
                color = when (hexagonsColors) {
                    0 -> ContextCompat.getColor(this, R.color.moderate0)
                    1 -> ContextCompat.getColor(this, R.color.moderate1)
                    2 -> ContextCompat.getColor(this, R.color.moderate2)
                    else -> Color.TRANSPARENT
                }
            }
            3 -> {
                string = resources.getString(R.string.good)
                color = when (hexagonsColors) {
                    0 -> ContextCompat.getColor(this, R.color.good0)
                    1 -> ContextCompat.getColor(this, R.color.good1)
                    2 -> ContextCompat.getColor(this, R.color.good2)
                    else -> Color.TRANSPARENT
                }
            }
            4 -> {
                string = resources.getString(R.string.great)
                color = when (hexagonsColors) {
                    0 -> ContextCompat.getColor(this, R.color.great0)
                    1 -> ContextCompat.getColor(this, R.color.great1)
                    2 -> ContextCompat.getColor(this, R.color.great2)
                    else -> Color.TRANSPARENT
                }
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