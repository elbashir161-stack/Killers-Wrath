import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Parked vehicles in car-park bays around Killer; far ones are recycled nearby. */
public class VehicleManager {
    public final Vehicle[] list;
    private final World world;
    private int tick = 0;

    public VehicleManager(World w, Player k, int count, boolean starters, int traffic) {
        world = w;
        list = new Vehicle[count + traffic];
        int n = 0;
        // starter vehicles parked along the avenue beside Killer's start
        if (starters && count >= 4) {
            list[n++] = new Vehicle(VehicleKit.MOTO, 0, Vehicle.HUP, k.x + 20, k.y + 30);
            list[n++] = new Vehicle(VehicleKit.SEDAN, 0, Vehicle.HUP, k.x + 24, k.y - 80);
            list[n++] = new Vehicle(VehicleKit.SPORTS, 1, Vehicle.HUP, k.x + 24, k.y + 160);
            list[n++] = new Vehicle(VehicleKit.PICKUP, 3, Vehicle.HUP, k.x + 24, k.y - 200);
        }
        for (int i = n; i < count; i++) {
            int r = NpcManager.rnd(100);
            int type = (r < 40) ? VehicleKit.SEDAN : (r < 60) ? VehicleKit.SPORTS : (r < 80) ? VehicleKit.PICKUP : VehicleKit.MOTO;
            Vehicle v = new Vehicle(type, NpcManager.rnd(VehicleKit.colorCount(type)), Vehicle.HUP, -1000, -1000);
            list[i] = v;
            place(v, k, 150, 1100);
        }
        for (int i = count; i < count + traffic; i++) {           // traffic with people inside
            Vehicle v = new Vehicle(VehicleKit.SEDAN, 0, Vehicle.HRIGHT, -1000, -1000);
            v.ai = true;
            list[i] = v;
            placeTraffic(v, k);
        }
    }

    private boolean ok(int sx, int sy, Player k, int minD, int maxD, Vehicle self) {
        int dx = sx - k.x, dy = sy - k.y;
        int d2 = dx * dx + dy * dy;
        if (d2 < minD * minD || d2 > maxD * maxD) return false;
        for (int i = 0; i < list.length; i++) {
            Vehicle o = list[i];
            if (o == null || o == self) continue;
            int ox = o.x - sx, oy = o.y - sy;
            if (ox > -45 && ox < 45 && oy > -45 && oy < 45) return false;
        }
        return true;
    }

    private boolean place(Vehicle v, Player k, int minD, int maxD) {
        int cnt = 0;
        for (int i = 0; i < world.stallCount; i++)
            if (ok(world.stallX[i], world.stallY[i], k, minD, maxD, v)) cnt++;
        if (cnt == 0) return false;
        int pick = NpcManager.rnd(cnt);
        for (int i = 0; i < world.stallCount; i++) {
            if (!ok(world.stallX[i], world.stallY[i], k, minD, maxD, v)) continue;
            if (pick == 0) {
                v.x = world.stallX[i];
                v.y = world.stallY[i];
                v.h = (NpcManager.rnd(2) == 0) ? Vehicle.HUP : Vehicle.HDOWN;
                v.sp = 0;
                v.driven = false;
                return true;
            }
            pick--;
        }
        return false;
    }

    public void update(Player k) {
        if (list.length == 0) return;
        tick++;
        if (tick % 4 != 0) return;
        Vehicle v = list[(tick / 4) % list.length];
        if (v.driven) return;
        if (v.ai) {
            int ax = v.x - k.x, ay = v.y - k.y;
            if (v.recycle || ax * ax + ay * ay > 1100 * 1100) placeTraffic(v, k);
            return;
        }
        int dx = v.x - k.x, dy = v.y - k.y;
        if (dx * dx + dy * dy > 1400 * 1400) place(v, k, 450, 1100);
    }

    /** Computer-driven cars: drive, stop for people, and push Killer out of the way if they touch him. */
    public void updateTraffic(NpcManager npcs, Player pl, boolean plOnFoot) {
        for (int i = 0; i < list.length; i++) {
            Vehicle v = list[i];
            if (!v.ai || v.driven) continue;
            v.aiDrive(world, this, npcs, pl, plOnFoot);
            if (plOnFoot && (v.contains(pl.x, pl.y - 2) || v.contains(pl.x, pl.y - 8))) {
                int dx = pl.x - v.x, dy = pl.y - v.y;
                int nx = pl.x, ny = pl.y;
                if (Math.abs(dx) * v.extY() > Math.abs(dy) * v.extX()) nx += (dx > 0 ? 3 : -3);
                else ny += (dy > 0 ? 3 : -3);
                if (pl.freeTerrainAt(nx, ny, world)) {
                    pl.x = nx;
                    pl.y = ny;
                }
                v.sp = 0;
            }
        }
    }

    /** Puts a traffic car in a lane of some road, 360..950 pixels from Killer, going the right way. */
    private boolean placeTraffic(Vehicle v, Player k) {
        for (int tries = 0; tries < 40; tries++) {
            int x, y, h;
            if (NpcManager.rnd(100) < 55) {
                int i = NpcManager.rnd(5);
                boolean east = NpcManager.rnd(2) == 0;
                y = World.hLaneY(i, east);
                x = k.x + NpcManager.rnd(1801) - 900;
                h = east ? Vehicle.HRIGHT : Vehicle.HLEFT;
            } else {
                int j = NpcManager.rnd(7);
                boolean south = (j >= 5) || NpcManager.rnd(2) == 0;
                x = World.vLaneX(j, south);
                y = k.y + NpcManager.rnd(1801) - 900;
                h = south ? Vehicle.HDOWN : Vehicle.HUP;
                if (!World.vRoadRows(j, y)) continue;
            }
            int dx = x - k.x, dy = y - k.y;
            int d2 = dx * dx + dy * dy;
            if (d2 < 360 * 360 || d2 > 950 * 950) continue;
            if (x < 100 || y < 100 || x > world.mw * World.TS - 100 || y > world.mh * World.TS - 100) continue;
            boolean free = true;
            for (int i = 0; i < list.length; i++) {
                Vehicle o = list[i];
                if (o == null || o == v) continue;
                int ox = o.x - x, oy = o.y - y;
                if (ox > -90 && ox < 90 && oy > -90 && oy < 90) { free = false; break; }
            }
            if (!free) continue;
            int r = NpcManager.rnd(100);
            v.type = (r < 40) ? VehicleKit.SEDAN : (r < 65) ? VehicleKit.PICKUP : (r < 80) ? VehicleKit.SPORTS : VehicleKit.MOTO;
            v.color = NpcManager.rnd(VehicleKit.colorCount(v.type));
            v.x = x;
            v.y = y;
            v.h = h;
            v.startAI(36 + NpcManager.rnd(14), (v.type == VehicleKit.MOTO || NpcManager.rnd(3) > 0) ? 1 : 2);
            return true;
        }
        return false;
    }

    /** Solid for people on foot: every vehicle that is not being driven. */
    public boolean hitParked(int px, int py) {
        for (int i = 0; i < list.length; i++) {
            Vehicle v = list[i];
            if (!v.driven && v.contains(px, py)) return true;
        }
        return false;
    }

    public boolean hitOther(int px, int py, Vehicle self) {
        for (int i = 0; i < list.length; i++) {
            Vehicle v = list[i];
            if (v != self && v.contains(px, py)) return true;
        }
        return false;
    }

    /** Closest parked vehicle whose edge is within maxD pixels, or null. */
    public Vehicle nearest(int px, int py, int maxD) {
        Vehicle best = null;
        int bestD = maxD * maxD;
        for (int i = 0; i < list.length; i++) {
            Vehicle v = list[i];
            if (v.driven) continue;
            int ex = Math.abs(px - v.x) - v.extX();
            int ey = Math.abs(py - v.y) - v.extY();
            if (ex < 0) ex = 0;
            if (ey < 0) ey = 0;
            int d = ex * ex + ey * ey;
            if (d <= bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
    }

    public void drawVehicle(Graphics g, Vehicle v, int camX, int camY) {
        int t = (v.driven && v.type == VehicleKit.MOTO) ? VehicleKit.MOTO_RIDER : v.type;
        Image im = VehicleKit.image(t, v.color, v.h);
        g.drawImage(im, v.x - im.getWidth() / 2 - camX, v.y - im.getHeight() / 2 - camY,
                Graphics.TOP | Graphics.LEFT);
    }
}
