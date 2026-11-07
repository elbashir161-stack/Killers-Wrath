import javax.microedition.lcdui.Image;

/** Real trees: tall trunk, lumpy shaded crown, ground shadow. Sizes follow the people (a person is 30px). */
public class TreeArt {
    private static int seed;

    private static int rnd(int n) {
        seed = seed * 1103515245 + 12345;
        return ((seed >>> 16) & 0x7FFF) % n;
    }

    /** 0 = oak (big), 1 = maple, 2 = pine, 3 = young tree. */
    public static Image tree(int variant) {
        seed = 777 + variant * 131;
        int[] oak = {0xFF1E5A22, 0xFF2F7D32, 0xFF4CA043, 0xFF7BC46A};
        int[] maple = {0xFF3B6E22, 0xFF5C9E36, 0xFF86BF4A, 0xFFB5DB6A};
        switch (variant) {
            case 0: return broad(104, 116, 52, 44, 42, 30, 15, 14, 8, 64, oak);
            case 1: return broad(88, 100, 44, 38, 34, 25, 12, 12, 7, 56, maple);
            case 2: return pine();
            default: return broad(70, 78, 35, 27, 25, 15, 8, 10, 5, 38, oak);
        }
    }

    private static Image broad(int w, int h, int ccx, int ccy, int rx, int ry, int blobs, int rbase,
                               int trunkW, int trunkTop, int[] pal) {
        int[] p = new int[w * h];
        shadow(p, w, h);
        trunk(p, w, w / 2, trunkTop, h - 4, trunkW);
        int[] bx = new int[blobs + 1], by = new int[blobs + 1], br = new int[blobs + 1];
        int n = 0;
        bx[n] = ccx; by[n] = ccy; br[n] = rbase + 6; n++;
        int guard = 0;
        while (n <= blobs && guard < 500) {
            guard++;
            int dx = rnd(2 * rx + 1) - rx, dy = rnd(2 * ry + 1) - ry;
            if (dx * dx * ry * ry + dy * dy * rx * rx > rx * rx * ry * ry) continue;
            bx[n] = ccx + dx; by[n] = ccy + dy; br[n] = rbase - 2 + rnd(8);
            n++;
        }
        // back (high) blobs first so lower ones overlap them
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (by[j] < by[i]) {
                    int t = by[i]; by[i] = by[j]; by[j] = t;
                    t = bx[i]; bx[i] = bx[j]; bx[j] = t;
                    t = br[i]; br[i] = br[j]; br[j] = t;
                }
            }
        }
        int darkY = ccy + ry * 6 / 10;
        for (int i = 0; i < n; i++) blob(p, w, h, bx[i], by[i], br[i], pal, darkY);
        return Image.createRGBImage(outline(p, w, h), w, h, true);
    }

    private static Image pine() {
        int w = 64, h = 118, cx = w / 2;
        int[] p = new int[w * h];
        shadow(p, w, h);
        trunk(p, w, cx, 100, h - 4, 6);
        int[] tops = {4, 26, 46, 64, 82};
        int[] bots = {32, 52, 70, 88, 106};
        int[] hw = {10, 16, 21, 26, 30};
        int dark = 0xFF14452A, mid = 0xFF1E6B3A, light = 0xFF2E8B4A;
        for (int t = 0; t < 5; t++) {
            for (int y = tops[t]; y < bots[t]; y++) {
                int half = hw[t] * (y - tops[t] + 3) / (bots[t] - tops[t] + 3);
                for (int x = cx - half; x <= cx + half; x++) {
                    int s = (x - cx) * 100 / (half + 1);
                    int c = (s < -35) ? light : (s > 25 ? dark : mid);
                    if (((x * 7 + y * 13) & 7) == 0 && c == mid) c = light;
                    if (y > bots[t] - 4) c = (c == light) ? mid : dark;   // darker under each tier
                    if (x >= 0 && x < w) p[y * w + x] = c;
                }
            }
        }
        return Image.createRGBImage(outline(p, w, h), w, h, true);
    }

    private static void shadow(int[] p, int w, int h) {
        int cx = w / 2, cy = h - 5, rx = w * 28 / 100, ry = 7;
        for (int y = cy - ry; y <= cy + ry; y++)
            for (int x = cx - rx; x <= cx + rx; x++) {
                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                int dx = x - cx, dy = y - cy;
                if (dx * dx * ry * ry + dy * dy * rx * rx <= rx * rx * ry * ry) p[y * w + x] = 0x55000000;
            }
    }

    private static void trunk(int[] p, int w, int cx, int top, int base, int tw) {
        for (int y = top; y <= base; y++) {
            int width = tw + ((y > base - 10) ? (y - (base - 10)) / 2 : 0);
            int x0 = cx - width / 2;
            for (int x = x0; x < x0 + width; x++) {
                int c = 0xFF5A3E24;
                if (x < x0 + 2) c = 0xFF74502E;
                else if (x >= x0 + width - 3) c = 0xFF3E2A18;
                else if (((y * 5 + x) & 7) == 0) c = 0xFF4A3220;
                p[y * w + x] = c;
            }
        }
    }

    private static void blob(int[] p, int w, int h, int cx, int cy, int r, int[] pal, int darkY) {
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                if (x < 0 || y < 0 || x >= w || y >= h) continue;
                int dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy > r * r) continue;
                int s = (dx * 5 + dy * 7) * 100 / (r * 10);
                int c;
                if (s > 35) c = pal[0];
                else if (s > -10) c = pal[1];
                else if (s > -45) c = pal[2];
                else c = pal[3];
                if (((x * 7 + y * 13) & 7) == 0 && c != pal[0]) c = (c == pal[1]) ? pal[2] : pal[3];
                if (y > darkY) c = SpriteKit.adj(c, 82);
                p[y * w + x] = c;
            }
        }
    }

    private static int[] outline(int[] p, int w, int h) {
        int[] q = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (p[i] != 0) { q[i] = p[i]; continue; }
                boolean n = (x > 0 && (p[i - 1] >>> 24) == 0xFF) || (x < w - 1 && (p[i + 1] >>> 24) == 0xFF)
                        || (y > 0 && (p[i - w] >>> 24) == 0xFF) || (y < h - 1 && (p[i + w] >>> 24) == 0xFF);
                if (n) q[i] = 0xFF0C2E10;
            }
        }
        return q;
    }
}
