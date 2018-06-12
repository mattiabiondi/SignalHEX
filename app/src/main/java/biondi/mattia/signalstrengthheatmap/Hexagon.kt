package biondi.mattia.signalstrengthheatmap

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Point
import kotlin.math.*

var firstHexagon: HexagonLayout? = null

class Hexagon(var x: Double, var y: Double) {
    var z = (-x - y)

    init {
        if (x + y + z != 0.0) throw IllegalArgumentException("x + y + z must be 0")
    }
}

class Orientation(val f0: Double, val f1: Double, val f2: Double, val f3: Double,
                  val b0: Double, val b1: Double, val b2: Double, val b3: Double,
                  val start_angle: Double)

val layout_flat = Orientation(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0,
                            sqrt(3.0) / 3.0, -1.0 / 3.0, 0.0, 2.0 / 3.0,
                                0.5)

val layout_pointy = Orientation(3.0 / 2.0, 0.0, sqrt(3.0) / 2.0, sqrt(3.0),
                                2.0 / 3.0, 0.0, -1.0 / 3.0, sqrt(3.0) / 3.0,
                                0.0)

class HexagonLayout(val orientation: Orientation, val size: Point, val origin: LatLng) {

    private fun hexagonToLatLng(hex: Hexagon): LatLng {
        val x = (orientation.f0 * hex.x + orientation.f1 * hex.y) * size.x
        val y = (orientation.f2 * hex.x + orientation.f3 * hex.y) * size.y
        return LatLng(x + origin.latitude, y + origin.longitude)
    }

    private fun latLngToHexagon(latLng: LatLng): Hexagon {
        val lt = LatLng((latLng.latitude - origin.latitude) / size.x,
                (latLng.longitude - origin.longitude) / size.y)
        val x = orientation.b0 * lt.latitude + orientation.b1 * lt.longitude
        val y = orientation.b2 * lt.latitude + orientation.b3 * lt.longitude
        return Hexagon(x, y)
    }

    private fun cornerOffset(corner: Int): LatLng {
        val angle = 2.0 * PI * (orientation.start_angle + corner) / 6
        return LatLng(size.x * cos(angle), size.y * sin(angle))
    }

    fun getCorners(hex: Hexagon): List<LatLng> {
        val corners = mutableListOf<LatLng>()
        val center = hexagonToLatLng(hex)
        for (i in 0 until 6) {
            val offset = cornerOffset(i)
            corners.add(LatLng(center.latitude + offset.latitude, center.longitude + offset.longitude))
        }
        return corners
    }

    fun nearestHexagon(origin: LatLng): Hexagon {
        val hexagon = latLngToHexagon(origin)

        var rx = round(hexagon.x)
        var ry = round(hexagon.y)
        val rz = round(hexagon.z)

        val xDiff = abs(rx - hexagon.x)
        val yDiff = abs(ry - hexagon.y)
        val zDiff = abs(rz - hexagon.z)

        if ((xDiff > yDiff) && (xDiff > zDiff))
            rx = -ry-rz
        else if (yDiff > zDiff)
            ry = -rx-rz

        return Hexagon(rx, ry)
    }
}