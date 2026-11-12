import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Npc {
    public static final int NORMAL = 0, RECOGNISING = 1, FLEEING = 2, INSIDE = 3, STAND = 4, DEAD = 5;
    public static final int DOWN = 6, HURT = 7;
    public static final int P_NORMAL = 0, P_COWARD = 1, P_FROZEN = 2, P_BRAVE = 3, P_PANIC = 4;
    private static final int[] CYCLE = {1, 0, 2, 0};

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

    // NEW: simple life, no zebra obsession
    private int targetX, targetY;
    private boolean hasTarget = false;
    private int stuckCount = 0;
    private int knockX, knockY, knockT = 0;
    private int persistMx = 0, persistMy = 0;
    private int persistT = 0;
    private int homeX, homeY; // where this person lives
    private int insideTimer = 0;
    private int lifeMode = 0; // 0 = wander near home, 1 = go far, 2 = go inside house

    public Npc(int x, int y, int look, int personality) {
        this.x = x;
        this.y = y;
        this.homeX = x;
        this.homeY = y;
        this.look = look;
        this.personality = personality;
        this.dir = NpcManager.rnd(4);
        this.state = NORMAL;
        this.idle = NpcManager.rnd(40);
    }

    public void reset(int nx, int ny) {
        x = nx;
        y = ny;
        homeX = nx;
        homeY = ny;
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
        knockT = 0;
        knockX = 0;
        knockY = 0;
        persistMx = 0;
        persistMy = 0;
        persistT = 0;
        insideTimer = 0;
        lifeMode = 0;
    }

    public void update(NpcManager m, World w, Player k) {
        mgr = m;
        bubble = null;
        bubbleT = 0;
        if (state == DEAD) return;

        // Inside house - hidden for a while, like they went home
        if (state == INSIDE) {
            insideTimer--;
            if (insideTimer <= 0) {
                state = NORMAL;
                hasTarget = false;
                idle = 10;
                // Come out in front of house
                x = homeX + NpcManager.rnd(60) - 30;
                y = homeY + NpcManager.rnd(60) - 30;
            }
            return;
        }

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

    private void updateNormal(World w, Player k) {
        if (idle > 0) {
            idle--;
            frame = 0;
            return;
        }

        if (!hasTarget) {
            pickLifeTarget(w);
            if (!hasTarget) {
                idle = 20;
                return;
            }
        }

        // Reached target?
        if (abs(targetX - x) < 10 && abs(targetY - y) < 10) {
            hasTarget = false;
            // If we were going home, go inside
            if (lifeMode == 2) {
                state = INSIDE;
                insideTimer = 300 + NpcManager.rnd(600); // stay inside 5-15 seconds
                return;
            }
            // Otherwise stand, look around, like they have something to do
            idle = 30 + NpcManager.rnd(120);
            frame = 0;
            // Sometimes change direction like looking around
            if (NpcManager.rnd(3) == 0) dir = NpcManager.rnd(4);
            return;
        }

        int speed = 1;
        // If they are far from home, walk a bit faster to get there
        if (lifeMode == 1) speed = 2;

        if (persistT <= 0) {
            int adx = abs(targetX - x);
            int ady = abs(targetY - y);
            if (adx > ady) {
                persistMx = (targetX > x) ? speed : -speed;
                persistMy = 0;
            } else {
                persistMx = 0;
                persistMy = (targetY > y) ? speed : -speed;
            }
            persistT = 15;
        }
        persistT--;

        if (tryStep(w, persistMx, persistMy)) {
            animTick++;
            frame = CYCLE[(animTick / 6) % 4];
            stuckCount = 0;
            return;
        }

        // Try other axis
        int otherMx = 0, otherMy = 0;
        if (persistMx != 0) {
            otherMy = (targetY > y) ? speed : -speed;
        } else {
            otherMx = (targetX > x) ? speed : -speed;
        }
        if (tryStep(w, otherMx, otherMy)) {
            animTick++;
            frame = CYCLE[(animTick / 6) % 4];
            stuckCount = 0;
            persistMx = otherMx;
            persistMy = otherMy;
            persistT = 15;
            return;
        }

        // Stuck - don't vibrate, just pick a new life target
        stuckCount++;
        frame = 0;
        if (stuckCount > 25) {
            stuckCount = 0;
            persistT = 0;
            hasTarget = false;
            idle = 10;
        }
    }

    private boolean tryStep(World w, int mx, int my) {
        if (mx == 0 && my == 0) return false;
        int nx = x + mx;
        int ny = y + my;
        // NEW: can walk ANYWHERE that is not solid - road, walkway, compound, in front of house
        // No more "only sidewalk" rule. That was the problem.
        if (!free(nx, ny, w)) return false;
        return walk(w, mx, my);
    }

    // NEW LIFE SYSTEM
    private void pickLifeTarget(World w) {
        int roll = NpcManager.rnd(100);
        for (int t = 0; t < 30; t++) {
            int tx, ty;
            if (roll < 70) {
                // 70% - Wander near home: around houses, compound, front of house
                // Stay within 250px of home, so they look like they live there
                tx = homeX + NpcManager.rnd(501) - 250;
                ty = homeY + NpcManager.rnd(501) - 250;
                lifeMode = 0;
            } else if (roll < 85) {
                // 15% - Go home / enter house
                tx = homeX + NpcManager.rnd(40) - 20;
                ty = homeY + NpcManager.rnd(40) - 20;
                lifeMode = 2;
            } else {
                // 15% - Go far, to another area - this is the ONLY time they cross road
                // They just walk straight across, they don't hunt for zebra
                tx = NpcManager.rnd(10000);
                ty = NpcManager.rnd(5500);
                lifeMode = 1;
                // New home after far trip - they moved area, so they don't ping-pong back
                homeX = tx;
                homeY = ty;
            }
            if (tx < 0 || ty < 0) continue;
            if (!free(tx, ty, w)) continue;
            // Don't pick a target too close, otherwise they do 2 steps back and forth
            if (abs(tx - x) < 60 && abs(ty - y) < 60) continue;
            targetX = tx;
            targetY = ty;
            hasTarget = true;
            persistT = 0;
            return;
        }
        hasTarget = false;
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
