/** Small colour helper shared by the world, trees and vehicles. */
public class Gfx {
    /** Brightens (pct > 100) or darkens (pct < 100) a colour. */
    public static int adj(int c, int pct) {
        int r = ((c >> 16) & 0xFF) * pct / 100;
        int g = ((c >> 8) & 0xFF) * pct / 100;
        int b = (c & 0xFF) * pct / 100;
        if (r > 255) r = 255;
        if (g > 255) g = 255;
        if (b > 255) b = 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
