import javax.microedition.lcdui.Image;

/** 8 pedestrian looks (4 directions x 3 frames), the same looks at 70% size for kids, and lying-down versions. */
public class NpcSprites {
    public static final int W = SpriteKit.W, H = SpriteKit.H;
    public static final int KW = W * 7 / 10, KH = H * 7 / 10, KFEET = (H - 2) * 7 / 10;
    public static final int LOOKS = 8;
    public static Image[][][] looks; // [look][dir][frame]
    public static Image[][][] kids;  // same, 70% size
    public static Image[][] dead;    // [look][0 head right / 1 head left]
    public static Image[][] deadKid;

    private static final int[] SKIN = {0xFFD9A982, 0xFFB87F58, 0xFF7E5236, 0xFFE4BC98, 0xFFA66E48, 0xFF5E3A28, 0xFFDDB38C, 0xFFC4916A};
    private static final int[] HAIR = {0xFF22160E, 0xFF5A3418, 0xFFB09048, 0xFF161616, 0xFF9A9A98, 0xFF6E2814, 0xFF2E2216, 0xFF1E1E1E};
    private static final int[] SHIRT = {0xFF8E3A30, 0xFF3A5F88, 0xFF4A7050, 0xFFA88A38, 0xFFC4C4B8, 0xFFA8642E, 0xFF6A4A7E, 0xFF2F7A7A};
    private static final int[] PANTS = {0xFF24405E, 0xFF6E6248, 0xFF222228, 0xFF4A4A52, 0xFF30306E, 0xFF54402A, 0xFF1C3250, 0xFF5E3232};
    private static final int[] KID_SHIRT = {0xFFB03030, 0xFF3A8A4A, 0xFFC87A28, 0xFF7A4AA8, 0xFF2F8AB0, 0xFFC8B030, 0xFFB04A80, 0xFF4A9A7A};
    private static final int[] KID_PANTS = {0xFF2A3A5A, 0xFF4A3A2A, 0xFF303038, 0xFF3A4A6A, 0xFF5A4A3A, 0xFF2A4A4A, 0xFF4A2A3A, 0xFF3A3A4A};
    private static final boolean[] FEMALE = {false, true, false, true, false, true, false, true};

    public static void init() {
        if (looks != null) return;
        looks = new Image[LOOKS][4][3];
        kids = new Image[LOOKS][4][3];
        dead = new Image[LOOKS][2];
        deadKid = new Image[LOOKS][2];
        for (int l = 0; l < LOOKS; l++) {
            int style = FEMALE[l] ? 2 : 1;
            for (int d = 0; d < 4; d++) {
                for (int f = 0; f < 3; f++) {
                    int[] px = SpriteKit.build(d, f, SKIN[l], HAIR[l], style, SHIRT[l], PANTS[l], FEMALE[l], false);
                    looks[l][d][f] = Image.createRGBImage(px, W, H, true);
                    int[] kp = scale(SpriteKit.build(d, f, SKIN[l], HAIR[l], style, KID_SHIRT[l], KID_PANTS[l], FEMALE[l], false));
                    kids[l][d][f] = Image.createRGBImage(kp, KW, KH, true);
                    if (d == Sprites.DOWN && f == 0) {
                        makeDead(dead[l], px, W, H);
                        makeDead(deadKid[l], kp, KW, KH);
                    }
                }
            }
        }
    }

    /** Shrinks a W x H picture to KW x KH (70%). */
    private static int[] scale(int[] src) {
        int[] o = new int[KW * KH];
        for (int y = 0; y < KH; y++) {
            int sy = (y * 10 + 5) / 7;
            if (sy >= H) sy = H - 1;
            for (int x = 0; x < KW; x++) {
                int sx = (x * 10 + 5) / 7;
                if (sx >= W) sx = W - 1;
                o[y * KW + x] = src[sy * W + sx];
            }
        }
        return o;
    }

    /** Lie down: rotate 90 degrees each way; the new picture is h wide and w tall. */
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
