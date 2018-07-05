package biondi.mattia.signalhex

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.location_permission_layout.*

class LocationPermissionFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Riempie la View con location permission layout
        return inflater.inflate(R.layout.location_permission_layout, container, false)
    }

    override fun onStart() {
        super.onStart()

        // Se siamo qua allora i tasti vanno disattivati
        activity!!.invalidateOptionsMenu()

        // Bottone a schermo per richiedere i permessi
        val button = locationPermissionRequestButton
        button.setOnClickListener { getLocationPermission() }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permessi non ottenuti
            ActivityCompat.requestPermissions(activity as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        } else {
            // Permessi già ottenuti
            addFragment()
        }
    }

    // Aggiunge il Fragment corretto
    private fun addFragment() {
        if (locationPermission()) {
            fragmentManager!!.beginTransaction().replace(R.id.fragment_frame, MapFragment()).commit()
        } else {
            fragmentManager!!.beginTransaction().replace(R.id.fragment_frame, LocationPermissionFragment()).commit()
        }
    }

    // Controlla se si hanno i permessi di posizione
    private fun locationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity!!.applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}