import javax.microedition.lcdui.Image;

/**
 * 12 adult kinds (6 men, 6 women), 8 kinds of child, and lying-down versions.
 * Everything is drawn by code (SpriteKit + Outfits), no picture files.
 */
public class NpcSprites {
    public static final int W = SpriteKit.W, H = SpriteKit.H;
    public static final int LOOKS = 12, KIDS = 8;
    public static Image[][][] looks; // [kind][dir][frame]
    public static Image[][][] kids;
    public static Image[][] dead;    // [kind][0 head right / 1 head left]
    public static Image[][] deadKid;

    // skin tones, light to deep brown
    private static final int[] SK = {0xFFE8C4A0, 0xFFD9A982, 0xFFC4916A, 0xFFB87F58, 0xFFA66E48, 0xFF8A5A3A, 0xFF6E4630, 0xFF4A2E20};
    private static final int GREY = 0xFF9A9A98, BLACK = 0xFF161616, BROWN = 0xFF5A3418, BLOND = 0xFFC8A858, RED = 0xFF8A3418;

    // adult kinds: skin, hair colour, base hair (0 bald 1 short 2 long), shirt, trousers, woman?, extras, accent colour
    private static final int[] A_SKIN = {1, 4, 2, 6, 0, 3,   1, 2, 5, 4, 0, 6};
    private static final int[] A_HAIR = {BLACK, BROWN, BLACK, BLACK, GREY, BLACK,   BROWN, BLACK, BLACK, BROWN, GREY, BLACK};
    private static final int[] A_STYLE = {1, 1, 1, 1, 1, 1,   0, 2, 0, 0, 0, 2};
    private static final int[] A_SHIRT = {0xFF2E3440, 0xFFE07A18, 0xFF2A4A8A, 0xFF5A7A7A, 0xFF6A4A32, 0xFF2A5AA8,
            0xFFE8E8E8, 0xFF7A3A5A, 0xFF1E2A44, 0xFFE05A8A, 0xFF7A4A8A, 0xFF3A3A42};
    private static final int[] A_PANTS = {0xFF2E3440, 0xFF30406A, 0xFF1E2E4E, 0xFF3A3A42, 0xFF3A3A3A, 0xFF2A5AA8,
            0xFFE0E0E0, 0xFF7A3A5A, 0xFF1E2A44, 0xFF2A2A32, 0xFF7A4A8A, 0xFF2A3A5A};
    private static final boolean[] A_FEM = {false, false, false, false, false, false, true, true, true, true, true, true};
    private static final int[] A_EX = {
            Outfits.TIE,
            Outfits.HARDHAT | Outfits.VEST,
            Outfits.POLICECAP | Outfits.BADGE,
            Outfits.CAPBACK,
            Outfits.CANE,
            Outfits.HEADBAND | Outfits.STRIPE,
            Outfits.BUN | Outfits.CROSS | Outfits.NURSECAP,
            Outfits.GLASSES,
            Outfits.BUN | Outfits.BADGE,
            Outfits.PONY | Outfits.CROP,
            Outfits.BUN | Outfits.GLASSES,
            Outfits.HAT};
    private static final int[] A_ACC = {0, 0xFFE8C020, 0, 0xFFB03030, 0, 0xFFEDEDED,
            0xFFE0508A, 0, 0, 0xFFE05A8A, 0, 0xFF6A2A2A};

    // child kinds
    private static final int[] K_SKIN = {2, 3, 5, 7, 1, 4, 3, 6};
    private static final int[] K_HAIR = {BLACK, BROWN, BLACK, BLACK, BLOND, BLACK, BROWN, BLACK};
    private static final int[] K_STYLE = {1, 0, 1, 0, 1, 1, 1, 0};
    private static final int[] K_SHIRT = {0xFFD8D8E0, 0xFFD870A0, 0xFF3A6A9A, 0xFFE070A8, 0xFFC03030, 0xFF3A5AC0, 0xFFE8C030, 0xFF3A9A4A};
    private static final int[] K_PANTS = {0xFF2A3A5A, 0xFFD870A0, 0xFF303038, 0xFF3A3A4A, 0xFF2A3A5A, 0xFF4A3A2A, 0xFFE0E0E0, 0xFF3A3A4A};
    private static final boolean[] K_FEM = {false, true, false, true, false, false, false, false};
    private static final int[] K_EX = {Outfits.BACKPACK, Outfits.PIGTAILS, Outfits.CAP, Outfits.BRAIDS, Outfits.BALL,
            Outfits.GLASSES, 0, Outfits.AFRO};
    private static final int[] K_ACC = {0xFFB03030, 0xFFE05A8A, 0xFFE0E0E0, 0xFFE05A8A, 0, 0, 0, 0};
    private static final int[] K_SCALE = {70, 70, 70, 70, 70, 70, 50, 70};     // percent; kind 6 is a toddler

    public static void init() {
        if (looks != null) return;
        looks = new Image[LOOKS][4][3];
        kids = new Image[KIDS][4][3];
        dead = new Image[LOOKS][2];
        deadKid = new Image[KIDS][2];
        for (int k = 0; k < LOOKS; k++) {
            for (int d = 0; d < 4; d++) {
                for (int f = 0; f < 3; f++) {
                    int[] px = SpriteKit.build(d, f, SK[A_SKIN[k]], A_HAIR[k], A_STYLE[k], A_SHIRT[k], A_PANTS[k], A_FEM[k], false);
                    px = Outfits.apply(px, d, A_EX[k], A_HAIR[k], A_ACC[k], SK[A_SKIN[k]]);
                    looks[k][d][f] = Image.createRGBImage(px, W, H, true);
                    if (d == Sprites.DOWN && f == 0) makeDead(dead[k], px, W, H);
                }
            }
        }
        for (int k = 0; k < KIDS; k++) {
            int kw = W * K_SCALE[k] / 100, kh = H * K_SCALE[k] / 100;
            for (int d = 0; d < 4; d++) {
                for (int f = 0; f < 3; f++) {
                    int[] px = SpriteKit.build(d, f, SK[K_SKIN[k]], K_HAIR[k], K_STYLE[k], K_SHIRT[k], K_PANTS[k], K_FEM[k], false);
                    px = Outfits.apply(px, d, K_EX[k], K_HAIR[k], K_ACC[k], SK[K_SKIN[k]]);
                    int[] sp = scale(px, kw, kh);
                    kids[k][d][f] = Image.createRGBImage(sp, kw, kh, true);
                    if (d == Sprites.DOWN && f == 0) makeDead(deadKid[k], sp, kw, kh);
                }
            }
        }
    }

    private static int[] scale(int[] src, int nw, int nh) {
        int[] o = new int[nw * nh];
        for (int y = 0; y < nh; y++) {
            int sy = (y * H + H / 2) / nh;
            if (sy >= H) sy = H - 1;
            for (int x = 0; x < nw; x++) {
                int sx = (x * W + W / 2) / nw;
                if (sx >= W) sx = W - 1;
                o[y * nw + x] = src[sy * W + sx];
            }
        }
        return o;
    }

    private static void makeDead(Image[] out, int[] px, int w, int h) {
        int[] cw = new int[w * h];
        int[] ccw = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = px[y * w + x];
                cw[x * h + (h - 1 - y)] = v;
                ccw[(w - 1 - x) * h + y] = v;
            }
        }
        out[0] = Image.createRGBImage(cw, h, w, true);
        out[1] = Image.createRGBImage(ccw, h, w, true);
    }
}
