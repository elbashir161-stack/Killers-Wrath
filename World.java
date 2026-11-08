import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * The city: 640 x 360 tiles. One tile = about 1 metre (a person is ~2 tiles tall,
 * a house ~12 tiles wide, a road ~8 tiles wide). Dark, grimy look: brick, concrete, rust, worn asphalt.
 * Every building gets its own material, roof details, windows, awnings, and sometimes an annex.
 */
public class World {
    public static final int TS = 16, MW = 640, MH = 360, MAXDOORS = 2048;

    public static final int GRASS = 0, PATH = 1, SIDEWALK = 2, ROAD = 3, ROAD_VY = 4, ROAD_HY = 5,
            ROAD_VW = 6, ROAD_HW = 7, ZEBRA_V = 8, ZEBRA_H = 9, PARKING = 10, PARK_LINE = 11,
            PLAZA = 12, WATER = 13, FIELD = 14, TRACK = 15, TARMAC = 16, RUNWAY = 17, TREE = 18,
            ISLAND = 19, RUNWAY_MARK = 20, FENCE_H = 21, FENCE_V = 22, DUMP_L = 23, DUMP_R = 24,
            BARREL = 25, CRATE = 26, CONE = 27, LAMP = 28, MANHOLE = 29, TRASH = 30, BIN = 31,
            PUDDLE = 32, ROAD_B = 33, ROAD_C = 34, GRASS2 = 35, GRASS3 = 36, SIDEWALK2 = 37;
    private static final int B0 = 48, STYLES = 16;
    // building tile parts (each style has PARTS tiles)
    private static final int P_TL = 0, P_T = 1, P_TR = 2, P_L = 3, P_C = 4, P_R = 5, P_BL = 6, P_B = 7, P_BR = 8,
            P_VENT = 9, P_AC = 10, P_SKY = 11, P_TANK = 12, P_CHIM = 13,
            P_WINM = 14, P_WINB = 15, P_WINX = 16, P_WALL = 17, P_WALLM = 18, P_GLASS = 19, P_AWN = 20,
            P_DLT = 21, P_DLM = 22, P_DLB = 23, P_DRT = 24, P_DRM = 25, P_DRB = 26, PARTS = 27;

    // style = material set: roof texture (0 gravel, 1 corrugated, 2 shingle, 3 tar, 4 clay), wall texture
    // (0 brick, 1 concrete, 2 stucco, 3 siding, 4 metal, 5 glass), accent colour (doors, awnings)
    private static final int[] ROOF_T = {2, 2, 2, 0, 1, 0, 3, 3, 0, 3, 3, 4, 3, 3, 3, 3};
    private static final int[] ROOF_C = {0x4A3A30, 0x3A3F44, 0x3E4A40, 0x4A4C4C, 0x6A4A32, 0x8C9490, 0x3C3F42, 0x2E3436,
            0x55585A, 0x4A3A2A, 0x2A2C2E, 0x8A6A2A, 0x3A4A3A, 0x30404A, 0x3A2E2E, 0x4A2A2A};
    private static final int[] WALL_T = {0, 2, 3, 1, 4, 1, 0, 5, 5, 2, 0, 2, 3, 2, 0, 0};
    private static final int[] WALL_C = {0x6B4036, 0x8A877C, 0x7A7F6A, 0x77786F, 0x5E625C, 0xA9ADA2, 0x8A6E5A, 0x2F4A4E,
            0x46666E, 0x9A8A5A, 0x3E3236, 0x9A927A, 0x4A6A52, 0x5A6E7A, 0x7A3E32, 0x8A3A30};
    private static final int[] ACC_C = {0x2F4A3A, 0x6B2A24, 0x5A3A22, 0x2A3A4A, 0x8A6A20, 0x3C6A5A, 0x24384A, 0x1F2A2C,
            0x3A5A6A, 0x7A2A20, 0x1A1A1A, 0x2A5A4A, 0x2A4A32, 0x24384A, 0x2A2A2A, 0xCFCFC0};
    private static final int[] HOUSE_STYLES = {0, 1, 2, 3, 13, 14, 12};

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
    private int sd = 5;
    private int lbx, lby, lbw, lbh; // last landmark building

    public World() {
        makeTiles();
        for (int i = 0; i < 4; i++) trees[i] = TreeArt.tree(i);
        for (int i = 0; i < 1024; i++) solidT[i] = (i >= B0);
        int[] hard = {WATER, TREE, ISLAND, FENCE_H, FENCE_V, DUMP_L, DUMP_R, BARREL, CRATE, LAMP, BIN};
        for (int i = 0; i < hard.length; i++) solidT[hard[i]] = true;
        generate();
    }

    // ===================== queries =====================

    public int tileAt(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= MW || ty >= MH) return -1;
        return map[ty * MW + tx];
    }

    public boolean isSidewalk(int tx, int ty) {
        int t = tileAt(tx, ty);
        return t == SIDEWALK || t == SIDEWALK2;
    }

    /** Solid for people on foot: terrain plus parked vehicles. */
    public boolean solidAt(int px, int py) {
        return solidTerrain(px, py) || (vehicles != null && vehicles.hitParked(px, py));
    }

    public boolean solidTerrain(int px, int py) {
        if (px < 0 || py < 0 || px >= MW * TS || py >= MH * TS) return true;
        return solidT[map[(py / TS) * MW + (px / TS)]];
    }

    /** Hard ground (road, pavement, car park...): tyre tracks show here. */
    public boolean isHardGround(int px, int py) {
        int t = tileAt(px / TS, py / TS);
        return t == SIDEWALK || t == SIDEWALK2 || t == PATH || (t >= ROAD && t <= PLAZA && t != 12 + 100)
                || t == TARMAC || t == RUNWAY || t == RUNWAY_MARK || t == MANHOLE || t == PUDDLE
                || t == TRASH || t == ROAD_B || t == ROAD_C;
    }

    /** Name of the part of the city at this pixel position (shown on screen and in save slots). */
    public String districtName(int px, int py) {
        int tx = px / TS, ty = py / TS;
        if (get(tx, ty) == WATER) return "Riverside";
        int c = 0;
        for (int i = 0; i < 7; i++) if (tx > VX[i]) c++;
        int r = 0;
        for (int i = 0; i < 5; i++) if (ty > HY[i]) r++;
        if (r == 5) return "Highway";
        if (r == 1 && c >= 5) return "Airport";
        if (r == 0 && c >= 5) return "North Woods";
        if (c == 0 || c == 7) return "Outskirts";
        switch (r * 10 + c) {
            case 12: return "Hospital";
            case 13: return "City Park";
            case 21: return "Mall";
            case 22: return "Market";
            case 23: case 24: return "Downtown";
            case 25: return "Police HQ";
            case 31: return "School";
            case 32: return "Clothes Row";
            case 33: return "Food Street";
            case 34: return "Fire Station";
            case 35: return "Bank";
            case 36: return "Hotel";
            case 41: return "Weapons Alley";
            case 42: return "Corner Shop";
            case 45: return "Gold Dome";
            case 46: return "Gas Station";
            default: return "Residential";
        }
    }

    /** Nearest standing place to (px,py) that is not inside a wall, tree or water. */
    public int[] safeSpot(int px, int py) {
        for (int r = 0; r < 40; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if ((dx < 0 ? -dx : dx) != r && (dy < 0 ? -dy : dy) != r) continue;
                    int x = px + dx * 8, y = py + dy * 8;
                    if (!solidTerrain(x - 4, y - 5) && !solidTerrain(x + 3, y - 5)
                            && !solidTerrain(x - 4, y - 1) && !solidTerrain(x + 3, y - 1)) return new int[] {x, y};
                }
            }
        }
        return new int[] {380 * TS + 8, 120 * TS + 8};
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
                if (t >= B0) c = 0x8A7A62;
                else if (t == WATER) c = 0x1F4A58;
                else if (t == TREE) c = 0x1C3A22;
                else if (t == GRASS || t == GRASS2 || t == GRASS3 || t == FIELD || t == ISLAND) c = 0x3A4A2C;
                else if (isRoadTile(t)) c = 0x44464A;
                else if (t == PARKING || t == PARK_LINE || t == TARMAC || t == RUNWAY || t == RUNWAY_MARK) c = 0x56585C;
                else c = 0x8A8676;
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

    private static boolean isRoadTile(int t) {
        return (t >= ROAD && t <= ZEBRA_H) || t == ROAD_B || t == ROAD_C || t == MANHOLE || t == PUDDLE || t == CONE;
    }

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
        litter();
        weather();
        addSign("To Highway", 320 * TS, (hhi(4) + 6) * TS, 0x1E5A3A);
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
                addSign("+", (lbx + lbw / 2) * TS, (lby + lbh / 2) * TS, 0xA02020);
                break;
            case 13: park(x0, y0, x1, y1); break;
            case 14: residential(x0, y0, x1, y1); break;
            case 21: landmark(x0, y0, x1, y1, 100, 24, 8, 3, "MALL", 20); break;
            case 22:
                residential(x0, y0, x1, y0 + 23);
                landmark(x0, y0 + 24, x1, y1, 40, 14, 4, 3, "MARKET", 8);
                break;
            case 23: downtown(x0, y0, x1, y1); break;
            case 24: downtown(x0, y0, x1, y1); break;
            case 25: landmark(x0, y0, x1, y1, 40, 14, 13, 3, "POLICE", 16); break;
            case 26: residential(x0, y0, x1, y1); break;
            case 31: school(x0, y0, x1, y1); break;
            case 32:
                residential(x0, y0, x1, y0 + 23);
                landmark(x0, y0 + 24, x1, y1, 30, 12, 1, 3, "CLOTHES", 8);
                break;
            case 33: {
                int mid = (x0 + x1) / 2;
                landmark(x0, y0, mid - 1, y1, 26, 12, 9, 3, "FOOD", 14);
                landmark(mid, y0, x1, y1, 26, 12, 12, 3, "FRUITS", 14);
                break;
            }
            case 34: landmark(x0, y0, x1, y1, 38, 14, 15, 3, "FIRE STATION", 14); break;
            case 35: landmark(x0, y0, x1, y1, 30, 14, 3, 3, "BANK", 14); break;
            case 36: landmark(x0, y0, x1, y1, 34, 24, 14, 3, "HOTEL", 10); break;
            case 41:
                residential(x0, y0, x0 + 59, y1);
                landmark(x0 + 60, y0, x1, y1, 36, 12, 10, 3, "WEAPONS", 10);
                break;
            case 42:
                residential(x0, y0, x1, y0 + 23);
                landmark(x0, y0 + 24, x1, y1, 22, 10, 2, 3, "SHOP", 8);
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

    private boolean freeGround(int x, int y) {
        int t = get(x, y);
        return t == GRASS || t == PARKING || t == PLAZA || t == PATH;
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
                    int style = HOUSE_STYLES[rnd(HOUSE_STYLES.length)];
                    if (rnd(100) < 45) {                         // an annex makes an L-shaped house
                        int ww = 5 + rnd(3), wh = hh - 2;
                        boolean left = rnd(2) == 0;
                        int wx = left ? hx - ww : hx + hw;
                        if (wh >= 3 && wx >= lx + 1 && wx + ww <= lx + lw - 1 && dry(wx, hy + 2, ww, wh + 3))
                            building(wx, hy + 2, ww, wh, style, 3, null, false);
                    }
                    building(hx, hy, hw, hh, style, 3, null, true);
                    if (rnd(100) < 60) fenceLot(lx, ly, lw, ld);
                }
                sprinkle(lx, ly, lx + lw - 1, ly + ld - 1, 7, 35);
            }
        }
    }

    /** Wooden fence along the back and one side of a lot; the front stays open. */
    private void fenceLot(int lx, int ly, int lw, int ld) {
        for (int x = lx; x < lx + lw; x++) if (get(x, ly) == GRASS) set(x, ly, FENCE_H);
        for (int y = ly; y < ly + ld - 8; y++) if (get(lx, y) == GRASS) set(lx, y, FENCE_V);
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
        if (get(x0 + 3, ly + 1) == PLAZA) set(x0 + 3, ly + 1, BIN);
        if (get(x1 - 3, ly + 1) == PLAZA) set(x1 - 3, ly + 1, BIN);
        junk(bx, by, w, h, fac);
        sprinkle(x0, y0, bx - 2, y1, 6, 55);
        sprinkle(bx + w + 2, y0, x1, y1, 6, 55);
        lbx = bx; lby = by; lbw = w; lbh = h;
    }

    /** Dumpsters, barrels and crates beside a shop. */
    private void junk(int bx, int by, int bw, int bh, int fac) {
        int yy = by + bh + fac - 2;
        if (rnd(100) < 75) {
            int xx = (rnd(2) == 0) ? bx - 5 : bx + bw + 3;
            if (freeGround(xx, yy) && freeGround(xx + 1, yy)) {
                set(xx, yy, DUMP_L);
                set(xx + 1, yy, DUMP_R);
            }
        }
        for (int k = 0; k < 3; k++) {
            if (rnd(100) < 55) {
                int xx = (rnd(2) == 0) ? bx - 3 - rnd(5) : bx + bw + 1 + rnd(5);
                int y2 = yy - 1 + rnd(4);
                if (freeGround(xx, y2)) set(xx, y2, rnd(2) == 0 ? BARREL : CRATE);
            }
        }
    }

    private void school(int x0, int y0, int x1, int y1) {
        int bw = x1 - x0 + 1;
        int w = 70;
        int bx = x0 + (bw - w) / 2, by = y0 + 3;
        building(bx, by, w, 14, 6, 3, "SCHOOL", true);
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
        int[] towerStyles = {7, 7, 3, 8, 5};
        for (int r = 0; r < nr; r++) {
            for (int c = 0; c < nc; c++) {
                int w = 16 + rnd(10), h = 10 + rnd(8);
                int style = towerStyles[rnd(towerStyles.length)];
                int bx = x0 + c * cw + (cw - w) / 2, by = y0 + r * ch + 2;
                building(bx, by, w, h, style, 4, null, true);
                if (w >= 14 && h >= 9) building(bx + 3, by + 2, w - 6, h - 5, style, 0, null, false); // raised top
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
     * Roof (w x h tiles) with random rooftop details, plus a south wall of 'fac' rows with windows, bars or boards,
     * and an awning on shops. A building with a door gets a double door 2 tiles wide and 3 tall (taller than a person).
     */
    private void building(int x, int y, int w, int h, int style, int fac, String label, boolean door) {
        if (door && fac < 3) fac = 3;
        int base = B0 + style * PARTS;
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
                else {
                    part = P_C;
                    if (w >= 6 && h >= 5 && rnd(100) < 6) part = P_VENT + rnd(5);   // vent, AC, skylight, tank, chimney
                }
                set(x + i, y + j, base + part);
            }
        }
        boolean glass = (WALL_T[style] == 5);
        boolean awning = (label != null && !glass) || rnd(100) < 12;
        int winKind = rnd(4);                       // 0,1 plain  2 barred  3 mixed with boarded-up
        int doorCol = x + w / 2 - 1;
        for (int k = 0; k < fac; k++) {
            boolean winRow = (fac >= 4 && k >= 1) || (fac == 3 && k == 1);
            for (int i = 0; i < w; i++) {
                int part;
                if (glass) part = P_GLASS;
                else if (k == 0) part = awning ? P_AWN : P_WALL;
                else if (winRow && (x + i) % 2 == 0) {
                    part = (winKind == 2) ? P_WINB : P_WINM;
                    if (winKind == 3 && (x + i) % 4 == 0) part = P_WINX;
                } else part = P_WALLM;
                if (door) {
                    int row = k - (fac - 3);
                    if (row >= 0 && x + i == doorCol) part = (row == 0) ? P_DLT : (row == 1 ? P_DLM : P_DLB);
                    if (row >= 0 && x + i == doorCol + 1) part = (row == 0) ? P_DRT : (row == 1 ? P_DRM : P_DRB);
                }
                set(x + i, y + h + k, base + part);
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
                    if (t == SIDEWALK || isRoadTile(t) || t == -1) stop = true;
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
        g.setColor(0xC8C8B8);
        g.drawRect(0, 0, w - 1, h - 1);
        g.setFont(f);
        g.drawString(text, 4, 2, Graphics.TOP | Graphics.LEFT);
        signImg[signCount] = im;
        signX[signCount] = cx - w / 2;
        signY[signCount] = cy - h / 2;
        signCount++;
    }

    private static int signBg(String t) {
        if (t.equals("FIRE STATION") || t.equals("MARKET") || t.equals("SHOP") || t.equals("GAS")) return 0x8A1818;
        if (t.equals("FOOD")) return 0xB06A14;
        if (t.equals("FRUITS")) return 0x2A6A2A;
        if (t.equals("WEAPONS")) return 0x111111;
        return 0x1B2A55;
    }

    // ---------------- roads ----------------

    private void roads() {
        int bottom = hlo(4) - 1;
        for (int i = 0; i < 5; i++) {
            fill(0, hlo(i) - 2, MW - 1, hlo(i) - 1, SIDEWALK);
            fill(0, hhi(i) + 1, MW - 1, hhi(i) + 2, SIDEWALK);
        }
        for (int i = 0; i < 7; i++) {
            fill(vlo(i) - 2, vtop(i), vlo(i) - 1, bottom, SIDEWALK);
            fill(vhi(i) + 1, vtop(i), vhi(i) + 2, bottom, SIDEWALK);
        }
        for (int i = 0; i < 5; i++) fill(0, hlo(i), MW - 1, hhi(i), ROAD);
        for (int i = 0; i < 7; i++) fill(vlo(i), vtop(i), vhi(i), bottom, ROAD);
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
        // street lamps on the outer edge of the pavements
        for (int i = 0; i < 5; i++) {
            int yN = hlo(i) - 2, yS = hhi(i) + 2;
            for (int tx = 10; tx < MW; tx += 28) {
                if (get(tx, yN) == SIDEWALK) set(tx, yN, LAMP);
                if (get(tx + 14, yS) == SIDEWALK) set(tx + 14, yS, LAMP);
            }
        }
        for (int i = 0; i < 7; i++) {
            int xW = vlo(i) - 2, xE = vhi(i) + 2;
            for (int ty = vtop(i) + 14; ty < hlo(4) - 2; ty += 28) {
                if (get(xW, ty) == SIDEWALK) set(xW, ty, LAMP);
                if (get(xE, ty + 14) == SIDEWALK) set(xE, ty + 14, LAMP);
            }
        }
    }

    /** Manholes, puddles, cones and rubbish. */
    private void litter() {
        for (int i = 0; i < 90; i++) {
            int tx = rnd(MW), ty = rnd(MH);
            if (get(tx, ty) == ROAD) set(tx, ty, MANHOLE);
        }
        for (int i = 0; i < 200; i++) {
            int tx = rnd(MW), ty = rnd(MH);
            if (get(tx, ty) == ROAD) set(tx, ty, PUDDLE);
        }
        for (int i = 0; i < 700; i++) {
            int tx = rnd(MW), ty = rnd(MH);
            if (get(tx, ty) == SIDEWALK && rnd(3) == 0) set(tx, ty, TRASH);
        }
        int cones = 0;
        for (int i = 0; i < 4000 && cones < 90; i++) {
            int tx = rnd(MW), ty = rnd(MH);
            if (get(tx, ty) != ROAD) continue;
            if (get(tx - 1, ty) == SIDEWALK || get(tx + 1, ty) == SIDEWALK
                    || get(tx, ty - 1) == SIDEWALK || get(tx, ty + 1) == SIDEWALK) {
                set(tx, ty, CONE);
                cones++;
            }
        }
    }

    /** Last step: worn patches on roads, dead grass, cracked pavement. */
    private void weather() {
        for (int ty = 0; ty < MH; ty++) {
            for (int tx = 0; tx < MW; tx++) {
                int t = map[ty * MW + tx];
                int r = rnd(100);
                if (t == ROAD) {
                    if (r < 14) map[ty * MW + tx] = ROAD_B;
                    else if (r < 24) map[ty * MW + tx] = ROAD_C;
                } else if (t == GRASS) {
                    if (r < 14) map[ty * MW + tx] = GRASS2;
                    else if (r < 24) map[ty * MW + tx] = GRASS3;
                } else if (t == SIDEWALK && r < 12) {
                    map[ty * MW + tx] = SIDEWALK2;
                }
            }
        }
    }

    // ===================== tile art =====================

    private static void col(Graphics g, int c) { g.setColor(c & 0xFFFFFF); }
    private static int a(int c, int pct) { return Gfx.adj(c, pct); }
    private Image tile() { return Image.createImage(TS, TS); }

    private int r16() {
        sd = sd * 1103515245 + 12345;
        return (sd >>> 16) & 15;
    }

    private void speck(Graphics g, int color, int n) {
        col(g, color);
        for (int i = 0; i < n; i++) g.fillRect(r16(), r16(), 1, 1);
    }

    private void makeTiles() {
        tiles[GRASS] = grass(0);
        tiles[GRASS2] = grass(1);
        tiles[GRASS3] = grass(2);
        tiles[TREE] = grass(0);
        tiles[ISLAND] = grass(0);
        tiles[PATH] = paved(0x6F6A5C, 0x5C584C, false);
        tiles[SIDEWALK] = paved(0x5E5C54, 0x4C4A44, false);
        tiles[SIDEWALK2] = paved(0x5A5850, 0x4C4A44, true);
        tiles[ROAD] = road(0);
        tiles[ROAD_VY] = road(1);
        tiles[ROAD_HY] = road(2);
        tiles[ROAD_VW] = road(3);
        tiles[ROAD_HW] = road(4);
        tiles[ZEBRA_V] = road(5);
        tiles[ZEBRA_H] = road(6);
        tiles[ROAD_B] = road(7);
        tiles[ROAD_C] = road(8);
        tiles[PARKING] = flat(0x303236, 0x3A3C40);
        tiles[PARK_LINE] = parkLine();
        tiles[PLAZA] = paved(0x6A675C, 0x585548, false);
        tiles[WATER] = water();
        tiles[FIELD] = flat(0x3A5A32, 0x44683A);
        tiles[TRACK] = flat(0x6A3A2E, 0x5A3026);
        tiles[TARMAC] = flat(0x3A3C40, 0x34363A);
        tiles[RUNWAY] = flat(0x26282A, 0x2C2E30);
        tiles[RUNWAY_MARK] = runwayMark();
        tiles[FENCE_H] = fenceH();
        tiles[FENCE_V] = fenceV();
        tiles[DUMP_L] = dumpster(true);
        tiles[DUMP_R] = dumpster(false);
        tiles[BARREL] = barrel();
        tiles[CRATE] = crate();
        tiles[CONE] = cone();
        tiles[LAMP] = lamp();
        tiles[MANHOLE] = manhole();
        tiles[TRASH] = trash();
        tiles[BIN] = bin();
        tiles[PUDDLE] = puddle();
        for (int s = 0; s < STYLES; s++) {
            for (int p = 0; p < PARTS; p++) {
                if (p <= P_CHIM) tiles[B0 + s * PARTS + p] = roof(s, p);
                else tiles[B0 + s * PARTS + p] = facade(s, p);
            }
        }
        for (int i = 0; i < 1024; i++) if (tiles[i] == null) tiles[i] = tiles[GRASS];
    }

    private Image grass(int v) {
        Image im = tile();
        Graphics g = im.getGraphics();
        sd = 11 + v * 7;
        int base = (v == 0) ? 0x34402A : (v == 1) ? 0x3B4028 : 0x474830;
        col(g, base); g.fillRect(0, 0, TS, TS);
        speck(g, a(base, 122), 10);
        speck(g, a(base, 78), 10);
        if (v == 1) {
            col(g, 0x4A432C); g.fillRect(3, 4, 6, 4); g.fillRect(8, 9, 5, 3);
            speck(g, 0x5A5036, 6);
        }
        if (v == 2) speck(g, 0x5E5A38, 14);
        return im;
    }

    private Image flat(int c1, int c2) {
        Image im = tile();
        Graphics g = im.getGraphics();
        sd = c1 & 0xFFF;
        col(g, c1); g.fillRect(0, 0, TS, TS);
        speck(g, c2, 8);
        return im;
    }

    private Image paved(int c1, int joint, boolean cracked) {
        Image im = tile();
        Graphics g = im.getGraphics();
        sd = (c1 & 0xFFF) + 3;
        col(g, c1); g.fillRect(0, 0, TS, TS);
        col(g, joint);
        g.fillRect(TS - 1, 0, 1, TS); g.fillRect(0, TS - 1, TS, 1);
        speck(g, a(c1, 114), 5);
        speck(g, a(c1, 82), 5);
        if (cracked) {
            col(g, a(c1, 58));
            g.drawLine(2, 3, 7, 9); g.drawLine(7, 9, 13, 11); g.drawLine(7, 9, 6, 14);
        }
        return im;
    }

    /** 0 plain, 1 yellow vertical, 2 yellow horizontal, 3 white vertical, 4 white horizontal, 5/6 zebra, 7 cracked, 8 patched. */
    private Image road(int k) {
        Image im = flat(0x2A2C2E, 0x353739);
        Graphics g = im.getGraphics();
        if (k == 1) { col(g, 0xB89C3C); g.fillRect(7, 0, 2, TS); }
        else if (k == 2) { col(g, 0xB89C3C); g.fillRect(0, 7, TS, 2); }
        else if (k == 3) { col(g, 0xA8A89C); g.fillRect(7, 0, 1, TS); }
        else if (k == 4) { col(g, 0xA8A89C); g.fillRect(0, 7, TS, 1); }
        else if (k == 5) { col(g, 0xBEBCB0); g.fillRect(1, 0, 5, TS); g.fillRect(9, 0, 5, TS); }
        else if (k == 6) { col(g, 0xBEBCB0); g.fillRect(0, 1, TS, 5); g.fillRect(0, 9, TS, 5); }
        else if (k == 7) {
            col(g, 0x1A1B1D);
            g.drawLine(1, 2, 6, 7); g.drawLine(6, 7, 10, 7); g.drawLine(10, 7, 14, 13); g.drawLine(6, 7, 5, 12);
        } else if (k == 8) {
            col(g, 0x303234); g.fillRect(2, 3, 11, 9);
            col(g, 0x232527); g.drawRect(2, 3, 10, 8);
        }
        return im;
    }

    private Image parkLine() {
        Image im = flat(0x303236, 0x3A3C40);
        Graphics g = im.getGraphics();
        col(g, 0xA0A096); g.fillRect(0, 0, 1, TS);
        return im;
    }

    private Image water() {
        Image im = tile();
        Graphics g = im.getGraphics();
        col(g, 0x1F4552); g.fillRect(0, 0, TS, TS);
        col(g, 0x2E6070); g.fillRect(2, 4, 5, 1); g.fillRect(9, 10, 5, 1);
        return im;
    }

    private Image runwayMark() {
        Image im = flat(0x26282A, 0x2C2E30);
        Graphics g = im.getGraphics();
        col(g, 0xBEBEB0); g.fillRect(3, 7, 9, 2);
        return im;
    }

    // ---- props (each sits on its own concrete / grass / asphalt pad) ----

    private Image fenceH() {
        Image im = grass(0);
        Graphics g = im.getGraphics();
        col(g, 0x6A5A44);
        for (int x = 0; x < TS; x += 4) g.fillRect(x, 3, 3, 10);
        col(g, 0x4A3E2E);
        g.fillRect(0, 5, TS, 1); g.fillRect(0, 10, TS, 1);
        col(g, 0x827058);
        for (int x = 0; x < TS; x += 4) g.fillRect(x, 3, 1, 10);
        return im;
    }

    private Image fenceV() {
        Image im = grass(0);
        Graphics g = im.getGraphics();
        col(g, 0x6A5A44);
        for (int y = 0; y < TS; y += 4) g.fillRect(5, y, 6, 3);
        col(g, 0x4A3E2E);
        g.fillRect(6, 0, 1, TS); g.fillRect(9, 0, 1, TS);
        return im;
    }

    private Image dumpster(boolean left) {
        Image im = flat(0x4A4C48, 0x3E403C);
        Graphics g = im.getGraphics();
        int x0 = left ? 2 : 0, x1 = left ? 15 : 13;
        col(g, 0x2A4A3C); g.fillRect(x0, 3, x1 - x0 + 1, 10);
        col(g, 0x1C3328); g.fillRect(x0, 11, x1 - x0 + 1, 2);
        col(g, 0x3A6A54); g.fillRect(x0, 3, x1 - x0 + 1, 2);
        col(g, 0x14231C); g.fillRect(x0, 7, x1 - x0 + 1, 1);
        col(g, 0x6A4A32); g.fillRect(x0 + 3, 9, 3, 1);
        return im;
    }

    private Image barrel() {
        Image im = flat(0x4A4C48, 0x3E403C);
        Graphics g = im.getGraphics();
        col(g, 0x6A3A2A); g.fillArc(2, 2, 12, 12, 0, 360);
        col(g, 0x8A5036); g.drawArc(2, 2, 11, 11, 0, 360);
        col(g, 0x3A1E14); g.fillArc(4, 4, 8, 8, 0, 360);
        col(g, 0x241008); g.fillArc(6, 6, 4, 4, 0, 360);
        return im;
    }

    private Image crate() {
        Image im = flat(0x4A4C48, 0x3E403C);
        Graphics g = im.getGraphics();
        col(g, 0x6E5A3A); g.fillRect(2, 2, 12, 12);
        col(g, 0x4E3E28); g.drawRect(2, 2, 11, 11);
        g.drawLine(2, 2, 13, 13); g.drawLine(13, 2, 2, 13);
        col(g, 0x8A7450); g.fillRect(3, 3, 2, 1);
        return im;
    }

    private Image cone() {
        Image im = road(0);
        Graphics g = im.getGraphics();
        col(g, 0x1A1B1D); g.fillArc(3, 4, 11, 10, 0, 360);
        col(g, 0xB85A1A); g.fillArc(4, 3, 9, 9, 0, 360);
        col(g, 0xD8D0B8); g.fillArc(6, 5, 5, 5, 0, 360);
        col(g, 0xB85A1A); g.fillArc(7, 6, 3, 3, 0, 360);
        return im;
    }

    private Image lamp() {
        Image im = paved(0x5E5C54, 0x4C4A44, false);
        Graphics g = im.getGraphics();
        col(g, 0x2A2C2E); g.fillArc(4, 4, 9, 9, 0, 360);
        col(g, 0x3E4044); g.fillArc(5, 4, 7, 7, 0, 360);
        col(g, 0xC8B45A); g.fillRect(7, 6, 3, 3);
        col(g, 0x77703E); g.drawArc(2, 2, 13, 13, 0, 360);
        return im;
    }

    private Image manhole() {
        Image im = road(0);
        Graphics g = im.getGraphics();
        col(g, 0x1E1F21); g.fillArc(2, 2, 12, 12, 0, 360);
        col(g, 0x3E4043); g.drawArc(2, 2, 11, 11, 0, 360);
        g.drawLine(4, 8, 12, 8); g.drawLine(8, 4, 8, 12);
        return im;
    }

    private Image trash() {
        Image im = paved(0x5E5C54, 0x4C4A44, false);
        Graphics g = im.getGraphics();
        col(g, 0xA89E88); g.fillRect(3, 5, 3, 2); g.fillRect(10, 9, 3, 2);
        col(g, 0x3A3A3A); g.fillRect(7, 3, 2, 2); g.fillRect(5, 11, 2, 2);
        col(g, 0x6A7A5A); g.fillRect(11, 4, 2, 1);
        return im;
    }

    private Image bin() {
        Image im = paved(0x5E5C54, 0x4C4A44, false);
        Graphics g = im.getGraphics();
        col(g, 0x242B26); g.fillArc(3, 3, 10, 10, 0, 360);
        col(g, 0x3C4842); g.fillArc(4, 4, 8, 8, 0, 360);
        col(g, 0x1A201C); g.fillArc(6, 6, 4, 4, 0, 360);
        return im;
    }

    private Image puddle() {
        Image im = road(0);
        Graphics g = im.getGraphics();
        col(g, 0x1B262C); g.fillArc(1, 4, 14, 8, 0, 360);
        col(g, 0x2C3D46); g.fillArc(3, 5, 6, 3, 0, 360);
        return im;
    }

    // ---- roofs ----

    private Image roof(int style, int part) {
        Image im = tile();
        Graphics g = im.getGraphics();
        int base = ROOF_C[style], type = ROOF_T[style];
        sd = style * 31 + 7;
        col(g, base); g.fillRect(0, 0, TS, TS);
        if (type == 0) {                                      // gravel
            speck(g, a(base, 120), 16); speck(g, a(base, 80), 16);
        } else if (type == 1) {                               // corrugated metal
            for (int x = 0; x < TS; x += 4) {
                col(g, a(base, 116)); g.fillRect(x, 0, 2, TS);
                col(g, a(base, 84)); g.fillRect(x + 2, 0, 2, TS);
            }
            speck(g, 0x5A3A22, 6);
        } else if (type == 2) {                               // shingles
            for (int r = 0; r < 4; r++) {
                int y = r * 4, xo = (r % 2 == 0) ? 0 : 4;
                col(g, a(base, 82)); g.fillRect(0, y + 3, TS, 1);
                col(g, a(base, 112)); g.fillRect(0, y, TS, 1);
                col(g, a(base, 72));
                for (int x = xo; x < TS; x += 8) g.fillRect(x, y, 1, 3);
            }
        } else if (type == 3) {                               // tar and seams
            col(g, a(base, 78)); g.fillRect(0, 7, TS, 1); g.fillRect(11, 0, 1, TS);
            speck(g, a(base, 120), 8);
        } else {                                              // clay tiles
            for (int r = 0; r < 3; r++) {
                int y = r * 5 + 1;
                col(g, a(base, 120)); g.fillRect(0, y, TS, 1);
                col(g, a(base, 78)); g.fillRect(0, y + 3, TS, 2);
                col(g, a(base, 90));
                for (int x = (r % 2) * 3; x < TS; x += 6) g.fillRect(x, y + 1, 1, 2);
            }
        }
        boolean top = part <= P_TR;
        boolean bot = part >= P_BL && part <= P_BR;
        boolean left = part == P_TL || part == P_L || part == P_BL;
        boolean right = part == P_TR || part == P_R || part == P_BR;
        col(g, a(base, 60));
        if (top) g.fillRect(0, 1, TS, 2);
        if (left) g.fillRect(1, 0, 2, TS);
        if (right) g.fillRect(TS - 3, 0, 2, TS);
        col(g, a(base, 138));
        if (top) g.fillRect(0, 0, TS, 1);
        if (left) g.fillRect(0, 0, 1, TS);
        if (right) g.fillRect(TS - 1, 0, 1, TS);
        if (bot) {
            col(g, a(base, 52)); g.fillRect(0, TS - 3, TS, 3);
            col(g, a(base, 138)); g.fillRect(0, TS - 4, TS, 1);
        }
        if (part == P_VENT) {
            col(g, 0x6E7072); g.fillRect(4, 4, 8, 8);
            col(g, 0x3A3C3E); g.drawRect(4, 4, 7, 7);
            g.fillRect(6, 6, 4, 1); g.fillRect(6, 8, 4, 1);
        } else if (part == P_AC) {
            col(g, 0x8A8C8A); g.fillRect(2, 3, 12, 10);
            col(g, 0x4A4C4E); g.drawRect(2, 3, 11, 9);
            col(g, 0x2A2C2E); g.fillArc(4, 4, 8, 8, 0, 360);
            col(g, 0x5A5C5E); g.drawLine(8, 4, 8, 11); g.drawLine(4, 8, 11, 8);
        } else if (part == P_SKY) {
            col(g, 0x34383A); g.fillRect(3, 3, 10, 10);
            col(g, 0x3E6A70); g.fillRect(4, 4, 8, 8);
            col(g, 0x74A0A4); g.drawLine(5, 5, 8, 5); g.drawLine(5, 5, 5, 8);
            col(g, 0x34383A); g.drawLine(8, 4, 8, 11);
        } else if (part == P_TANK) {
            col(g, 0x2A2420); g.fillArc(1, 2, 14, 14, 0, 360);
            col(g, 0x6A4E3A); g.fillArc(2, 1, 12, 12, 0, 360);
            col(g, 0x8A6A50); g.fillArc(3, 2, 6, 5, 0, 360);
            col(g, 0x3A2A20); g.drawArc(2, 1, 11, 11, 0, 360);
        } else if (part == P_CHIM) {
            col(g, 0x5A3A30); g.fillRect(5, 5, 6, 6);
            col(g, 0x2A1A14); g.fillRect(6, 6, 4, 4);
            col(g, 0x7A5A4A); g.drawLine(5, 5, 10, 5);
        }
        return im;
    }

    // ---- walls ----

    private void wallTexture(Graphics g, int type, int base) {
        col(g, base); g.fillRect(0, 0, TS, TS);
        if (type == 0) {                                      // brick
            col(g, a(base, 70));
            for (int y = 3; y < TS; y += 4) g.fillRect(0, y, TS, 1);
            for (int r = 0; r < 4; r++) {
                int xo = (r % 2 == 0) ? 0 : 4;
                for (int x = xo; x < TS; x += 8) g.fillRect(x, r * 4, 1, 3);
            }
            speck(g, a(base, 122), 6); speck(g, a(base, 84), 6);
        } else if (type == 1) {                               // concrete panels
            col(g, a(base, 82)); g.fillRect(0, 7, TS, 1); g.fillRect(7, 0, 1, TS);
            speck(g, a(base, 112), 8); speck(g, a(base, 84), 8);
        } else if (type == 2) {                               // stucco
            speck(g, a(base, 116), 18); speck(g, a(base, 84), 18);
        } else if (type == 3) {                               // siding
            for (int y = 0; y < TS; y += 4) {
                col(g, a(base, 116)); g.fillRect(0, y, TS, 1);
                col(g, a(base, 80)); g.fillRect(0, y + 3, TS, 1);
            }
        } else if (type == 4) {                               // metal panels
            col(g, a(base, 84));
            for (int x = 0; x < TS; x += 4) g.fillRect(x, 0, 1, TS);
            speck(g, 0x6A4A32, 7);
        }
    }

    private Image facade(int style, int part) {
        Image im = tile();
        Graphics g = im.getGraphics();
        int wall = WALL_C[style], wt = WALL_T[style];
        sd = style * 17 + part;
        wallTexture(g, wt == 5 ? 2 : wt, wall);
        if (part == P_GLASS) {
            col(g, a(wall, 80)); g.fillRect(0, 7, TS, 1); g.fillRect(7, 0, 1, TS);
            col(g, a(wall, 135)); g.fillRect(2, 2, 3, 1); g.fillRect(10, 9, 3, 1); g.fillRect(3, 10, 1, 3);
        }
        boolean eave = (part == P_WALL || part == P_AWN || part == P_GLASS || part == P_DLT || part == P_DRT);
        if (eave) {
            col(g, a(ROOF_C[style], 62));
            g.fillRect(0, 0, TS, 2);
        }
        if (part == P_WINM || part == P_WINB || part == P_WINX) windowArt(g, part);
        else if (part == P_AWN) awningArt(g, ACC_C[style]);
        else if (part >= P_DLT) doorArt(g, part, ACC_C[style]);
        // grime where the wall meets the ground
        if (part == P_WALLM || part == P_WINM || part == P_WINB || part == P_WINX || part == P_DLB || part == P_DRB) {
            col(g, a(wall, 70)); g.fillRect(0, 14, TS, 2);
        }
        return im;
    }

    private void windowArt(Graphics g, int part) {
        col(g, 0x24262A); g.fillRect(4, 3, 8, 9);                  // frame
        if (part == P_WINX) {                                      // boarded up
            col(g, 0x5E4C34); g.fillRect(5, 4, 6, 7);
            col(g, 0x44362A); g.fillRect(5, 6, 6, 1); g.fillRect(5, 9, 6, 1);
            col(g, 0x8A7450); g.fillRect(5, 4, 6, 1);
            return;
        }
        col(g, 0x35504E); g.fillRect(5, 4, 6, 7);                  // dirty glass
        col(g, 0x5E8280); g.fillRect(5, 4, 2, 1);
        col(g, 0x24262A); g.fillRect(8, 4, 1, 7);
        if (part == P_WINB) {                                      // security bars
            col(g, 0x8A8C88);
            g.fillRect(5, 4, 1, 7); g.fillRect(7, 4, 1, 7); g.fillRect(9, 4, 1, 7); g.fillRect(11, 4, 1, 7);
        }
        col(g, 0x6E6A5C); g.fillRect(3, 12, 10, 1);                // sill
    }

    private void awningArt(Graphics g, int acc) {
        for (int x = 0; x < TS; x += 8) {
            col(g, acc); g.fillRect(x, 3, 4, 9);
            col(g, 0xA8A290); g.fillRect(x + 4, 3, 4, 9);
        }
        col(g, a(acc, 62)); g.fillRect(0, 12, TS, 2);              // shadow under the awning
        for (int x = 0; x < TS; x += 4) { col(g, a(acc, 130)); g.fillRect(x, 11, 2, 1); }
    }

    /** Double door made of 6 tiles (left/right x top/middle/bottom): 2 tiles wide, about 38 px tall. */
    private void doorArt(Graphics g, int part, int acc) {
        boolean left = (part == P_DLT || part == P_DLM || part == P_DLB);
        boolean top = (part == P_DLT || part == P_DRT);
        boolean bot = (part == P_DLB || part == P_DRB);
        int x0 = left ? 3 : 0, x1 = left ? 15 : 12;
        int y0 = top ? 8 : 0, y1 = bot ? 13 : 15;
        col(g, a(acc, 40));
        g.fillRect(x0, y0, x1 - x0 + 1, y1 - y0 + 1);                 // frame
        int lx0 = left ? x0 + 1 : x0, lx1 = left ? x1 : x1 - 1;
        int ly0 = top ? y0 + 1 : y0;
        col(g, acc);
        g.fillRect(lx0, ly0, lx1 - lx0 + 1, y1 - ly0 + 1);            // leaf
        col(g, a(acc, 125));
        int py = top ? y0 + 3 : y0 + 2;
        g.fillRect(lx0 + 2, py, lx1 - lx0 - 3, y1 - py - 1);          // panel
        col(g, a(acc, 40));
        if (left) g.fillRect(15, ly0, 1, y1 - ly0 + 1); else g.fillRect(0, ly0, 1, y1 - ly0 + 1);
        if (!top && !bot) {
            col(g, 0xB89C48);
            if (left) g.fillRect(12, 6, 2, 4); else g.fillRect(2, 6, 2, 4);
        }
        if (bot) {
            col(g, 0x6E6A5C);
            g.fillRect(0, 14, TS, 2);                                 // step
        }
    }
}
