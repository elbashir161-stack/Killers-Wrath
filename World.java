import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * The city: 640 x 360 tiles. One tile = about 1 metre (a person is ~2 tiles tall,
 * a house ~12 tiles wide, a road ~8 tiles wide). Layout follows the reference map.
 */
public class World {
    public static final int TS = 16, MW = 640, MH = 360, MAXDOORS = 2048;

    public static final int GRASS = 0, PATH = 1, SIDEWALK = 2, ROAD = 3, ROAD_VY = 4, ROAD_HY = 5,
            ROAD_VW = 6, ROAD_HW = 7, ZEBRA_V = 8, ZEBRA_H = 9, PARKING = 10, PARK_LINE = 11,
            PLAZA = 12, WATER = 13, FIELD = 14, TRACK = 15, TARMAC = 16, RUNWAY = 17, TREE = 18,
            ISLAND = 19, RUNWAY_MARK = 20;
    private static final int B0 = 24, PARTS = 21, STYLES = 12;
    private static final int P_TL = 0, P_T = 1, P_TR = 2, P_L = 3, P_C = 4, P_R = 5, P_BL = 6, P_B = 7,
            P_BR = 8, P_VENT = 9, P_WIN = 10, P_DLT = 11, P_GLASS = 12, P_WALL = 13, P_DLM = 14,
            P_DLB = 15, P_DRT = 16, P_DRM = 17, P_DRB = 18, P_WINM = 19, P_WALLM = 20;

    private static final int[] ROOF = {0xA8442F, 0x4F6B8A, 0x8A6A45, 0x777E86, 0x4C8A55, 0xD7E3EE,
            0xB8A98A, 0x3C4A5C, 0x9AA1A8, 0xE07A20, 0x3A3A40, 0xC9A227};
    private static final int[] WALL = {0xE9DCC0, 0xF2F2EE, 0xD9B48F, 0xD8D8D2, 0xE8E0B8, 0x7FA9D6,
            0xD8C9A8, 0x4A7FB0, 0x86A9C8, 0xF2E0B8, 0x55505A, 0xEADFC8};

    // roads (centre, width)
    private static final int[] VX = {19, 165, 287, 387, 471, 570, 628};
    private static final int[] VW = {8, 9, 8, 12, 8, 8, 8};
    private static final int[] HY = {27, 93, 165, 237, 310};
    private static final int[] HW = {8, 12, 9, 9, 16};

    private final short[] map = new short[MW * MH];
    private final boolean[] solidT = new boolean[1024];
    private final Image[] tiles = new Image[1024];
    private final Image[] trees = new Image[4];
    public final int[] treeBuf = new int[700];

    public final int[] doorX = new int[MAXDOORS];
    public final int[] doorY = new int[MAXDOORS];
    private final int[] pathCol = new int[MAXDOORS];
    private final int[] pathRow = new int[MAXDOORS];
    public int doorCount = 0;

    // car-park bays (centres in px) and the vehicle / people systems
    public final int[] stallX = new int[8192];
    public final int[] stallY = new int[8192];
    public int stallCount = 0;
    public VehicleManager vehicles;
    public NpcManager npcs;

    private final Image[] signImg = new Image[96];
    private final int[] signX = new int[96];
    private final int[] signY = new int[96];
    private int signCount = 0;

    private int seed = 20261107;
    private int lbx, lby, lbw, lbh; // last landmark building

    public World() {
        makeTiles();
        for (int i = 0; i < 4; i++) trees[i] = TreeArt.tree(i);
        for (int i = 0; i < 1024; i++) solidT[i] = (i >= B0);
        solidT[WATER] = true;
        solidT[TREE] = true;
        solidT[ISLAND] = true;
        for (int i = 0; i < B0; i++) if (i != WATER && i != TREE && i != ISLAND) solidT[i] = false;
        generate();
    }

    // ===================== queries =====================

    public int tileAt(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= MW || ty >= MH) return -1;
        return map[ty * MW + tx];
    }

    public boolean isSidewalk(int tx, int ty) {
        return tileAt(tx, ty) == SIDEWALK;
    }

    /** Solid for people on foot: terrain plus parked vehicles. */
    public boolean solidAt(int px, int py) {
        return solidTerrain(px, py) || (vehicles != null && vehicles.hitParked(px, py));
    }

    public boolean solidTerrain(int px, int py) {
        if (px < 0 || py < 0 || px >= MW * TS || py >= MH * TS) return true;
        return solidT[map[(py / TS) * MW + (px / TS)]];
    }

    // ===================== drawing =====================

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
        for (int i = 0; i < signCount; i++) {
            int sx = signX[i] - camX, sy = signY[i] - camY;
            if (sx > sw || sy > sh || sx + signImg[i].getWidth() < 0 || sy + signImg[i].getHeight() < 0) continue;
            g.drawImage(signImg[i], sx, sy, Graphics.TOP | Graphics.LEFT);
        }
    }

    /** Fills treeBuf with the trees that can be seen; returns how many. Each entry is ty * MW + tx. */
    public int collectTrees(int camX, int camY, int sw, int sh) {
        int n = 0;
        int tx0 = camX / TS - 4, tx1 = (camX + sw) / TS + 4;
        int ty0 = camY / TS - 1, ty1 = (camY + sh) / TS + 8;
        for (int ty = ty0; ty <= ty1; ty++) {
            if (ty < 0 || ty >= MH) continue;
            for (int tx = tx0; tx <= tx1; tx++) {
                if (tx < 0 || tx >= MW) continue;
                if (map[ty * MW + tx] == TREE && n < treeBuf.length) treeBuf[n++] = ty * MW + tx;
            }
        }
        return n;
    }

    public int treeBaseY(int idx) {
        return (idx / MW) * TS + 13;
    }

    public void drawTree(Graphics g, int idx, int camX, int camY) {
        int tx = idx % MW, ty = idx / MW;
        int h = ((tx * 31 + ty * 17) & 0x7FFF) % 20;
        Image im = trees[(h < 7) ? 0 : (h < 12) ? 1 : (h < 17) ? 2 : 3];
        g.drawImage(im, tx * TS + 8 - im.getWidth() / 2 - camX, ty * TS + 13 - im.getHeight() - camY,
                Graphics.TOP | Graphics.LEFT);
    }

    public Image buildMinimap() {
        int w = MW / 8, h = MH / 8;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int t = map[(y * 8 + 4) * MW + x * 8 + 4];
                int c;
                if (t >= B0) c = 0xC8B890;
                else if (t == WATER) c = 0x2060C0;
                else if (t == TREE) c = 0x1F6B2A;
                else if (t == GRASS || t == FIELD || t == ISLAND) c = 0x4FA04A;
                else if (t == ROAD || (t >= ROAD_VY && t <= ZEBRA_H)) c = 0x555560;
                else if (t == PARKING || t == PARK_LINE || t == TARMAC || t == RUNWAY || t == RUNWAY_MARK) c = 0x6A6A74;
                else c = 0xB8B0A0;
                px[y * w + x] = 0xFF000000 | c;
            }
        }
        return Image.createRGBImage(px, w, h, false);
    }

    // ===================== generation =====================

    private int rnd(int n) {
        seed = seed * 1103515245 + 12345;
        return ((seed >>> 16) & 0x7FFF) % n;
    }

    private static int vlo(int i) { return VX[i] - VW[i] / 2; }
    private static int vhi(int i) { return vlo(i) + VW[i] - 1; }
    private static int hlo(int i) { return HY[i] - HW[i] / 2; }
    private static int hhi(int i) { return hlo(i) + HW[i] - 1; }
    private static int vtop(int i) { return i >= 5 ? hlo(1) : 0; }

    private void set(int tx, int ty, int t) {
        if (tx >= 0 && ty >= 0 && tx < MW && ty < MH) map[ty * MW + tx] = (short) t;
    }

    private int get(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= MW || ty >= MH) return -1;
        return map[ty * MW + tx];
    }

    private void fill(int x0, int y0, int x1, int y1, int t) {
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) set(x, y, t);
    }

    private void disc(int cx, int cy, int r, int t) {
        for (int y = -r; y <= r; y++)
            for (int x = -r; x <= r; x++)
                if (x * x + y * y <= r * r) set(cx + x, cy + y, t);
    }

    private static boolean isWater(int tx, int ty) {
        return tx * 76 + ty * 142 < 7600;
    }

    private void generate() {
        fill(0, 0, MW - 1, MH - 1, GRASS);
        for (int ty = 0; ty < 80; ty++)
            for (int tx = 0; tx < 110; tx++)
                if (isWater(tx, ty)) set(tx, ty, WATER);
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 8; c++) block(c, r);
        roads();
        extendPaths();
        addSign("To Highway", 320 * TS, (hhi(4) + 6) * TS, 0x1E7A3A);
    }

    private int bx0(int c) { return c == 0 ? 0 : vhi(c - 1) + 3; }
    private int bx1(int c) { return c == 7 ? MW - 1 : vlo(c) - 3; }
    private int by0(int r) { return r == 0 ? 0 : hhi(r - 1) + 3; }
    private int by1(int r) { return r == 5 ? MH - 1 : hlo(r) - 3; }

    private void block(int c, int r) {
        int x0 = bx0(c), x1 = bx1(c), y0 = by0(r), y1 = by1(r);
        if (r >= 0 && r <= 1 && c >= 6) return;           // belongs to airport / forest on the right
        if (r == 0 && c == 5) { forest(x0, y0, MW - 1, y1); return; }
        if (r == 1 && c == 5) { airport(x0, y0, MW - 2, y1); return; }
        if (r == 5 || c == 0 || c == 7) { forest(x0, y0, x1, y1); return; }
        switch (r * 10 + c) {
            case 1: case 2: case 3: case 4:
                residential(x0, y0, x1, y1); break;
            case 11: residential(x0, y0, x1, y1); break;
            case 12:
                landmark(x0, y0, x1, y1, 64, 24, 5, 3, "HOSPITAL", 12);
                addSign("+", (lbx + lbw / 2) * TS, (lby + lbh / 2) * TS, 0xC02020);
                break;
            case 13: park(x0, y0, x1, y1); break;
            case 14: residential(x0, y0, x1, y1); break;
            case 21: landmark(x0, y0, x1, y1, 100, 24, 8, 3, "MALL", 20); break;
            case 22:
                residential(x0, y0, x1, y0 + 23);
                landmark(x0, y0 + 24, x1, y1, 40, 14, 0, 3, "MARKET", 8);
                break;
            case 23: downtown(x0, y0, x1, y1); break;
            case 24: downtown(x0, y0, x1, y1); break;
            case 25: landmark(x0, y0, x1, y1, 40, 14, 1, 3, "POLICE", 16); break;
            case 26: residential(x0, y0, x1, y1); break;
            case 31: school(x0, y0, x1, y1); break;
            case 32:
                residential(x0, y0, x1, y0 + 23);
                landmark(x0, y0 + 24, x1, y1, 30, 12, 1, 3, "CLOTHES", 8);
                break;
            case 33: {
                int mid = (x0 + x1) / 2;
                landmark(x0, y0, mid - 1, y1, 26, 12, 9, 3, "FOOD", 14);
                landmark(mid, y0, x1, y1, 26, 12, 4, 3, "FRUITS", 14);
                break;
            }
            case 34: landmark(x0, y0, x1, y1, 38, 14, 0, 3, "FIRE STATION", 14); break;
            case 35: landmark(x0, y0, x1, y1, 30, 14, 6, 3, "BANK", 14); break;
            case 36: landmark(x0, y0, x1, y1, 34, 24, 7, 3, "HOTEL", 10); break;
            case 41:
                residential(x0, y0, x0 + 59, y1);
                landmark(x0 + 60, y0, x1, y1, 36, 12, 10, 3, "WEAPONS", 10);
                break;
            case 42:
                residential(x0, y0, x1, y0 + 23);
                landmark(x0, y0 + 24, x1, y1, 22, 10, 0, 3, "SHOP", 8);
                break;
            case 43: residential(x0, y0, x1, y1); break;
            case 44: residential(x0, y0, x1, y1); break;
            case 45:
                residential(x0, y0, x1, y0 + 23);
                landmark(x0, y0 + 24, x1, y1, 22, 14, 11, 3, null, 6);
                break;
            case 46: gas(x0, y0, x1, y1); break;
            default: forest(x0, y0, x1, y1); break;
        }
    }

    // ---------------- block types ----------------

    private void tree(int tx, int ty) {
        if (get(tx, ty) == GRASS) set(tx, ty, TREE);
    }

    private void sprinkle(int x0, int y0, int x1, int y1, int step, int pct) {
        for (int gy = y0; gy <= y1; gy += step)
            for (int gx = x0; gx <= x1; gx += step)
                if (rnd(100) < pct) {
                    int tx = gx + rnd(step), ty = gy + rnd(step);
                    if (tx <= x1 && ty <= y1) tree(tx, ty);
                }
    }

    private void forest(int x0, int y0, int x1, int y1) {
        sprinkle(x0, y0, x1, y1, 6, 60);
    }

    private boolean dry(int x, int y, int w, int h) {
        return get(x, y) != WATER && get(x + w, y) != WATER && get(x, y + h) != WATER
                && get(x + w, y + h) != WATER && get(x + w / 2, y + h / 2) != WATER;
    }

    private void residential(int x0, int y0, int x1, int y1) {
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        int nc = bw / 20; if (nc < 1) nc = 1;
        int nr = bh / 22; if (nr < 1) nr = 1;
        int lw = bw / nc, ld = bh / nr;
        for (int r = 0; r < nr; r++) {
            for (int c = 0; c < nc; c++) {
                int lx = x0 + c * lw, ly = y0 + r * ld;
                int hw = 9 + rnd(5), hh = 7 + rnd(4);
                if (hw > lw - 2) hw = lw - 2;
                if (hh > ld - 11) hh = ld - 11;
                if (hh < 4) hh = 4;
                int hx = lx + (lw - hw) / 2, hy = ly + 2 + rnd(3);
                if (rnd(100) < 85 && dry(hx, hy, hw, hh + 3)) {
                    building(hx, hy, hw, hh, rnd(5), 3, null, true);
                }
                sprinkle(lx, ly, lx + lw - 1, ly + ld - 1, 7, 35);
            }
        }
    }

    private void parking(int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int m = (y - y0) % 12;
                boolean stall = (m < 5 || m >= 7) && (x % 3 == 0);
                set(x, y, stall ? PARK_LINE : PARKING);
            }
        }
        // remember each bay so vehicles can be parked in them
        for (int y = y0; y + 4 <= y1; y++) {
            int m = (y - y0) % 12;
            if (m != 0 && m != 7) continue;
            for (int x = x0; x + 2 <= x1; x++) {
                if (x % 3 != 0 || stallCount >= stallX.length) continue;
                stallX[stallCount] = x * TS + 24;
                stallY[stallCount] = y * TS + 40;
                stallCount++;
            }
        }
    }

    /** One main building near the top of the block, a plaza and car park in front. */
    private void landmark(int x0, int y0, int x1, int y1, int w, int h, int style, int fac,
                          String label, int lotRows) {
        if (fac < 3) fac = 3;
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        if (w > bw - 6) w = bw - 6;
        int avail = bh - 3 - fac - 1 - 6 - lotRows;
        if (h > avail) h = avail;
        if (h < 6) h = 6;
        int bx = x0 + (bw - w) / 2, by = y0 + 3;
        building(bx, by, w, h, style, fac, label, true);
        int ly = by + h + fac;
        fill(x0 + 2, ly, x1 - 2, Math.min(ly + 4, y1), PLAZA);
        if (ly + 6 < y1 - 3) parking(x0 + 4, ly + 6, x1 - 4, y1 - 3);
        sprinkle(x0, y0, bx - 2, y1, 6, 55);
        sprinkle(bx + w + 2, y0, x1, y1, 6, 55);
        lbx = bx; lby = by; lbw = w; lbh = h;
    }

    private void school(int x0, int y0, int x1, int y1) {
        int bw = x1 - x0 + 1;
        int w = 70;
        int bx = x0 + (bw - w) / 2, by = y0 + 3;
        building(bx, by, w, 14, 2, 3, "SCHOOL", true);
        int ly = by + 17;
        fill(x0 + 2, ly, x1 - 2, ly + 4, PLAZA);
        int fx = x0 + 10, fy = ly + 9;
        fill(fx - 3, fy - 3, fx + 53, fy + 29, TRACK);
        fill(fx, fy, fx + 50, fy + 26, FIELD);
        sprinkle(x0, y0, bx - 2, y1, 6, 55);
        sprinkle(bx + w + 2, y0, x1, y1, 6, 45);
        sprinkle(fx + 60, ly + 6, x1, y1, 6, 45);
    }

    private void park(int x0, int y0, int x1, int y1) {
        int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        fill(x0, cy - 1, x1, cy + 1, PATH);
        fill(cx - 1, y0, cx + 1, y1, PATH);
        disc(cx, cy, 9, PLAZA);
        disc(cx, cy, 3, WATER);
        sprinkle(x0, y0, x1, y1, 6, 50);
    }

    private void downtown(int x0, int y0, int x1, int y1) {
        fill(x0, y0, x1, y1, PLAZA);
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        int nc = bw / 30; if (nc < 1) nc = 1;
        int nr = bh / 29; if (nr < 1) nr = 1;
        int cw = bw / nc, ch = bh / nr;
        for (int r = 0; r < nr; r++) {
            for (int c = 0; c < nc; c++) {
                int w = 16 + rnd(10), h = 10 + rnd(8);
                int style = (rnd(3) == 0) ? 3 : 7;
                int bx = x0 + c * cw + (cw - w) / 2, by = y0 + r * ch + 2;
                building(bx, by, w, h, style, 4, null, true);
            }
        }
    }

    private void gas(int x0, int y0, int x1, int y1) {
        fill(x0, y0, x1, y1, PARKING);
        int bw = x1 - x0 + 1;
        building(x0 + (bw - 12) / 2, y0 + 3, 12, 6, 3, 3, "GAS", true);
        building(x0 + (bw - 26) / 2, y0 + 22, 26, 7, 0, 0, null, false);
        sprinkle(x0, y0 + 34, x1, y1, 6, 30);
    }

    private void airport(int x0, int y0, int x1, int y1) {
        fill(x0, y0, x1, y1, TARMAC);
        fill(x0 + 3, y0 + 2, x1 - 3, y0 + 9, RUNWAY);
        fill(x0 + 3, y0 + 5, x1 - 3, y0 + 5, RUNWAY_MARK);
        int bw = x1 - x0 + 1;
        building(x0 + (bw - 80) / 2, y1 - 26, 80, 12, 8, 3, "AIRPORT", true);
        building(x0 + 8, y1 - 22, 7, 7, 3, 5, null, false);
        fill(x0, y1 - 10, x1, y1, PLAZA);
    }

    // ---------------- buildings ----------------

    /**
     * Roof (w x h tiles) plus a south wall of 'fac' rows. A building with a door gets a double door
     * 2 tiles wide and 3 tiles tall (about 2.3 m, taller than a person).
     */
    private void building(int x, int y, int w, int h, int style, int fac, String label, boolean door) {
        if (door && fac < 3) fac = 3;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                boolean t = (j == 0), b = (j == h - 1), l = (i == 0), r = (i == w - 1);
                int part;
                if (t && l) part = P_TL;
                else if (t && r) part = P_TR;
                else if (t) part = P_T;
                else if (b && l) part = P_BL;
                else if (b && r) part = P_BR;
                else if (b) part = P_B;
                else if (l) part = P_L;
                else if (r) part = P_R;
                else part = ((i * 7 + j * 13 + x + y) % 23 == 0) ? P_VENT : P_C;
                set(x + i, y + j, B0 + style * PARTS + part);
            }
        }
        boolean glass = (style == 5 || style == 7 || style == 8);
        int doorCol = x + w / 2 - 1;
        for (int k = 0; k < fac; k++) {
            boolean winRow = (fac >= 4) || k == 1;
            for (int i = 0; i < w; i++) {
                int part;
                if (glass) part = P_GLASS;
                else if (k == 0) part = (winRow && (x + i) % 2 == 0) ? P_WIN : P_WALL;
                else part = (winRow && (x + i) % 2 == 0) ? P_WINM : P_WALLM;
                if (door) {
                    int row = k - (fac - 3);
                    if (row >= 0 && x + i == doorCol) part = (row == 0) ? P_DLT : (row == 1 ? P_DLM : P_DLB);
                    if (row >= 0 && x + i == doorCol + 1) part = (row == 0) ? P_DRT : (row == 1 ? P_DRM : P_DRB);
                }
                set(x + i, y + h + k, B0 + style * PARTS + part);
            }
        }
        if (door && fac > 0 && doorCount < MAXDOORS) {
            int ar = y + h + fac;
            doorX[doorCount] = (doorCol + 1) * TS;       // middle of the double door
            doorY[doorCount] = ar * TS + 8;
            pathCol[doorCount] = doorCol;
            pathRow[doorCount] = ar;
            doorCount++;
        }
        if (label != null && fac > 0) addSign(label, (x + w / 2) * TS + 8, (y + h) * TS - 6, 0);
    }

    /** Lay a 2-tile-wide paved walkway from every door down to the pavement or road. */
    private void extendPaths() {
        for (int i = 0; i < doorCount; i++) {
            int col = pathCol[i], r0 = pathRow[i];
            for (int dy = 0; dy < 80; dy++) {
                int y = r0 + dy;
                boolean stop = false;
                for (int c = col; c <= col + 1; c++) {
                    int t = get(c, y);
                    if (t == SIDEWALK || (t >= ROAD && t <= ZEBRA_H) || t == -1) stop = true;
                    else if (t == GRASS || t == TREE || t == PATH) set(c, y, PATH);
                    else stop = true;
                }
                if (stop) break;
            }
        }
    }

    private void addSign(String text, int cx, int cy, int bg) {
        if (signCount >= signImg.length) return;
        if (bg == 0) bg = signBg(text);
        Font f = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        int w = f.stringWidth(text) + 8, h = f.getHeight() + 4;
        Image im = Image.createImage(w, h);
        Graphics g = im.getGraphics();
        g.setColor(bg);
        g.fillRect(0, 0, w, h);
        g.setColor(0xFFFFFF);
        g.drawRect(0, 0, w - 1, h - 1);
        g.setFont(f);
        g.drawString(text, 4, 2, Graphics.TOP | Graphics.LEFT);
        signImg[signCount] = im;
        signX[signCount] = cx - w / 2;
        signY[signCount] = cy - h / 2;
        signCount++;
    }

    private static int signBg(String t) {
        if (t.equals("FIRE STATION") || t.equals("MARKET") || t.equals("SHOP") || t.equals("GAS")) return 0xB01818;
        if (t.equals("FOOD")) return 0xE08010;
        if (t.equals("FRUITS")) return 0x2E8B2E;
        if (t.equals("WEAPONS")) return 0x111111;
        return 0x1B2A6B;
    }

    // ---------------- roads ----------------

    private void roads() {
        int bottom = hlo(4) - 1;
        // sidewalks
        for (int i = 0; i < 5; i++) {
            fill(0, hlo(i) - 2, MW - 1, hlo(i) - 1, SIDEWALK);
            fill(0, hhi(i) + 1, MW - 1, hhi(i) + 2, SIDEWALK);
        }
        for (int i = 0; i < 7; i++) {
            fill(vlo(i) - 2, vtop(i), vlo(i) - 1, bottom, SIDEWALK);
            fill(vhi(i) + 1, vtop(i), vhi(i) + 2, bottom, SIDEWALK);
        }
        // asphalt
        for (int i = 0; i < 5; i++) fill(0, hlo(i), MW - 1, hhi(i), ROAD);
        for (int i = 0; i < 7; i++) fill(vlo(i), vtop(i), vhi(i), bottom, ROAD);
        // lane markings
        for (int i = 0; i < 7; i++) {
            int cx = vlo(i) + VW[i] / 2;
            for (int ty = vtop(i); ty <= bottom; ty++) {
                boolean cross = false;
                for (int k = 0; k < 4; k++) if (ty >= hlo(k) - 3 && ty <= hhi(k) + 3) cross = true;
                if (cross) continue;
                if (ty % 6 < 4) set(cx, ty, ROAD_VY);
                if (VW[i] >= 12 && ty % 6 < 3) {
                    set(vlo(i) + VW[i] / 4, ty, ROAD_VW);
                    set(vhi(i) - VW[i] / 4, ty, ROAD_VW);
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            int cy = hlo(i) + HW[i] / 2;
            for (int tx = 0; tx < MW; tx++) {
                boolean cross = false;
                for (int k = 0; k < 7; k++)
                    if (tx >= vlo(k) - 3 && tx <= vhi(k) + 3 && cy >= vtop(k) && cy <= bottom) cross = true;
                if (cross) continue;
                if (i == 4) {
                    set(tx, cy - 1, ROAD_HY);
                    set(tx, cy, ROAD_HY);
                    if (tx % 6 < 3) {
                        set(tx, hlo(i) + 4, ROAD_HW);
                        set(tx, hlo(i) + 12, ROAD_HW);
                    }
                } else {
                    if (tx % 6 < 4) set(tx, cy, ROAD_HY);
                    if (HW[i] >= 12 && tx % 6 < 3) {
                        set(tx, hlo(i) + HW[i] / 4, ROAD_HW);
                        set(tx, hhi(i) - HW[i] / 4, ROAD_HW);
                    }
                }
            }
        }
        // zebra crossings at every junction (not on the highway)
        for (int v = 0; v < 7; v++) {
            for (int h = 0; h < 4; h++) {
                if (HY[h] < vtop(v)) continue;
                int x0 = vlo(v), x1 = vhi(v), y0 = hlo(h), y1 = hhi(h);
                for (int tx = x0; tx <= x1; tx++) {
                    set(tx, y0 - 2, ZEBRA_V); set(tx, y0 - 1, ZEBRA_V);
                    set(tx, y1 + 1, ZEBRA_V); set(tx, y1 + 2, ZEBRA_V);
                }
                for (int ty = y0; ty <= y1; ty++) {
                    set(x0 - 2, ty, ZEBRA_H); set(x0 - 1, ty, ZEBRA_H);
                    set(x1 + 1, ty, ZEBRA_H); set(x1 + 2, ty, ZEBRA_H);
                }
            }
        }
    }

    // ===================== tile art =====================

    private static void col(Graphics g, int c) { g.setColor(c & 0xFFFFFF); }

    private Image tile() { return Image.createImage(TS, TS); }

    private void makeTiles() {
        tiles[GRASS] = grass();
        tiles[PATH] = paved(0xD2CBB9, 0xBDB6A3);
        tiles[SIDEWALK] = paved(0xBDB8AC, 0xA9A498);
        tiles[ROAD] = road(0);
        tiles[ROAD_VY] = road(1);
        tiles[ROAD_HY] = road(2);
        tiles[ROAD_VW] = road(3);
        tiles[ROAD_HW] = road(4);
        tiles[ZEBRA_V] = road(5);
        tiles[ZEBRA_H] = road(6);
        tiles[PARKING] = flat(0x4A4A50, 0x55555C);
        tiles[PARK_LINE] = parkLine();
        tiles[PLAZA] = plaza();
        tiles[WATER] = water();
        tiles[FIELD] = field();
        tiles[TRACK] = flat(0xB5533A, 0xA64A33);
        tiles[TARMAC] = flat(0x55555C, 0x4C4C53);
        tiles[RUNWAY] = flat(0x2E2E33, 0x35353A);
        tiles[RUNWAY_MARK] = runwayMark();
        tiles[TREE] = grass();      // the tree itself is a big sprite, see drawTree
        tiles[ISLAND] = grass();
        for (int s = 0; s < STYLES; s++) {
            for (int p = 0; p < PARTS; p++) {
                if (p <= P_BR || p == P_VENT) tiles[B0 + s * PARTS + p] = roof(s, p);
                else tiles[B0 + s * PARTS + p] = facade(s, p);
            }
        }
        for (int i = 0; i < 1024; i++) if (tiles[i] == null) tiles[i] = tiles[GRASS];
    }

    private Image grass() {
        Image im = tile();
        Graphics g = im.getGraphics();
        col(g, 0x3E8E3A); g.fillRect(0, 0, TS, TS);
        col(g, 0x4FA04A);
        g.fillRect(2, 3, 1, 1); g.fillRect(9, 6, 1, 1); g.fillRect(13, 12, 1, 1); g.fillRect(5, 11, 1, 1);
        col(g, 0x2F7A2E);
        g.fillRect(11, 2, 1, 1); g.fillRect(4, 8, 1, 1); g.fillRect(8, 14, 1, 1);
        return im;
    }

    private Image flat(int a, int b) {
        Image im = tile();
        Graphics g = im.getGraphics();
        col(g, a); g.fillRect(0, 0, TS, TS);
        col(g, b);
        g.fillRect(3, 4, 1, 1); g.fillRect(11, 9, 1, 1); g.fillRect(7, 13, 1, 1); g.fillRect(13, 2, 1, 1);
        return im;
    }

    private Image paved(int a, int b) {
        Image im = tile();
        Graphics g = im.getGraphics();
        col(g, a); g.fillRect(0, 0, TS, TS);
        col(g, b);
        g.fillRect(TS - 1, 0, 1, TS); g.fillRect(0, TS - 1, TS, 1);
        return im;
    }

    private Image road(int k) {
        Image im = flat(0x3A3A3F, 0x45454A);
        Graphics g = im.getGraphics();
        if (k == 1) { col(g, 0xE8C830); g.fillRect(7, 0, 2, TS); }
        else if (k == 2) { col(g, 0xE8C830); g.fillRect(0, 7, TS, 2); }
        else if (k == 3) { col(g, 0xDADADA); g.fillRect(7, 0, 1, TS); }
        else if (k == 4) { col(g, 0xDADADA); g.fillRect(0, 7, TS, 1); }
        else if (k == 5) { col(g, 0xE6E6E6); g.fillRect(1, 0, 5, TS); g.fillRect(9, 0, 5, TS); }
        else if (k == 6) { col(g, 0xE6E6E6); g.fillRect(0, 1, TS, 5); g.fillRect(0, 9, TS, 5); }
        return im;
    }

    private Image parkLine() {
        Image im = flat(0x4A4A50, 0x55555C);
        Graphics g = im.getGraphics();
        col(g, 0xCFCFCF); g.fillRect(0, 0, 1, TS);
        return im;
    }

    private Image plaza() {
        Image im = tile();
        Graphics g = im.getGraphics();
        col(g, 0xCFC8B8); g.fillRect(0, 0, TS, TS);
        col(g, 0xBDB6A6); g.fillRect(0, 7, TS, 1); g.fillRect(7, 0, 1, TS);
        return im;
    }

    private Image water() {
        Image im = tile();
        Graphics g = im.getGraphics();
        col(g, 0x2468B8); g.fillRect(0, 0, TS, TS);
        col(g, 0x3A82D0); g.fillRect(2, 4, 5, 1); g.fillRect(9, 10, 5, 1);
        return im;
    }

    private Image field() {
        Image im = tile();
        Graphics g = im.getGraphics();
        col(g, 0x2F8A38); g.fillRect(0, 0, TS, TS);
        col(g, 0x37963F); g.fillRect(0, 0, TS, 8);
        return im;
    }

    private Image runwayMark() {
        Image im = flat(0x2E2E33, 0x35353A);
        Graphics g = im.getGraphics();
        col(g, 0xE6E6E6); g.fillRect(3, 7, 9, 2);
        return im;
    }

    private Image roof(int style, int part) {
        Image im = tile();
        Graphics g = im.getGraphics();
        int base = ROOF[style];
        col(g, base); g.fillRect(0, 0, TS, TS);
        col(g, SpriteKit.adj(base, 86));
        for (int y = 3; y < TS; y += 4) g.fillRect(0, y, TS, 1);
        col(g, SpriteKit.adj(base, 116));
        for (int y = 0; y < TS; y += 4) g.fillRect(0, y, TS, 1);
        boolean top = part <= P_TR;
        boolean bot = part >= P_BL && part <= P_BR;
        boolean left = part == P_TL || part == P_L || part == P_BL;
        boolean right = part == P_TR || part == P_R || part == P_BR;
        col(g, SpriteKit.adj(base, 58));
        if (top) g.fillRect(0, 0, TS, 2);
        if (left) g.fillRect(0, 0, 2, TS);
        if (right) g.fillRect(TS - 2, 0, 2, TS);
        if (bot) g.fillRect(0, TS - 3, TS, 3);
        if (part == P_VENT) {
            col(g, 0x9A9AA0); g.fillRect(4, 4, 8, 8);
            col(g, 0x6A6A70); g.drawRect(4, 4, 7, 7);
            g.fillRect(6, 6, 4, 4);
        }
        return im;
    }

    private Image facade(int style, int part) {
        Image im = tile();
        Graphics g = im.getGraphics();
        int wall = WALL[style];
        col(g, wall); g.fillRect(0, 0, TS, TS);
        boolean eave = (part == P_WIN || part == P_WALL || part == P_GLASS || part == P_DLT || part == P_DRT);
        if (part == P_GLASS) {
            col(g, SpriteKit.adj(wall, 82));
            g.fillRect(0, 7, TS, 1); g.fillRect(7, 0, 1, TS);
            col(g, SpriteKit.adj(wall, 130));
            g.fillRect(2, 2, 3, 1); g.fillRect(10, 9, 3, 1);
        }
        if (eave) {
            col(g, SpriteKit.adj(ROOF[style], 70));
            g.fillRect(0, 0, TS, 2);
        }
        if (part == P_WIN || part == P_WINM) {
            col(g, 0x3C3C44); g.fillRect(4, 4, 8, 8);
            col(g, 0x8FC4E8); g.fillRect(5, 5, 6, 6);
            col(g, 0xD6EEFA); g.fillRect(5, 5, 2, 1);
            col(g, 0x3C3C44); g.fillRect(8, 5, 1, 6);
        } else if (part == P_WALLM || part == P_WALL) {
            col(g, SpriteKit.adj(wall, 92)); g.fillRect(0, 15, TS, 1);
        } else if (part >= P_DLT && part != P_GLASS && part != P_WALL && part != P_WINM && part != P_WALLM) {
            doorArt(g, part);
        }
        return im;
    }

    /** Double door made of 6 tiles (left/right x top/middle/bottom): 2 tiles wide, about 38 px tall. */
    private void doorArt(Graphics g, int part) {
        boolean left = (part == P_DLT || part == P_DLM || part == P_DLB);
        boolean top = (part == P_DLT || part == P_DRT);
        boolean bot = (part == P_DLB || part == P_DRB);
        int x0 = left ? 3 : 0, x1 = left ? 15 : 12;
        int y0 = top ? 8 : 0, y1 = bot ? 13 : 15;
        col(g, 0x2E1D10);
        g.fillRect(x0, y0, x1 - x0 + 1, y1 - y0 + 1);                 // frame
        int lx0 = left ? x0 + 1 : x0, lx1 = left ? x1 : x1 - 1;
        int ly0 = top ? y0 + 1 : y0;
        col(g, 0x5B3A22);
        g.fillRect(lx0, ly0, lx1 - lx0 + 1, y1 - ly0 + 1);            // leaf
        col(g, 0x6E4A2E);
        g.fillRect(lx0 + 2, top ? y0 + 3 : y0 + 2, lx1 - lx0 - 3, y1 - (top ? y0 + 3 : y0 + 2) - 1); // panel
        col(g, 0x2E1D10);
        if (left) g.fillRect(15, ly0, 1, y1 - ly0 + 1); else g.fillRect(0, ly0, 1, y1 - ly0 + 1);   // gap between leaves
        if (!top && !bot) {
            col(g, 0xE0C050);
            if (left) g.fillRect(12, 6, 2, 4); else g.fillRect(2, 6, 2, 4);                        // handles
        }
        if (bot) {
            col(g, 0x9A9484);
            g.fillRect(0, 14, TS, 2);                                                               // step
        }
    }
}
