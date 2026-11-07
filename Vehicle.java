import javax.microedition.lcdui.game.GameCanvas;

/**
 * One car or motorcycle. x,y = centre in world pixels, h = heading 0..15 (0 east, 4 south, 8 west, 12 north),
 * sp = speed in 1/16 px per tick (negative = reversing).
 * Controls: UP gas, DOWN brake then reverse, LEFT/RIGHT steer (only while moving).
 */
public class Vehicle {
    public static final int HRIGHT = 0, HDOWN = 4, HLEFT = 8, HUP = 12;

    public int type, color, h, x, y, sp;
    public boolean driven;
    private int accX, accY, steerT;

    public Vehicle(int type, int color, int h, int x, int y) {
        this.type = type;
        this.color = color;
        this.h = h;
        this.x = x;
        this.y = y;
    }

    public int len() { return VehicleKit.LEN[type]; }
    public int wid() { return VehicleKit.WID[type]; }

    /** Half-size of the bounding box around the rotated body. */
    public int extX() { return (len() * abs(VehicleKit.COS[h]) + wid() * abs(VehicleKit.SIN[h])) / 512; }
    public int extY() { return (len() * abs(VehicleKit.SIN[h]) + wid() * abs(VehicleKit.COS[h])) / 512; }

    private static int abs(int v) { return v < 0 ? -v : v; }

    public boolean contains(int px, int py) {
        int dx = px - x, dy = py - y;
        if (dx > 48 || dx < -48 || dy > 48 || dy < -48) return false;
        int c = VehicleKit.COS[h], s = VehicleKit.SIN[h];
        int u = (dx * c + dy * s) / 256;
        int v = (-dx * s + dy * c) / 256;
        return u > -len() / 2 && u < len() / 2 && v > -wid() / 2 && v < wid() / 2;
    }

    public void drive(int keys, World w, VehicleManager vm) {
        boolean up = (keys & GameCanvas.UP_PRESSED) != 0;
        boolean down = (keys & GameCanvas.DOWN_PRESSED) != 0;
        boolean left = (keys & GameCanvas.LEFT_PRESSED) != 0;
        boolean right = (keys & GameCanvas.RIGHT_PRESSED) != 0;
        int max = VehicleKit.MAXSP[type] * 16;
        int rev = 3 * 16;
        int acc = VehicleKit.ACC[type];
        if (up) {
            if (sp < 0) sp += 14;
            else { sp += acc; if (sp > max) sp = max; }
        } else if (down) {
            if (sp > 0) { sp -= 14; if (sp < 0) sp = 0; }
            else { sp -= acc; if (sp < -rev) sp = -rev; }
        } else {
            if (sp > 0) { sp -= 3; if (sp < 0) sp = 0; }
            else if (sp < 0) { sp += 3; if (sp > 0) sp = 0; }
        }
        // steering only when rolling; reverse flips it like a real car
        if ((left || right) && (sp >= 16 || sp <= -16)) {
            steerT--;
            if (steerT <= 0) {
                steerT = (abs(sp) > 64) ? 4 : 3;
                int d = left ? -1 : 1;
                if (sp < 0) d = -d;
                int nh = (h + d) & 15;
                if (fits(x, y, nh, w, vm)) h = nh;
            }
        } else if (!left && !right) {
            steerT = 0;
        }
        move(w, vm);
    }

    private void move(World w, VehicleManager vm) {
        accX += sp * VehicleKit.COS[h] / 256;
        accY += sp * VehicleKit.SIN[h] / 256;
        boolean crash = false;
        while (accX >= 16) {
            if (fits(x + 1, y, h, w, vm)) { x++; accX -= 16; } else { accX = 0; crash = true; }
        }
        while (accX <= -16) {
            if (fits(x - 1, y, h, w, vm)) { x--; accX += 16; } else { accX = 0; crash = true; }
        }
        while (accY >= 16) {
            if (fits(x, y + 1, h, w, vm)) { y++; accY -= 16; } else { accY = 0; crash = true; }
        }
        while (accY <= -16) {
            if (fits(x, y - 1, h, w, vm)) { y--; accY += 16; } else { accY = 0; crash = true; }
        }
        if (crash) sp = sp / 2;
    }

    /** Is the whole body free of walls, water, trunks and other vehicles at this place and heading? */
    private boolean fits(int nx, int ny, int nh, World w, VehicleManager vm) {
        int hl = len() / 2 - 1, hw = wid() / 2 - 1;
        int c = VehicleKit.COS[nh], s = VehicleKit.SIN[nh];
        for (int u = -hl; ; u += 7) {
            if (u > hl) u = hl;
            for (int v = -hw; ; v += 7) {
                if (v > hw) v = hw;
                int px = nx + (u * c - v * s) / 256;
                int py = ny + (u * s + v * c) / 256;
                if (w.solidTerrain(px, py) || vm.hitOther(px, py, this)) return false;
                if (v == hw) break;
            }
            if (u == hl) break;
        }
        return true;
    }
}
