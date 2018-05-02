package biondi.mattia.signalstrengthheatmap

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback

class WIFI_Fragment : Fragment(), OnMapReadyCallback {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Riempie la View con Wi-Fi Layout
        return inflater!!.inflate(R.layout.wifi_fragment, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Comandi da eseguire dopo che la View si è mostrata all'utente

        // Imposta il titolo dell'Activity a "Wi-Fi"
        activity.setTitle(R.string.wifi)

        // Recupera il Fragment in cui mostrare la Wi-Fi Map (la mappa con solo il segnale Wi-Fi)
        // Utilizza childFragmentManager perchè il Map Fragment è un Fragment nel Fragment
        // Nota: è necessario il cast a MapFragment in quanto la funzione ritorna un Fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.wifi_map) as MapFragment

        // Acquisisce la mappa e la inizializza quando l'istanza GoogleMap è pronta per essere utilizzata
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
    }
}