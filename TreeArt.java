import javax.microedition.lcdui.Image;

/**
 * Real trees: tall trunk, lumpy shaded crown, ground shadow. Sizes follow the people (a person is 34px).
 * 0 oak, 1 maple, 2 pine, 3 young tree, 4 tall fir, 5 dead tree, 6 old giant oak, 7 birch.
 */
public class TreeArt {
    private static int seed;

    private static int rnd(int n) {
        seed = seed * 1103515245 + 12345;
        return ((seed >>> 16) & 0x7FFF) % n;
    }

    private static final int[] BARK = {0xFF74502E, 0xFF5A3E24, 0xFF3E2A18};
    private static final int[] BIRCH_BARK = {0xFFD0D0C4, 0xFFB4B4A6, 0xFF8A8A7C};

    public static Image tree(int variant) {
        seed = 777 + variant * 131;
        int[] oak = {0xFF14381A, 0xFF22522A, 0xFF346C3A, 0xFF4E8648};
        int[] maple = {0xFF2A4420, 0xFF3F6228, 0xFF58803A, 0xFF789E50};
        int[] birchLeaf = {0xFF2A4A22, 0xFF3E6A2E, 0xFF5A8A3C, 0xFF7AA84E};
        switch (variant) {
            case 0: return broad(104, 116, 52, 44, 42, 30, 15, 14, 8, 64, oak, BARK, false);
            case 1: return broad(88, 100, 44, 38, 34, 25, 12, 12, 7, 56, maple, BARK, false);
            case 2: return conifer(64, 118, new int[] {4, 26, 46, 64, 82}, new int[] {32, 52, 70, 88, 106},
                    new int[] {10, 16, 21, 26, 30}, 100);
            case 3: return broad(70, 78, 35, 27, 25, 15, 8, 10, 5, 38, oak, BARK, false);
            case 4: return conifer(58, 132, new int[] {4, 24, 42, 60, 78, 96}, new int[] {30, 48, 66, 84, 102, 122},
                    new int[] {8, 13, 17, 21, 25, 28}, 118);
            case 5: return dead();
            case 6: return broad(124, 134, 62, 50, 50, 36, 19, 16, 10, 76, oak, BARK, false);
            default: return broad(64, 98, 32, 34, 26, 20, 9, 11, 5, 52, birchLeaf, BIRCH_BARK, true);
        }
    }

    private static Image broad(int w, int h, int ccx, int ccy, int rx, int ry, int blobs, int rbase,
                               int trunkW, int trunkTop, int[] pal, int[] bark, boolean marks) {
        int[] p = new int[w * h];
        shadow(p, w, h);
        trunk(p, w, w / 2, trunkTop, h - 4, trunkW, bark);
        if (marks) {
            for (int y = trunkTop + 4; y < h - 8; y += 6) {
                int x0 = w / 2 - trunkW / 2;
                for (int x = x0 + 1; x < x0 + trunkW - 1; x++) if (((x + y) & 1) == 0) p[y * w + x] = 0xFF2A2A26;
            }
        }
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
        for (int i = 0; i < n; i++) {                    // back (high) blobs first
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

    private static Image conifer(int w, int h, int[] tops, int[] bots, int[] hw, int trunkTop) {
        int cx = w / 2;
        int[] p = new int[w * h];
        shadow(p, w, h);
        trunk(p, w, cx, trunkTop, h - 4, 6, BARK);
        int dark = 0xFF0C2A1A, mid = 0xFF14432A, light = 0xFF1F5C38;
        for (int t = 0; t < tops.length; t++) {
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

    /** A bare, gnarled dead tree. */
    private static Image dead() {
        int w = 72, h = 106, cx = 36;
        int[] p = new int[w * h];
        shadow(p, w, h);
        for (int y = 24; y <= h - 4; y++) {                          // tapering trunk
            int width = 3 + (y - 24) * 5 / (h - 28) + ((y > h - 14) ? (y - (h - 14)) / 2 : 0);
            int x0 = cx - width / 2 + ((y < 50) ? (50 - y) / 12 : 0);
            for (int x = x0; x < x0 + width; x++) {
                int c = 0xFF5A4C3E;
                if (x < x0 + 1) c = 0xFF7A6A58;
                else if (x >= x0 + width - 2) c = 0xFF372E26;
                p[y * w + x] = c;
            }
        }
        int[][] br = {{36, 66, 14, 42, 2}, {14, 42, 8, 24, 1}, {36, 54, 58, 32, 2}, {58, 32, 64, 18, 1},
                {36, 42, 26, 18, 2}, {36, 36, 46, 12, 1}, {36, 78, 54, 66, 1}, {36, 82, 20, 72, 1}, {26, 18, 20, 8, 1}};
        for (int i = 0; i < br.length; i++) limb(p, w, h, br[i][0], br[i][1], br[i][2], br[i][3], br[i][4]);
        return Image.createRGBImage(outline(p, w, h), w, h, true);
    }

    private static void limb(int[] p, int w, int h, int x0, int y0, int x1, int y1, int t) {
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(dx < 0 ? -dx : dx, dy < 0 ? -dy : dy);
        for (int s = 0; s <= steps; s++) {
            int x = x0 + dx * s / steps, y = y0 + dy * s / steps;
            for (int j = 0; j <= t; j++)
                for (int i = 0; i <= t; i++) {
                    int px = x + i, py = y + j;
                    if (px >= 0 && px < w && py >= 0 && py < h) p[py * w + px] = (i == 0) ? 0xFF6E5E4C : 0xFF4A3E32;
                }
        }
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

    private static void trunk(int[] p, int w, int cx, int top, int base, int tw, int[] bark) {
        for (int y = top; y <= base; y++) {
            int width = tw + ((y > base - 10) ? (y - (base - 10)) / 2 : 0);
            int x0 = cx - width / 2;
            for (int x = x0; x < x0 + width; x++) {
                int c = bark[1];
                if (x < x0 + 2) c = bark[0];
                else if (x >= x0 + width - 3) c = bark[2];
                else if (((y * 5 + x) & 7) == 0) c = Gfx.adj(bark[1], 82);
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
                if (y > darkY) c = Gfx.adj(c, 82);
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
