/**
 * Draws human figures (24 x 34) from limbs instead of boxes: oval head, short neck, tapered torso,
 * slim arms and legs that bend while walking, shoes, shading and a dark outline.
 * Proportions: head about 1/5 of the height, legs about 40%.
 */
public class SpriteKit {
    public static final int W = 24, H = 34;
    private static final int OUTLINE = 0xFF0A0706;
    private static final int SHOE = 0xFF17171B;

    private static int skin, skinD, skinL, hair, style, shirt, sleeve, pants, belt;
    private static boolean female, killer, longSleeve;

    public static int adj(int c, int pct) {
        int r = ((c >> 16) & 0xFF) * pct / 100;
        int g = ((c >> 8) & 0xFF) * pct / 100;
        int b = (c & 0xFF) * pct / 100;
        if (r > 255) r = 255;
        if (g > 255) g = 255;
        if (b > 255) b = 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * dir: 0 down, 1 up, 2 left, 3 right. f: walk frame 0..2.
     * hairStyle: 0 bald, 1 short, 2 long.
     */
    public static int[] build(int dir, int f, int skinC, int hairC, int hairStyle,
                              int shirtC, int pantsC, boolean fem, boolean isKiller) {
        if (dir == Sprites.LEFT) {
            return mirror(build(Sprites.RIGHT, f, skinC, hairC, hairStyle, shirtC, pantsC, fem, isKiller));
        }
        skin = skinC;
        skinD = adj(skinC, 78);
        skinL = adj(skinC, 128);
        hair = hairC;
        style = hairStyle;
        shirt = shirtC;
        female = fem;
        killer = isKiller;
        longSleeve = isKiller || ((shirtC & 0xFF) % 2 == 0);
        sleeve = longSleeve ? adj(shirtC, 92) : shirtC;
        pants = pantsC;
        belt = isKiller ? 0xFF050507 : 0xFF2A2420;

        int[] p = new int[W * H];
        if (dir == Sprites.RIGHT) side(p, f);
        else frontBack(p, f, dir == Sprites.DOWN);
        return outline(p);
    }

    // ---------------- front / back ----------------

    private static void frontBack(int[] p, int f, boolean front) {
        // legs (a lifted leg is shorter, which reads as a step)
        float lk = 25f, la = 30f, rk = 25f, ra = 30f;
        if (f == 1) { lk = 24.2f; la = 28.6f; rk = 25.4f; ra = 30.4f; }
        if (f == 2) { rk = 24.2f; ra = 28.6f; lk = 25.4f; la = 30.4f; }
        int legC = female ? skin : pants;
        capsule(p, 10.4f, 19.5f, 10.3f, lk, 1.5f, legC);
        capsule(p, 10.3f, lk, 10.3f, la, 1.35f, legC);
        capsule(p, 13.6f, 19.5f, 13.7f, rk, 1.5f, legC);
        capsule(p, 13.7f, rk, 13.7f, ra, 1.35f, legC);
        ellipse(p, 10.3f, la + 1.2f, 2.0f, 1.2f, SHOE, true);
        ellipse(p, 13.7f, ra + 1.2f, 2.0f, 1.2f, SHOE, true);

        // torso: shoulders wide, waist narrow
        if (female) {
            torso(p, 12f, 10, 18, 4.1f, 3.0f, 3.3f, shirt);
            torso(p, 12f, 18, 22, 3.6f, 4.2f, 4.8f, pants);            // skirt
        } else {
            float sh = killer ? 5.5f : 4.9f;
            torso(p, 12f, 10, 19, sh, sh - 1.0f, sh - 1.4f, shirt);
            hline(p, 12f, 19, sh - 1.4f, belt);
        }
        // neck and collar
        rect(p, 11, 8, 2, 2, skinD);
        if (front) {
            rect(p, 11, 10, 2, 1, skinD);
            if (killer) rect(p, 11, 11, 1, 8, adj(shirt, 160));          // zipper
        } else {
            rect(p, 10, 10, 4, 1, adj(shirt, 130));
        }

        // arms swing opposite to the legs
        float sw = (f == 1) ? -0.9f : (f == 2 ? 0.9f : 0f);
        arm(p, 6.7f, 11.2f, 6.1f, 15.4f + sw * 0.5f, 6.4f, 19.0f + sw);
        arm(p, 17.3f, 11.2f, 17.9f, 15.4f - sw * 0.5f, 17.6f, 19.0f - sw);

        // head
        ellipse(p, 12f, 5.2f, 2.95f, 3.5f, skin, true);
        if (front) {
            rect(p, 8, 5, 1, 2, skinD);
            rect(p, 15, 5, 1, 2, skinD);
            if (style > 0) {
                hairCap(p, 12f, 5.2f, 2.95f, 3.5f, 3);
                rect(p, 9, 4, 1, 2, hair);
                rect(p, 14, 4, 1, 2, hair);
                px(p, 11, 2, adj(hair, 140));
                px(p, 12, 2, adj(hair, 140));
                if (style == 2) {
                    capsule(p, 9.3f, 4.5f, 9.0f, 12.5f, 1.1f, hair);
                    capsule(p, 14.7f, 4.5f, 15.0f, 12.5f, 1.1f, hair);
                }
            } else {
                px(p, 10, 3, skinL);
                px(p, 11, 3, skinL);
            }
            int eye = killer ? 0xFFE01818 : 0xFF14100E;
            px(p, 10, 5, eye);
            px(p, 13, 5, eye);
            if (killer) {
                int brow = 0xFF120A06;
                px(p, 9, 3, brow); px(p, 10, 4, brow);      // angry brows: low in the middle
                px(p, 14, 3, brow); px(p, 13, 4, brow);
            } else {
                int brow = (style > 0) ? adj(hair, 70) : skinD;
                px(p, 10, 4, brow);
                px(p, 13, 4, brow);
            }
            px(p, 11, 6, adj(skin, 82));
            px(p, 12, 6, adj(skin, 82));
            px(p, 11, 7, killer ? 0xFF1E0C08 : adj(skin, 60));
            px(p, 12, 7, killer ? 0xFF1E0C08 : adj(skin, 60));
        } else {
            if (style > 0) {
                hairCap(p, 12f, 5.2f, 2.95f, 3.5f, 6);
                if (style == 2) capsule(p, 12f, 6f, 12f, 13f, 2.6f, hair);
            } else {
                hairCapColor(p, 12f, 5.2f, 2.95f, 3.5f, 7, adj(skin, 78));
            }
        }
    }

    // ---------------- side (facing right) ----------------

    private static void side(int[] p, int f) {
        float s = (f == 1) ? 1f : (f == 2 ? -1f : 0f);
        int legC = female ? skin : pants;
        int legFar = adj(legC, 78);
        int slFar = adj(sleeve, 78);

        // far arm and far leg (behind the body)
        arm2(p, 12f, 11.2f, 12f - s * 1.3f, 15.4f, 12f - s * 2.6f, 19.0f, slFar);
        float fkx = 11.8f - s * 1.5f, fax = 11.8f - s * 3.0f, fay = (-s >= 0) ? 30.2f : 29.2f;
        capsule(p, 12f, 19.5f, fkx, 25f, 1.45f, legFar);
        capsule(p, fkx, 25f, fax, fay, 1.3f, legFar);
        ellipse(p, fax + 1.3f, fay + 1.1f, 2.7f, 1.2f, adj(SHOE, 80), false);

        // torso
        if (female) {
            torso(p, 12.1f, 10, 18, 2.9f, 2.4f, 2.6f, shirt);
            torso(p, 12.1f, 18, 22, 2.8f, 3.4f, 3.9f, pants);
        } else {
            float sh = killer ? 3.2f : 2.9f;
            torso(p, 12.1f, 10, 19, sh, sh - 0.2f, sh - 0.4f, shirt);
            hline(p, 12.1f, 19, sh - 0.4f, belt);
        }
        rect(p, 11, 8, 2, 2, skinD);

        // near leg
        float nkx = 12.2f + s * 1.5f, nax = 12.2f + s * 3.0f, nay = (s >= 0) ? 30.2f : 29.2f;
        capsule(p, 12f, 19.5f, nkx, 25f, 1.5f, legC);
        capsule(p, nkx, 25f, nax, nay, 1.35f, legC);
        ellipse(p, nax + 1.3f, nay + 1.1f, 2.7f, 1.2f, SHOE, true);

        // head
        if (style == 2) capsule(p, 10.3f, 4.5f, 10.0f, 13f, 1.7f, hair);
        ellipse(p, 12.5f, 5.2f, 2.9f, 3.5f, skin, true);
        px(p, 15, 6, skin);                                   // nose
        px(p, 11, 5, skinD);                                  // ear
        px(p, 11, 6, skinD);
        if (style > 0) {
            hairCap(p, 12.5f, 5.2f, 2.9f, 3.5f, 3);
            rect(p, 9, 4, 3, 3, hair);
            px(p, 12, 2, adj(hair, 140));
            px(p, 13, 2, adj(hair, 140));
        } else {
            px(p, 12, 3, skinL);
            px(p, 13, 3, skinL);
        }
        int eye = killer ? 0xFFE01818 : 0xFF14100E;
        px(p, 14, 5, eye);
        px(p, 13, 4, killer ? 0xFF120A06 : ((style > 0) ? adj(hair, 70) : skinD));
        px(p, 14, 4, killer ? 0xFF120A06 : ((style > 0) ? adj(hair, 70) : skinD));
        px(p, 14, 7, killer ? 0xFF1E0C08 : adj(skin, 60));

        // near arm
        arm2(p, 12f, 11.2f, 12f + s * 1.3f, 15.4f, 12f + s * 2.6f, 19.0f, sleeve);
    }

    // ---------------- parts ----------------

    private static void arm(int[] p, float sx, float sy, float ex, float ey, float hx, float hy) {
        arm2(p, sx, sy, ex, ey, hx, hy, sleeve);
    }

    private static void arm2(int[] p, float sx, float sy, float ex, float ey, float hx, float hy, int slv) {
        capsule(p, sx, sy, ex, ey, 1.15f, slv);
        capsule(p, ex, ey, hx, hy, 0.95f, longSleeve ? slv : skin);
        ellipse(p, hx, hy + 0.7f, 1.1f, 1.2f, skin, false);
    }

    private static void torso(int[] p, float cx, int y0, int y1, float hwTop, float hwMid, float hwBot, int color) {
        int light = adj(color, 122), dark = adj(color, 74);
        for (int y = y0; y <= y1; y++) {
            float t = (y1 == y0) ? 0f : (float) (y - y0) / (float) (y1 - y0);
            float hw = (t < 0.6f) ? hwTop + (hwMid - hwTop) * (t / 0.6f)
                                  : hwMid + (hwBot - hwMid) * ((t - 0.6f) / 0.4f);
            if (y == y0) hw -= 0.8f;                           // rounded shoulders
            int xl = (int) Math.floor(cx - hw + 0.5f);
            int xr = (int) Math.floor(cx + hw + 0.5f) - 1;
            for (int x = xl; x <= xr; x++) {
                float rel = (x + 0.5f - cx) / hw;
                int c = color;
                if (rel < -0.55f) c = light;
                else if (rel > 0.55f) c = dark;
                put(p, x, y, c);
            }
        }
    }

    private static void hline(int[] p, float cx, int y, float hw, int c) {
        int xl = (int) Math.floor(cx - hw + 0.5f), xr = (int) Math.floor(cx + hw + 0.5f) - 1;
        for (int x = xl; x <= xr; x++) put(p, x, y, c);
    }

    private static void hairCap(int[] p, float cx, float cy, float rx, float ry, int maxY) {
        hairCapColor(p, cx, cy, rx, ry, maxY, hair);
    }

    /** Colours the part of the head ellipse that lies at or above row maxY. */
    private static void hairCapColor(int[] p, float cx, float cy, float rx, float ry, int maxY, int c) {
        for (int y = 0; y <= maxY && y < H; y++) {
            for (int x = 0; x < W; x++) {
                float nx = (x + 0.5f - cx) / rx, ny = (y + 0.5f - cy) / ry;
                if (nx * nx + ny * ny <= 1f) p[y * W + x] = c;
            }
        }
    }

    private static void px(int[] p, int x, int y, int c) {
        put(p, x, y, c);
    }

    private static void put(int[] p, int x, int y, int c) {
        if (x >= 0 && x < W && y >= 0 && y < H) p[y * W + x] = c;
    }

    private static void rect(int[] p, int x, int y, int w, int h, int c) {
        for (int j = y; j < y + h; j++)
            for (int i = x; i < x + w; i++) put(p, i, j, c);
    }

    /** A rounded limb from (x0,y0) to (x1,y1), radius r, light on the left and dark on the right. */
    private static void capsule(int[] p, float x0, float y0, float x1, float y1, float r, int c) {
        float dx = x1 - x0, dy = y1 - y0;
        float len2 = dx * dx + dy * dy;
        int minx = (int) Math.floor(Math.min(x0, x1) - r), maxx = (int) Math.ceil(Math.max(x0, x1) + r);
        int miny = (int) Math.floor(Math.min(y0, y1) - r), maxy = (int) Math.ceil(Math.max(y0, y1) + r);
        int light = adj(c, 122), dark = adj(c, 74);
        for (int y = miny; y <= maxy; y++) {
            for (int x = minx; x <= maxx; x++) {
                if (x < 0 || x >= W || y < 0 || y >= H) continue;
                float px = x + 0.5f, py = y + 0.5f;
                float t = (len2 == 0f) ? 0f : ((px - x0) * dx + (py - y0) * dy) / len2;
                if (t < 0f) t = 0f;
                if (t > 1f) t = 1f;
                float ex = px - (x0 + t * dx), ey = py - (y0 + t * dy);
                if (ex * ex + ey * ey > r * r) continue;
                float v = ex / r;
                p[y * W + x] = (v < -0.45f) ? light : (v > 0.45f ? dark : c);
            }
        }
    }

    private static void ellipse(int[] p, float cx, float cy, float rx, float ry, int c, boolean shade) {
        int light = adj(c, 125), dark = adj(c, 76);
        int minx = (int) Math.floor(cx - rx), maxx = (int) Math.ceil(cx + rx);
        int miny = (int) Math.floor(cy - ry), maxy = (int) Math.ceil(cy + ry);
        for (int y = miny; y <= maxy; y++) {
            for (int x = minx; x <= maxx; x++) {
                if (x < 0 || x >= W || y < 0 || y >= H) continue;
                float nx = (x + 0.5f - cx) / rx, ny = (y + 0.5f - cy) / ry;
                if (nx * nx + ny * ny > 1f) continue;
                int col = c;
                if (shade) {
                    float s = nx * 0.6f + ny * 0.5f;
                    if (s > 0.45f) col = dark;
                    else if (s < -0.4f) col = light;
                }
                p[y * W + x] = col;
            }
        }
    }

    private static int[] outline(int[] p) {
        int[] q = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int i = y * W + x;
                if (p[i] != 0) {
                    q[i] = p[i];
                } else if ((x > 0 && p[i - 1] != 0) || (x < W - 1 && p[i + 1] != 0)
                        || (y > 0 && p[i - W] != 0) || (y < H - 1 && p[i + W] != 0)) {
                    q[i] = OUTLINE;
                }
            }
        }
        return q;
    }

    private static int[] mirror(int[] p) {
        int[] m = new int[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                m[y * W + x] = p[y * W + (W - 1 - x)];
        return m;
    }
}
