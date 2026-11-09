import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Parked vehicles in car-park bays around Killer; far ones are recycled nearby. */
public class VehicleManager {
    public final Vehicle[] list;
    private final World world;
    private int tick = 0;

    public VehicleManager(World w, Player k, int count, boolean starters) {
        world = w;
        list = new Vehicle[count];
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
        int dx = v.x - k.x, dy = v.y - k.y;
        if (dx * dx + dy * dy > 1400 * 1400) place(v, k, 450, 1100);
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
