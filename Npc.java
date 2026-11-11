import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Npc {
    public static final int NORMAL = 0, RECOGNISING = 1, FLEEING = 2, INSIDE = 3, STAND = 4, DEAD = 5;
    public static final int DOWN = 6, HURT = 7;
    public static final int P_NORMAL = 0, P_COWARD = 1, P_FROZEN = 2, P_BRAVE = 3, P_PANIC = 4;
    private static final int[] CYCLE = {1, 0, 2, 0};

    // ---- REAL ZEBRA CROSSINGS FROM YOUR World.java ----
    // Calculated from VX and HY in World.java. Do not change.
    private static final int[][] CROSSINGS = {
        {2640, 1488},
        {4592, 1488},
        {6192, 1488},
        {7536, 1488},
        {2640, 2640},
        {4592, 2640},
        {6192, 2640},
        {7536, 2640},
        {2640, 3792},
        {4592, 3792},
        {6192, 3792},
        {2640, 4960},
        {4592, 4960},
        {6192, 4960},
    };

    public int x, y, dir, frame, state, personality, look;
    public int fx, fy;
    private int animTick, timer, idle, walkT, wdx, wdy, style, stuck, rT, rdx, rdy, sayCool, bubbleT;
    private int door = -1;
    private boolean heardOnly;
    private String bubble;
    public int hp = 100;
    public boolean kid = false;
    private NpcManager mgr;
    public int hitsTaken = 0;
    public int downTimer = 0;

    // ---- Far target walk ----
    private int targetX, targetY;
    private boolean hasTarget = false;
    private int stuckCount = 0;
    private int crossingIndex = -1;
    private boolean goingToCrossing = false;
    private boolean crossingRoad = false;

    // ---- Real hit knockback ----
    private int knockX, knockY, knockT = 0;

    public Npc(int x, int y, int look, int personality) {
        this.x = x;
        this.y = y;
        this.look = look;
        this.personality = personality;
        this.dir = NpcManager.rnd(4);
        this.state = NORMAL;
        this.idle = NpcManager.rnd(40);
    }

    public void reset(int nx, int ny) {
        x = nx;
        y = ny;
        state = NORMAL;
        idle = NpcManager.rnd(40);
        walkT = 0;
        frame = 0;
        bubble = null;
        bubbleT = 0;
        door = -1;
        hp = 100;
        hitsTaken = 0;
        downTimer = 0;
        hasTarget = false;
        goingToCrossing = false;
        crossingRoad = false;
        crossingIndex = -1;
        knockT = 0;
        knockX = 0;
        knockY = 0;
    }

    public void update(NpcManager m, World w, Player k) {
        mgr = m;
        bubble = null;
        bubbleT = 0;
        if (state == DEAD) return;

        // ---- Knockback slide - makes the HIT look real ----
        if (knockT > 0) {
            knockT--;
            for (int i = 0; i < 2; i++) {
                int nx = x + knockX;
                int ny = y + knockY;
                if (free(nx, ny, w)) {
                    x = nx;
                    y = ny;
                } else {
                    knockT = 0;
                    break;
                }
            }
            if (knockT % 3 == 0) {
                if (knockX > 0) knockX--;
                else if (knockX < 0) knockX++;
                if (knockY > 0) knockY--;
                else if (knockY < 0) knockY++;
            }
            return;
        }

        if (state == DOWN) {
            downTimer--;
            if (downTimer <= 0) {
                if (hitsTaken >= maxHits() - 1) {
                    state = HURT;
                    timer = 120;
                } else {
                    state = FLEEING;
                    timer = 150;
                    pickEscapeDir(w);
                }
                frame = 0;
            }
            return;
        }

        if (state == HURT) {
            timer--;
            if (timer <= 0) {
                state = FLEEING;
                timer = 150;
                pickEscapeDir(w);
            }
            if (timer % 2 == 0) {
                walk(w, wdx, wdy);
            }
            return;
        }

        if (state == FLEEING) {
            timer--;
            if (timer <= 0) {
                state = NORMAL;
                idle = 20;
                return;
            }
            if (walk(w, wdx * 2, wdy * 2)) {
                animTick++;
                frame = CYCLE[(animTick / 4) % 4];
            } else {
                pickEscapeDir(w);
            }
            return;
        }

        state = NORMAL;
        updateNormal(w, k);
    }

    private int maxHits() {
        return kid ? 2 : 3;
    }

    private void pickEscapeDir(World w) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 0; i < 4; i++) {
            int d = NpcManager.rnd(4);
            if (free(x + dirs[d][0] * 6, y + dirs[d][1] * 6, w)) {
                wdx = dirs[d][0];
                wdy = dirs[d][1];
                return;
            }
        }
        wdx = 1;
        wdy = 0;
    }

    // ---- Far target walking, sidewalk + zebra ----
    private void updateNormal(World w, Player k) {
        if (idle > 0) {
            idle--;
            frame = 0;
            return;
        }

        if (!hasTarget) {
            pickFarTarget(w);
            if (!hasTarget) {
                idle = 20;
                return;
            }
        }

        int dxToTarget = targetX - x;
        int dyToTarget = targetY - y;
        if (abs(dxToTarget) < 6 && abs(dyToTarget) < 6) {
            if (goingToCrossing) {
                goingToCrossing = false;
                crossingRoad = true;
                targetX = fx;
                targetY = fy;
            } else if (crossingRoad) {
                crossingRoad = false;
                hasTarget = false;
                idle = 20 + NpcManager.rnd(60);
                return;
            } else {
                hasTarget = false;
                if (NpcManager.rnd(6) == 0) {
                    idle = 20 + NpcManager.rnd(60);
                } else {
                    idle = 10;
                }
                frame = 0;
                return;
            }
        }

        int wantX = targetX;
        int wantY = targetY;
        int speed = 1;
        if (crossingRoad) {
            speed = 2;
        }

        int mx = 0, my = 0;
        int adx = abs(wantX - x);
        int ady = abs(wantY - y);
        if (adx > ady) {
            mx = (wantX > x) ? speed : -speed;
            my = 0;
        } else {
            mx = 0;
            my = (wantY > y) ? speed : -speed;
        }

        if (walk(w, mx, my)) {
            animTick++;
            frame = CYCLE[(animTick / 6) % 4];
            stuckCount = 0;
            walkT = 1;
        } else {
            int mx2 = 0, my2 = 0;
            if (adx > ady) {
                my2 = (wantY > y) ? speed : -speed;
            } else {
                mx2 = (wantX > x) ? speed : -speed;
            }
            if (walk(w, mx2, my2)) {
                animTick++;
                frame = CYCLE[(animTick / 6) % 4];
                stuckCount = 0;
            } else {
                stuckCount++;
                if (stuckCount > 30) {
                    stuckCount = 0;
                    hasTarget = false;
                    goingToCrossing = false;
                    crossingRoad = false;
                    idle = 8;
                    frame = 0;
                }
            }
        }
    }

    private void pickFarTarget(World w) {
        for (int t = 0; t < 20; t++) {
            int tx = NpcManager.rnd(800);
            int ty = NpcManager.rnd(800);
            int dx = tx - x;
            int dy = ty - y;
            if (abs(dx) < 200 && abs(dy) < 200) continue;
            if (!free(tx, ty, w)) continue;

            int cross = findCrossingFor(tx, ty);
            if (cross >= 0 && !isNearCrossing(x, y)) {
                fx = tx;
                fy = ty;
                targetX = CROSSINGS[cross][0];
                targetY = CROSSINGS[cross][1];
                crossingIndex = cross;
                goingToCrossing = true;
                crossingRoad = false;
                hasTarget = true;
                return;
            }

            targetX = tx;
            targetY = ty;
            hasTarget = true;
            goingToCrossing = false;
            crossingRoad = false;
            return;
        }
        hasTarget = false;
    }

    private int findCrossingFor(int tx, int ty) {
        if (CROSSINGS.length == 0) return -1;
        if (abs(tx - x) < 150 && abs(ty - y) < 150) return -1;
        int best = -1;
        int bestD = 999999;
        for (int i = 0; i < CROSSINGS.length; i++) {
            int dx = CROSSINGS[i][0] - x;
            int dy = CROSSINGS[i][1] - y;
            int d = abs(dx) + abs(dy);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    private boolean isNearCrossing(int px, int py) {
        for (int i = 0; i < CROSSINGS.length; i++) {
            if (abs(CROSSINGS[i][0] - px) < 20 && abs(CROSSINGS[i][1] - py) < 20) return true;
        }
        return false;
    }

    public void startRecognise(int sx, int sy, boolean heard, int delay) {
    }

    public void hitByCar(int carSpeed, NpcManager m, int sx, int sy) {
        if (state == DEAD || state == INSIDE) return;
        int hits = 1;
        if (carSpeed >= 40) hits = 2;
        for (int i = 0; i < hits; i++) {
            hurt(40, m, sx, sy);
            if (state == DEAD) break;
            if (state == DOWN && hits == 2 && i == 0) break;
        }
    }

    public void hurt(int dmg, NpcManager m, int sx, int sy) {
        if (state == DEAD || state == INSIDE) return;
        hitsTaken++;
        hp -= dmg;
        m.addBlood(x, y - 6, dmg >= 60 ? 2 : (dmg >= 30 ? 1 : 0));

        // ---- REAL HIT - push them away from the car ----
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            dx = (dir == Sprites.LEFT) ? -1 : (dir == Sprites.RIGHT ? 1 : 0);
            dy = (dir == Sprites.UP) ? -1 : (dir == Sprites.DOWN ? 1 : 0);
            if (dx == 0 && dy == 0) dx = 1;
        }
        if (dx > 0) dx = 1; else if (dx < 0) dx = -1;
        if (dy > 0) dy = 1; else if (dy < 0) dy = -1;

        int power = 4;
        if (dmg >= 40) power = 6;
        if (hitsTaken > 1) power += 2;

        knockX = dx * power;
        knockY = dy * power;
        knockT = 10;

        hasTarget = false;
        goingToCrossing = false;
        crossingRoad = false;

        if (hitsTaken >= maxHits()) {
            state = DEAD;
            frame = 0;
            bubble = null;
            m.kills++;
            knockT = 12;
            return;
        }
        state = DOWN;
        downTimer = 75;
        frame = 0;
        bubble = null;
    }

    private boolean walk(World w, int mx, int my) {
        boolean movedX = false, movedY = false;
        if (mx != 0) movedX = moveAxis(w, mx < 0 ? -1 : 1, 0, abs(mx));
        if (my != 0) movedY = moveAxis(w, 0, my < 0 ? -1 : 1, abs(my));
        if (movedX) dir = (mx < 0) ? Sprites.LEFT : Sprites.RIGHT;
        else if (movedY) dir = (my < 0) ? Sprites.UP : Sprites.DOWN;
        return movedX || movedY;
    }

    private boolean moveAxis(World w, int dx, int dy, int steps) {
        boolean moved = false;
        for (int i = 0; i < steps; i++) {
            int nx = x + dx, ny = y + dy;
            if (free(nx, ny, w)) {
                x = nx;
                y = ny;
                moved = true;
            } else {
                break;
            }
        }
        return moved;
    }

    private boolean free(int nx, int ny, World w) {
        boolean inside = w.vehicles != null && w.vehicles.hitParked(x, y - 2);
        if (blockedAt(nx - 4, ny - 5, w, inside) || blockedAt(nx + 3, ny - 5, w, inside)
                || blockedAt(nx - 4, ny - 1, w, inside) || blockedAt(nx + 3, ny - 1, w, inside)) return false;
        return mgr == null || !mgr.blocks(nx, ny, x, y, this);
    }

    private boolean blockedAt(int px, int py, World w, boolean ignoreVehicles) {
        return ignoreVehicles ? w.solidTerrain(px, py) : w.solidAt(px, py);
    }

    private static int abs(int v) { return v < 0 ? -v : v; }

    public void draw(Graphics g, int camX, int camY) {
        if (state == INSIDE || state == DEAD) return;
        if (kid) {
            Image ki = NpcSprites.kids[look][dir][frame];
            g.drawImage(ki, x - ki.getWidth() / 2 - camX, y - (ki.getHeight() - 1) - camY,
                    Graphics.TOP | Graphics.LEFT);
            return;
        }
        g.drawImage(NpcSprites.looks[look][dir][frame], x - NpcSprites.W / 2 - camX, y - (NpcSprites.H - 2) - camY,
                Graphics.TOP | Graphics.LEFT);
    }

    public void drawBubble(Graphics g, Font f, int camX, int camY) {
    }
}
