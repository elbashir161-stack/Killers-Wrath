import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Parked + traffic vehicles. Traffic cars have 1 or 2 people inside,
 *  wait for pedestrians, and never kill - they push the person away. */
public class VehicleManager {
    public final Vehicle[] list;
    private final World world;
    private int tick = 0;

    // --- traffic + occupants ---
    private final int[] occupants; // 1 or 2 people inside
    private final boolean[] isTraffic;
    private final int[] trafficDir; // -1 left, 1 right
    private final int[] trafficSpeed;
    private final int[] waitTimer; // how long car waits
    private final int[] hitCooldown; // don't hit same person every frame

    public VehicleManager(World w, Player k, int count, boolean starters) {
        world = w;
        list = new Vehicle[count];
        occupants = new int[count];
        isTraffic = new boolean[count];
        trafficDir = new int[count];
        trafficSpeed = new int[count];
        waitTimer = new int[count];
        hitCooldown = new int[count];

        int n = 0;
        // starter vehicles parked along the avenue beside Killer's start
        if (starters && count >= 4) {
            list[n] = new Vehicle(VehicleKit.MOTO, 0, Vehicle.HUP, k.x + 20, k.y + 30);
            occupants[n] = 1; n++;
            list[n] = new Vehicle(VehicleKit.SEDAN, 0, Vehicle.HUP, k.x + 24, k.y - 80);
            occupants[n] = 2; n++;
            list[n] = new Vehicle(VehicleKit.SPORTS, 1, Vehicle.HUP, k.x + 24, k.y + 160);
            occupants[n] = 1; n++;
            list[n] = new Vehicle(VehicleKit.PICKUP, 3, Vehicle.HUP, k.x + 24, k.y - 200);
            occupants[n] = 2; n++;
        }
        for (int i = n; i < count; i++) {
            int r = NpcManager.rnd(100);
            int type = (r < 40) ? VehicleKit.SEDAN : (r < 60) ? VehicleKit.SPORTS : (r < 80) ? VehicleKit.PICKUP : VehicleKit.MOTO;
            Vehicle v = new Vehicle(type, NpcManager.rnd(VehicleKit.colorCount(type)), Vehicle.HUP, -1000, -1000);
            list[i] = v;
            // 1 or 2 people inside - random, like real traffic
            occupants[i] = (NpcManager.rnd(2) == 0) ? 1 : 2;

            // Half the far cars become TRAFFIC cars driving on road
            if (i % 2 == 0) {
                makeTraffic(v, i, k);
            } else {
                place(v, k, 150, 1100);
            }
        }
    }

    private void makeTraffic(Vehicle v, int idx, Player k) {
        isTraffic[idx] = true;
        trafficDir[idx] = (NpcManager.rnd(2) == 0) ? -1 : 1;
        trafficSpeed[idx] = 2 + NpcManager.rnd(2); // 2 or 3 - slow, safe
        // Put car on a road lane near player, not on top of player
        int lane = NpcManager.rnd(3); // 3 lanes
        int laneY = k.y + (lane - 1) * 48;
        int laneX = k.x + (NpcManager.rnd(2) == 0 ? -1 : 1) * (200 + NpcManager.rnd(500));
        v.x = laneX;
        v.y = laneY;
        v.h = (trafficDir[idx] == 1) ? Vehicle.HUP : Vehicle.HDOWN;
        v.sp = trafficSpeed[idx];
        v.driven = false;
        // MOTO only has 1 person
        if (v.type == VehicleKit.MOTO) occupants[idx] = 1;
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

        // --- TRAFFIC: every frame, move traffic cars ---
        for (int i = 0; i < list.length; i++) {
            if (!isTraffic[i]) continue;
            Vehicle v = list[i];
            if (v == null) continue;
            if (hitCooldown[i] > 0) hitCooldown[i]--;

            // 1. Is a walking person in front? -> WAIT, common sense
            boolean personAhead = isPersonAhead(v, i, k);
            if (personAhead) {
                waitTimer[i] = 20; // wait a bit
                v.sp = 0;
            } else if (waitTimer[i] > 0) {
                waitTimer[i]--;
                v.sp = 0;
            } else {
                v.sp = trafficSpeed[i];
            }

            // 2. Move only if not waiting
            if (v.sp > 0) {
                v.x += trafficDir[i] * v.sp;
            }

            // 3. If we still touched the player -> NOT death, push away + injury, car stops
            if (hitCooldown[i] == 0 && v.contains(k.x, k.y)) {
                // Push player away from car, to the side
                int pushX = trafficDir[i] * 18;
                int pushY = (k.y >= v.y) ? 22 : -22;
                k.x += pushX;
                k.y += pushY;
                // Car stops after hit, like real driver
                v.sp = 0;
                waitTimer[i] = 40;
                hitCooldown[i] = 60;
                // NOTE: Player never dies here. Your Player/Npc code should
                // make him fall / stay down 3 sec, then walk slow. We only push.
            }

            // 4. Recycle far traffic cars near player again
            int dx = v.x - k.x, dy = v.y - k.y;
            if (dx * dx + dy * dy > 1400 * 1400) {
                makeTraffic(v, i, k);
            }
        }

        // --- PARKED: your old recycle, every 4th tick ---
        if (tick % 4 != 0) return;
        Vehicle v = list[(tick / 4) % list.length];
        if (v.driven || isTraffic[(tick / 4) % list.length]) return;
        int dx = v.x - k.x, dy = v.y - k.y;
        if (dx * dx + dy * dy > 1400 * 1400) place(v, k, 450, 1100);
    }

    /** True if player is in front of this traffic car, close enough to wait for. */
    private boolean isPersonAhead(Vehicle v, int idx, Player k) {
        int dir = trafficDir[idx];
        int dx = k.x - v.x;
        // Only if player is in front direction
        if (dir == 1 && dx < 0) return false;
        if (dir == -1 && dx > 0) return false;
        if (dx < -55 || dx > 55) return false;
        int dy = k.y - v.y;
        if (dy < -22 || dy > 22) return false;
        return true;
    }

    /** For your NpcManager to use: should a walking NPC be waited for? */
    public boolean shouldWaitForNpc(int carIdx, int npcX, int npcY) {
        if (carIdx < 0 || carIdx >= list.length) return false;
        if (!isTraffic[carIdx]) return false;
        Vehicle v = list[carIdx];
        int dir = trafficDir[carIdx];
        int dx = npcX - v.x;
        if (dir == 1 && dx < 0) return false;
        if (dir == -1 && dx > 0) return false;
        if (dx < -55 || dx > 55) return false;
        int dy = npcY - v.y;
        if (dy < -22 || dy > 22) return false;
        return true;
    }

    /** For your NpcManager to use: push NPC away if hit, returns true if hit. */
    public boolean knockNpcAway(int carIdx, int[] npcPos) {
        // npcPos = {x, y}, we push it and return true
        if (carIdx < 0 || carIdx >= list.length) return false;
        Vehicle v = list[carIdx];
        if (!isTraffic[carIdx]) return false;
        if (hitCooldown[carIdx] > 0) return false;
        int nx = npcPos[0], ny = npcPos[1];
        if (!v.contains(nx, ny)) return false;
        npcPos[0] = nx + trafficDir[carIdx] * 18;
        npcPos[1] = ny + ((ny >= v.y) ? 22 : -22);
        waitTimer[carIdx] = 40;
        hitCooldown[carIdx] = 60;
        return true;
    }

    /** How many people inside this car? 1 or 2 */
    public int occupantsOf(int idx) {
        if (idx < 0 || idx >= occupants.length) return 1;
        return occupants[idx];
    }

    /** Solid for people on foot: every vehicle that is not being driven. */
    public boolean hitParked(int px, int py) {
        for (int i = 0; i < list.length; i++) {
            Vehicle v = list[i];
            if (!v.driven && !isTraffic[i] && v.contains(px, py)) return true;
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
        int sx = v.x - im.getWidth() / 2 - camX;
        int sy = v.y - im.getHeight() / 2 - camY;
        g.drawImage(im, sx, sy, Graphics.TOP | Graphics.LEFT);

        // Draw heads inside the car: 1 or 2 people
        int idx = -1;
        for (int i = 0; i < list.length; i++) if (list[i] == v) { idx = i; break; }
        if (idx >= 0) {
            int people = occupants[idx];
            int cx = v.x - camX;
            int cy = v.y - camY;
            g.setColor(0xFFCC99); // skin color for head
            if (people <= 1) {
                g.fillArc(cx - 4, cy - 5, 8, 8, 0, 360);
            } else {
                g.fillArc(cx - 10, cy - 5, 8, 8, 0, 360); // driver
                g.fillArc(cx + 2, cy - 5, 8, 8, 0, 360);  // passenger
            }
            // Dark hair on top
            g.setColor(0x000000);
            if (people <= 1) {
                g.fillArc(cx - 4, cy - 5, 8, 4, 0, 360);
            } else {
                g.fillArc(cx - 10, cy - 5, 8, 4, 0, 360);
                g.fillArc(cx + 2, cy - 5, 8, 4, 0, 360);
            }
        }
    }
}
