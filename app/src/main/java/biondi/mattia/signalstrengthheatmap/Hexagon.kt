package biondi.mattia.signalstrengthheatmap

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Point
import kotlin.math.*

class Hexagon(q: Double, r: Double) {
    var x = q
    var y = r
    var z = (-q - r)

    init {
        if (x + y + z != 0.0) throw IllegalArgumentException("x + y + z must be 0")
    }

    fun add(b: Hexagon): Hexagon {
        return Hexagon(x + b.x, y + b.y)
    }

    fun subtract(b: Hexagon): Hexagon {
        return Hexagon(x - b.x, y - b.y)
    }

    fun lenght(): Double {
        return ((abs(x) + abs(y) + abs(z)) / 2)
    }

    fun distance(b: Hexagon): Double {
        return subtract(b).lenght()
    }

    fun direction(direction: Int): Hexagon {
        if (direction in 0..5)
            return directions[direction]
        else throw IllegalArgumentException("Direction must be between 0 and 5") // TODO controlla se scritto bene
    }

    fun neighbor(direction: Int): Hexagon {
        return add(direction(direction))
    }

}

val directions = arrayOf(
        Hexagon(1.0, 0.0), Hexagon(1.0, -1.0), Hexagon(0.0, -1.0),
        Hexagon(-1.0, 0.0), Hexagon(-1.0, 1.0), Hexagon(0.0, 1.0)
)

fun equals(a: Hexagon, b: Hexagon): Boolean {
    return (a.x == b.x) && (a.y == b.y) && (a.z == b.z)
}

class Orientation(val f0: Double, val f1: Double, val f2: Double, val f3: Double,
                  val b0: Double, val b1: Double, val b2: Double, val b3: Double,
                  val start_angle: Double)

val layout_pointy = Orientation(sqrt(3.0), sqrt(3.0) / 2.0, 0.0, 3.0 / 2.0,
                            sqrt(3.0) / 3.0, -1.0 / 3.0, 0.0, 2.0 / 3.0,
                                0.5)

val layout_flat = Orientation(3.0 / 2.0, 0.0, sqrt(3.0) / 2.0, sqrt(3.0),
                                2.0 / 3.0, 0.0, -1.0 / 3.0, sqrt(3.0) / 3.0,
                                0.0)

class HexagonLayout(val orientation: Orientation, val size: Point, val origin: Point) {

    fun hexagonToPixel(hex: Hexagon): Point {
        val x = (orientation.f0 * hex.x + orientation.f1 * hex.y) * size.x
        val y = (orientation.f2 * hex.x + orientation.f3 * hex.y) * size.y
        return Point(x + origin.x, y + origin.y)
    }

    fun pixelToHexagon(point: Point): Hexagon {
        val pt = Point((point.x - origin.x) / size.x,
                (point.y - origin.y) / size.y)
        val x = orientation.b0 * pt.x + orientation.b1 + pt.y
        val y = orientation.b2 * pt.x + orientation.b3 + pt.y
        return Hexagon(x, y)
    }

    fun cornerOffset(corner: Int): Point {
        val angle = 2.0 * PI * (orientation.start_angle + corner) / 6
        return Point(size.x * cos(angle), size.y * sin(angle))
    }

    fun polygonCorners(hex: Hexagon): List<LatLng> {
        val corners = mutableListOf<LatLng>()
        val center = hexagonToPixel(hex)
        for (i in 0 until 6) {
            val offset = cornerOffset(i)
            corners.add(LatLng(center.x + offset.x, center.y + offset.y))
        }
        return corners
    }
}









