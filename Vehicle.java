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

    // ---- traffic (driven by the computer) ----
    public boolean ai;
    public int occupants;          // people inside: 0 = empty and can be entered, more = locked
    public boolean recycle;        // a traffic car that should be moved somewhere else
    private int aiSpeed = 44, junction = -1, turnWant, turnCoord, rotLeft, rotDir, rotT, waitT;
    public boolean skidding, skidValid;   // laying tyre tracks right now / tracks continue from last tick
    public int lastLX, lastLY, lastRX, lastRY;

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
        skidding = (down && sp > 56) || ((left || right) && (sp > 96 || sp < -96));
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

    public void startAI(int speed, int people) {
        ai = true;
        aiSpeed = speed;
        sp = speed;
        occupants = people;
        junction = -1;
        turnWant = 0;
        rotLeft = 0;
        waitT = 0;
        recycle = false;
        driven = false;
    }

    /** One tick of computer driving: keep to the lane, stop for anything ahead, sometimes turn at a junction. */
    public void aiDrive(World w, VehicleManager vm, NpcManager npcs, Player pl, boolean plOnFoot) {
        if (rotLeft > 0) {                                   // turning: swing the nose round, a step every 2 ticks
            if (--rotT <= 0) {
                h = (h + rotDir) & 15;
                rotLeft--;
                rotT = 2;
                if (rotLeft == 0) snapToLane();
            }
            if (sp > 24) sp -= 6;
            move(w, vm);
            return;
        }
        if (blockedAhead(w, vm, npcs, pl, plOnFoot)) {
            sp -= 24;
            if (sp < 0) sp = 0;
            if (++waitT > 500) recycle = true;
        } else {
            waitT = 0;
            if (sp < aiSpeed) sp += 3; else if (sp > aiSpeed) sp -= 3;
        }
        junctionLogic();
        move(w, vm);
        if (x < 40 || y < 40 || x > w.mw * World.TS - 40 || y > w.mh * World.TS - 40) recycle = true;
    }

    private boolean blockedAhead(World w, VehicleManager vm, NpcManager npcs, Player pl, boolean plOnFoot) {
        int c = VehicleKit.COS[h], s = VehicleKit.SIN[h];
        int look = len() / 2 + 16 + ((sp > 0 ? sp : 0) >> 4) * 10;
        int w2 = wid() / 2;
        for (int d = len() / 2 + 2; d <= look; d += 8) {
            for (int off = -w2; off <= w2; off += w2) {
                int px = x + (d * c - off * s) / 256, py = y + (d * s + off * c) / 256;
                if (vm.hitOther(px, py, this) || w.solidTerrain(px, py)) return true;
                if (npcs.personAtPoint(px, py)) return true;
                if (plOnFoot && Math.abs(px - pl.x) < 8 && py > pl.y - 14 && py < pl.y + 2) return true;
            }
        }
        return false;
    }

    private void junctionLogic() {
        int tx = x / World.TS, ty = y / World.TS;
        boolean horiz = (h == 0 || h == 8);
        int j = horiz ? World.vRoadAt(tx, ty) : World.hRoadAt(ty);
        if (j < 0) {
            junction = -1;
            return;
        }
        if (junction != j) {                                  // just entered a junction: decide
            junction = j;
            int r = NpcManager.rnd(100);
            turnWant = (r < 24) ? 1 : (r < 38 ? -1 : 0);      // 1 = right, -1 = left
            if (!horiz && h == 4 && j == 4) turnWant = (r < 50) ? 1 : -1;   // the road ends at the highway
            if (turnWant != 0) {
                if (h == 0) {
                    if (turnWant > 0) turnCoord = World.vLaneX(j, true);
                    else if (j < 5) turnCoord = World.vLaneX(j, false); else turnWant = 0;
                } else if (h == 8) {
                    if (turnWant > 0) { if (j < 5) turnCoord = World.vLaneX(j, false); else turnWant = 0; }
                    else turnCoord = World.vLaneX(j, true);
                } else if (h == 4) {
                    turnCoord = (turnWant > 0) ? World.hLaneY(j, false) : World.hLaneY(j, true);
                } else {
                    turnCoord = (turnWant > 0) ? World.hLaneY(j, true) : World.hLaneY(j, false);
                }
            }
        }
        if (turnWant != 0 && reached(turnCoord)) {
            rotLeft = 4;
            rotDir = turnWant;
            rotT = 2;
            turnWant = 0;
        }
    }

    private boolean reached(int coord) {
        if (h == 0) return x >= coord;
        if (h == 8) return x <= coord;
        if (h == 4) return y >= coord;
        return y <= coord;
    }

    private void snapToLane() {
        int tx = x / World.TS, ty = y / World.TS;
        if (h == 0 || h == 8) {
            int i = World.hRoadAt(ty);
            if (i >= 0) y = World.hLaneY(i, h == 0);
        } else {
            int j = World.vRoadAt(tx, ty);
            if (j >= 0) x = World.vLaneX(j, h == 4);
        }
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
