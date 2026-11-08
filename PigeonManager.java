import javax.microedition.lcdui.Graphics;

/** Spawns and updates all pigeons. */
public class PigeonManager {
    public Pigeon[] list;
    private int seed = 12345;

    private int rnd(int n) {
        seed = seed * 1103515245 + 12345;
        return ((seed >>> 16) & 0x7FFF) % n;
    }

    public PigeonManager(World world, int count) {
        list = new Pigeon[count];
        for (int i = 0; i < count; i++) {
            int px, py;
            if (i < 10) {
                // First 10 spawn RIGHT next to player start so you see them instantly
                px = (380 + (rnd(20) - 10)) * World.TS;
                py = (120 + (rnd(20) - 10)) * World.TS;
            } else {
                int tx = 50 + rnd(world.mw - 100);
                int ty = 50 + rnd(world.mh - 100);
                px = tx * World.TS;
                py = ty * World.TS;
            }
            int[] p = world.safeSpot(px, py);
            int state = (rnd(100) < 30) ? Pigeon.PERCHED : Pigeon.GROUND;
            if (state == Pigeon.PERCHED) p[1] -= 24;
            list[i] = new Pigeon(p[0], p[1], state);
        }
    }

    public void update(World world, Player player) {
        for (int i = 0; i < list.length; i++) {
            list[i].update(world, player.x, player.y, rnd(100));
        }
    }

    public void draw(Graphics g, int camX, int camY, int sw, int sh) {
        for (int i = 0; i < list.length; i++) {
            Pigeon p = list[i];
            int sx = p.x - camX, sy = p.y - camY;
            if (sx < -20 || sx > sw + 20 || sy < -20 || sy > sh + 20) continue;
            p.draw(g, camX, camY);
        }
    }
    }
