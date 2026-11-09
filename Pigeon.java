import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** One pigeon: pecks and hops on the pavement, flies off when Killer or a car gets close. */
public class Pigeon {
    public int x, y;          // position on the ground (feet)
    public int z = 0;         // height above the ground while flying
    public boolean flying = false;
    public boolean faceRight = true;
    private int fvx, fvy, flyT, peckT, walkT, wdx, wdy, flap;

    public Pigeon(int x, int y) {
        this.x = x;
        this.y = y;
        peckT = NpcManager.rnd(60);
    }

    public void update(PigeonManager m, World w, Player k, Vehicle driven) {
        int dx = x - k.x, dy = y - k.y;
        int d2 = dx * dx + dy * dy;
        boolean scared = d2 < 70 * 70;
        if (!scared && driven != null) {
            int vx = x - driven.x, vy = y - driven.y;
            int sp = driven.sp < 0 ? -driven.sp : driven.sp;
            if (sp > 16 && vx * vx + vy * vy < 110 * 110) scared = true;
        }
        if (!flying) {
            if (scared) {                                       // take off, away from the danger
                flying = true;
                int ax = dx, ay = dy;
                if (driven != null) { ax = x - driven.x; ay = y - driven.y; }
                fvx = (ax > 0 ? 3 : ax < 0 ? -3 : 2 - NpcManager.rnd(5)) + NpcManager.rnd(3) - 1;
                fvy = (ay > 0 ? 2 : ay < 0 ? -2 : 0) + NpcManager.rnd(3) - 1;
                faceRight = fvx >= 0;
                flyT = 80 + NpcManager.rnd(60);
                z = 0;
                return;
            }
            peckT--;
            if (peckT < 0) peckT = 40 + NpcManager.rnd(60);
            if (walkT > 0) {                                    // a few quick steps
                walkT--;
                int nx = x + wdx, ny = y + wdy;
                if (!w.solidAt(nx, ny) && m.goodGround(nx, ny)) { x = nx; y = ny; }
                else walkT = 0;
            } else if (NpcManager.rnd(120) == 0) {
                walkT = 8 + NpcManager.rnd(14);
                wdx = NpcManager.rnd(3) - 1;
                wdy = NpcManager.rnd(3) - 1;
                if (wdx != 0) faceRight = wdx > 0;
            }
        } else {
            x += fvx;
            y += fvy;
            if (z < 34) z += 2;
            flap++;
            flyT--;
            if (flyT <= 0) {
                if (m.goodGround(x, y) && !w.solidAt(x, y)) { flying = false; z = 0; peckT = 30; }
                else flyT = 25;
            }
            if (d2 > 700 * 700) m.place(this, k);                // far away: reuse it near Killer
        }
    }

    /** Frame to draw: 0 stand, 1 peck, 2 step, 3 wings up, 4 wings down. */
    public int frame() {
        if (flying) return (flap / 3) % 2 == 0 ? 3 : 4;
        if (walkT > 0) return ((x + y) & 2) == 0 ? 0 : 2;
        return peckT < 12 ? 1 : 0;
    }

    public void reset(int nx, int ny) {
        x = nx;
        y = ny;
        z = 0;
        flying = false;
        walkT = 0;
        peckT = NpcManager.rnd(60);
    }

    public void draw(Graphics g, int camX, int camY) {
        Image im = PigeonManager.sprite(frame(), faceRight);
        g.drawImage(im, x - im.getWidth() / 2 - camX, y - im.getHeight() + 1 - z - camY,
                Graphics.TOP | Graphics.LEFT);
    }
}
