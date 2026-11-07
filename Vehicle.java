import javax.microedition.lcdui.game.GameCanvas;

/** One car or motorcycle. x,y is its centre in world pixels; speed is in 1/16 px per tick. */
public class Vehicle {
    public int type, color, dir, x, y, sp;
    public boolean driven;
    private int frac;

    public Vehicle(int type, int color, int dir, int x, int y) {
        this.type = type;
        this.color = color;
        this.dir = dir;
        this.x = x;
        this.y = y;
    }

    public int hx() { return ((dir >= 2) ? VehicleKit.LEN[type] : VehicleKit.WID[type]) / 2; }
    public int hy() { return ((dir >= 2) ? VehicleKit.WID[type] : VehicleKit.LEN[type]) / 2; }

    public boolean contains(int px, int py) {
        int dx = px - x, dy = py - y;
        return dx > -hx() && dx < hx() && dy > -hy() && dy < hy();
    }

    /** Arrow keys steer: the direction pressed becomes the heading; the opposite one brakes, then reverses. */
    public void drive(int keys, World w, VehicleManager vm, NpcManager npcs) {
        int h = 0, v = 0;
        if ((keys & GameCanvas.LEFT_PRESSED) != 0) h = -1;
        if ((keys & GameCanvas.RIGHT_PRESSED) != 0) h = 1;
        if ((keys & GameCanvas.UP_PRESSED) != 0) v = -1;
        if ((keys & GameCanvas.DOWN_PRESSED) != 0) v = 1;
        int want = -1;
        boolean horiz = dir >= 2;
        if (h != 0 && v != 0) want = horiz ? (v < 0 ? Sprites.UP : Sprites.DOWN) : (h < 0 ? Sprites.LEFT : Sprites.RIGHT);
        else if (h != 0) want = (h < 0) ? Sprites.LEFT : Sprites.RIGHT;
        else if (v != 0) want = (v < 0) ? Sprites.UP : Sprites.DOWN;

        int max = VehicleKit.MAXSP[type] * 16;
        if (want < 0) {
            sp -= 3;
            if (sp < 0) sp = 0;
        } else if (want == dir) {
            sp += VehicleKit.ACC[type];
            if (sp > max) sp = max;
        } else if (want == (dir ^ 1)) {
            sp -= 14;
            if (sp <= 0) {
                sp = 0;
                turn(want, w, vm, npcs);
            }
        } else {
            if (turn(want, w, vm, npcs)) sp = sp * 3 / 4;
        }
        move(w, vm, npcs);
    }

    private boolean turn(int nd, World w, VehicleManager vm, NpcManager npcs) {
        int old = dir;
        dir = nd;
        if (!fullFree(w, vm, npcs)) {
            dir = old;
            return false;
        }
        return true;
    }

    private void move(World w, VehicleManager vm, NpcManager npcs) {
        int total = sp + frac;
        int steps = total >> 4;
        frac = total & 15;
        int dx = (dir == Sprites.LEFT) ? -1 : (dir == Sprites.RIGHT ? 1 : 0);
        int dy = (dir == Sprites.UP) ? -1 : (dir == Sprites.DOWN ? 1 : 0);
        for (int i = 0; i < steps; i++) {
            if (edgeFree(x + dx, y + dy, w, vm, npcs)) {
                x += dx;
                y += dy;
            } else {
                sp = 0;
                frac = 0;
                break;
            }
        }
    }

    private boolean blocked(int px, int py, World w, VehicleManager vm, NpcManager npcs) {
        return w.solidTerrain(px, py) || vm.hitOther(px, py, this) || npcs.blocksPoint(px, py);
    }

    private boolean edgeFree(int nx, int ny, World w, VehicleManager vm, NpcManager npcs) {
        int hx = hx(), hy = hy();
        if (dir == Sprites.RIGHT || dir == Sprites.LEFT) {
            int px = (dir == Sprites.RIGHT) ? nx + hx - 1 : nx - hx;
            for (int j = -hy + 1; ; j += 6) {
                if (j > hy - 1) j = hy - 1;
                if (blocked(px, ny + j, w, vm, npcs)) return false;
                if (j == hy - 1) break;
            }
        } else {
            int py = (dir == Sprites.DOWN) ? ny + hy - 1 : ny - hy;
            for (int i = -hx + 1; ; i += 6) {
                if (i > hx - 1) i = hx - 1;
                if (blocked(nx + i, py, w, vm, npcs)) return false;
                if (i == hx - 1) break;
            }
        }
        return true;
    }

    private boolean fullFree(World w, VehicleManager vm, NpcManager npcs) {
        int hx = hx(), hy = hy();
        for (int j = -hy + 1; ; j += 6) {
            if (j > hy - 1) j = hy - 1;
            for (int i = -hx + 1; ; i += 6) {
                if (i > hx - 1) i = hx - 1;
                if (blocked(x + i, y + j, w, vm, npcs)) return false;
                if (i == hx - 1) break;
            }
            if (j == hy - 1) break;
        }
        return true;
    }
}
