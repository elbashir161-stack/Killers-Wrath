import javax.microedition.lcdui.Graphics;

/** Tyre tracks left on hard ground when Killer brakes hard or turns sharply at speed. They fade away. */
public class SkidMarks {
    private static final int MAX = 600;
    private static final int LIFE = 1500;      // ticks (about a minute)
    private final int[] x1 = new int[MAX], y1 = new int[MAX], x2 = new int[MAX], y2 = new int[MAX];
    private final int[] age = new int[MAX];
    private int head = 0;

    public SkidMarks() {
        for (int i = 0; i < MAX; i++) age[i] = -1;
    }

    /** Call every tick while driving; lays tracks only while the vehicle is skidding. */
    public void update(Vehicle v, World w) {
        if (!v.skidding) {
            v.skidValid = false;
            return;
        }
        int c = VehicleKit.COS[v.h], s = VehicleKit.SIN[v.h];
        boolean bike = (v.type == VehicleKit.MOTO);
        int back = v.len() * (bike ? 38 : 28) / 100;
        int side = bike ? 0 : v.wid() / 2 - 3;
        int ax = v.x - c * back / 256, ay = v.y - s * back / 256;
        int lx = ax - s * side / 256, ly = ay + c * side / 256;
        int rx = ax + s * side / 256, ry = ay - c * side / 256;
        if (v.skidValid) {
            if (w.isHardGround(lx, ly)) add(v.lastLX, v.lastLY, lx, ly);
            if (!bike && w.isHardGround(rx, ry)) add(v.lastRX, v.lastRY, rx, ry);
        }
        v.lastLX = lx; v.lastLY = ly; v.lastRX = rx; v.lastRY = ry;
        v.skidValid = true;
    }

    private void add(int ax, int ay, int bx, int by) {
        x1[head] = ax; y1[head] = ay; x2[head] = bx; y2[head] = by;
        age[head] = 0;
        head = (head + 1) % MAX;
    }

    public void age() {
        for (int i = 0; i < MAX; i++) {
            if (age[i] < 0) continue;
            if (++age[i] > LIFE) age[i] = -1;
        }
    }

    public void draw(Graphics g, int camX, int camY, int sw, int sh) {
        for (int i = 0; i < MAX; i++) {
            if (age[i] < 0) continue;
            int ax = x1[i] - camX, ay = y1[i] - camY, bx = x2[i] - camX, by = y2[i] - camY;
            if ((ax < -8 && bx < -8) || (ax > sw + 8 && bx > sw + 8)
                    || (ay < -8 && by < -8) || (ay > sh + 8 && by > sh + 8)) continue;
            int t = age[i] * 100 / LIFE;                       // 0 fresh .. 100 gone
            int v = 0x0B + (0x30 - 0x0B) * t / 100;
            g.setColor(v << 16 | (v + 1) << 8 | (v + 2));
            g.drawLine(ax, ay, bx, by);
            int dx = ax > bx ? ax - bx : bx - ax, dy = ay > by ? ay - by : by - ay;
            if (dx > dy) g.drawLine(ax, ay + 1, bx, by + 1);
            else g.drawLine(ax + 1, ay, bx + 1, by);
        }
    }
}
