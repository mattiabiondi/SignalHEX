package biondi.mattia.signalhex

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Point
import kotlin.math.*

/** Thanks to
 * https://www.redblobgames.com/grids/hexagons/
 */

// Esagono in forma "astratta"
// Ha due coordinata, la terza è calcolata in quanto si deriva dal valore delle altre due
class Hexagon(var x: Double, var y: Double) {
    var z = (-x - y)

    // Controlla che la somma delle coordinate faccia zero
    init {
        if (x + y + z != 0.0) throw IllegalArgumentException("x + y + z must be 0")
    }

    // Spesso appare il valore -0, che è diverso da 0 quando si confrontano le stringhe.
    // Questa funzione lo riporta a 0
    fun hexagonToString(): String {
        val x = if (abs(x) == 0.0) "0.0" else x.toString()
        val y = if (abs(y) == 0.0) "0.0" else y.toString()
        return "$x $y"
    }
}

// Orientamento dell'esagono
class Orientation(val f0: Double, val f1: Double, val f2: Double, val f3: Double,
                  val b0: Double, val b1: Double, val b2: Double, val b3: Double,
                  val start_angle: Double)

// Esagono con lato sopra e sotto
val layout_flat = Orientation(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0,
        sqrt(3.0) / 3.0, -1.0 / 3.0, 0.0, 2.0 / 3.0,
        0.5)

// Esagono con spigolo sopra e sotto
val layout_pointy = Orientation(3.0 / 2.0, 0.0, sqrt(3.0) / 2.0, sqrt(3.0),
        2.0 / 3.0, 0.0, -1.0 / 3.0, sqrt(3.0) / 3.0,
        0.0)

// L'esagono vero e proprio
class HexagonLayout(val orientation: Orientation, val size: Point, private val origin: LatLng) {

    // Conversione da coordinate esagonali a LatLng
    private fun hexagonToLatLng(hex: Hexagon): LatLng {
        val x = (orientation.f0 * hex.x + orientation.f1 * hex.y) * size.x
        val y = (orientation.f2 * hex.x + orientation.f3 * hex.y) * size.y
        return LatLng(x + origin.latitude, y + origin.longitude)
    }

    // Conversione da LatLng a coordinate esagonali
    private fun latLngToHexagon(latLng: LatLng): Hexagon {
        val lt = LatLng((latLng.latitude - origin.latitude) / size.x,
                (latLng.longitude - origin.longitude) / size.y)
        val x = orientation.b0 * lt.latitude + orientation.b1 * lt.longitude
        val y = orientation.b2 * lt.latitude + orientation.b3 * lt.longitude
        return Hexagon(x, y)
    }

    // Calcola la posizione dello spigolo
    private fun cornerOffset(corner: Int): LatLng {
        val angle = 2.0 * PI * (orientation.start_angle + corner) / 6
        return LatLng(size.x * cos(angle), size.y * sin(angle))
    }

    // Ottiene i 6 spigoli
    fun getCorners(hex: Hexagon): List<LatLng> {
        val corners = mutableListOf<LatLng>()
        val center = hexagonToLatLng(hex)
        for (i in 0 until 6) {
            val offset = cornerOffset(i)
            corners.add(LatLng(center.latitude + offset.latitude, center.longitude + offset.longitude))
        }
        return corners
    }

    // Calcola l'esagono da creare rispettando la griglia in base alla posizione in cui siamo
    fun nearestHexagon(origin: LatLng): Hexagon {
        val hexagon = latLngToHexagon(origin)

        var rx = round(hexagon.x)
        var ry = round(hexagon.y)
        val rz = round(hexagon.z)

        val xDiff = abs(rx - hexagon.x)
        val yDiff = abs(ry - hexagon.y)
        val zDiff = abs(rz - hexagon.z)

        if ((xDiff > yDiff) && (xDiff > zDiff))
            rx = -ry - rz
        else if (yDiff > zDiff)
            ry = -rx - rz

        return Hexagon(rx, ry)
    }
}

// Modifica le dimensioni degli esagoni
class HexagonsDimension : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Hexagons dimension")
                .setSingleChoiceItems(R.array.hexagons_dimension_list, hexagonsDimension) { _, which ->
                    hexagonsDimension = which
                    dismiss()
                    activity.recreate() //TODO penso si possa fare di meglio 1
                }
        return builder.create()
    }
}

// Modifica i colori degli esagoni
class HexagonsColors : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Hexagons colors")
                .setSingleChoiceItems(R.array.hexagons_colors_list, hexagonsColors) { _, which ->
                    hexagonsColors = which
                    dismiss()
                    activity.recreate() //TODO penso si possa fare di meglio 2
                }
        return builder.create()
    }
}

// Modifica la trasparenza degli esagoni
class HexagonsTransparency : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Hexagons transparency")
                .setSingleChoiceItems(R.array.hexagons_transparency_list, hexagonsAlpha) { _, which ->
                    hexagonsAlpha = which
                    dismiss()
                    activity.recreate() //TODO penso si possa fare di meglio 3
                }
        return builder.create()
    }
}