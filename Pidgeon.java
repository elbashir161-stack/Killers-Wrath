import javax.microedition.lcdui.Graphics;

/** Pigeons: peck on ground, fly away if you get close, ignore you if perched high up. */
public class Pigeon {
    public static final int GROUND = 0, FLYING = 1, PERCHED = 2;

    public int x, y;
    public int state = GROUND;
    public int tx, ty;
    public int flap = 0;
    public int peck = 0;
    public int dir = 1;
    public int wait = 0;

    public Pigeon(int x, int y, int state) {
        this.x = x;
        this.y = y;
        this.state = state;
    }

    public void update(World world, int px, int py, int rnd) {
        if (state == PERCHED) {
            peck++;
            return;
        }
        if (state == GROUND) {
            peck++;
            if (peck % 60 == 0) {
                x += (rnd % 3) - 1;
                if (rnd % 2 == 0) dir = -dir;
            }
            int dx = x - px, dy = y - py;
            int dist = (dx < 0 ? -dx : dx) + (dy < 0 ? -dy : dy);
            if (dist < 48) {
                state = FLYING;
                flap = 0;
                int ax = (dx >= 0 ? 1 : -1) * (80 + (rnd % 70));
                int ay = (dy >= 0 ? 1 : -1) * (40 + (rnd % 60));
                tx = x + ax;
                ty = y + ay - 20;
                if (tx < 0) tx = 0;
                if (ty < 0) ty = 0;
                if (tx > world.mw * World.TS) tx = world.mw * World.TS;
                if (ty > world.mh * World.TS) ty = world.mh * World.TS;
            }
        } else if (state == FLYING) {
            flap++;
            int dx = tx - x, dy = ty - y;
            if (dx > 0) { x += 4; dir = 1; }
            else if (dx < 0) { x -= 4; dir = -1; }
            if (dy > 0) y += 3;
            else if (dy < 0) y -= 3;
            if ((dx < 0 ? -dx : dx) < 5 && (dy < 0 ? -dy : dy) < 5) {
                state = GROUND;
                peck = 0;
            }
        }
    }

    public void draw(Graphics g, int camX, int camY) {
        int sx = x - camX, sy = y - camY;
        g.setColor(0x8A8A92);
        if (state == FLYING) {
            int w = (flap % 10 < 5) ? 6 : 2;
            g.fillRect(sx - 5, sy - w, 10, 3);
            g.fillRect(sx - 2, sy, 4, 3);
            g.setColor(0x2A2A2E);
            g.fillRect(sx + dir * 3, sy - 2, 2, 2);
        } else if (state == PERCHED) {
            g.fillRect(sx - 3, sy - 4, 6, 4);
            g.setColor(0x2A2A2E);
            g.fillRect(sx + dir * 2, sy - 6, 2, 2);
            g.setColor(0x5A5A62);
            g.fillRect(sx - dir * 4, sy - 3, 3, 2);
        } else {
            int headY = (peck % 40 < 8) ? 2 : 0;
            g.fillRect(sx - 3, sy - 2, 6, 3);
            g.setColor(0x2A2A2E);
            g.fillRect(sx + dir * 3, sy - 3 + headY, 2, 2);
            g.setColor(0x3A6A3A);
            g.fillRect(sx + dir * 1, sy - 2, 2, 2);
        }
    }
}
