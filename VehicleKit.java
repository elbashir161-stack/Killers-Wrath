import javax.microedition.lcdui.Image;

/**
 * Vehicles drawn in the old Gangstar / GTA "3/4" view: you see the roof, the side panels, the glass, and the wheels.
 * Each picture is rendered from a small 3D model (boxes and wheels) at one of 16 headings, so it looks right at every angle.
 * Heading 0 = east, 4 = south, 8 = west, 12 = north. Car = 4.5 m x 1.8 m at 16 px per metre.
 */
public class VehicleKit {
    public static final int SEDAN = 0, SPORTS = 1, PICKUP = 2, MOTO = 3, MOTO_RIDER = 4;
    public static final int[] LEN = {72, 70, 80, 36, 36};
    public static final int[] WID = {30, 28, 32, 16, 16};
    public static final int[] MAXSP = {7, 9, 6, 8, 8};   // px per tick at top speed
    public static final int[] ACC = {3, 4, 3, 5, 5};     // in 1/16 px per tick
    public static final int[] COS = new int[16];
    public static final int[] SIN = new int[16];
    private static final int[] C12 = {256, 222, 128, 0, -128, -222, -256, -222, -128, 0, 128, 222};
    private static final int[] S12 = {0, 128, 222, 256, 222, 128, 0, -128, -222, -256, -222, -128};

    static {
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8.0;
            COS[i] = (int) Math.floor(Math.cos(a) * 256 + 0.5);
            SIN[i] = (int) Math.floor(Math.sin(a) * 256 + 0.5);
        }
    }

    private static final int[] CAR_COLORS = {0xFFC8C8BC, 0xFF34527A, 0xFF8A2E2E, 0xFF2E4A36, 0xFF7A7E80, 0xFFB8742A};
    private static final int[] MOTO_COLORS = {0xFF8A2A2A, 0xFF2A2A2E, 0xFF2B4A6A};
    private static final int OUTLINE = 0xFF0A0A0B;
    private static final int SHADOW = 0x55000000;
    private static final int TIRE = 0xFF0C0C0E;
    private static final Image[][][] cache = new Image[5][6][16];

    // projection state for the picture being drawn
    private static int cx, cy, ch, sh;
    private static final int TILT = 230, ZK = 190;    // ground squash 0.9, height scale 0.74 (x256)

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
        int L = LEN[type];
        int D = L + 30;
        int[] b = new int[D * D];
        cx = D / 2;
        cy = D / 2;
        ch = COS[h];
        sh = SIN[h];
        int body = (type >= MOTO) ? MOTO_COLORS[color] : CAR_COLORS[color];
        if (type >= MOTO) moto(b, D, body, type == MOTO_RIDER);
        else car(b, D, type, body);
        return Image.createRGBImage(outline(b, D), D, D, true);
    }

    // ---------------- projection ----------------

    private static int px(int u, int v) { return cx + (u * ch - v * sh) / 256; }
    private static int py(int u, int v, int z) { return cy + (((u * sh + v * ch) / 256) * TILT - z * ZK) / 256; }

    // ---------------- car ----------------

    private static void car(int[] b, int D, int type, int body) {
        int L = LEN[type], W = WID[type];
        int hl = L / 2, hw = W / 2;
        int glass = 0xFF1F2E38, dark = Gfx.adj(body, 62);
        int z0 = 3, z1 = (type == SPORTS) ? 9 : 11;
        // shadow on the road
        int[] sx = {px(-hl, -hw) + 3, px(hl, -hw) + 3, px(hl, hw) + 3, px(-hl, hw) + 3};
        int[] sy = {py(-hl, -hw, 0) + 3, py(hl, -hw, 0) + 3, py(hl, hw, 0) + 3, py(-hl, hw, 0) + 3};
        poly(b, D, sx, sy, 4, SHADOW);
        // far-side wheels first, then the body hides their top
        int far = (ch >= 0) ? -1 : 1;
        int au = hl - 15;
        wheel(b, D, -au, far * (hw - 1), 5, 6);
        wheel(b, D, au, far * (hw - 1), 5, 6);
        box(b, D, -hl, hl, -hw, hw, z0, z1, body, body, 0, true);
        box(b, D, hl - 1, hl + 2, -hw + 2, hw - 2, z0, z0 + 4, 0xFF2E2E34, 0xFF2E2E34, 0, false);   // bumpers
        box(b, D, -hl - 2, -hl + 1, -hw + 2, hw - 2, z0, z0 + 4, 0xFF2E2E34, 0xFF2E2E34, 0, false);
        if (type == SEDAN) {
            box(b, D, -18, 12, -hw + 3, hw - 3, z1, z1 + 7, Gfx.adj(body, 112), body, glass, false);
        } else if (type == SPORTS) {
            box(b, D, -14, 6, -hw + 4, hw - 4, z1, z1 + 5, Gfx.adj(body, 108), body, glass, false);
            box(b, D, -hl, -hl + 4, -hw + 2, hw - 2, z1, z1 + 3, dark, dark, 0, false);              // spoiler
        } else {
            box(b, D, 2, 24, -hw + 3, hw - 3, z1, z1 + 9, Gfx.adj(body, 112), body, glass, false);   // cab
            box(b, D, -hl + 1, -hl + 3, -hw, hw, z1, z1 + 4, dark, dark, 0, false);                  // tailgate
            box(b, D, -hl + 1, 0, -hw, -hw + 2, z1, z1 + 4, dark, dark, 0, false);                   // bed rails
            box(b, D, -hl + 1, 0, hw - 2, hw, z1, z1 + 4, dark, dark, 0, false);
        }
        int near = -far;
        wheel(b, D, -au, near * (hw + 1), 5, 6);
        wheel(b, D, au, near * (hw + 1), 5, 6);
    }

    // ---------------- motorcycle ----------------

    private static void moto(int[] b, int D, int body, boolean rider) {
        int[] sx = {px(-16, -4) + 3, px(16, -4) + 3, px(16, 4) + 3, px(-16, 4) + 3};
        int[] sy = {py(-16, -4, 0) + 3, py(16, -4, 0) + 3, py(16, 4, 0) + 3, py(-16, 4, 0) + 3};
        poly(b, D, sx, sy, 4, SHADOW);
        wheel(b, D, -13, 0, 6, 7);
        wheel(b, D, 13, 0, 6, 7);
        int dk = 0xFF2A2A30;
        box(b, D, -10, 10, -2, 2, 5, 9, dk, dk, 0, false);                       // frame and engine
        box(b, D, -9, -1, -3, 3, 9, 12, 0xFF1A1A1E, 0xFF1A1A1E, 0, false);      // seat
        box(b, D, -1, 9, -3, 3, 9, 15, body, body, 0, false);                    // tank
        box(b, D, 9, 11, -8, 8, 16, 18, dk, dk, 0, false);                       // handlebars
        box(b, D, 10, 13, -2, 2, 9, 14, 0xFFE8E0A8, 0xFFE8E0A8, 0, false);      // headlight
        if (!rider) return;
        int pants = 0xFF14161A, jacket = 0xFF23272D, jl = 0xFF343A44, skin = 0xFF553322;
        int near = (ch >= 0) ? 1 : -1;
        box(b, D, -4, 4, -near * 6, -near * 3, 8, 15, pants, pants, 0, false);   // far leg
        box(b, D, 0, 9, -near * 6, -near * 4, 17, 20, jl, jl, 0, false);        // far arm
        box(b, D, -9, 0, -4, 4, 13, 25, jacket, jl, 0, false);                   // body
        box(b, D, -4, 4, near * 3, near * 6, 8, 15, pants, pants, 0, false);     // near leg
        box(b, D, 0, 9, near * 4, near * 6, 17, 20, jl, jl, 0, false);           // near arm
        int hx0 = px(-4, 0), hy0 = py(-4, 0, 29);                                // head
        int[] hxs = new int[12], hys = new int[12];
        for (int i = 0; i < 12; i++) {
            hxs[i] = hx0 + (C12[i] * 5) / 256;
            hys[i] = hy0 + (S12[i] * 5) / 256;
        }
        poly(b, D, hxs, hys, 12, skin);
        for (int i = 0; i < 12; i++) hys[i] -= 1;
        poly(b, D, new int[] {hx0 - 2, hx0 + 2, hx0 + 2, hx0 - 2}, new int[] {hy0 - 4, hy0 - 4, hy0 - 3, hy0 - 3}, 4, Gfx.adj(skin, 140));
    }

    // ---------------- 3D drawing helpers ----------------

    /** A wheel standing on the ground: a disc in the (forward, up) plane at lateral position v. */
    private static void wheel(int[] b, int D, int u, int v, int z, int r) {
        int[] xs = new int[12], ys = new int[12];
        for (int i = 0; i < 12; i++) {
            int uu = u + r * C12[i] / 256, zz = z + r * S12[i] / 256;
            xs[i] = px(uu, v);
            ys[i] = py(uu, v, zz);
        }
        poly(b, D, xs, ys, 12, TIRE);
        for (int i = 0; i < 12; i++) {
            int uu = u + (r * 5 / 12) * C12[i] / 256, zz = z + (r * 5 / 12) * S12[i] / 256;
            xs[i] = px(uu, v);
            ys[i] = py(uu, v, zz);
        }
        poly(b, D, xs, ys, 12, 0xFF6A6C70);
    }

    /**
     * A box. Faces that point towards the viewer are drawn (sides in two shades, then the top).
     * glass != 0: draw windows on the visible sides. lights: head and tail lamps on the end faces.
     */
    private static void box(int[] b, int D, int u0, int u1, int v0, int v1, int z0, int z1,
                            int top, int side, int glass, boolean lights) {
        int[] X = new int[8], Y = new int[8];
        for (int i = 0; i < 8; i++) {
            int u = ((i & 1) == 0) ? u0 : u1;
            int v = ((i & 2) == 0) ? v0 : v1;
            int z = ((i & 4) == 0) ? z0 : z1;
            X[i] = px(u, v);
            Y[i] = py(u, v, z);
        }
        // face = corners (bottom-left, bottom-right, top-right, top-left) and its outward normal (nu, nv)
        face(b, D, X, Y, 0, 1, 5, 4, 0, -1, side, glass, false, false);   // -v side
        face(b, D, X, Y, 2, 3, 7, 6, 0, 1, side, glass, false, false);    // +v side
        face(b, D, X, Y, 0, 2, 6, 4, -1, 0, side, glass, lights, false);  // rear end
        face(b, D, X, Y, 1, 3, 7, 5, 1, 0, side, glass, false, lights);   // front end
        poly(b, D, new int[] {X[4], X[5], X[7], X[6]}, new int[] {Y[4], Y[5], Y[7], Y[6]}, 4, top);
    }

    private static void face(int[] b, int D, int[] X, int[] Y, int a, int c, int d, int e, int nu, int nv,
                             int side, int glass, boolean rear, boolean front) {
        int ny = nu * sh + nv * ch;
        if (ny <= 0) return;                                  // facing away from the viewer
        int shade = (nu != 0) ? 60 : 76;
        int[] xs = {X[a], X[c], X[d], X[e]}, ys = {Y[a], Y[c], Y[d], Y[e]};
        poly(b, D, xs, ys, 4, Gfx.adj(side, shade));
        if (glass != 0) inset(b, D, xs, ys, 0.10f, 0.90f, 0.15f, 0.85f, glass);
        if (front) {
            inset(b, D, xs, ys, 0.08f, 0.30f, 0.45f, 0.85f, 0xFFE8E0A8);
            inset(b, D, xs, ys, 0.70f, 0.92f, 0.45f, 0.85f, 0xFFE8E0A8);
        }
        if (rear) {
            inset(b, D, xs, ys, 0.08f, 0.28f, 0.45f, 0.85f, 0xFFB01818);
            inset(b, D, xs, ys, 0.72f, 0.92f, 0.45f, 0.85f, 0xFFB01818);
        }
    }

    /** A smaller quad inside a face, between fractions s0..s1 along it and t0..t1 up it. */
    private static void inset(int[] b, int D, int[] xs, int[] ys, float s0, float s1, float t0, float t1, int color) {
        int[] ox = new int[4], oy = new int[4];
        float[][] st = {{s0, t0}, {s1, t0}, {s1, t1}, {s0, t1}};
        for (int i = 0; i < 4; i++) {
            float s = st[i][0], t = st[i][1];
            ox[i] = (int) (xs[0] * (1 - s) * (1 - t) + xs[1] * s * (1 - t) + xs[2] * s * t + xs[3] * (1 - s) * t + 0.5f);
            oy[i] = (int) (ys[0] * (1 - s) * (1 - t) + ys[1] * s * (1 - t) + ys[2] * s * t + ys[3] * (1 - s) * t + 0.5f);
        }
        poly(b, D, ox, oy, 4, color);
    }

    /** Scanline polygon fill. */
    private static void poly(int[] b, int D, int[] xs, int[] ys, int n, int color) {
        int minY = ys[0], maxY = ys[0];
        for (int i = 1; i < n; i++) {
            if (ys[i] < minY) minY = ys[i];
            if (ys[i] > maxY) maxY = ys[i];
        }
        if (minY < 0) minY = 0;
        if (maxY >= D) maxY = D - 1;
        int[] xi = new int[n + 2];
        for (int y = minY; y <= maxY; y++) {
            int cnt = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                int y0 = ys[i], y1 = ys[j];
                if (y0 == y1) continue;
                if ((y >= y0 && y < y1) || (y >= y1 && y < y0))
                    xi[cnt++] = xs[i] + ((xs[j] - xs[i]) * (y - y0) * 2 + (y1 - y0)) / (2 * (y1 - y0));
            }
            for (int i = 1; i < cnt; i++) {                   // sort the crossings
                int v = xi[i], k = i - 1;
                while (k >= 0 && xi[k] > v) { xi[k + 1] = xi[k]; k--; }
                xi[k + 1] = v;
            }
            for (int i = 0; i + 1 < cnt; i += 2) {
                int xa = xi[i], xb = xi[i + 1];
                if (xa < 0) xa = 0;
                if (xb >= D) xb = D - 1;
                for (int x = xa; x <= xb; x++) b[y * D + x] = color;
            }
        }
    }

    private static int[] outline(int[] p, int D) {
        int[] q = new int[D * D];
        for (int y = 0; y < D; y++) {
            for (int x = 0; x < D; x++) {
                int i = y * D + x;
                if (p[i] != 0) { q[i] = p[i]; continue; }
                boolean n = (x > 0 && (p[i - 1] >>> 24) == 0xFF) || (x < D - 1 && (p[i + 1] >>> 24) == 0xFF)
                        || (y > 0 && (p[i - D] >>> 24) == 0xFF) || (y < D - 1 && (p[i + D] >>> 24) == 0xFF);
                if (n) q[i] = OUTLINE;
            }
        }
        return q;
    }
}
