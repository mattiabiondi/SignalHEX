package biondi.mattia.signalhex

import android.arch.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Polygon

class MapViewModel: ViewModel() {

    // La mappa
    var map: GoogleMap? = null

    /*
    // Lista degli esagoni disegnati sulla mappa
    var edgePolygon = mutableListOf<Polygon>()
    var umtsPolygon = mutableListOf<Polygon>()
    var ltePolygon = mutableListOf<Polygon>()
    var wifiPolygon = mutableListOf<Polygon>()

    var edgeHexagon = mutableListOf<Hexagon>()
    var umtsHexagon = mutableListOf<Hexagon>()
    var lteHexagon = mutableListOf<Hexagon>()
    var wifiHexagon = mutableListOf<Hexagon>()

    var firstHexagon: HexagonLayout? = null
    */
}