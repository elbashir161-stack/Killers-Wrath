import java.util.Random;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** Pedestrians live around Killer; far-away ones are recycled near him. */
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
    private final World world;
    private int tick = 0;

    public NpcManager(World w, Player k, int count) {
        world = w;
        doorX = w.doorX;
        doorY = w.doorY;
        doorUse = new int[World.MAXDOORS];
        npcs = new Npc[count];
        for (int i = 0; i < count; i++) {
            int[] p = placeAround(k, 100, 560);
            int px = (p != null) ? p[0] : k.x + 120;
            int py = (p != null) ? p[1] : k.y;
            int r = rnd(100);
            int pers = (r < 10) ? Npc.P_BRAVE : (r < 25) ? Npc.P_COWARD : (r < 40) ? Npc.P_FROZEN
                    : (r < 55) ? Npc.P_PANIC : Npc.P_NORMAL;
            npcs[i] = new Npc(px, py, rnd(NpcSprites.LOOKS), pers);
        }
    }

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
        // recycle one far-away calm pedestrian per tick next to Killer's surroundings
        tick++;
        Npc n = npcs[tick % npcs.length];
        if (n.state == Npc.NORMAL) {
            int dx = n.x - k.x, dy = n.y - k.y;
            if (dx * dx + dy * dy > 640 * 640) {
                int[] p = placeAround(k, 320, 560);
                if (p != null) n.reset(p[0], p[1]);
            }
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

    public void drawNpcs(Graphics g, int camX, int camY, int sw, int sh, int py, boolean below) {
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if (n.state == Npc.INSIDE) continue;
            if (below != (n.y > py)) continue;
            int sx = n.x - camX, sy = n.y - camY;
            if (sx < -20 || sx > sw + 20 || sy < -8 || sy > sh + 34) continue;
            n.draw(g, camX, camY);
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
