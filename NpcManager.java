import java.util.Random;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** Spawns pedestrians, owns the houses' doors and spreads panic by sound. */
public class NpcManager {
    private static final Random R = new Random();

    /** Random 0..n-1 (CLDC 1.1 Random has no nextInt(int)). */
    public static int rnd(int n) {
        return (R.nextInt() & 0x7FFFFFFF) % n;
    }

    public final Npc[] npcs;
    public final int[] doorX = new int[64];
    public final int[] doorY = new int[64];
    public final int[] doorUse = new int[64];

    public NpcManager(World w, Player k, int count) {
        // every house door in the test city (matches World.generate)
        int i = 0;
        for (int bx = 0; bx < 4; bx++) {
            for (int by = 0; by < 4; by++) {
                for (int s = 0; s < 2; s++) {
                    for (int r = 0; r < 2; r++) {
                        int dtx = bx * 16 + (s == 0 ? 2 : 12);
                        int dty = by * 16 + (r == 0 ? 4 : 14);
                        if (dty + 1 >= World.MH - 1) {
                            doorX[i] = -1;
                        } else {
                            doorX[i] = dtx * World.TS + 8;
                            doorY[i] = (dty + 1) * World.TS + 8;
                        }
                        i++;
                    }
                }
            }
        }
        npcs = new Npc[count];
        int placed = 0, tries = 0;
        while (placed < count && tries < count * 200) {
            tries++;
            int tx = 1 + rnd(World.MW - 2), ty = 1 + rnd(World.MH - 2);
            int lx = tx % 16, ly = ty % 16;
            if (lx == 7 || lx == 8 || ly == 7 || ly == 8) continue;
            if (!(lx == 6 || lx == 9 || ly == 6 || ly == 9)) continue; // sidewalks only
            int px = tx * World.TS + 8, py = ty * World.TS + 12;
            if (w.solidAt(px, py)) continue;
            int dx = px - k.x, dy = py - k.y;
            if (dx * dx + dy * dy < 100 * 100) continue;
            int r = rnd(100);
            int p = (r < 10) ? Npc.P_BRAVE : (r < 25) ? Npc.P_COWARD : (r < 40) ? Npc.P_FROZEN
                    : (r < 55) ? Npc.P_PANIC : Npc.P_NORMAL;
            npcs[placed++] = new Npc(px, py, rnd(NpcSprites.LOOKS), p);
        }
        for (int j = placed; j < count; j++) npcs[j] = new Npc(8 * World.TS, 24 * World.TS, 0, Npc.P_NORMAL);
    }

    public void update(World w, Player k) {
        for (int i = 0; i < npcs.length; i++) npcs[i].update(this, w, k);
    }

    /** A pedestrian shouts: calm neighbours within earshot become alarmed. */
    public void hear(Npc s) {
        for (int i = 0; i < npcs.length; i++) {
            Npc n = npcs[i];
            if (n == s || n.state != Npc.NORMAL || n.personality == Npc.P_BRAVE) continue;
            int dx = n.x - s.x, dy = n.y - s.y;
            if (dx * dx + dy * dy < 110 * 110) {
                n.startRecognise(s.fx, s.fy, true, 6 + rnd(14));
            }
        }
    }

    /** Nearest free house door that is reachable from the south side, or -1. */
    public int findDoor(int x, int y) {
        int best = -1, bestD = 220 * 220;
        for (int i = 0; i < 64; i++) {
            if (doorX[i] < 0 || doorUse[i] >= 2) continue;
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
            if (sx < -16 || sx > sw + 16 || sy < -8 || sy > sh + 30) continue;
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
