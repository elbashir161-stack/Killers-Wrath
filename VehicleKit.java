import javax.microedition.lcdui.Image;

/**
 * Vehicles drawn in standard top-down / 3/4 perspective with smoother proportions,
 * realistic scaling, distinct vehicle types (Sedan, Sports Car, SUV/Truck, Motorcycle),
 * and 7 rich color palettes per vehicle type!
 */
public class VehicleKit {
    public static final int SEDAN = 0, SPORTS = 1, TRUCK = 2, MOTO = 3, MOTO_RIDER = 4;
    
    // Adjusted lengths and widths for proper relative scaling (in pixels)
    // Sedan: 72x30, Sports: 74x28, Truck/SUV: 84x34, Moto: 38x16
    public static final int[] LEN = {72, 74, 84, 38, 38};
    public static final int[] WID = {30, 28, 34, 16, 16};
    
    public static final int[] MAXSP = {7, 10, 6, 8, 8};   // Top speed (px per tick)
    public static final int[] ACC = {3, 5, 2, 5, 5};      // Acceleration
    
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

    // 7 rich color palettes per type! (0: Red, 1: Blue, 2: Silver, 3: Black, 4: White, 5: Green, 6: Yellow/Gold)
    private static final int[][] PALETTES = {
        // SEDAN
        {0xFF8A2E2E, 0xFF2A4A8A, 0xFF7A7E80, 0xFF1C1E20, 0xFFD8D8CC, 0xFF2E4A36, 0xFFB8742A},
        // SPORTS CAR
        {0xFFC81E1E, 0xFF1E5AC8, 0xFF8A8E94, 0xFF161618, 0xFFECECE8, 0xFF1EC85A, 0xFFD4A018},
        // TRUCK / SUV
        {0xFF6A2424, 0xFF243A6A, 0xFF5A6064, 0xFF1A1C1E, 0xFFC2C2B8, 0xFF263A2C, 0xFF9E6020},
        // MOTORCYCLE
        {0xFF9E1C1C, 0xFF1C4AE8, 0xFF707478, 0xFF141416, 0xFFE0E0D8, 0xFF1CA03A, 0xFFC88A14},
        // MOTORCYCLE + RIDER
        {0xFF9E1C1C, 0xFF1C4AE8, 0xFF707478, 0xFF141416, 0xFFE0E0D8, 0xFF1CA03A, 0xFFC88A14}
    };

    private static final int OUTLINE = 0xFF0A0A0B;
    private static final int SHADOW = 0x55000000;
    private static final int TIRE = 0xFF0C0C0E;
    
    // Cache array updated to support up to 7 colors per type
    private static final Image[][][] cache = new Image[5][7][16];

    // Projection constants
    private static int cx, cy, ch, sh;
    private static final int TILT = 230, ZK = 190;

    public static int colorCount(int type) { 
        return 7; 
    }

    public static Image image(int type, int color, int h) {
        h &= 15;
        color = (color < 0 || color >= 7) ? 0 : color;
        Image im = cache[type][color][h];
        if (im == null) {
            im = make(type, color, h);
            cache[type][color][h] = im;
        }
        return im;
    }

    private static Image make(int type, int color, int h) {
        int L = LEN[type];
        int D = L + 34; // Canvas size for rotation clearance
        int[] b = new int[D * D];
        cx = D / 2;
        cy = D / 2;
        ch = COS[h];
        sh = SIN[h];
        
        int bodyColor = PALETTES[type][color];
        if (type >= MOTO) {
            moto(b, D, bodyColor, type == MOTO_RIDER);
        } else {
            car(b, D, type, bodyColor);
        }
        return Image.createRGBImage(outline(b, D), D, D, true);
    }

    private static int px(int u, int v) { 
        return cx + (u * ch - v * sh) / 256; 
    }
    
    private static int py(int u, int v, int z) { 
        return cy + (((u * sh + v * ch) / 256) * TILT - z * ZK) / 256; 
    }

    // ---------------- Cars & Trucks ----------------

    private static void car(int[] b, int D, int type, int body) {
        int L = LEN[type], W = WID[type];
        int hl = L / 2, hw = W / 2;
        int glass = 0xFF1F2E38;
        int dark = Gfx.adj(body, 62);
        
        int z0 = 3;
        int z1 = (type == SPORTS) ? 9 : (type == TRUCK ? 13 : 11);

        // Ground shadow
        int[] sx = {px(-hl, -hw) + 3, px(hl, -hw) + 3, px(hl, hw) + 3, px(-hl, hw) + 3};
        int[] sy = {py(-hl, -hw, 0) + 3, py(hl, -hw, 0) + 3, py(hl, hw, 0) + 3, py(-hl, hw, 0) + 3};
        poly(b, D, sx, sy, 4, SHADOW);

        // Wheels
        int far = (ch >= 0) ? -1 : 1;
        int au = hl - 16;
        int wheelZ = (type == TRUCK) ? 6 : 5;
        int wheelR = (type == TRUCK) ? 7 : 6;
        
        wheel(b, D, -au, far * (hw - 1), wheelZ, wheelR);
        wheel(b, D, au, far * (hw - 1), wheelZ, wheelR);

        // Main chassis body
        box(b, D, -hl, hl, -hw, hw, z0, z1, body, body, 0, true);
        
        // Front & Rear Bumpers
        box(b, D, hl - 1, hl + 2, -hw + 2, hw - 2, z0, z0 + 4, 0xFF2E2E34, 0xFF2E2E34, 0, false);
        box(b, D, -hl - 2, -hl + 1, -hw + 2, hw - 2, z0, z0 + 4, 0xFF2E2E34, 0xFF2E2E34, 0, false);

        if (type == SEDAN) {
            // Sleek Sedan Roof & Cabin
            box(b, D, -18, 12, -hw + 3, hw - 3, z1, z1 + 7, Gfx.adj(body, 112), body, glass, false);
        } else if (type == SPORTS) {
            // Low-profile Aerodynamic Sports Cabin & Rear Spoiler
            box(b, D, -16, 6, -hw + 4, hw - 4, z1, z1 + 5, Gfx.adj(body, 115), body, glass, false);
            box(b, D, -hl, -hl + 4, -hw + 2, hw - 2, z1 + 2, z1 + 5, dark, dark, 0, false); // Raised spoiler
        } else if (type == TRUCK) {
            // High Heavy-duty Truck/SUV Cabin & Flatbed/Cargo
            box(b, D, 4, 28, -hw + 2, hw - 2, z1, z1 + 10, Gfx.adj(body, 110), body, glass, false); // Cab
            box(b, D, -hl + 2, 2, -hw, hw, z1, z1 + 5, dark, dark, 0, false);                      // Truck Bed
            box(b, D, -hl + 2, 0, -hw + 3, hw - 3, z1, z1 + 2, 0xFF1C1E20, 0xFF1C1E20, 0, false);   // Bed Interior
        }

        // Near-side Wheels
        int near = -far;
        wheel(b, D, -au, near * (hw + 1), wheelZ, wheelR);
        wheel(b, D, au, near * (hw + 1), wheelZ, wheelR);
    }

    // ---------------- Motorcycles ----------------

    private static void moto(int[] b, int D, int body, boolean rider) {
        int[] sx = {px(-18, -4) + 3, px(18, -4) + 3, px(18, 4) + 3, px(-18, 4) + 3};
        int[] sy = {py(-18, -4, 0) + 3, py(18, -4, 0) + 3, py(18, 4, 0) + 3, py(-18, 4, 0) + 3};
        poly(b, D, sx, sy, 4, SHADOW);

        wheel(b, D, -14, 0, 6, 7);
        wheel(b, D, 14, 0, 6, 7);
        int dk = 0xFF2A2A30;
        
        box(b, D, -11, 11, -2, 2, 5, 9, dk, dk, 0, false);                  // Frame & Engine block
        box(b, D, -10, -1, -3, 3, 9, 12, 0xFF1A1A1E, 0xFF1A1A1E, 0, false); // Leather Seat
        box(b, D, -1, 10, -3, 3, 9, 15, body, body, 0, false);               // Fuel Tank
        box(b, D, 10, 12, -8, 8, 16, 18, dk, dk, 0, false);                  // Handlebars
        box(b, D, 11, 14, -2, 2, 9, 14, 0xFFFFFFC0, 0xFFFFFFC0, 0, false); // Headlight

        if (!rider) return;

        int pants = 0xFF14161A, jacket = 0xFF23272D, jl = 0xFF343A44, skin = 0xFF553322;
        int near = (ch >= 0) ? 1 : -1;
        
        box(b, D, -4, 4, -near * 6, -near * 3, 8, 15, pants, pants, 0, false); // Far leg
        box(b, D, 0, 9, -near * 6, -near * 4, 17, 20, jl, jl, 0, false);        // Far arm
        box(b, D, -9, 0, -4, 4, 13, 25, jacket, jl, 0, false);               // Rider Body
        box(b, D, -4, 4, near * 3, near * 6, 8, 15, pants, pants, 0, false);   // Near leg
        box(b, D, 0, 9, near * 4, near * 6, 17, 20, jl, jl, 0, false);         // Near arm

        // Helmet/Head
        int hx0 = px(-4, 0), hy0 = py(-4, 0, 29);
        int[] hxs = new int[12], hys = new int[12];
        for (int i = 0; i < 12; i++) {
            hxs[i] = hx0 + (C12[i] * 5) / 256;
            hys[i] = hy0 + (S12[i] * 5) / 256;
        }
        poly(b, D, hxs, hys, 12, skin);
        for (int i = 0; i < 12; i++) hys[i] -= 1;
        poly(b, D, new int[] {hx0 - 2, hx0 + 2, hx0 + 2, hx0 - 2}, new int[] {hy0 - 4, hy0 - 4, hy0 - 3, hy0 - 3}, 4, Gfx.adj(skin, 140));
    }

    // ---------------- 3D Helpers ----------------

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
        poly(b, D, xs, ys, 12, 0xFF6A6C70); // Chrome Hubcap
    }

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
        face(b, D, X, Y, 0, 1, 5, 4, 0, -1, side, glass, false, false);   // -v side
        face(b, D, X, Y, 2, 3, 7, 6, 0, 1, side, glass, false, false);    // +v side
        face(b, D, X, Y, 0, 2, 6, 4, -1, 0, side, glass, lights, false);  // rear end
        face(b, D, X, Y, 1, 3, 7, 5, 1, 0, side, glass, false, lights);   // front end
        poly(b, D, new int[] {X[4], X[5], X[7], X[6]}, new int[] {Y[4], Y[5], Y[7], Y[6]}, 4, top);
    }

    private static void face(int[] b, int D, int[] X, int[] Y, int a, int c, int d, int e, int nu, int nv,
                             int side, int glass, boolean rear, boolean front) {
        int ny = nu * sh + nv * ch;
        if (ny <= 0) return;
        int shade = (nu != 0) ? 60 : 76;
        int[] xs = {X[a], X[c], X[d], X[e]}, ys = {Y[a], Y[c], Y[d], Y[e]};
        poly(b, D, xs, ys, 4, Gfx.adj(side, shade));
        if (glass != 0) inset(b, D, xs, ys, 0.10f, 0.90f, 0.15f, 0.85f, glass);
        if (front) {
            // Bright modern headlights
            inset(b, D, xs, ys, 0.08f, 0.30f, 0.45f, 0.85f, 0xFFFFFFC0);
            inset(b, D, xs, ys, 0.70f, 0.92f, 0.45f, 0.85f, 0xFFFFFFC0);
        }
        if (rear) {
            // Tail lights
            inset(b, D, xs, ys, 0.08f, 0.28f, 0.45f, 0.85f, 0xFFC01818);
            inset(b, D, xs, ys, 0.72f, 0.92f, 0.45f, 0.85f, 0xFFC01818);
        }
    }

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
            for (int i = 1; i < cnt; i++) {
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
                                               
