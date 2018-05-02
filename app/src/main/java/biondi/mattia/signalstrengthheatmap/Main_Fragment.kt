package biondi.mattia.signalstrengthheatmap

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class Main_Fragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Riempie la View con Main Layout
        return inflater!!.inflate(R.layout.content_main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Comandi da eseguire dopo che la View si è mostrata all'utente

        // Riporta il titolo dell'Activity allo stato originale
        activity.setTitle(R.string.app_name)
    }
}