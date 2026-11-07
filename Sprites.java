import javax.microedition.lcdui.Image;

/** Builds Killer's pixel-art sprites at startup (16x24, 4 directions x 3 frames). */
public class Sprites {
    public static final int W = 16, H = 24;
    public static final int DOWN = 0, UP = 1, LEFT = 2, RIGHT = 3;
    public static Image[][] killer;

    private static final int SKIN = 0xFF4A2E20, SKIN_DARK = 0xFF3A2218, SKIN_HI = 0xFF6B4630;
    private static final int BROW = 0xFF140A06, EYE = 0xFFE01010, MOUTH = 0xFF24120C;
    private static final int JACKET = 0xFF23272D, JACKET_LT = 0xFF353B44, BELT = 0xFF0A0A0C;
    private static final int PANTS = 0xFF14161A, SHOE = 0xFF08080A;

    public static void init() {
        if (killer != null) return;
        killer = new Image[4][3];
        for (int f = 0; f < 3; f++) {
            killer[DOWN][f] = Image.createRGBImage(frontBack(true, f), W, H, true);
            killer[UP][f] = Image.createRGBImage(frontBack(false, f), W, H, true);
            int[] side = sidePixels(f);
            killer[RIGHT][f] = Image.createRGBImage(side, W, H, true);
            killer[LEFT][f] = Image.createRGBImage(mirror(side), W, H, true);
        }
    }

    private static void rect(int[] p, int x, int y, int w, int h, int c) {
        for (int j = y; j < y + h; j++)
            for (int i = x; i < x + w; i++)
                if (i >= 0 && i < W && j >= 0 && j < H) p[j * W + i] = c;
    }

    private static int[] frontBack(boolean front, int f) {
        int[] p = new int[W * H];
        int ll = (f == 1) ? 5 : 6;
        int rl = (f == 2) ? 5 : 6;
        // legs + shoes
        rect(p, 5, 16, 3, ll, PANTS);
        rect(p, 5, 16 + ll, 3, 2, SHOE);
        rect(p, 8, 16, 3, rl, PANTS);
        rect(p, 8, 16 + rl, 3, 2, SHOE);
        // torso (dark jacket) + belt
        rect(p, 4, 9, 8, 7, JACKET);
        rect(p, 4, 15, 8, 1, BELT);
        if (front) rect(p, 7, 10, 1, 5, JACKET_LT);
        else rect(p, 6, 10, 4, 1, JACKET_LT);
        // arms swing opposite to legs
        int aL = 9 + (f == 2 ? 1 : 0);
        int aR = 9 + (f == 1 ? 1 : 0);
        rect(p, 2, aL, 2, 5, JACKET);
        rect(p, 2, aL + 5, 2, 1, SKIN);
        rect(p, 12, aR, 2, 5, JACKET);
        rect(p, 12, aR + 5, 2, 1, SKIN);
        // bald head, ears, neck
        rect(p, 5, 1, 6, 7, SKIN);
        rect(p, 6, 1, 3, 1, SKIN_HI);
        rect(p, 4, 4, 1, 2, SKIN_DARK);
        rect(p, 11, 4, 1, 2, SKIN_DARK);
        rect(p, 7, 8, 2, 1, SKIN_DARK);
        if (front) {
            rect(p, 6, 3, 2, 1, BROW);
            rect(p, 8, 3, 2, 1, BROW);
            p[4 * W + 6] = EYE;   // red eyes
            p[4 * W + 9] = EYE;
            rect(p, 7, 6, 2, 1, MOUTH);
        } else {
            rect(p, 5, 6, 6, 2, SKIN_DARK);
        }
        return p;
    }

    /** Facing right. */
    private static int[] sidePixels(int f) {
        int[] p = new int[W * H];
        int ax, ah, bx, bh;
        if (f == 0) { ax = 6; ah = 6; bx = 8; bh = 6; }
        else if (f == 1) { ax = 4; ah = 5; bx = 9; bh = 6; }
        else { ax = 9; ah = 5; bx = 4; bh = 6; }
        rect(p, ax, 16, 2, ah, PANTS);
        rect(p, ax, 16 + ah, 3, 2, SHOE);
        rect(p, bx, 16, 2, bh, PANTS);
        rect(p, bx, 16 + bh, 3, 2, SHOE);
        rect(p, 5, 9, 6, 7, JACKET);
        rect(p, 5, 15, 6, 1, BELT);
        // head
        rect(p, 5, 1, 6, 7, SKIN);
        rect(p, 6, 1, 3, 1, SKIN_HI);
        rect(p, 11, 5, 1, 2, SKIN);        // nose
        rect(p, 6, 4, 1, 2, SKIN_DARK);    // ear
        rect(p, 7, 8, 2, 1, SKIN_DARK);
        rect(p, 8, 3, 3, 1, BROW);
        p[4 * W + 9] = EYE;
        rect(p, 9, 6, 2, 1, MOUTH);
        // near arm on top
        int armX = (f == 1) ? 6 : (f == 2 ? 8 : 7);
        rect(p, armX, 9, 2, 5, JACKET_LT);
        rect(p, armX, 14, 2, 1, SKIN);
        return p;
    }

    private static int[] mirror(int[] p) {
        int[] m = new int[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                m[y * W + x] = p[y * W + (W - 1 - x)];
        return m;
    }
}
