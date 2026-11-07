import javax.microedition.lcdui.Image;

/** Pixel-art sprites for pedestrians: 8 different looks, 4 directions, 3 frames. */
public class NpcSprites {
    public static final int W = 16, H = 24;
    public static final int LOOKS = 8;
    public static Image[][][] looks; // [look][dir][frame]

    private static final int[] SKIN = {0xFFE8B890, 0xFFC98E62, 0xFF8A5A3A, 0xFFF1C9A5,
            0xFFB57A52, 0xFF6B4430, 0xFFEBC09A, 0xFFD6A072};
    private static final int[] HAIR = {0xFF2A1A10, 0xFF6B3E1E, 0xFFD8B050, 0xFF1A1A1A,
            0xFFB0B0B0, 0xFF8A2E16, 0xFF3A2A1A, 0xFF222222};
    private static final int[] SHIRT = {0xFFC0392B, 0xFF2E6FB0, 0xFF3A9A4A, 0xFFE0B020,
            0xFFEDEDED, 0xFFE07020, 0xFF8A4AA8, 0xFF1AA0A0};
    private static final int[] PANTS = {0xFF2B4A7A, 0xFF8A7A55, 0xFF2A2A30, 0xFF5A5A62,
            0xFF3A3A8A, 0xFF6A4A2A, 0xFF1F3A5A, 0xFF7A3A3A};
    private static final boolean[] FEMALE = {false, true, false, true, false, true, false, true};
    private static final int SHOE = 0xFF111111, EYE = 0xFF101010;

    public static void init() {
        if (looks != null) return;
        looks = new Image[LOOKS][4][3];
        for (int l = 0; l < LOOKS; l++) {
            for (int f = 0; f < 3; f++) {
                looks[l][Sprites.DOWN][f] = Image.createRGBImage(frontBack(true, f, l), W, H, true);
                looks[l][Sprites.UP][f] = Image.createRGBImage(frontBack(false, f, l), W, H, true);
                int[] side = sidePixels(f, l);
                looks[l][Sprites.RIGHT][f] = Image.createRGBImage(side, W, H, true);
                looks[l][Sprites.LEFT][f] = Image.createRGBImage(mirror(side), W, H, true);
            }
        }
    }

    private static int shade(int c) {
        return 0xFF000000 | ((c >> 1) & 0x007F7F7F);
    }

    private static void rect(int[] p, int x, int y, int w, int h, int c) {
        for (int j = y; j < y + h; j++)
            for (int i = x; i < x + w; i++)
                if (i >= 0 && i < W && j >= 0 && j < H) p[j * W + i] = c;
    }

    private static int[] frontBack(boolean front, int f, int l) {
        int[] p = new int[W * H];
        int skin = SKIN[l], dark = shade(skin), hair = HAIR[l];
        int shirt = SHIRT[l], sleeve = shade(shirt), pants = PANTS[l];
        boolean fem = FEMALE[l];
        int legC = fem ? skin : pants;
        int ll = (f == 1) ? 5 : 6;
        int rl = (f == 2) ? 5 : 6;
        rect(p, 5, 16, 3, ll, legC);
        rect(p, 5, 16 + ll, 3, 2, SHOE);
        rect(p, 8, 16, 3, rl, legC);
        rect(p, 8, 16 + rl, 3, 2, SHOE);
        if (fem) rect(p, 4, 15, 8, 4, pants);
        rect(p, 4, 9, 8, 7, shirt);
        if (!fem) rect(p, 4, 15, 8, 1, 0xFF222222);
        int aL = 9 + (f == 2 ? 1 : 0);
        int aR = 9 + (f == 1 ? 1 : 0);
        rect(p, 2, aL, 2, 5, sleeve);
        rect(p, 2, aL + 5, 2, 1, skin);
        rect(p, 12, aR, 2, 5, sleeve);
        rect(p, 12, aR + 5, 2, 1, skin);
        // head
        rect(p, 5, 1, 6, 7, skin);
        rect(p, 7, 8, 2, 1, dark);
        if (front) {
            rect(p, 5, 1, 6, 2, hair);
            rect(p, 5, 3, 1, 2, hair);
            rect(p, 10, 3, 1, 2, hair);
            p[4 * W + 6] = EYE;
            p[4 * W + 9] = EYE;
            rect(p, 7, 6, 2, 1, dark);
            if (fem) {
                rect(p, 4, 2, 1, 7, hair);
                rect(p, 11, 2, 1, 7, hair);
            } else {
                rect(p, 4, 4, 1, 2, dark);
                rect(p, 11, 4, 1, 2, dark);
            }
        } else {
            rect(p, 5, 1, 6, 6, hair);
            if (fem) rect(p, 4, 2, 8, 7, hair);
        }
        return p;
    }

    /** Facing right. */
    private static int[] sidePixels(int f, int l) {
        int[] p = new int[W * H];
        int skin = SKIN[l], dark = shade(skin), hair = HAIR[l];
        int shirt = SHIRT[l], sleeve = shade(shirt), pants = PANTS[l];
        boolean fem = FEMALE[l];
        int legC = fem ? skin : pants;
        int ax, ah, bx, bh;
        if (f == 0) { ax = 6; ah = 6; bx = 8; bh = 6; }
        else if (f == 1) { ax = 4; ah = 5; bx = 9; bh = 6; }
        else { ax = 9; ah = 5; bx = 4; bh = 6; }
        rect(p, ax, 16, 2, ah, legC);
        rect(p, ax, 16 + ah, 3, 2, SHOE);
        rect(p, bx, 16, 2, bh, legC);
        rect(p, bx, 16 + bh, 3, 2, SHOE);
        if (fem) rect(p, 5, 15, 6, 4, pants);
        rect(p, 5, 9, 6, 7, shirt);
        rect(p, 5, 1, 6, 7, skin);
        rect(p, 11, 5, 1, 2, skin);
        rect(p, 7, 8, 2, 1, dark);
        rect(p, 5, 1, 6, 3, hair);
        rect(p, 5, 3, 3, 4, hair);
        if (fem) rect(p, 4, 2, 4, 9, hair);
        p[4 * W + 9] = EYE;
        rect(p, 9, 6, 2, 1, dark);
        int armX = (f == 1) ? 6 : (f == 2 ? 8 : 7);
        rect(p, armX, 9, 2, 5, sleeve);
        rect(p, armX, 14, 2, 1, skin);
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
