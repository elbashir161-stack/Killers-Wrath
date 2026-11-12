import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Npc {
    public static final int NORMAL = 0, RECOGNISING = 1, FLEEING = 2, INSIDE = 3, STAND = 4, DEAD = 5;
    public static final int DOWN = 6, HURT = 7;
    public static final int TALKING = 8, DOOR_WAIT = 9;
    public static final int IDLE = 0, WALK = 1;
    public static final int P_NORMAL = 0, P_COWARD = 1, P_FROZEN = 2, P_BRAVE = 3, P_PANIC = 4;
    private static final int[] CYCLE = {1, 0, 2, 0};

    public int x, y, dir, frame, state, personality, look;
    public int homeX, homeY;
    public int fx, fy;
    private int animTick, timer, idle, walkT, wdx, wdy, stuck, rT, rdx, rdy, sayCool, bubbleT;
    private int door = -1;
    private boolean heardOnly;
    private String bubble;
    public int hp = 100;
    public boolean kid = false;
    private NpcManager mgr;
    public int hitsTaken = 0;
    public int downTimer = 0;

    // ---- Life - most stay home, few walk ----
    private int targetX, targetY;
    private boolean hasTarget = false;
    private int stuckCount = 0;
    private int lifeMode = 0; // 0 walk, 2 inside, 3 stand around home
    private int insideTimer = 0;
    private int doorWaitTimer = 0;
    private boolean wantsToGoInside = false;
    private boolean wantsToComeOut = false;
    public boolean doorOpenRequest = false;
    public boolean doorOpened = false;
    private boolean turningBack = false;
    private int turnBackTries = 0;
    private int turnBackX, turnBackY;

    // ---- Talking - small ... box ----
    public Npc talkPartner = null;
    public int talkTimer = 0;
    public int talkDotTimer = 0;
    public int talkDots = 1;
    public boolean talkedThisMeeting = false;
    public int walkAroundTimer = 0;
    public int walkAroundDir = 0;

    // ---- Fear - vampire ----
    public boolean isAfraid = false;
    public int speed = 1;
    public int animTimer = 0;

    // ---- Hit knockback ----
    private int knockX, knockY, knockT = 0;

    public Npc(int x, int y, int look, int personality) {
        this.x = x;
        this.y = y;
        this.look = look;
        this.personality = personality;
        this.dir = NpcManager.rnd(4);
        this.state = NORMAL;
        this.idle = NpcManager.rnd(40);
        this.homeX = x;
        this.homeY = y;
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
        knockT = 0;
        knockX = 0;
        knockY = 0;
        homeX = nx;
        homeY = ny;
        lifeMode = 0;
        insideTimer = 0;
        doorWaitTimer = 0;
        wantsToGoInside = false;
        wantsToComeOut = false;
        doorOpenRequest = false;
        doorOpened = false;
        turningBack = false;
        turnBackTries = 0;
        talkPartner = null;
        talkTimer = 0;
        talkDots = 1;
        talkedThisMeeting = false;
        walkAroundTimer = 0;
        isAfraid = false;
    }

    public void update(NpcManager m, World w, Player k) {
        mgr = m;
        bubble = null;
        bubbleT = 0;
        if (state == DEAD) return;

        // ---- DOOR WAIT - house animates door in this time ----
        if (state == DOOR_WAIT) {
            doorWaitTimer--;
            frame = 0;
            if (doorWaitTimer <= 0) {
                doorOpenRequest = false;
                doorOpened = true;
                if (wantsToGoInside) {
                    state = INSIDE;
                    insideTimer = 300 + NpcManager.rnd(600);
                    wantsToGoInside = false;
                    hasTarget = false;
                    if (door >= 0 && door < mgr.doorUse.length) mgr.doorUse[door]--;
                    door = -1;
                } else if (wantsToComeOut) {
                    state = NORMAL;
                    wantsToComeOut = false;
                    hasTarget = false;
                    pickLifeTarget(w);
                }
            }
            return;
        }

        // ---- INSIDE ----
        if (state == INSIDE) {
            insideTimer--;
            if (insideTimer <= 0) {
                int d = mgr.findDoor(homeX, homeY);
                if (d >= 0) {
                    door = d;
                    mgr.doorUse[d]++;
                    x = mgr.doorX[d];
                    y = mgr.doorY[d] + 20;
                } else {
                    x = homeX;
                    y = homeY + 20;
                }
                wantsToComeOut = true;
                doorOpenRequest = true;
                doorWaitTimer = 30;
                state = DOOR_WAIT;
            }
            return;
        }

        // ---- TALKING - dots animate . -> .. -> ... ----
        if (state == TALKING) {
            talkDotTimer++;
            if (talkDotTimer > 20) {
                talkDotTimer = 0;
                talkDots++;
                if (talkDots > 3) talkDots = 1;
            }
            talkTimer--;
            frame = 0;
            if (talkPartner != null) {
                if (talkPartner.x < x) dir = Sprites.LEFT;
                else if (talkPartner.x > x) dir = Sprites.RIGHT;
                else if (talkPartner.y < y) dir = Sprites.UP;
                else dir = Sprites.DOWN;
            }
            if (talkTimer <= 0) {
                state = NORMAL;
                if (talkPartner != null) {
                    talkPartner.state = NORMAL;
                    talkPartner.talkPartner = null;
                    talkPartner.talkedThisMeeting = true;
                    talkPartner.walkAroundTimer = 30;
                    talkPartner.walkAroundDir = NpcManager.rnd(2);
                }
                talkPartner = null;
                talkedThisMeeting = true;
                walkAroundTimer = 30;
                walkAroundDir = NpcManager.rnd(2);
                hasTarget = false;
            }
            return;
        }

        // ---- WALK AROUND after talking ----
        if (walkAroundTimer > 0) {
            walkAroundTimer--;
            int sx = (walkAroundDir == 0) ? -1 : 1;
            if (free(x + sx, y, w)) {
                x += sx;
                animTick++;
                frame = CYCLE[(animTick / 6) % 4];
            }
            if (walkAroundTimer <= 0) hasTarget = false;
            return;
        }

        // ---- Knockback slide ----
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
            if (timer % 2 == 0) walk(w, wdx, wdy);
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
        // ---- Fear - vampire check is in WorldCanvas, here just distance ----
        // If your WorldCanvas passes playerIsVampire, it is handled there.
        // Here we keep normal walk.

        if (idle > 0) {
            idle--;
            frame = 0;
            return;
        }

        // ---- TALKING CHECK - before walking around each other ----
        if (!talkedThisMeeting && mgr != null && mgr.npcs != null) {
            for (int i = 0; i < mgr.npcs.length; i++) {
                Npc other = mgr.npcs[i];
                if (other == this) continue;
                if (other.state == TALKING || other.state == INSIDE || other.state == DEAD || other.state == DOWN || other.state == HURT) continue;
                int dx = other.x - x;
                if (dx < 0) dx = -dx;
                int dy = other.y - y;
                if (dy < 0) dy = -dy;
                if (dx < 20 && dy < 20) {
                    if (NpcManager.rnd(10) < 7) {
                        state = TALKING;
                        talkPartner = other;
                        talkTimer = 180 + NpcManager.rnd(60);
                        talkDotTimer = 0;
                        talkDots = 1;
                        other.state = TALKING;
                        other.talkPartner = this;
                        other.talkTimer = talkTimer;
                        other.talkDotTimer = 0;
                        other.talkDots = 1;
                        hasTarget = false;
                        other.hasTarget = false;
                        return;
                    } else {
                        talkedThisMeeting = true;
                        walkAroundTimer = 25;
                        walkAroundDir = NpcManager.rnd(2);
                        return;
                    }
                }
            }
        }
        if (talkPartner == null) {
            boolean stillClose = false;
            if (mgr != null && mgr.npcs != null) {
                for (int i = 0; i < mgr.npcs.length; i++) {
                    Npc o = mgr.npcs[i];
                    if (o == this) continue;
                    int dx = o.x - x;
                    if (dx < 0) dx = -dx;
                    int dy = o.y - y;
                    if (dy < 0) dy = -dy;
                    if (dx < 40 && dy < 40) stillClose = true;
                }
            }
            if (!stillClose) talkedThisMeeting = false;
        }

        // ---- TURNING BACK from fence / wall ----
        if (turningBack) {
            int bdx = turnBackX - x;
            int bdy = turnBackY - y;
            if (abs(bdx) < 10 && abs(bdy) < 10) {
                turningBack = false;
                hasTarget = false;
                return;
            }
            int mx = 0, my = 0;
            if (abs(bdx) > abs(bdy)) mx = (bdx > 0) ? 1 : -1;
            else my = (bdy > 0) ? 1 : -1;
            if (walk(w, mx, my)) {
                animTick++;
                frame = CYCLE[(animTick / 6) % 4];
            } else {
                turningBack = false;
                hasTarget = false;
            }
            return;
        }

        if (!hasTarget) {
            pickLifeTarget(w);
            if (!hasTarget) {
                idle = 20;
                return;
            }
        }

        int dxToTarget = targetX - x;
        int dyToTarget = targetY - y;
        if (abs(dxToTarget) < 10 && abs(dyToTarget) < 10) {
            if (lifeMode == 2) {
                int d = mgr.findDoor(x, y);
                if (d >= 0) {
                    door = d;
                    mgr.doorUse[d]++;
                    wantsToGoInside = true;
                    doorOpenRequest = true;
                    doorWaitTimer = 30;
                    state = DOOR_WAIT;
                    return;
                } else {
                    wantsToGoInside = true;
                    doorOpenRequest = true;
                    doorWaitTimer = 30;
                    state = DOOR_WAIT;
                    return;
                }
            }
            hasTarget = false;
            if (lifeMode == 3) {
                idle = 300 + NpcManager.rnd(400);
                if (NpcManager.rnd(3) == 0) dir = NpcManager.rnd(4);
            } else if (NpcManager.rnd(6) == 0) {
                idle = 20 + NpcManager.rnd(60);
            } else {
                idle = 10;
            }
            frame = 0;
            return;
        }

        int wantX = targetX;
        int wantY = targetY;
        int speed = 1;
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
            if (adx > ady) my2 = (wantY > y) ? speed : -speed;
            else mx2 = (wantX > x) ? speed : -speed;
            if (walk(w, mx2, my2)) {
                animTick++;
                frame = CYCLE[(animTick / 6) % 4];
                stuckCount = 0;
            } else {
                stuckCount++;
                if (stuckCount > 30) {
                    stuckCount = 0;
                    turnBackTries++;
                    if (turnBackTries > 2) {
                        turnBackTries = 0;
                        hasTarget = false;
                        idle = 8;
                        frame = 0;
                    } else {
                        turningBack = true;
                        int backX = x;
                        int backY = y;
                        if (adx > ady) backX = x - ((wantX > x) ? 120 : -120);
                        else backY = y - ((wantY > y) ? 120 : -120);
                        if (backX == x && backY == y) backX = x - 120;
                        turnBackX = backX;
                        turnBackY = backY;
                    }
                }
            }
        }
    }

    private void pickLifeTarget(World w) {
        int roll = NpcManager.rnd(100);
        if (roll < 50) {
            lifeMode = 2;
            int d = (mgr != null) ? mgr.findDoor(x, y) : -1;
            if (d >= 0) {
                targetX = mgr.doorX[d];
                targetY = mgr.doorY[d] + 10;
                hasTarget = true;
                return;
            }
            lifeMode = 3;
            targetX = homeX + NpcManager.rnd(60) - 30;
            targetY = homeY + NpcManager.rnd(60) - 30;
            if (free(targetX, targetY, w)) {
                hasTarget = true;
                return;
            }
            hasTarget = false;
            return;
        } else if (roll < 85) {
            lifeMode = 3;
            for (int t = 0; t < 20; t++) {
                int tx = homeX + NpcManager.rnd(60) - 30;
                int ty = homeY + NpcManager.rnd(60) - 30;
                if (!free(tx, ty, w)) continue;
                targetX = tx;
                targetY = ty;
                hasTarget = true;
                return;
            }
            hasTarget = false;
            return;
        } else {
            lifeMode = 0;
            for (int t = 0; t < 20; t++) {
                int dist = 100 + NpcManager.rnd(101);
                int angle = NpcManager.rnd(4);
                int tx = homeX;
                int ty = homeY;
                if (angle == 0) tx += dist;
                else if (angle == 1) tx -= dist;
                else if (angle == 2) ty += dist;
                else ty -= dist;
                if (!free(tx, ty, w)) continue;
                if (abs(tx - x) < 60 && abs(ty - y) < 60) continue;
                targetX = tx;
                targetY = ty;
                hasTarget = true;
                return;
            }
            hasTarget = false;
            return;
        }
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

    // ---- ONLY javax methods here - copied from your outdated file ----
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

    // ---- TALKING BOX - only javax ----
    public void drawBubble(Graphics g, Font f, int camX, int camY) {
        if (state != TALKING) return;
        if (state == INSIDE || state == DEAD) return;
        String dots = ".";
        if (talkDots == 2) dots = "..";
        else if (talkDots >= 3) dots = "...";
        int bx = x - 5 - camX;
        int by = y - 28 - camY;
        int boxW = 26;
        int boxH = 14;
        g.setColor(0xFFFFFF);
        g.fillRect(bx, by, boxW, boxH);
        g.setColor(0x000000);
        g.drawRect(bx, by, boxW, boxH);
        g.setFont(f);
        g.drawString(dots, bx + 6, by + 1, Graphics.TOP | Graphics.LEFT);
    }
}
