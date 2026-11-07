import javax.microedition.lcdui.Image;

/** 8 different pedestrian looks, built with SpriteKit, plus lying-down (dead) versions. */
public class NpcSprites {
    public static final int W = SpriteKit.W, H = SpriteKit.H;
    public static final int LOOKS = 8;
    public static Image[][][] looks; // [look][dir][frame]
    public static Image[][] dead;    // [look][0 head right / 1 head left], 30 x 20

    private static final int[] SKIN = {0xFFE8B890, 0xFFC98E62, 0xFF8A5A3A, 0xFFF1C9A5,
            0xFFB57A52, 0xFF6B4430, 0xFFEBC09A, 0xFFD6A072};
    private static final int[] HAIR = {0xFF2A1A10, 0xFF6B3E1E, 0xFFD8B050, 0xFF1A1A1A,
            0xFFB0B0B0, 0xFF8A2E16, 0xFF3A2A1A, 0xFF222222};
    private static final int[] SHIRT = {0xFFC0392B, 0xFF2E6FB0, 0xFF3A9A4A, 0xFFE0B020,
            0xFFEDEDED, 0xFFE07020, 0xFF8A4AA8, 0xFF1AA0A0};
    private static final int[] PANTS = {0xFF2B4A7A, 0xFF8A7A55, 0xFF2A2A30, 0xFF5A5A62,
            0xFF3A3A8A, 0xFF6A4A2A, 0xFF1F3A5A, 0xFF7A3A3A};
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
