package biondi.mattia.signalstrengthheatmap;

import android.graphics.Color;
import java.util.HashMap;

public class Gradient {
    private static final int DEFAULT_COLOR_MAP_SIZE = 1000;
    public final int mColorMapSize;
    public int[] mColors;
    public float[] mStartPoints;

    public Gradient(int[] colors, float[] startPoints) {
        this(colors, startPoints, DEFAULT_COLOR_MAP_SIZE);
    }

    public Gradient(int[] colors, float[] startPoints, int colorMapSize) {
        if (colors.length != startPoints.length) {
            throw new IllegalArgumentException("colors and startPoints should be same length");
        } else if (colors.length == 0) {
            throw new IllegalArgumentException("No colors have been defined");
        } else {
            for(int i = 1; i < startPoints.length; ++i) {
                if (startPoints[i] <= startPoints[i - 1]) {
                    throw new IllegalArgumentException("startPoints should be in increasing order");
                }
            }

            this.mColorMapSize = colorMapSize;
            this.mColors = new int[colors.length];
            this.mStartPoints = new float[startPoints.length];
            System.arraycopy(colors, 0, this.mColors, 0, colors.length);
            System.arraycopy(startPoints, 0, this.mStartPoints, 0, startPoints.length);
        }
    }

    private HashMap<Integer, Gradient.ColorInterval> generateColorIntervals() {
        HashMap<Integer, Gradient.ColorInterval> colorIntervals = new HashMap();
        int i;
        if (this.mStartPoints[0] != 0.0F) {
            i = Color.argb(0, Color.red(this.mColors[0]), Color.green(this.mColors[0]), Color.blue(this.mColors[0]));
            colorIntervals.put(0, new Gradient.ColorInterval(i, this.mColors[0], (float)this.mColorMapSize * this.mStartPoints[0]));
        }

        for(i = 1; i < this.mColors.length; ++i) {
            colorIntervals.put((int)((float)this.mColorMapSize * this.mStartPoints[i - 1]), new Gradient.ColorInterval(this.mColors[i - 1], this.mColors[i], (float)this.mColorMapSize * (this.mStartPoints[i] - this.mStartPoints[i - 1])));
        }

        if (this.mStartPoints[this.mStartPoints.length - 1] != 1.0F) {
            i = this.mStartPoints.length - 1;
            colorIntervals.put((int)((float)this.mColorMapSize * this.mStartPoints[i]), new Gradient.ColorInterval(this.mColors[i], this.mColors[i], (float)this.mColorMapSize * (1.0F - this.mStartPoints[i])));
        }

        return colorIntervals;
    }

    int[] generateColorMap(double opacity) {
        HashMap<Integer, Gradient.ColorInterval> colorIntervals = this.generateColorIntervals();
        int[] colorMap = new int[this.mColorMapSize];
        Gradient.ColorInterval interval = (Gradient.ColorInterval)colorIntervals.get(0);
        int start = 0;

        int i;
        for(i = 0; i < this.mColorMapSize; ++i) {
            if (colorIntervals.containsKey(i)) {
                interval = (Gradient.ColorInterval)colorIntervals.get(i);
                start = i;
            }

            float ratio = (float)(i - start) / interval.duration;
            colorMap[i] = interpolateColor(interval.color1, interval.color2, ratio);
        }

        if (opacity != 1.0D) {
            for(i = 0; i < this.mColorMapSize; ++i) {
                int c = colorMap[i];
                colorMap[i] = Color.argb((int)((double)Color.alpha(c) * opacity), Color.red(c), Color.green(c), Color.blue(c));
            }
        }

        return colorMap;
    }

    static int interpolateColor(int color1, int color2, float ratio) {
        int alpha = (int)((float)(Color.alpha(color2) - Color.alpha(color1)) * ratio + (float)Color.alpha(color1));
        float[] hsv1 = new float[3];
        Color.RGBToHSV(Color.red(color1), Color.green(color1), Color.blue(color1), hsv1);
        float[] hsv2 = new float[3];
        Color.RGBToHSV(Color.red(color2), Color.green(color2), Color.blue(color2), hsv2);
        if (hsv1[0] - hsv2[0] > 180.0F) {
            hsv2[0] += 360.0F;
        } else if (hsv2[0] - hsv1[0] > 180.0F) {
            hsv1[0] += 360.0F;
        }

        float[] result = new float[3];

        for(int i = 0; i < 3; ++i) {
            result[i] = (hsv2[i] - hsv1[i]) * ratio + hsv1[i];
        }

        return Color.HSVToColor(alpha, result);
    }

    private class ColorInterval {
        private final int color1;
        private final int color2;
        private final float duration;

        private ColorInterval(int color1, int color2, float duration) {
            this.color1 = color1;
            this.color2 = color2;
            this.duration = duration;
        }
    }
}
