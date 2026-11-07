import javax.microedition.lcdui.Image;

/** Draws tree crowns (leafy blobs with an outline) as transparent images. */
public class TreeArt {
    private static final int[][] OFF = {{-10, -8}, {9, -9}, {-12, 6}, {11, 7}, {0, -12}, {0, 0}, {-3, 11}, {13, -1}, {-13, -2}};

    public static Image crown(int size, int seed) {
        int[] p = new int[size * size];
        int c = size / 2;
        disc(p, size, c, c, size / 2 - 2, 0xFF1B5E20);
        for (int pass = 0; pass < 3; pass++) {
            int col = (pass == 0) ? 0xFF2E7D32 : (pass == 1) ? 0xFF3F9A45 : 0xFF63B85C;
            for (int k = 0; k < OFF.length; k++) {
                int ox = OFF[(k + seed) % OFF.length][0] * size / 64;
                int oy = OFF[(k + seed) % OFF.length][1] * size / 64;
                int r = size / 5 + (k % 3) * size / 40 - pass * size / 20;
                disc(p, size, c + ox - pass * 2, c + oy - pass * 2, r, col);
            }
        }
        // outline
        int[] q = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int i = y * size + x;
                if (p[i] != 0) q[i] = p[i];
                else if ((x > 0 && p[i - 1] != 0) || (x < size - 1 && p[i + 1] != 0)
                        || (y > 0 && p[i - size] != 0) || (y < size - 1 && p[i + size] != 0))
                    q[i] = 0xFF0C2E10;
            }
        }
        return Image.createRGBImage(q, size, size, true);
    }

    private static void disc(int[] p, int size, int cx, int cy, int r, int col) {
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                if (x < 0 || y < 0 || x >= size || y >= size) continue;
                int dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy <= r * r) p[y * size + x] = col;
            }
        }
    }
}
