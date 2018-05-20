package biondi.mattia.signalstrengthheatmap

import android.graphics.Bitmap
import android.graphics.Color
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.geometry.Bounds
import com.google.maps.android.heatmaps.WeightedLatLng
import com.google.maps.android.quadtree.PointQuadTree
import java.io.ByteArrayOutputStream

class WeightedHeatmapTileProvider(private var data: Collection<WeightedLatLng>) : TileProvider {
    private val DEFAULT_RADIUS = 20
    private val DEFAULT_OPACITY = 0.7
    private val DEFAULT_GRADIENT_COLORS = intArrayOf(Color.GREEN, Color.RED)
    private val DEFAULT_GRADIENT_START_POINTS = floatArrayOf(0.2F, 1.0F)
    private var DEFAULT_GRADIENT = Gradient(DEFAULT_GRADIENT_COLORS, DEFAULT_GRADIENT_START_POINTS)
    private var DEFAULT_MAX_INTENSITY = 9
    private val WORLD_WIDTH = 1.0
    private val TILE_DIM = 512
    private val MIN_RADIUS = 10
    private val MAX_RADIUS = 50
    private var radius: Int? = null
    private var gradient: Gradient? = null
    private var opacity: Double? = null
    private var maxIntensity: Int? = null
    private var tree: PointQuadTree<WeightedLatLng>? = null
    private var bounds: Bounds? = null
    private var colorMap: IntArray? = null
    private var kernel: DoubleArray? = null

    fun setWeightedData(data: Collection<WeightedLatLng>) {
        this.data = data
        if (this.data.isEmpty()) {
            throw IllegalArgumentException("No input points.")
        } else {
            bounds = getBounds(data)
            tree = PointQuadTree(bounds)
            val iterator = this.data.iterator()

            while (iterator.hasNext()) {
                val weightedLatLng = iterator.next()
                tree?.add(weightedLatLng)
            }
        }
    }

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val tileWidth = 1.0 / Math.pow(2.0, zoom.toDouble())
        val padding = tileWidth * radius!!.toDouble() / TILE_DIM
        val tileWidthPadded = tileWidth + 2.0 * padding
        val bucketWidth = tileWidthPadded / (TILE_DIM + radius!!.toDouble() *2)
        val minX = x * tileWidth - padding
        val maxX = (x + 1) * tileWidth + padding
        val minY = y * tileWidth - padding
        val maxY = (y + 1) * tileWidth + padding
        var xOffset = 0.0
        var wrappedPoints: Collection<WeightedLatLng> = ArrayList()
        var tileBounds: Bounds

        if (minX < 0.0) {
            tileBounds = Bounds(minX + 1.0, 1.0, minY, maxY)
            xOffset = -1.0
            wrappedPoints = tree!!.search(tileBounds)
        } else if (maxX > 1.0) {
            tileBounds = Bounds(0.0, maxX - 1.0, minY, maxY)
            xOffset = 1.0
            wrappedPoints = tree!!.search(tileBounds)
        }

        tileBounds = Bounds(minX, maxX, minY, maxY)
        val paddedBounds = Bounds(bounds!!.minX - padding, bounds!!.maxX + padding, bounds!!.minY - padding, bounds!!.maxY + padding)
        if (!tileBounds.intersects(paddedBounds)) {
            return TileProvider.NO_TILE
        } else {
            val points = tree?.search(tileBounds)
            if (points!!.isEmpty()) {
                return TileProvider.NO_TILE
            } else {
                val intensity = Array(TILE_DIM + radius!! * 2) { DoubleArray(TILE_DIM + radius!! * 2) }
                var weightedLatLng: WeightedLatLng
                var point: com.google.maps.android.geometry.Point
                var bucketX: Int
                var bucketY: Int

                val iterator1 = points.iterator()
                while (iterator1.hasNext()) {
                    weightedLatLng = iterator1.next()
                    point = weightedLatLng.point
                    bucketX = ((point.x - minX) / bucketWidth).toInt()
                    bucketY = ((point.y - minY) / bucketWidth).toInt()
                    intensity[bucketX][bucketY] = weightedLatLng.intensity //TODO evita la proprietà additiva
                }

                val iterator2 = wrappedPoints.iterator()
                while (iterator2.hasNext()) {
                    weightedLatLng = iterator2.next()
                    point = weightedLatLng.point
                    bucketX = ((point.x + xOffset - minX) / bucketWidth).toInt()
                    bucketY = ((point.y - minY) / bucketWidth).toInt()
                    intensity[bucketX][bucketY] = weightedLatLng.intensity //TODO evita la proprietà additiva
                }

                // La griglia con le intensità per quella zona viene passato a convolve che la adatta in base alla dimensione del raggio
                val convolved = convolve(intensity, kernel!!)
                val bitmap = colorize(convolved, colorMap!!, maxIntensity!!.toDouble())
                return convertBitmap(bitmap)
            }
        }
    }

    fun setGradient(gradient: Gradient) {
        this.gradient = gradient
        colorMap = gradient.generateColorMap(opacity!!)
    }

    fun setRadius(radius: Int) {
        this.radius = radius
        if (this.radius!! in MIN_RADIUS..MAX_RADIUS) {
            kernel = generateKernel(this.radius!!, this.radius!! / 3.0)
        } else {
            throw IllegalArgumentException("Radius not within bounds.")
        }
    }

    fun setOpacity(opacity: Double) {
        this.opacity = opacity
        if (this.opacity!! in 0.0..WORLD_WIDTH) {
            setGradient(gradient!!)
        } else {
            throw IllegalArgumentException("Opacity must be in range [0, 1]")
        }
    }

    fun setMaxIntensity(intensity: Int) {
        this.maxIntensity = intensity - 1
    }

    private fun convertBitmap(bitmap: Bitmap): Tile {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bitmapdata = stream.toByteArray()
        return Tile(TILE_DIM, TILE_DIM, bitmapdata)
    }

    private fun getBounds(points: Collection<WeightedLatLng>): Bounds {
        val iterator = points.iterator()
        val first = iterator.next()
        var minX = first.point.x
        var maxX = first.point.x
        var minY = first.point.y
        var maxY = first.point.y

        while (iterator.hasNext()) {
            val l = iterator.next()
            val x = l.point.x
            val y = l.point.y

            if (x < minX) minX = x

            if (x > maxX) maxX = x

            if (y < minY) minY = y

            if (y > maxY) maxY = y
        }

        return Bounds(minX, maxX, minY, maxY)
    }

    private fun generateKernel(radius: Int, sd: Double): DoubleArray {
        val kernel = DoubleArray(radius * 2 + 1)

        for (i in -radius..radius) {
            kernel[i + radius] = Math.exp((-i * i).toDouble() / (2.0 * sd * sd))
        }

        return kernel
    }

    private fun convolve(grid: Array<DoubleArray>, kernel: DoubleArray): Array<DoubleArray> {
        val radius = Math.floor(kernel.size.toDouble() / 2.0).toInt()
        val dimOld = grid.size
        val dim = dimOld - 2 * radius
        val lowerLimit = radius
        val upperLimit = radius + dim - 1
        val intermediate = Array(dimOld) { DoubleArray(dimOld) }

        var x = 0
        var y: Int
        var initial: Int
        var double: Double
        while (x < dimOld) {
            y = 0
            while (y < dimOld) {
                double = grid[x][y]
                if (double != 0.0) {
                    val xUpperLimit = (if (upperLimit < x + radius) upperLimit else x + radius) + 1
                    initial = if (lowerLimit > x - radius) lowerLimit else x - radius

                    for (x2 in initial until xUpperLimit) {
                        //intermediate[x2][y] += double * kernel[x2 - (x - radius)] //todo
                        intermediate[x2][y] += double * kernel[x2 - (x - radius)]
                    }
                }
                ++y
            }
            ++x
        }

        val outputGrid = Array(dim) { DoubleArray(dim) }

        x = lowerLimit
        while (x < upperLimit + 1) {
            y = 0
            while (y < dimOld) {
                double = intermediate[x][y]
                if (double != 0.0) {
                    val yUpperLimit = (if (upperLimit < y + radius) upperLimit else y + radius) + 1
                    initial = if (lowerLimit > y - radius) lowerLimit else y - radius

                    for (y2 in initial until yUpperLimit) {
                        //outputGrid[x - radius][y2 - radius] += double * kernel[y2 - (y - radius)] //todo
                        outputGrid[x - radius][y2 - radius] = double * kernel[y2 - (y - radius)]

                    }
                }
                ++y
            }
            ++x
        }

        return outputGrid
    }

    private fun colorize(grid: Array<DoubleArray>, colorMap: IntArray, max: Double): Bitmap {
        val maxColor = colorMap[colorMap.size - 1]
        val colorMapScaling = (colorMap.size - 1).toDouble() / max
        val dim = grid.size
        val colors = IntArray(dim * dim)

        for (i in 0 until dim) {
            for (j in 0 until dim) {
                val a = grid[j][i]
                val index = i * dim + j
                val col = (a * colorMapScaling).toInt()
                if (a != 0.0) {
                    if (col < colorMap.size) {
                        colors[index] = colorMap[col]
                    } else {
                        colors[index] = maxColor
                    }
                } else {
                    colors[index] = 0
                }
            }
        }

        val tile = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
        tile.setPixels(colors, 0, dim, 0, 0, dim, dim)
        return tile
    }

    init {
        radius = DEFAULT_RADIUS
        gradient = DEFAULT_GRADIENT
        opacity = DEFAULT_OPACITY
        maxIntensity = DEFAULT_MAX_INTENSITY
        kernel = generateKernel(radius!!, radius!!.toDouble() / 3.0)
        setGradient(gradient!!)
        setWeightedData(data)
    }
}