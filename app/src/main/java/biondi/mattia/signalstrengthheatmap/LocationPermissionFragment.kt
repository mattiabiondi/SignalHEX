package biondi.mattia.signalstrengthheatmap

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class LocationPermissionFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Riempie la View con UMTS Layout
        return inflater!!.inflate(R.layout.location_permission_layout, container, false)
    }
}