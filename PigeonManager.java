import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Pigeons on the pavements and plazas around Killer. They fly off when he (or a car) comes close. */
public class PigeonManager {
    public final Pigeon[] pigeons;
    private final World world;
    private static Image[][] sprites;   // [frame][0 right, 1 left]
    private static final int W = 16, H = 12;

    public PigeonManager(World w, Player k, int count) {
        world = w;
        pigeons = new Pigeon[count];
        for (int i = 0; i < count; i++) {
            pigeons[i] = new Pigeon(k.x, k.y);
            place(pigeons[i], k);
        }
        if (sprites == null && count > 0) buildSprites();
    }

    /** True for ground a pigeon may stand on: pavement, plaza or path. */
    public boolean goodGround(int px, int py) {
        int t = world.tileAt(px / World.TS, py / World.TS);
        return t == World.SIDEWALK || t == World.SIDEWALK2 || t == World.PLAZA || t == World.PATH;
    }

    /** Puts a pigeon on some pavement between 120 and 560 pixels from Killer. */
    public void place(Pigeon p, Player k) {
        for (int i = 0; i < 60; i++) {
            int tx = k.x / World.TS + NpcManager.rnd(71) - 35;
            int ty = k.y / World.TS + NpcManager.rnd(71) - 35;
            int px = tx * World.TS + 4 + NpcManager.rnd(8), py = ty * World.TS + 6 + NpcManager.rnd(8);
            if (!goodGround(px, py)) continue;
            int dx = px - k.x, dy = py - k.y;
            int d2 = dx * dx + dy * dy;
            if (d2 < 120 * 120 || d2 > 560 * 560) continue;
            p.reset(px, py);
            return;
        }
    }

    public void update(Player k, Vehicle driven) {
        for (int i = 0; i < pigeons.length; i++) pigeons[i].update(this, world, k, driven);
    }

    /** Shadows of birds in the air. */
    public void drawShadows(Graphics g, int camX, int camY, int sw, int sh) {
        g.setColor(0x14161A);
        for (int i = 0; i < pigeons.length; i++) {
            Pigeon p = pigeons[i];
            if (!p.flying) continue;
            int sx = p.x - camX, sy = p.y - camY;
            if (sx < -10 || sx > sw + 10 || sy < -10 || sy > sh + 10) continue;
            g.fillArc(sx - 4, sy - 1, 9, 3, 0, 360);
        }
    }

    public void drawOne(Graphics g, int i, int camX, int camY) {
        pigeons[i].draw(g, camX, camY);
    }

    /** Birds in the air are drawn over everything, even tree tops. */
    public void drawFlying(Graphics g, int camX, int camY, int sw, int sh) {
        for (int i = 0; i < pigeons.length; i++) {
            Pigeon p = pigeons[i];
            if (!p.flying) continue;
            int sx = p.x - camX, sy = p.y - p.z - camY;
            if (sx < -20 || sx > sw + 20 || sy < -20 || sy > sh + 20) continue;
            p.draw(g, camX, camY);
        }
    }

    public static Image sprite(int frame, boolean right) {
        return sprites[frame][right ? 0 : 1];
    }

    // ---------------- art ----------------

    private static void buildSprites() {
        sprites = new Image[5][2];
        for (int f = 0; f < 5; f++) {
            int[] p = pigeon(f);
            sprites[f][0] = Image.createRGBImage(outline(p), W, H, true);
            int[] m = new int[W * H];
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) m[y * W + x] = p[y * W + (W - 1 - x)];
            sprites[f][1] = Image.createRGBImage(outline(m), W, H, true);
        }
    }

    private static int[] pigeon(int f) {
        int[] p = new int[W * H];
        int body = 0xFF7C8590, dark = 0xFF565E68, wing = 0xFF616A75, neck = 0xFF4E8A78, beak = 0xFFD8A040, foot = 0xFFD0704A;
        boolean peck = (f == 1);
        boolean air = (f >= 3);
        int by = air ? 5 : 6;
        ell(p, 6f, by + 0.5f, 4.6f, 2.6f, body);                       // body
        ell(p, 2.5f, by + 1f, 2.4f, 1.4f, dark);                        // tail
        ell(p, 6f, by + 0.5f, 3f, 1.5f, wing);                          // folded wing
        float hx = 11f, hy = peck ? 8f : (air ? 3.5f : 4f);
        ell(p, 9.3f, peck ? 6.6f : by - 0.5f, 2f, 1.8f, neck);          // glossy neck
        ell(p, hx, hy, 2f, 1.8f, 0xFF646C78);                           // head
        px(p, (int) hx + 1, (int) hy, 0xFFE8E0C0);                      // eye
        px(p, (int) hx + 2, (int) hy + 1, beak);                        // beak
        px(p, (int) hx + 3, (int) hy + 1, beak);
        if (air) {                                                      // flapping wings
            boolean up = (f == 3);
            for (int i = 0; i < 6; i++) {
                int wy = up ? by - 1 - i : by + 1 + i / 2;
                px(p, 3 + i, wy, 0xFF9AA2AC);
                px(p, 4 + i, wy, 0xFF7C8590);
            }
        } else {
            px(p, 5, by + 3, foot); px(p, 5, by + 4, foot);
            if (f == 2) { px(p, 8, by + 3, foot); px(p, 9, by + 4, foot); }
            else { px(p, 8, by + 3, foot); px(p, 8, by + 4, foot); }
        }
        return p;
    }

    private static void px(int[] p, int x, int y, int c) {
        if (x >= 0 && x < W && y >= 0 && y < H) p[y * W + x] = c;
    }

    private static void ell(int[] p, float cx, float cy, float rx, float ry, int c) {
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                float nx = (x + 0.5f - cx) / rx, ny = (y + 0.5f - cy) / ry;
                if (nx * nx + ny * ny <= 1f) p[y * W + x] = c;
            }
    }

    private static int[] outline(int[] p) {
        int[] q = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int i = y * W + x;
                if (p[i] != 0) q[i] = p[i];
                else if ((x > 0 && p[i - 1] != 0) || (x < W - 1 && p[i + 1] != 0)
                        || (y > 0 && p[i - W] != 0) || (y < H - 1 && p[i + W] != 0)) q[i] = 0xFF14161A;
            }
        }
        return q;
    }
}
