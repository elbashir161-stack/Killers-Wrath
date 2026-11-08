import javax.microedition.lcdui.Image;

/**
 * Vehicles at true scale (car = 4.5m x 1.8m) in the old top-down GTA view: you see the roof, a little of the
 * side glass, the bonnet, and the tyres poking out at the corners. 16 headings: 0 = east, 4 = south, 8 = west, 12 = north.
 */
public class VehicleKit {
    public static final int SEDAN = 0, SPORTS = 1, PICKUP = 2, MOTO = 3, MOTO_RIDER = 4;
    public static final int[] LEN = {72, 70, 80, 36, 36};
    public static final int[] WID = {30, 28, 32, 16, 16};
    public static final int[] MAXSP = {7, 9, 6, 8, 8};   // px per tick at top speed
    public static final int[] ACC = {3, 4, 3, 5, 5};     // in 1/16 px per tick
    public static final int[] COS = new int[16];
    public static final int[] SIN = new int[16];

    static {
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8.0;
            COS[i] = (int) Math.floor(Math.cos(a) * 256 + 0.5);
            SIN[i] = (int) Math.floor(Math.sin(a) * 256 + 0.5);
        }
    }

    private static final int M = 3;   // empty margin around the body (room for tyres, shadow, outline)
    private static final int[] CAR_COLORS = {0xFFC8C8BC, 0xFF34527A, 0xFF6A2A2A, 0xFF2E4A36, 0xFF7A7E80, 0xFF9A5A2A};
    private static final int[] MOTO_COLORS = {0xFF8A2A2A, 0xFF2A2A2E, 0xFF2B4A6A};
    private static final int OUTLINE = 0xFF0A0A0B;
    private static final int SHADOW = 0x55000000;
    private static final int TIRE = 0xFF0C0C0E;
    private static final Image[][][] cache = new Image[5][6][16];

    public static int colorCount(int type) { return (type >= MOTO) ? 3 : 6; }

    public static Image image(int type, int color, int h) {
        h &= 15;
        Image im = cache[type][color][h];
        if (im == null) {
            im = make(type, color, h);
            cache[type][color][h] = im;
        }
        return im;
    }

    private static Image make(int type, int color, int h) {
        int L = LEN[type], W = WID[type];
        int s = L + 2 * M, t = W + 2 * M;
        int body = (type >= MOTO) ? MOTO_COLORS[color] : CAR_COLORS[color];
        int[] r = new int[s * t];
        if (type == MOTO) moto(r, s, L, W, body);
        else if (type == MOTO_RIDER) { moto(r, s, L, W, body); rider(r, s); }
        else car(r, s, L, W, type, body);
        r = outline(r, s, t);
        if (h == 0) return Image.createRGBImage(r, s, t, true);
        if (h == 8) {
            int[] o = new int[s * t];
            for (int y = 0; y < t; y++)
                for (int x = 0; x < s; x++) o[y * s + x] = r[y * s + (s - 1 - x)];
            return Image.createRGBImage(o, s, t, true);
        }
        if (h == 4 || h == 12) {
            int[] d = new int[s * t];                       // transpose: front ends up at the bottom
            for (int y = 0; y < t; y++)
                for (int x = 0; x < s; x++) d[x * t + y] = r[y * s + x];
            if (h == 4) return Image.createRGBImage(d, t, s, true);
            int[] u = new int[s * t];
            for (int y = 0; y < s; y++)
                for (int x = 0; x < t; x++) u[y * t + x] = d[(s - 1 - y) * t + x];
            return Image.createRGBImage(u, t, s, true);
        }
        int D = (int) Math.sqrt(s * s + t * t) + 2;         // in-between headings: rotate the east-facing picture
        int c = COS[h], sn = SIN[h];
        int[] o = new int[D * D];
        for (int y = 0; y < D; y++) {
            for (int x = 0; x < D; x++) {
                int X2 = 2 * x + 1 - D, Y2 = 2 * y + 1 - D;
                int un = X2 * c + Y2 * sn + s * 256;
                int vn = -X2 * sn + Y2 * c + t * 256;
                if (un < 0 || vn < 0) continue;
                int u = un / 512, v = vn / 512;
                if (u >= s || v >= t) continue;
                o[y * D + x] = r[v * s + u];
            }
        }
        return Image.createRGBImage(o, D, D, true);
    }

    // ---------------- art (facing east) ----------------

    private static void car(int[] p, int s, int L, int W, int type, int body) {
        int glass = 0xFF2A4452, glassHi = 0xFF5E8294, glassSide = 0xFF1E3340;
        int dark = Gfx.adj(body, 66), light = Gfx.adj(body, 128);
        rrect(p, s, 2, 3, L - 2, W - 2, 6, SHADOW);                 // shadow on the road
        int rwx = (type == PICKUP) ? 11 : 10, fwx = L - 22;
        wheel(p, s, rwx, -2, 10); wheel(p, s, rwx, W - 3, 10);      // tyres: visible at the sides
        wheel(p, s, fwx, -2, 10); wheel(p, s, fwx, W - 3, 10);
        rrect(p, s, 0, 0, L, W, 6, body);                           // body
        tint(p, s, L, 0, 1, 128);                                   // lit top edge
        tint(p, s, L, W - 4, W - 1, 66);                            // side panel in shade
        rect(p, s, L - 2, 3, 2, W - 6, 0xFF2E2E34);                 // bumpers
        rect(p, s, 0, 3, 2, W - 6, 0xFF2E2E34);
        int rgx, rgw, roofx, roofw, wsx, wsw;
        if (type == SEDAN) { rgx = 17; rgw = 5; roofx = 22; roofw = 21; wsx = 43; wsw = 8; }
        else if (type == SPORTS) { rgx = 22; rgw = 5; roofx = 27; roofw = 12; wsx = 39; wsw = 9; }
        else { rgx = 37; rgw = 3; roofx = 40; roofw = 12; wsx = 52; wsw = 8; }
        rect(p, s, roofx - 1, 2, roofw + 2, W - 4, glassSide);      // side glass seen under the roof
        rect(p, s, roofx, 4, roofw, W - 8, Gfx.adj(body, 112));   // roof
        rect(p, s, roofx, 4, roofw, 1, light);
        rect(p, s, rgx, 3, rgw, W - 6, glass);                      // rear window
        rect(p, s, wsx, 3, wsw, W - 6, glass);                      // windscreen, wider than the roof
        rect(p, s, wsx + 1, 4, 1, W - 8, glassHi);
        int hx = wsx + wsw + 2;
        rect(p, s, hx, W / 2, L - hx - 4, 1, dark);                 // bonnet crease
        rect(p, s, wsx - 1, -1, 3, 2, dark);                        // mirrors
        rect(p, s, wsx - 1, W - 1, 3, 2, dark);
        if (type == SPORTS) {
            rect(p, s, 4, W / 2 - 1, L - 8, 2, 0xFFD8D8CC);         // stripe
            rect(p, s, 1, 3, 4, W - 6, dark);                       // spoiler
        }
        if (type == PICKUP) {
            rect(p, s, 4, 3, 33, W - 6, 0xFF2C2C32);                // load bed
            rect(p, s, 4, 3, 33, 1, 0xFF5A5A60);                    // bed rails
            rect(p, s, 4, W - 4, 33, 1, 0xFF5A5A60);
            for (int x = 8; x < 36; x += 5) rect(p, s, x, 4, 1, W - 8, 0xFF3E3E46);
        }
        rect(p, s, L - 4, 3, 3, 4, 0xFFE8E0A8);                     // headlights
        rect(p, s, L - 4, W - 7, 3, 4, 0xFFE8E0A8);
        rect(p, s, 0, 3, 2, 4, 0xFFB01818);                         // tail lights
        rect(p, s, 0, W - 7, 2, 4, 0xFFB01818);
        for (int y = 2; y < W - 2; y++)                             // grime
            for (int x = 3; x < L - 3; x++)
                if (((x * 7 + y * 13) % 29) == 0) put(p, s, x, y, dark);
    }

    private static void wheel(int[] p, int s, int x, int y, int len) {
        rect(p, s, x, y, len, 5, TIRE);
        rect(p, s, x + 1, y + 2, len - 2, 1, 0xFF26262A);
    }

    private static void moto(int[] p, int s, int L, int W, int body) {
        rrect(p, s, 1, 4, L - 2, 9, 4, SHADOW);
        rrect(p, s, 0, 5, 10, 5, 2, TIRE);                           // rear tyre
        rrect(p, s, 26, 5, 10, 5, 2, TIRE);                          // front tyre
        rect(p, s, 3, 7, 4, 1, 0xFF2C2C30);
        rect(p, s, 29, 7, 4, 1, 0xFF2C2C30);
        rect(p, s, 8, 7, 20, 2, 0xFF3A3A40);                         // frame
        rrect(p, s, 10, 3, 16, 10, 4, body);                         // tank
        tint(p, s, L, 3, 4, 128);
        tint(p, s, L, 11, 12, 66);
        rect(p, s, 12, 5, 6, 6, 0xFF1A1A1E);                         // seat
        rect(p, s, 19, 6, 5, 4, 0xFF55555C);                         // engine
        rect(p, s, 1, 11, 9, 1, 0xFF6A6A70);                         // exhaust
        rect(p, s, 25, 1, 2, 14, 0xFF2A2A30);                        // handlebars
        rect(p, s, 34, 7, 2, 2, 0xFFE8E0A8);
        rect(p, s, 0, 7, 2, 2, 0xFFB01818);
    }

    /** Killer sitting on the bike, seen from above: legs along the tank, torso, arms to the bars, bald head. */
    private static void rider(int[] p, int s) {
        int pants = 0xFF14161A, jacket = 0xFF23272D, jacketLt = 0xFF343A44, skin = 0xFF553322;
        rect(p, s, 11, 2, 9, 3, pants);
        rect(p, s, 11, 11, 9, 3, pants);
        rrect(p, s, 12, 3, 8, 10, 3, jacket);
        rect(p, s, 13, 4, 6, 1, jacketLt);
        rect(p, s, 19, 4, 7, 2, jacketLt);
        rect(p, s, 19, 10, 7, 2, jacketLt);
        rect(p, s, 25, 3, 2, 3, skin);
        rect(p, s, 25, 10, 2, 3, skin);
        disc(p, s, 17, 8, 3, skin);
        rect(p, s, 16, 6, 2, 1, Gfx.adj(skin, 135));
    }

    // ---------------- helpers (coordinates are relative to the body; M pixels of margin around it) ----------------

    private static void put(int[] p, int s, int x, int y, int c) {
        int ix = x + M, iy = y + M;
        if (ix < 0 || iy < 0 || ix >= s) return;
        int idx = iy * s + ix;
        if (idx < p.length) p[idx] = c;
    }

    private static void rect(int[] p, int s, int x, int y, int w, int h, int c) {
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++) put(p, s, x + i, y + j, c);
    }

    private static void disc(int[] p, int s, int cx, int cy, int r, int c) {
        for (int j = -r; j <= r; j++)
            for (int i = -r; i <= r; i++)
                if (i * i + j * j <= r * r + 1) put(p, s, cx + i, cy + j, c);
    }

    private static void rrect(int[] p, int s, int x, int y, int w, int h, int r, int c) {
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int ox = i < r ? r - i : (i >= w - r ? i - (w - r) + 1 : 0);
                int oy = j < r ? r - j : (j >= h - r ? j - (h - r) + 1 : 0);
                if (ox > 0 && oy > 0 && ox * ox + oy * oy > r * r + 1) continue;
                put(p, s, x + i, y + j, c);
            }
        }
    }

    private static void tint(int[] p, int s, int L, int y0, int y1, int pct) {
        for (int y = y0; y <= y1; y++) {
            for (int x = 0; x < L; x++) {
                int idx = (y + M) * s + (x + M);
                if (idx < p.length && (p[idx] >>> 24) == 0xFF) p[idx] = Gfx.adj(p[idx], pct);
            }
        }
    }

    private static int[] outline(int[] p, int s, int h) {
        int[] q = new int[s * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < s; x++) {
                int i = y * s + x;
                if (p[i] != 0) { q[i] = p[i]; continue; }
                boolean n = (x > 0 && (p[i - 1] >>> 24) == 0xFF) || (x < s - 1 && (p[i + 1] >>> 24) == 0xFF)
                        || (y > 0 && (p[i - s] >>> 24) == 0xFF) || (y < h - 1 && (p[i + s] >>> 24) == 0xFF);
                if (n) q[i] = OUTLINE;
            }
        }
        return q;
    }
}
