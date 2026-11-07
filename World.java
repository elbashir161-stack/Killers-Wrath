import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Tile map: 64x64 tiles of 16px (1024x1024 px city). Stage 1 test city. */
public class World {
    public static final int TS = 16, MW = 64, MH = 64;

    private static final int GRASS = 0, ROAD_HU = 1, ROAD_HL = 2, ROAD_VL = 3, ROAD_VR = 4,
            ROAD_X = 5, WALK = 6, ROOF = 7, FACADE = 10, DOOR = 13, TREE = 16;
    private static final int[] ROOF_COL = {0xA8442F, 0x4F6B8A, 0x8A6A45};
    private static final int[] ROOF_HI = {0xC0604A, 0x6A88AA, 0xA88860};
    private static final int[] ROOF_DK = {0x7E3222, 0x3A5068, 0x68502F};
    private static final int[] WALL_COL = {0xE2D6B8, 0xC9CED6, 0xD9B48F};

    private final byte[] map = new byte[MW * MH];
    private final Image[] tiles = new Image[17];

    public World() {
        makeTiles();
        generate();
    }

    private void generate() {
        for (int ty = 0; ty < MH; ty++) {
            for (int tx = 0; tx < MW; tx++) {
                int lx = tx % 16, ly = ty % 16, bx = tx / 16, by = ty / 16;
                boolean vRoad = (lx == 7 || lx == 8);
                boolean hRoad = (ly == 7 || ly == 8);
                int t;
                if (vRoad && hRoad) t = ROAD_X;
                else if (hRoad) t = (ly == 7) ? ROAD_HU : ROAD_HL;
                else if (vRoad) t = (lx == 7) ? ROAD_VL : ROAD_VR;
                else if (lx == 6 || lx == 9 || ly == 6 || ly == 9) t = WALK;
                else {
                    boolean bxIn = (lx >= 1 && lx <= 4) || (lx >= 11 && lx <= 14);
                    boolean byIn = (ly >= 1 && ly <= 4) || (ly >= 11 && ly <= 14);
                    if (bxIn && byIn) {
                        int col = (bx * 3 + by * 5 + (lx > 8 ? 1 : 0) + (ly > 8 ? 2 : 0)) % 3;
                        int cidx = (lx <= 4) ? lx - 1 : lx - 11;
                        boolean bottom = (ly == 4 || ly == 14);
                        if (!bottom) t = ROOF + col;
                        else t = (cidx == 1) ? DOOR + col : FACADE + col;
                    } else {
                        t = ((tx * 7 + ty * 13) % 11 == 0) ? TREE : GRASS;
                    }
                }
                if (tx == 0 || ty == 0 || tx == MW - 1 || ty == MH - 1) t = TREE;
                map[ty * MW + tx] = (byte) t;
            }
        }
    }

    /** Solid test in world pixel coordinates. */
    public boolean solidAt(int px, int py) {
        if (px < 0 || py < 0 || px >= MW * TS || py >= MH * TS) return true;
        return map[(py / TS) * MW + (px / TS)] >= ROOF;
    }

    public void draw(Graphics g, int camX, int camY, int sw, int sh) {
        int tx0 = camX / TS, ty0 = camY / TS;
        int tx1 = (camX + sw - 1) / TS, ty1 = (camY + sh - 1) / TS;
        for (int ty = ty0; ty <= ty1; ty++) {
            if (ty < 0 || ty >= MH) continue;
            for (int tx = tx0; tx <= tx1; tx++) {
                if (tx < 0 || tx >= MW) continue;
                g.drawImage(tiles[map[ty * MW + tx]], tx * TS - camX, ty * TS - camY,
                        Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    // ---- tile art ----

    private void makeTiles() {
        tiles[GRASS] = grass();
        tiles[ROAD_HU] = road(0);
        tiles[ROAD_HL] = road(1);
        tiles[ROAD_VL] = road(2);
        tiles[ROAD_VR] = road(3);
        tiles[ROAD_X] = road(4);
        tiles[WALK] = walk();
        for (int c = 0; c < 3; c++) {
            tiles[ROOF + c] = roof(c);
            tiles[FACADE + c] = facade(c, false);
            tiles[DOOR + c] = facade(c, true);
        }
        tiles[TREE] = tree();
    }

    private Image grass() {
        Image im = Image.createImage(TS, TS);
        Graphics g = im.getGraphics();
        g.setColor(0x3E8E3A);
        g.fillRect(0, 0, TS, TS);
        g.setColor(0x4FA04A);
        g.fillRect(2, 3, 1, 1); g.fillRect(9, 6, 1, 1); g.fillRect(13, 12, 1, 1); g.fillRect(5, 11, 1, 1);
        g.setColor(0x2F7A2E);
        g.fillRect(11, 2, 1, 1); g.fillRect(4, 8, 1, 1); g.fillRect(8, 14, 1, 1);
        return im;
    }

    private Image road(int k) {
        Image im = Image.createImage(TS, TS);
        Graphics g = im.getGraphics();
        g.setColor(0x3A3A3F);
        g.fillRect(0, 0, TS, TS);
        g.setColor(0x45454A);
        g.fillRect(3, 4, 1, 1); g.fillRect(11, 9, 1, 1); g.fillRect(7, 13, 1, 1); g.fillRect(13, 2, 1, 1);
        g.setColor(0xE8D44D);
        if (k == 0) g.fillRect(2, TS - 1, 6, 1);
        else if (k == 1) g.fillRect(2, 0, 6, 1);
        else if (k == 2) g.fillRect(TS - 1, 2, 1, 6);
        else if (k == 3) g.fillRect(0, 2, 1, 6);
        return im;
    }

    private Image walk() {
        Image im = Image.createImage(TS, TS);
        Graphics g = im.getGraphics();
        g.setColor(0xB9B5A9);
        g.fillRect(0, 0, TS, TS);
        g.setColor(0xA39F94);
        g.fillRect(TS - 1, 0, 1, TS);
        g.fillRect(0, TS - 1, TS, 1);
        return im;
    }

    private Image roof(int c) {
        Image im = Image.createImage(TS, TS);
        Graphics g = im.getGraphics();
        g.setColor(ROOF_COL[c]);
        g.fillRect(0, 0, TS, TS);
        g.setColor(ROOF_DK[c]);
        for (int y = 3; y < TS; y += 4) g.fillRect(0, y, TS, 1);
        g.setColor(ROOF_HI[c]);
        for (int y = 0; y < TS; y += 4) g.fillRect(0, y, TS, 1);
        return im;
    }

    private Image facade(int c, boolean door) {
        Image im = Image.createImage(TS, TS);
        Graphics g = im.getGraphics();
        g.setColor(WALL_COL[c]);
        g.fillRect(0, 0, TS, TS);
        g.setColor(ROOF_DK[c]);
        g.fillRect(0, 0, TS, 3);
        if (!door) {
            g.setColor(0x4A4A4A);
            g.fillRect(2, 5, 5, 6); g.fillRect(9, 5, 5, 6);
            g.setColor(0x7FB6D9);
            g.fillRect(3, 6, 3, 4); g.fillRect(10, 6, 3, 4);
            g.setColor(0xBFE3F5);
            g.fillRect(3, 6, 1, 1); g.fillRect(10, 6, 1, 1);
            g.setColor(0x9A9484);
            g.fillRect(0, 15, TS, 1);
        } else {
            g.setColor(0x5B3A22);
            g.fillRect(4, 4, 8, 12);
            g.setColor(0x6E4A2E);
            g.fillRect(5, 5, 6, 10);
            g.setColor(0xE0C050);
            g.fillRect(9, 10, 1, 2);
            g.setColor(0x9A9484);
            g.fillRect(3, 15, 10, 1);
        }
        return im;
    }

    private Image tree() {
        Image im = grass();
        Graphics g = im.getGraphics();
        g.setColor(0x2F7A2E);
        g.fillArc(3, 6, 12, 9, 0, 360);
        g.setColor(0x6B4A2B);
        g.fillRect(7, 10, 2, 5);
        g.setColor(0x1F6B2A);
        g.fillArc(1, 0, 14, 12, 0, 360);
        g.setColor(0x2E8B3A);
        g.fillArc(3, 2, 6, 5, 0, 360);
        return im;
    }
}
