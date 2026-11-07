/**
 * Draws human figures (20x30) with rounded heads, shading and a dark outline.
 * Used for Killer and for every pedestrian.
 */
public class SpriteKit {
    public static final int W = 20, H = 30;
    private static final int OUTLINE = 0xFF0C0806;
    private static final int SHOE = 0xFF1C1C22;

    // current figure (set by build)
    private static int skin, skinD, skinL, hair, style, shirt, sleeve, pants, legC, belt;
    private static boolean female, killer;

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
     * style: 0 bald, 1 short hair, 2 long hair.
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
        sleeve = isKiller ? adj(shirtC, 128) : adj(shirtC, 90);
        pants = pantsC;
        female = fem;
        killer = isKiller;
        legC = fem ? skinC : pantsC;
        belt = isKiller ? 0xFF050507 : 0xFF2A2420;

        int[] p = new int[W * H];
        if (dir == Sprites.RIGHT) side(p, f);
        else frontBack(p, f, dir == Sprites.DOWN);
        return outline(p);
    }

    // ---------------- front / back ----------------

    private static void frontBack(int[] p, int f, boolean front) {
        int ll = (f == 1) ? 6 : 7;
        int rl = (f == 2) ? 6 : 7;
        leg(p, 5, 20, 5, ll, legC);
        shoe(p, 4, 20 + ll, 6);
        leg(p, 10, 20, 5, rl, legC);
        shoe(p, 10, 20 + rl, 6);
        if (female) skirt(p, 4, 12);

        // torso with shoulders
        rect(p, 5, 12, 10, 8, shirt);
        rect(p, 4, 12, 12, 2, shirt);
        rect(p, 5, 13, 1, 7, adj(shirt, 124));
        rect(p, 13, 13, 2, 7, adj(shirt, 74));
        rect(p, 15, 12, 1, 2, adj(shirt, 74));
        if (!female) rect(p, 5, 19, 10, 1, belt);

        // neck and collar
        rect(p, 9, 11, 2, 1, skinD);
        if (front) {
            rect(p, 9, 12, 2, 2, skinD);
            if (killer) rect(p, 9, 14, 1, 5, adj(shirt, 155)); // zipper
        } else {
            rect(p, 8, 12, 4, 1, adj(shirt, 130));
        }

        // arms swing opposite the legs
        int aL = 12 + (f == 2 ? 1 : 0);
        int aR = 12 + (f == 1 ? 1 : 0);
        rect(p, 2, aL, 2, 6, sleeve);
        rect(p, 2, aL, 1, 6, adj(sleeve, 124));
        rect(p, 2, aL + 6, 2, 2, skin);
        rect(p, 16, aR, 2, 6, sleeve);
        rect(p, 17, aR, 1, 6, adj(sleeve, 72));
        rect(p, 16, aR + 6, 2, 2, skin);

        // head
        ell(p, 20, 13, 8, 9, skin, true, 0, H);
        if (front) {
            rect(p, 5, 6, 1, 2, skinD);
            rect(p, 14, 6, 1, 2, skinD);
            if (style > 0) {
                ell(p, 20, 13, 8, 9, hair, false, 2, 4);
                rect(p, 8, 2, 3, 1, adj(hair, 140));
                rect(p, 6, 5, 1, 2, hair);
                rect(p, 13, 5, 1, 2, hair);
                if (style == 2) {
                    rect(p, 5, 4, 2, 9, hair);
                    rect(p, 13, 4, 2, 9, hair);
                }
            } else {
                px(p, 8, 3, skinL);
                px(p, 9, 3, skinL);
            }
            int eye = killer ? 0xFFE01818 : 0xFF14100E;
            int brow = killer ? 0xFF120A06 : (style > 0 ? adj(hair, 80) : skinD);
            px(p, 8, 6, eye);
            px(p, 11, 6, eye);
            if (killer) {
                px(p, 7, 4, brow);
                px(p, 8, 5, brow);
                px(p, 12, 4, brow);
                px(p, 11, 5, brow);
            } else {
                rect(p, 7, 5, 2, 1, brow);
                rect(p, 11, 5, 2, 1, brow);
            }
            rect(p, 9, 8, 2, 1, skinD);
            rect(p, 9, 9, 2, 1, killer ? 0xFF1E0C08 : adj(skin, 62));
        } else {
            if (style > 0) {
                ell(p, 20, 13, 8, 9, hair, false, 2, 8);
                if (style == 2) rect(p, 5, 5, 10, 9, hair);
            } else {
                ell(p, 20, 13, 8, 9, adj(skin, 78), false, 7, 10);
            }
        }
    }

    // ---------------- side (facing right) ----------------

    private static void side(int[] p, int f) {
        int ax, ah, bx, bh;
        if (f == 0) { ax = 7; ah = 7; bx = 8; bh = 7; }
        else if (f == 1) { ax = 4; ah = 6; bx = 10; bh = 7; }
        else { ax = 10; ah = 6; bx = 4; bh = 7; }
        leg(p, ax, 20, 4, ah, adj(legC, 88));
        shoe(p, ax, 20 + ah, 5);
        leg(p, bx, 20, 4, bh, legC);
        shoe(p, bx, 20 + bh, 5);
        if (female) skirt(p, 6, 9);

        rect(p, 6, 12, 8, 8, shirt);
        rect(p, 6, 12, 1, 8, adj(shirt, 124));
        rect(p, 13, 12, 1, 8, adj(shirt, 74));
        if (!female) rect(p, 6, 19, 8, 1, belt);
        rect(p, 9, 11, 2, 1, skinD);

        ell(p, 22, 13, 8, 9, skin, true, 0, H);
        rect(p, 15, 7, 1, 2, skin);       // nose
        rect(p, 9, 6, 2, 2, skinD);       // ear
        if (style > 0) {
            ell(p, 22, 13, 8, 9, hair, false, 2, 4);
            rect(p, 7, 4, 3, 4, hair);
            rect(p, 9, 2, 3, 1, adj(hair, 140));
            if (style == 2) rect(p, 5, 3, 5, 11, hair);
        } else {
            px(p, 10, 3, skinL);
            px(p, 11, 3, skinL);
        }
        int eye = killer ? 0xFFE01818 : 0xFF14100E;
        int brow = killer ? 0xFF120A06 : (style > 0 ? adj(hair, 80) : skinD);
        px(p, 13, 6, eye);
        rect(p, 12, 5, 3, 1, brow);
        if (killer) px(p, 14, 4, brow);
        rect(p, 13, 9, 2, 1, killer ? 0xFF1E0C08 : adj(skin, 62));

        int armX = (f == 1) ? 7 : (f == 2 ? 11 : 9);
        rect(p, armX, 12, 3, 6, sleeve);
        rect(p, armX, 12, 1, 6, adj(sleeve, 124));
        rect(p, armX + 2, 12, 1, 6, adj(sleeve, 72));
        rect(p, armX, 18, 3, 2, skin);
    }

    // ---------------- helpers ----------------

    private static void skirt(int[] p, int x, int w) {
        rect(p, x, 19, w, 4, pants);
        rect(p, x, 19, 1, 4, adj(pants, 124));
        rect(p, x + w - 1, 19, 1, 4, adj(pants, 72));
        rect(p, x, 22, w, 1, adj(pants, 80));
    }

    private static void leg(int[] p, int x, int y, int w, int h, int c) {
        rect(p, x, y, w, h, c);
        rect(p, x, y, 1, h, adj(c, 124));
        rect(p, x + w - 1, y, 1, h, adj(c, 72));
    }

    private static void shoe(int[] p, int x, int y, int w) {
        rect(p, x, y, w, 2, SHOE);
        rect(p, x + 1, y, w - 2, 1, adj(SHOE, 175));
    }

    private static void px(int[] p, int x, int y, int c) {
        if (x >= 0 && x < W && y >= 0 && y < H) p[y * W + x] = c;
    }

    private static void rect(int[] p, int x, int y, int w, int h, int c) {
        for (int j = y; j < y + h; j++)
            for (int i = x; i < x + w; i++)
                if (i >= 0 && i < W && j >= 0 && j < H) p[j * W + i] = c;
    }

    /** Filled ellipse in doubled coordinates (cx, cy, rx, ry), optionally 3-tone shaded. */
    private static void ell(int[] p, int cx, int cy, int rx, int ry, int c, boolean shade,
                            int yMin, int yMax) {
        int light = adj(c, 125), dark = adj(c, 76);
        for (int y = 0; y < H; y++) {
            if (y < yMin || y > yMax) continue;
            for (int x = 0; x < W; x++) {
                int dx = 2 * x + 1 - cx, dy = 2 * y + 1 - cy;
                if (dx * dx * ry * ry + dy * dy * rx * rx > rx * rx * ry * ry) continue;
                int col = c;
                if (shade) {
                    int s = (dx * 100 / rx) * 6 + (dy * 100 / ry) * 4;
                    if (s > 330) col = dark;
                    else if (s < -300) col = light;
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
