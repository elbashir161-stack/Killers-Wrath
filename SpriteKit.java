/**
 * Character art for Killer and every pedestrian: outlined, shaded, with several body
 * builds, hair styles and extras. (Same class name and same build(...) call as before.)
 * Returns plain ARGB int[] (no MIDP classes). Image size is W x H; the feet touch
 * the bottom, so draw at (x - W / 2, y - 28) like before.
 */
public class SpriteKit {
    public static final int W = 20, H = 30;

    // body builds
    public static final int NORMAL = 0, HEAVY = 1, SLIM = 2;
    // hair styles
    public static final int BALD = 0, SHORT = 1, LONG = 2, SPIKY = 3, CAP = 4, HOOD = 5;
    // extras
    public static final int NONE = 0, SHADES = 1, MASK = 2, KILLER = 3;   // KILLER = red eyes, angry brow, mask, long sleeves, gloves

    private static final int OX = 1, OY = 2;   // where the 18 x 28 drawing sits inside W x H
    private static final int OUTLINE = 0xFF15110E;
    private static final int EYE = 0xFF15151C;
    private static final int BELT = 0xFF2A2018;
    private static final int SHOE = 0xFF2B2420;
    private static final int LENS = 0xFF15151A;
    private static final int MASKC = 0xFF2A2A30;
    private static final int RED = 0xFFE01818;
    private static final int GLOVE = 0xFF0A0A0C;

    // colours for the character being built
    private static int sk, skD, hr, hrD, sh, shL, shD, pn, pnD;
    private static boolean fem;
    private static int bd, hw, st, ex;

    /**
     * @param dir    Sprites.UP / DOWN / LEFT / RIGHT
     * @param frame  0 = standing, 1 and 2 = walking steps
     * @param style  BALD, SHORT, LONG, SPIKY or CAP
     * @param body   NORMAL, HEAVY or SLIM
     * @param extra  NONE, SHADES or MASK
     */
    public static int[] build(int dir, int frame, int skin, int hair, int style,
            int shirt, int pants, boolean female, int body, int extra) {
        sk = skin;      skD = shade(skin, 80);
        hr = hair;      hrD = shade(hair, 70);
        sh = shirt;     shL = shade(shirt, 118);  shD = shade(shirt, 72);
        pn = pants;     pnD = shade(pants, 75);
        fem = female;   bd = body;  st = style;  ex = extra;
        hw = (body == HEAVY) ? 5 : (body == SLIM ? 3 : 4);
        if (female && body == NORMAL) hw = 3;

        int[] p = new int[W * H];
        if (dir == Sprites.UP) {
            front(p, frame, true);
        } else if (dir == Sprites.DOWN) {
            front(p, frame, false);
        } else {
            side(p, frame);
            if (dir == Sprites.LEFT) flip(p);
        }
        outline(p);
        return p;
    }


    /**
     * Same call the old SpriteKit had, so Sprites.java and NpcSprites.java need no changes.
     * dir: Sprites.DOWN / UP / LEFT / RIGHT, f: walk frame 0..2, hairStyle: 0 bald, 1 short, 2 long.
     * isKiller = true gives the serious hooded look.
     */
    public static int[] build(int dir, int f, int skinC, int hairC, int hairStyle,
                              int shirtC, int pantsC, boolean fem, boolean isKiller) {
        if (isKiller) {
            return build(dir, f, skinC, hairC, HOOD, shade(shirtC, 125), pantsC, false, NORMAL, KILLER);
        }
        return build(dir, f, skinC, hairC, hairStyle, shirtC, pantsC, fem, NORMAL, NONE);
    }

    /** Same helper as before (other files use SpriteKit.adj). */
    public static int adj(int c, int pct) {
        return shade(c, pct);
    }

    // ------------------------------------------------------------ front / back

    private static void front(int[] p, int f, boolean back) {
        int tl = 9 - hw, tr = 8 + hw;
        int swing = (f == 1) ? 1 : (f == 2 ? -1 : 0);

        // legs
        int lw = (bd == HEAVY) ? 4 : (bd == SLIM ? 2 : 3);
        leg(p, 9 - lw, lw, (f == 1) ? 1 : 0, false);
        leg(p, 9, lw, (f == 2) ? 1 : 0, true);

        // torso
        rect(p, tl, 12, tr - tl + 1, 7, sh);
        rect(p, tl, 12, 1, 7, shL);
        rect(p, tr, 12, 1, 7, shD);
        rect(p, tl, 19, tr - tl + 1, 1, BELT);
        if (!back) {
            rect(p, 8, 19, 2, 1, 0xFFB8A040);    // belt buckle
            if (st != HOOD) {
                rect(p, 8, 12, 2, 1, skD);        // collar
                rect(p, 7, 12, 1, 1, shD);
                rect(p, 10, 12, 1, 1, shD);
            }
            if (ex == KILLER) rect(p, 8, 13, 1, 6, shade(sh, 165));   // zipper
        }
        rect(p, 8, 11, 2, 1, skD);                // neck

        // arms
        arm(p, tl - 2, swing, false);
        arm(p, tr + 1, -swing, true);

        if (back) headBack(p); else headFront(p);
    }

    private static void leg(int[] p, int x, int w, int lift, boolean right) {
        rect(p, x, 20, w, 4 - lift, pn);
        rect(p, right ? x : x + w - 1, 20, 1, 4 - lift, pnD);   // inner edge in shadow
        rect(p, x, 24 - lift, w, 2, SHOE);
        rect(p, x, 24 - lift, w, 1, shade(SHOE, 135));
    }

    private static void arm(int[] p, int x, int swing, boolean right) {
        rect(p, x, 12, 2, 3, sh);
        if (right) rect(p, x + 1, 12, 1, 3, shD); else rect(p, x, 12, 1, 3, shL);
        rect(p, x, 14, 2, 1, shD);
        if (ex == KILLER) {                            // long sleeves + black gloves
            rect(p, x, 15, 2, 2 + swing, sh);
            if (right) rect(p, x + 1, 15, 1, 2 + swing, shD); else rect(p, x, 15, 1, 2 + swing, shL);
            rect(p, x, 17 + swing, 2, 1, GLOVE);
            return;
        }
        rect(p, x, 15, 2, 3 + swing, sk);
        rect(p, right ? x + 1 : x, 15, 1, 3 + swing, skD);
    }

    private static void headBase(int[] p) {
        rect(p, 6, 1, 6, 1, sk);
        rect(p, 5, 2, 8, 8, sk);
        rect(p, 6, 10, 6, 1, sk);
        rect(p, 12, 4, 1, 6, skD);
    }

    private static void headFront(int[] p) {
        if (st == HOOD) { hoodFront(p); return; }
        headBase(p);
        rect(p, 6, 6, 2, 1, hrD);                 // eyebrows
        rect(p, 10, 6, 2, 1, hrD);
        px(p, 7, 7, EYE);
        px(p, 10, 7, EYE);
        px(p, 9, 8, skD);                         // nose
        rect(p, 8, 9, 2, 1, fem ? 0xFFB84A5A : skD);   // mouth

        if (st == SHORT) {
            rect(p, 6, 1, 6, 1, hr);
            rect(p, 5, 2, 8, 2, hr);
            px(p, 5, 4, hr); px(p, 12, 4, hr);
            px(p, 5, 5, hr); px(p, 12, 5, hr);
            rect(p, 7, 1, 2, 1, shade(hr, 125));
        } else if (st == LONG) {
            rect(p, 6, 1, 6, 1, hr);
            rect(p, 5, 2, 8, 2, hr);
            rect(p, 4, 3, 2, 10, hr);
            rect(p, 12, 3, 2, 10, hr);
            rect(p, 4, 3, 1, 10, hrD);
            rect(p, 13, 3, 1, 10, hrD);
            rect(p, 7, 1, 2, 1, shade(hr, 125));
        } else if (st == SPIKY) {
            px(p, 6, 0, hr); px(p, 8, 0, hr); px(p, 10, 0, hr);
            rect(p, 5, 1, 8, 1, hr);
            rect(p, 5, 2, 8, 2, hr);
            px(p, 5, 4, hr); px(p, 12, 4, hr);
        } else if (st == CAP) {
            rect(p, 6, 1, 6, 1, sh);
            rect(p, 5, 2, 8, 2, sh);
            rect(p, 4, 4, 10, 1, shD);            // brim
            rect(p, 5, 2, 8, 1, shL);
            px(p, 5, 5, hr); px(p, 12, 5, hr);
        }
        if (ex == SHADES) {
            rect(p, 6, 7, 2, 2, LENS);
            rect(p, 10, 7, 2, 2, LENS);
            rect(p, 8, 7, 2, 1, LENS);
            px(p, 6, 7, 0xFF6A7488);
            px(p, 10, 7, 0xFF6A7488);
        } else if (ex == MASK) {
            rect(p, 5, 8, 8, 2, MASKC);
            rect(p, 6, 10, 6, 1, MASKC);
            rect(p, 5, 8, 8, 1, shade(MASKC, 130));
        }
    }

    private static void headBack(int[] p) {
        if (st == HOOD) { hoodBack(p); return; }
        headBase(p);
        if (st == SHORT) {
            rect(p, 6, 1, 6, 1, hr);
            rect(p, 5, 2, 8, 6, hr);
            rect(p, 5, 7, 8, 1, hrD);
        } else if (st == LONG) {
            rect(p, 6, 1, 6, 1, hr);
            rect(p, 4, 2, 10, 11, hr);
            rect(p, 4, 12, 10, 1, hrD);
            rect(p, 8, 4, 2, 8, hrD);             // parting
        } else if (st == SPIKY) {
            px(p, 6, 0, hr); px(p, 8, 0, hr); px(p, 10, 0, hr);
            rect(p, 5, 1, 8, 6, hr);
            rect(p, 5, 6, 8, 1, hrD);
        } else if (st == CAP) {
            rect(p, 6, 1, 6, 1, sh);
            rect(p, 5, 2, 8, 3, sh);
            rect(p, 5, 4, 8, 1, shD);
            rect(p, 5, 5, 8, 2, hr);
        }
        if (ex == MASK) {
            rect(p, 5, 8, 8, 1, MASKC);            // knot / strap at the back
        }
    }

    // ------------------------------------------------------------ side (facing right)

    private static void side(int[] p, int f) {
        int sw = hw - 1;
        int tl = 9 - sw, tr = 8 + sw;
        int lw = (bd == HEAVY) ? 4 : 3;
        int x0 = 8;

        // legs: far leg darker, drawn first
        int nearX = x0, farX = x0, nearLift = 0, farLift = 0;
        if (f == 1) { nearX = x0 + 2; farX = x0 - 2; farLift = 1; }
        if (f == 2) { nearX = x0 - 2; farX = x0 + 2; nearLift = 1; }
        sideLeg(p, farX, lw, farLift, pnD);
        sideLeg(p, nearX, lw, nearLift, pn);

        // torso
        rect(p, tl, 12, tr - tl + 1, 7, sh);
        rect(p, tl, 12, 1, 7, shD);
        rect(p, tr, 12, 1, 7, shL);
        rect(p, tl, 19, tr - tl + 1, 1, BELT);
        rect(p, 7, 11, 3, 1, skD);

        // near arm swings opposite to the near leg
        int s = (f == 1) ? -2 : (f == 2 ? 2 : 0);
        rect(p, 8 + s / 2, 12, 2, 3, shL);
        rect(p, 8 + s / 2, 14, 2, 1, shD);
        if (ex == KILLER) {
            rect(p, 8 + s, 15, 2, 2, shL);
            rect(p, 8 + s, 17, 2, 1, GLOVE);
        } else {
            rect(p, 8 + s, 15, 2, 3, sk);
            rect(p, 8 + s, 17, 2, 1, skD);
        }

        headSide(p);
    }

    private static void sideLeg(int[] p, int x, int w, int lift, int c) {
        rect(p, x, 20, w, 4 - lift, c);
        rect(p, x, 24 - lift, w + 1, 2, SHOE);      // toe sticks out forward
        rect(p, x, 24 - lift, w + 1, 1, shade(SHOE, 135));
    }

    private static void headSide(int[] p) {
        if (st == HOOD) { hoodSide(p); return; }
        rect(p, 6, 1, 5, 1, sk);
        rect(p, 5, 2, 7, 8, sk);
        rect(p, 6, 10, 5, 1, sk);
        px(p, 12, 8, sk);                           // nose
        rect(p, 9, 6, 2, 1, hrD);                   // eyebrow
        px(p, 10, 7, EYE);
        px(p, 7, 7, skD); px(p, 7, 8, skD);         // ear
        px(p, 11, 9, fem ? 0xFFB84A5A : skD);       // mouth

        if (st == SHORT) {
            rect(p, 6, 1, 5, 1, hr);
            rect(p, 5, 2, 7, 2, hr);
            rect(p, 5, 4, 3, 3, hr);
            rect(p, 7, 1, 2, 1, shade(hr, 125));
        } else if (st == LONG) {
            rect(p, 6, 1, 5, 1, hr);
            rect(p, 5, 2, 7, 2, hr);
            rect(p, 4, 4, 4, 9, hr);
            rect(p, 4, 4, 1, 9, hrD);
            rect(p, 7, 1, 2, 1, shade(hr, 125));
        } else if (st == SPIKY) {
            px(p, 6, 0, hr); px(p, 8, 0, hr); px(p, 10, 0, hr);
            rect(p, 5, 1, 7, 3, hr);
            rect(p, 5, 4, 2, 3, hr);
        } else if (st == CAP) {
            rect(p, 6, 1, 5, 1, sh);
            rect(p, 5, 2, 7, 2, sh);
            rect(p, 5, 2, 7, 1, shL);
            rect(p, 9, 4, 5, 1, shD);               // brim forward
            rect(p, 5, 4, 3, 3, hr);
        }
        if (ex == SHADES) {
            rect(p, 9, 7, 3, 2, LENS);
            rect(p, 5, 7, 4, 1, LENS);
            px(p, 9, 7, 0xFF6A7488);
        } else if (ex == MASK) {
            rect(p, 8, 8, 4, 2, MASKC);
            rect(p, 7, 10, 4, 1, MASKC);
            rect(p, 5, 8, 3, 2, MASKC);
        }
    }


    // ------------------------------------------------------------ hood (serious look)

    private static boolean covered() { return ex == MASK || ex == KILLER; }

    private static void hoodFront(int[] p) {
        int shadow = shade(sk, 55);
        rect(p, 6, 0, 6, 1, shL);
        rect(p, 5, 1, 8, 1, sh);
        rect(p, 4, 2, 10, 9, sh);
        rect(p, 4, 2, 1, 9, shL);
        rect(p, 13, 2, 1, 9, shD);
        rect(p, 6, 3, 6, 8, shadow);              // face opening, in shadow
        rect(p, 7, 2, 4, 1, shD);                 // hood rim above the face
        int eye = (ex == KILLER) ? RED : EYE;
        px(p, 7, 7, eye);
        px(p, 10, 7, eye);
        if (ex == KILLER) {
            px(p, 6, 7, 0xFF6A1010);              // red glow
            px(p, 11, 7, 0xFF6A1010);
            px(p, 6, 5, 0xFF050303);              // angry brows
            px(p, 7, 6, 0xFF050303);
            px(p, 11, 5, 0xFF050303);
            px(p, 10, 6, 0xFF050303);
        }
        if (covered()) {
            rect(p, 6, 8, 6, 2, MASKC);
            rect(p, 7, 10, 4, 1, MASKC);
            rect(p, 6, 8, 6, 1, shade(MASKC, 135));
        } else {
            rect(p, 8, 9, 2, 1, shade(sk, 40));
        }
        rect(p, 6, 11, 6, 1, shD);                // hood folds around the neck
    }

    private static void hoodBack(int[] p) {
        rect(p, 6, 0, 6, 1, shL);
        rect(p, 5, 1, 8, 1, sh);
        rect(p, 4, 2, 10, 10, sh);
        rect(p, 4, 2, 1, 10, shL);
        rect(p, 13, 2, 1, 10, shD);
        rect(p, 8, 3, 1, 8, shD);                 // seam
        rect(p, 9, 3, 1, 8, shL);
        rect(p, 4, 11, 10, 1, shD);
    }

    private static void hoodSide(int[] p) {
        int shadow = shade(sk, 55);
        rect(p, 6, 0, 5, 1, shL);
        rect(p, 5, 1, 7, 1, sh);
        rect(p, 4, 2, 9, 3, sh);                  // hood and the brim over the eyes
        rect(p, 4, 5, 5, 6, sh);                  // back of the hood
        rect(p, 4, 2, 1, 9, shD);
        rect(p, 5, 1, 7, 1, shL);
        rect(p, 9, 5, 3, 6, shadow);              // face in shadow
        px(p, 12, 8, shadow);                     // nose
        int eye = (ex == KILLER) ? RED : EYE;
        px(p, 10, 7, eye);
        if (ex == KILLER) {
            px(p, 11, 7, 0xFF6A1010);
            rect(p, 10, 5, 2, 1, 0xFF050303);     // angry brow
            px(p, 9, 5, shD);
        }
        if (covered()) {
            rect(p, 9, 8, 4, 2, MASKC);
            rect(p, 8, 10, 4, 1, MASKC);
        }
        rect(p, 6, 11, 5, 1, shD);
    }

    // ------------------------------------------------------------ helpers

    private static void rect(int[] p, int x, int y, int w, int h, int c) {
        for (int j = y; j < y + h; j++) {
            int yy = j + OY;
            if (yy < 0 || yy >= H) continue;
            for (int i = x; i < x + w; i++) {
                int xx = i + OX;
                if (xx >= 0 && xx < W) p[yy * W + xx] = c;
            }
        }
    }

    private static void px(int[] p, int x, int y, int c) {
        rect(p, x, y, 1, 1, c);
    }

    private static void flip(int[] p) {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W / 2; x++) {
                int a = y * W + x, b = y * W + (W - 1 - x);
                int t = p[a];
                p[a] = p[b];
                p[b] = t;
            }
        }
    }

    /** Dark outline around everything that is drawn. */
    private static void outline(int[] p) {
        int[] src = new int[p.length];
        System.arraycopy(p, 0, src, 0, p.length);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if ((src[y * W + x] >>> 24) != 0) continue;
                boolean near = (x > 0 && (src[y * W + x - 1] >>> 24) != 0)
                        || (x < W - 1 && (src[y * W + x + 1] >>> 24) != 0)
                        || (y > 0 && (src[(y - 1) * W + x] >>> 24) != 0)
                        || (y < H - 1 && (src[(y + 1) * W + x] >>> 24) != 0);
                if (near) p[y * W + x] = OUTLINE;
            }
        }
    }

    /** Lighter (pct > 100) or darker (pct < 100) version of a colour. */
    public static int shade(int c, int pct) {
        int r = ((c >> 16) & 0xFF) * pct / 100;
        int g = ((c >> 8) & 0xFF) * pct / 100;
        int b = (c & 0xFF) * pct / 100;
        if (r > 255) r = 255;
        if (g > 255) g = 255;
        if (b > 255) b = 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
