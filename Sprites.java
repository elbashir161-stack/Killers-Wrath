import javax.microedition.lcdui.Image;

/** Killer's sprites (4 directions x 3 walk frames). */
public class Sprites {
    public static final int W = SpriteKit.W, H = SpriteKit.H;
    public static final int DOWN = 0, UP = 1, LEFT = 2, RIGHT = 3;
    public static Image[][] killer;

    public static void init() {
        if (killer != null) return;
        killer = new Image[4][3];
        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < 3; f++) {
                int[] px = SpriteKit.build(d, f, 0xFF553322, 0xFF000000, 0,
                        0xFF23272D, 0xFF14161A, false, true);
                killer[d][f] = Image.createRGBImage(px, W, H, true);
            }
        }
    }
}
