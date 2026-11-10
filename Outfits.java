/** Hair styles, hats, glasses, ties, backpacks... drawn over a basic person so every kind of pedestrian looks different. */
public class Outfits {
    public static final int TIE = 1, HARDHAT = 2, VEST = 4, POLICECAP = 8, CAPBACK = 16, CANE = 32, HEADBAND = 64,
            STRIPE = 128, BUN = 256, CROSS = 512, NURSECAP = 1024, GLASSES = 2048, PONY = 4096, CROP = 8192,
            HAT = 16384, BACKPACK = 32768, PIGTAILS = 65536, BRAIDS = 131072, BALL = 262144, CAP = 524288,
            AFRO = 1048576, BADGE = 2097152;
    private static final int W = SpriteKit.W, H = SpriteKit.H;

    /** dir 0 front, 1 back, 2 left, 3 right. hair/acc/skin are ARGB colours. */
    public static int[] apply(int[] src, int dir, int ex, int hair, int acc, int skin) {
        if (dir == 2) return mirror(apply(mirror(src), 3, ex, hair, acc, skin));
        int[] p = new int[src.length];
        System.arraycopy(src, 0, p, 0, src.length);
        boolean side = (dir == 3), front = (dir == 0), back = (dir == 1);
        int x0 = side ? 10 : 9, x1 = side ? 15 : 14;
        int hl = Gfx.adj(hair, 125), hd = Gfx.adj(hair, 72);
        int black = 0xFF14100E;

        if ((ex & AFRO) != 0) {
            for (int y = 0; y <= (back ? 7 : 5); y++)
                for (int x = 5; x <= 18; x++) {
                    float nx = (x + 0.5f - 11.7f) / 5.4f, ny = (y + 0.5f - 3.4f) / 4.6f;
                    if (nx * nx + ny * ny <= 1f) put(p, x, y, (nx + ny < -0.5f) ? hl : (nx + ny > 0.6f ? hd : hair));
                }
        }
        if ((ex & BUN) != 0) {
            int bx = side ? 10 : 12;
            for (int y = 0; y < 4; y++) for (int x = bx - 3; x <= bx + 2; x++) {
                float nx = (x + 0.5f - bx) / 2.3f, ny = (y + 0.5f - 1.2f) / 2.1f;
                if (nx * nx + ny * ny <= 1f) put(p, x, y, hair);
            }
        }
        if ((ex & PONY) != 0) {
            if (back) { rect(p, 11, 3, 2, 8, hair); rect(p, 11, 3, 2, 1, acc); }
            else if (side) { rect(p, 8, 3, 2, 8, hair); rect(p, 7, 9, 2, 3, hd); rect(p, 9, 3, 1, 1, acc); }
            else { rect(p, 11, 1, 2, 1, acc); }
        }
        if ((ex & PIGTAILS) != 0) {
            if (side) { rect(p, 9, 4, 1, 6, hair); rect(p, 9, 4, 1, 1, acc); }
            else { rect(p, 8, 4, 1, 6, hair); rect(p, 15, 4, 1, 6, hair); px(p, 8, 4, acc); px(p, 15, 4, acc); }
        }
        if ((ex & BRAIDS) != 0) {
            int[] cols = side ? new int[] {9} : new int[] {8, 15};
            for (int c = 0; c < cols.length; c++)
                for (int y = 5; y < 15; y++) px(p, cols[c], y, (y % 2 == 0) ? hair : hd);
        }
        if ((ex & HARDHAT) != 0) {
            rect(p, x0, 0, x1 - x0 + 1, 3, acc);
            rect(p, x0 - 1, 2, x1 - x0 + 3, 1, Gfx.adj(acc, 75));
            if (side) rect(p, x1, 2, 3, 1, Gfx.adj(acc, 75));
            rect(p, x0 + 1, 0, 2, 1, Gfx.adj(acc, 130));
        }
        if ((ex & POLICECAP) != 0) {
            rect(p, x0, 0, x1 - x0 + 1, 2, 0xFF1C2A4A);
            rect(p, x0, 2, x1 - x0 + 1, 1, 0xFF0C0C10);
            if (side) rect(p, x1, 2, 3, 1, 0xFF0C0C10);
            if (front) { px(p, 11, 1, 0xFFD8B040); px(p, 12, 1, 0xFFD8B040); }
        }
        if ((ex & CAP) != 0 || (ex & CAPBACK) != 0) {
            rect(p, x0, 1, x1 - x0 + 1, 2, acc);
            rect(p, x0 + 1, 1, 2, 1, Gfx.adj(acc, 130));
            if ((ex & CAP) != 0) {
                if (front) rect(p, x0, 3, x1 - x0 + 1, 1, Gfx.adj(acc, 70));
                if (side) rect(p, x1, 3, 3, 1, Gfx.adj(acc, 70));
            } else {
                if (back) rect(p, x0, 3, x1 - x0 + 1, 1, Gfx.adj(acc, 70));
                if (side) rect(p, x0 - 3, 3, 3, 1, Gfx.adj(acc, 70));
            }
        }
        if ((ex & NURSECAP) != 0) rect(p, x0 + 1, 0, 4, 2, 0xFFEDEDED);
        if ((ex & HEADBAND) != 0) rect(p, x0, 3, x1 - x0 + 1, 1, acc);
        if ((ex & HAT) != 0) {
            rect(p, x0 - 2, 2, x1 - x0 + 5, 1, Gfx.adj(acc, 80));
            rect(p, x0, 0, x1 - x0 + 1, 2, acc);
        }
        if ((ex & GLASSES) != 0 && !back) {
            if (front) {
                rect(p, 9, 4, 3, 1, black); rect(p, 9, 6, 3, 1, black); px(p, 9, 5, black); px(p, 11, 5, black);
                rect(p, 12, 4, 3, 1, black); rect(p, 12, 6, 3, 1, black); px(p, 12, 5, black); px(p, 14, 5, black);
            } else {
                rect(p, 13, 4, 3, 1, black); rect(p, 13, 6, 3, 1, black); px(p, 13, 5, black); px(p, 15, 5, black);
            }
        }
        if ((ex & TIE) != 0 && front) {
            px(p, 10, 10, 0xFFEDEDED); px(p, 13, 10, 0xFFEDEDED);
            rect(p, 11, 10, 2, 1, 0xFFB02030); rect(p, 11, 11, 2, 6, 0xFF8A1A28);
        }
        if ((ex & VEST) != 0) {
            if (side) { rect(p, 10, 14, 4, 1, 0xFFE8E4C0); rect(p, 10, 17, 4, 1, 0xFFE8E4C0); }
            else { rect(p, 9, 11, 1, 8, 0xFFE8E4C0); rect(p, 14, 11, 1, 8, 0xFFE8E4C0); }
        }
        if ((ex & STRIPE) != 0) {
            if (side) rect(p, 12, 11, 1, 9, 0xFFEDEDED);
            else { rect(p, 6, 12, 1, 7, 0xFFEDEDED); rect(p, 17, 12, 1, 7, 0xFFEDEDED); rect(p, 11, 22, 1, 7, 0xFFEDEDED); }
        }
        if ((ex & CROSS) != 0 && front) { rect(p, 14, 11, 1, 3, 0xFFE0508A); rect(p, 13, 12, 3, 1, 0xFFE0508A); }
        if ((ex & BADGE) != 0 && front) px(p, 14, 12, 0xFFD8B040);
        if ((ex & CROP) != 0) {
            if (side) rect(p, 10, 14, 4, 4, skin);
            else { rect(p, 9, 14, 6, 4, skin); if (front) px(p, 11, 16, Gfx.adj(skin, 60)); }
        }
        if ((ex & BACKPACK) != 0) {
            if (back) { rect(p, 8, 10, 8, 9, acc); rect(p, 8, 10, 8, 2, Gfx.adj(acc, 75)); }
            else if (side) rect(p, 8, 10, 3, 8, acc);
            else { rect(p, 9, 10, 1, 7, acc); rect(p, 14, 10, 1, 7, acc); }
        }
        if ((ex & CANE) != 0) {
            if (side) { rect(p, 16, 19, 1, 12, 0xFF6A4A2A); px(p, 15, 19, 0xFF6A4A2A); }
            else { rect(p, 18, 19, 1, 12, 0xFF6A4A2A); px(p, 17, 19, 0xFF6A4A2A); }
        }
        if ((ex & BALL) != 0) {
            int bx = side ? 17 : 17, by = 30;
            for (int y = by - 2; y <= by + 2; y++) for (int x = bx - 2; x <= bx + 2; x++)
                if ((x - bx) * (x - bx) + (y - by) * (y - by) <= 5) put(p, x, y, ((x + y) & 1) == 0 ? 0xFFEDEDED : 0xFFD85A20);
        }
        return p;
    }

    private static void px(int[] p, int x, int y, int c) { put(p, x, y, c); }

    private static void put(int[] p, int x, int y, int c) {
        if (x >= 0 && x < W && y >= 0 && y < H) p[y * W + x] = c;
    }

    private static void rect(int[] p, int x, int y, int w, int h, int c) {
        for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) put(p, i, j, c);
    }

    private static int[] mirror(int[] p) {
        int[] m = new int[W * H];
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) m[y * W + x] = p[y * W + (W - 1 - x)];
        return m;
    }
}
