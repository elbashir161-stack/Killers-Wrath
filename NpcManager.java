import java.util.Random;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Pedestrians live around Killer; far-away ones are recycled near him. Also: collisions, blood, deaths. */
public class NpcManager {
    private static final Random R = new Random();

    /** Random 0..n-1 (CLDC 1.1 Random has no nextInt(int)). */
    public static int rnd(int n) {
        return (R.nextInt() & 0x7FFFFFFF) % n;
    }

    public final Npc[] npcs;
    public final int[] doorX;
    public final int[] doorY;
    public final int[] doorUse;
    public Vehicle driven;              // the vehicle Killer is driving, or null
    public boolean playerOnFoot = true; // people bump into Killer only when he walks
    public int kills = 0;

    private final World world;
    private final Player player;
    private final int[] bloodX = new int[64];
    private final int[] bloodY = new int[64];
    private final int[] bloodS = new int[64];
    private int bloodN = 0, bloodHead = 0;
    private final Image[] bloodImg = new Image[3];
    private int tick = 0;

    public NpcManager(World w, Player k, int count) {
        world = w;
        player = k;
        doorX = w.doorX;
        doorY = w.doorY;
        doorUse = new int[World.MAXDOORS];
        bloodImg[0] = makeBlood(12);
        bloodImg[1] = makeBlood(20);
        bloodImg[2] = makeBlood(30);
        npcs = new Npc[count];
        for (int i = 0; i < count; i++) {
            int[] p = placeAround(k, 100, 560);
            int px = (p != null) ? p[0] : k.x + 120;
            int py = (p != null) ? p[1] : k.y;
            int r = rnd(100);
            int pers = (r < 10) ? Npc.P_BRAVE : (r < 25) ? Npc.P_COWARD : (r < 40) ? Npc.P_FROZEN
                    : (r < 55) ? Npc.P_PANIC : Npc.P_NORMAL;
            npcs[i] = new Npc(px, py, rnd(NpcSprites.LOOKS), pers);
            if (i % 6 == 2) {                            // some of the pedestrians are children
                npcs[i].kid = true;
                npcs[i].look = rnd(NpcSprites.KIDS);
            }
        }
    }

    // ---------------- placement ----------------

    /** A random sidewalk spot between minD and maxD pixels from Killer, or null. */
    private int[] placeAround(Player k, int minD, int maxD) {
        int rad = maxD / World.TS + 1;
        for (int i = 0; i < 80; i++) {
            int tx = k.x / World.TS + rnd(2 * rad + 1) - rad;
            int ty = k.y / World.TS + rnd(2 * rad + 1) - rad;
            if (!world.isSidewalk(tx, ty)) continue;
            int px = tx * World.TS + 8, py = ty * World.TS + 12;
            int dx = px - k.x, dy = py - k.y;
            int d2 = dx * dx + dy * dy;
            if (d2 < minD * minD || d2 > maxD * maxD) continue;
            return new int[] {px, py};
        }
        return null;
    }

    public void update(World w, Player k) {
        for (int i = 0; i < npcs.length; i++) npcs[i].update(this, w, k);
        if (npcs.length == 0) return;
        tick++;
        Npc n = npcs[tick % npcs.length];
        if (n.state == Npc.NORMAL || n.state == Npc.DEAD) {
            int dx = n.x - k.x, dy = n.y - k.y;
            if (dx * dx + dy * dy > 640 * 640) {
                int[] p = placeAround(k, 320, 560);
                if (p != null) n.reset(p[0], p[1]);
            }
        }
    }

    // ---------------- people are solid ----------------

    /**
     * Would a body (8x5 feet box) at nx,ny overlap another person? Bodies already overlapping may still
     * move apart. self == null means the mover is Killer.
     */
    public boolean blocks(int nx, int ny, int cx, int cy, Npc self) {
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if (n == self || n.state == Npc.INSIDE || n.state == Npc.DEAD) continue;
            if (Math.abs(nx - n.x) < 8 && Math.abs(ny - n.y) < 5
                    && !(Math.abs(cx - n.x) < 8 && Math.abs(cy - n.y) < 5)) return true;
        }
        if (self != null && playerOnFoot) {
            if (Math.abs(nx - player.x) < 8 && Math.abs(ny - player.y) < 5
                    && !(Math.abs(cx - player.x) < 8 && Math.abs(cy - player.y) < 5)) return true;
        }
        return false;
    }

    /** Is a standing person at this exact point? (traffic uses it to stop in front of people) */
    public boolean personAtPoint(int px, int py) {
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if (n.state == Npc.INSIDE || n.state == Npc.DEAD) continue;
            if (Math.abs(px - n.x) < 8 && py > n.y - 14 && py < n.y + 2) return true;
        }
        return false;
    }

    // ---------------- cars hit people ----------------

    /** Called every tick while Killer drives: anyone the body touches at speed is hurt. */
    public void vehicleHit(Vehicle v) {
        int spd = v.sp < 0 ? -v.sp : v.sp;
        if (spd < 24) return;
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if (n.state == Npc.DEAD || n.state == Npc.INSIDE) continue;
            if (v.contains(n.x, n.y - 2) || v.contains(n.x, n.y - 9)) {
                n.hurt((spd / 16) * 18, this, v.x, v.y);
                v.sp = v.sp * 7 / 10;
            }
        }
    }

    /** Someone was hurt: calm neighbours nearby get alarmed. */
    public void panicNear(int x, int y, int fx, int fy) {
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if ((n.state != Npc.NORMAL && n.state != Npc.STAND) || n.personality == Npc.P_BRAVE) continue;
            int dx = n.x - x, dy = n.y - y;
            if (dx * dx + dy * dy < 220 * 220) n.startRecognise(fx, fy, true, 4 + rnd(8));
        }
    }

    /** A pedestrian shouts: calm neighbours within earshot become alarmed. */
    public void hear(Npc s) {
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if (n == s || n.state != Npc.NORMAL || n.personality == Npc.P_BRAVE) continue;
            int dx = n.x - s.x, dy = n.y - s.y;
            if (dx * dx + dy * dy < 180 * 180) {
                n.startRecognise(s.fx, s.fy, true, 6 + rnd(14));
            }
        }
    }

    /** Nearest free house door that can be reached from the south side, or -1. */
    public int findDoor(int x, int y) {
        int best = -1, bestD = 320 * 320;
        for (int i = 0; i < world.doorCount; i++) {
            if (doorUse[i] >= 2) continue;
            if (y < doorY[i] - 8) continue;
            int dx = doorX[i] - x, dy = doorY[i] - y;
            int d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    // ---------------- blood ----------------

    public void addBlood(int x, int y, int size) {
        bloodX[bloodHead] = x;
        bloodY[bloodHead] = y;
        bloodS[bloodHead] = size;
        bloodHead = (bloodHead + 1) % 64;
        if (bloodN < 64) bloodN++;
    }

    private Image makeBlood(int size) {
        int[] p = new int[size * size];
        int sd = size * 7919;
        int c = size / 2;
        disc(p, size, c, c, size * 33 / 100, 0xDD7A0A0A);
        for (int i = 0; i < 6; i++) {
            sd = sd * 1103515245 + 12345;
            int ox = ((sd >>> 16) & 0x7FFF) % (size * 6 / 10 + 1) - size * 3 / 10;
            sd = sd * 1103515245 + 12345;
            int oy = ((sd >>> 16) & 0x7FFF) % (size * 6 / 10 + 1) - size * 3 / 10;
            disc(p, size, c + ox, c + oy, size / 9 + 1, (i % 2 == 0) ? 0xDD7A0A0A : 0xDDB01414);
        }
        disc(p, size, c - 1, c - 1, size / 6, 0xDDB01414);
        return Image.createRGBImage(p, size, size, true);
    }

    private static void disc(int[] p, int size, int cx, int cy, int r, int col) {
        for (int y = cy - r; y <= cy + r; y++)
            for (int x = cx - r; x <= cx + r; x++) {
                if (x < 0 || y < 0 || x >= size || y >= size) continue;
                int dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy <= r * r) p[y * size + x] = col;
            }
    }

    /** Blood pools and bodies lie on the ground, under everything that stands. */
    public void drawGround(Graphics g, int camX, int camY, int sw, int sh) {
        for (int i = 0; i < bloodN; i++) {
            Image im = bloodImg[bloodS[i]];
            int sx = bloodX[i] - im.getWidth() / 2 - camX, sy = bloodY[i] - im.getHeight() / 2 - camY;
            if (sx > sw || sy > sh || sx + im.getWidth() < 0 || sy + im.getHeight() < 0) continue;
            g.drawImage(im, sx, sy, Graphics.TOP | Graphics.LEFT);
        }
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if (n.state != Npc.DEAD) continue;
            Image im = n.kid ? NpcSprites.deadKid[n.look][(n.x + n.y) & 1] : NpcSprites.dead[n.look][(n.x + n.y) & 1];
            int sx = n.x - im.getWidth() / 2 - camX, sy = n.y - im.getHeight() + (n.kid ? 3 : 4) - camY;
            if (sx > sw || sy > sh || sx + im.getWidth() < 0 || sy + im.getHeight() < 0) continue;
            g.drawImage(im, sx, sy, Graphics.TOP | Graphics.LEFT);
        }
    }

    public void drawBubbles(Graphics g, Font f, int camX, int camY) {
        g.setFont(f);
        for (int i = 0; i < npcs.length; i++) npcs[i].drawBubble(g, f, camX, camY);
    }

    public int count(int state) {
        int c = 0;
        for (int i = 0; i < npcs.length; i++) if (npcs[i].state == state) c++;
        return c;
    }
}
