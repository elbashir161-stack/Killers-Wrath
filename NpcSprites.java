import javax.microedition.lcdui.Image;

/** 8 different pedestrian looks, built with SpriteKit, plus lying-down (dead) versions. */
public class NpcSprites {
    public static final int W = SpriteKit.W, H = SpriteKit.H;
    public static final int LOOKS = 8;
    public static Image[][][] looks; // [look][dir][frame]
    public static Image[][] dead;    // [look][0 head right / 1 head left], 30 x 20

    private static final int[] SKIN = {0xFFD9A982, 0xFFB87F58, 0xFF7E5236, 0xFFE4BC98, 0xFFA66E48, 0xFF5E3A28, 0xFFDDB38C, 0xFFC4916A};
    private static final int[] HAIR = {0xFF22160E, 0xFF5A3418, 0xFFB09048, 0xFF161616, 0xFF9A9A98, 0xFF6E2814, 0xFF2E2216, 0xFF1E1E1E};
    private static final int[] SHIRT = {0xFF8E3A30, 0xFF3A5F88, 0xFF4A7050, 0xFFA88A38, 0xFFC4C4B8, 0xFFA8642E, 0xFF6A4A7E, 0xFF2F7A7A};
    private static final int[] PANTS = {0xFF24405E, 0xFF6E6248, 0xFF222228, 0xFF4A4A52, 0xFF30306E, 0xFF54402A, 0xFF1C3250, 0xFF5E3232};
    private static final boolean[] FEMALE = {false, true, false, true, false, true, false, true};

    public static void init() {
        if (looks != null) return;
        looks = new Image[LOOKS][4][3];
        dead = new Image[LOOKS][2];
        for (int l = 0; l < LOOKS; l++) {
            int style = FEMALE[l] ? 2 : 1;
            for (int d = 0; d < 4; d++) {
                for (int f = 0; f < 3; f++) {
                    int[] px = SpriteKit.build(d, f, SKIN[l], HAIR[l], style,
                            SHIRT[l], PANTS[l], FEMALE[l], false);
                    looks[l][d][f] = Image.createRGBImage(px, W, H, true);
                    if (d == Sprites.DOWN && f == 0) {
                        int[] cw = new int[W * H];   // lie down: rotate 90 degrees, new size H x W
                        int[] ccw = new int[W * H];
                        for (int y = 0; y < H; y++) {
                            for (int x = 0; x < W; x++) {
                                int v = px[y * W + x];
                                cw[x * H + (H - 1 - y)] = v;
                                ccw[(W - 1 - x) * H + y] = v;
                            }
                        }
                        dead[l][0] = Image.createRGBImage(cw, H, W, true);
                        dead[l][1] = Image.createRGBImage(ccw, H, W, true);
                    }
                }
            }
        }
    }
}
