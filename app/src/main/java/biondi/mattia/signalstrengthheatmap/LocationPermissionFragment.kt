package biondi.mattia.signalstrengthheatmap

import android.app.Fragment
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.location_permission_layout.*

class LocationPermissionFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Riempie la View con UMTS HexagonLayout
        return inflater!!.inflate(R.layout.location_permission_layout, container, false)
    }

    override fun onStart() {
        super.onStart()
        activity.invalidateOptionsMenu()
        val button = locationPermissionRequestButton
        button.setOnClickListener { getLocationPermission() }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(activity.applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permessi non ottenuti
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        } else {
            // Permessi già ottenuti
            addFragment()
        }
    }

    private fun addFragment() {
        if (locationPermission()){
            fragmentManager.beginTransaction().replace(R.id.fragment_frame, MapFragment()).commit()
        } else {
            fragmentManager.beginTransaction().replace(R.id.fragment_frame, LocationPermissionFragment()).commit()
        }
    }

    private fun locationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity.applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}