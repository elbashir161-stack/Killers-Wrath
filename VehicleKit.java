import javax.microedition.lcdui.Image;

/** Draws vehicles (top-down, with outline and shading) at true scale: car = 4.5m x 1.8m. */
public class VehicleKit {
    public static final int SEDAN = 0, SPORTS = 1, PICKUP = 2, MOTO = 3;
    public static final int[] LEN = {72, 70, 80, 36};
    public static final int[] WID = {30, 28, 32, 16};
    public static final int[] MAXSP = {7, 9, 6, 8};   // px per tick at top speed
    public static final int[] ACC = {3, 4, 3, 5};     // in 1/16 px per tick

    private static final int[] CAR_COLORS = {0xFFB52A2A, 0xFF2B5DA8, 0xFFDADADA, 0xFF2A2A2E, 0xFF3C8A4A, 0xFF9AA0A8};
    private static final int[] MOTO_COLORS = {0xFFB52A2A, 0xFF2A2A2E, 0xFF2B5DA8};
    private static final int OUTLINE = 0xFF0C0806;
    private static final Image[][][] cache = new Image[4][6][4];

    public static int colorCount(int type) { return type == MOTO ? 3 : 6; }

    public static Image image(int type, int color, int dir) {
        Image im = cache[type][color][dir];
        if (im == null) {
            im = make(type, color, dir);
            cache[type][color][dir] = im;
        }
        return im;
    }

    private static Image make(int type, int color, int dir) {
        int L = LEN[type], W = WID[type];
        int s = L + 2, h = W + 2;
        int body = (type == MOTO) ? MOTO_COLORS[color] : CAR_COLORS[color];
        int[] r = new int[s * h];
        if (type == MOTO) moto(r, s, L, W, body);
        else car(r, s, L, W, type, body);
        r = outline(r, s, h);
        if (dir == Sprites.RIGHT) return Image.createRGBImage(r, s, h, true);
        if (dir == Sprites.LEFT) {
            int[] o = new int[s * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < s; x++) o[y * s + x] = r[y * s + (s - 1 - x)];
            return Image.createRGBImage(o, s, h, true);
        }
        // vertical: transpose (front ends up at the bottom = facing down)
        int[] d = new int[s * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < s; x++) d[x * h + y] = r[y * s + x];
        if (dir == Sprites.DOWN) return Image.createRGBImage(d, h, s, true);
        int[] u = new int[s * h];
        for (int y = 0; y < s; y++)
            for (int x = 0; x < h; x++) u[y * h + x] = d[(s - 1 - y) * h + x];
        return Image.createRGBImage(u, h, s, true);
    }

    // ---------------- art (facing right) ----------------

    private static void car(int[] p, int s, int L, int W, int type, int body) {
        int tire = 0xFF111114, glass = 0xFF35566E, glassHi = 0xFF6F9AB8;
        rrect(p, s, 0, 0, L, W, 6, body);
        tint(p, s, L, 0, 1, 125);
        tint(p, s, L, W - 3, W - 1, 72);
        int rwx = 11, fwx = L - 22;
        rect(p, s, rwx, 0, 9, 2, tire); rect(p, s, rwx, W - 2, 9, 2, tire);
        rect(p, s, fwx, 0, 9, 2, tire); rect(p, s, fwx, W - 2, 9, 2, tire);
        int rgx, rgw, roofx, roofw, wsx, wsw;
        if (type == SEDAN) { rgx = 17; rgw = 5; roofx = 22; roofw = 21; wsx = 43; wsw = 8; }
        else if (type == SPORTS) { rgx = 22; rgw = 5; roofx = 27; roofw = 12; wsx = 39; wsw = 9; }
        else { rgx = 37; rgw = 3; roofx = 40; roofw = 12; wsx = 52; wsw = 8; }
        int gh = W - 8;
        rect(p, s, rgx, 4, rgw, gh, glass);
        rect(p, s, roofx, 4, roofw, gh, SpriteKit.adj(body, 114));
        rect(p, s, roofx, W - 6, roofw, 2, SpriteKit.adj(body, 80));
        rect(p, s, wsx, 4, wsw, gh, glass);
        rect(p, s, wsx + 1, 5, 1, gh - 2, glassHi);
        int hx = wsx + wsw + 3;
        rect(p, s, hx, W / 2, L - hx - 5, 1, SpriteKit.adj(body, 86));
        if (type == SPORTS) {
            rect(p, s, 4, W / 2 - 1, L - 8, 2, 0xFFEFEFEF);
            rect(p, s, 2, 3, 4, W - 6, SpriteKit.adj(body, 60));
        }
        if (type == PICKUP) {
            rect(p, s, 5, 4, 31, gh, 0xFF3E3E44);
            for (int x = 8; x < 35; x += 5) rect(p, s, x, 4, 1, gh, 0xFF55555C);
            rect(p, s, 2, 5, 3, W - 10, SpriteKit.adj(body, 78));
        }
        rect(p, s, L - 3, 3, 3, 4, 0xFFFFF2B0);
        rect(p, s, L - 3, W - 7, 3, 4, 0xFFFFF2B0);
        rect(p, s, 0, 3, 2, 4, 0xFFC01818);
        rect(p, s, 0, W - 7, 2, 4, 0xFFC01818);
    }

    private static void moto(int[] p, int s, int L, int W, int body) {
        int tire = 0xFF0E0E10;
        rrect(p, s, 0, 6, 9, 4, 2, tire);
        rrect(p, s, 27, 6, 9, 4, 2, tire);
        rect(p, s, 9, 7, 18, 2, 0xFF3A3A40);
        rrect(p, s, 10, 3, 16, 10, 4, body);
        tint(p, s, L, 3, 4, 125);
        rect(p, s, 12, 5, 6, 6, 0xFF1A1A1E);
        rect(p, s, 19, 6, 5, 4, 0xFF55555C);
        rect(p, s, 25, 1, 2, 14, 0xFF2A2A30);
        rect(p, s, 34, 7, 2, 2, 0xFFFFF2B0);
        rect(p, s, 0, 7, 2, 2, 0xFFC01818);
    }

    // ---------------- helpers (all coordinates have a 1px margin for the outline) ----------------

    private static void put(int[] p, int s, int x, int y, int c) {
        int ix = x + 1, iy = y + 1;
        if (ix < 0 || iy < 0 || ix >= s) return;
        int idx = iy * s + ix;
        if (idx < p.length) p[idx] = c;
    }

    private static void rect(int[] p, int s, int x, int y, int w, int h, int c) {
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++) put(p, s, x + i, y + j, c);
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
                int idx = (y + 1) * s + (x + 1);
                if (idx < p.length && p[idx] != 0) p[idx] = SpriteKit.adj(p[idx], pct);
            }
        }
    }

    private static int[] outline(int[] p, int s, int h) {
        int[] q = new int[s * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < s; x++) {
                int i = y * s + x;
                if (p[i] != 0) q[i] = p[i];
                else if ((x > 0 && p[i - 1] != 0) || (x < s - 1 && p[i + 1] != 0)
                        || (y > 0 && p[i - s] != 0) || (y < h - 1 && p[i + s] != 0)) q[i] = OUTLINE;
            }
        }
        return q;
    }
}
