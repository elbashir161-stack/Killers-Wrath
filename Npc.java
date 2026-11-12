package game;

import java.awt.Graphics;
import java.awt.Color;

public class Npc {

    public static final int IDLE = 0;
    public static final int WALK = 1;
    public static final int INSIDE = 2;
    public static final int TALKING = 3;
    public static final int DOOR_WAIT = 4;

    public int x, y;
    public int homeX, homeY;
    public int dir = 2; // 0 up, 1 down, 2 left, 3 right
    public int state = IDLE;
    public int frame = 0;
    public int idle = 0;

    public int targetX, targetY;
    public boolean hasTarget = false;
    public int lifeMode = 0; // 0 walk, 2 inside, 3 stand around home

    // Turn back for fence / wall
    public boolean turningBack = false;
    public int turnBackTries = 0;
    public int turnBackX, turnBackY;

    // Inside
    public int insideTimer = 0;
    public boolean wantsToGoInside = false;
    public boolean wantsToComeOut = false;

    // Door trigger for house to animate
    public boolean doorOpenRequest = false;
    public int doorWaitTimer = 0;
    public boolean doorOpened = false;

    // Talking
    public Npc talkPartner = null;
    public int talkTimer = 0;
    public int talkDotTimer = 0;
    public int talkDots = 1; // 1 = ., 2 = .., 3 = ...
    public boolean talkedThisMeeting = false;
    public int walkAroundTimer = 0;
    public int walkAroundDir = 0;

    // Fear
    public boolean isAfraid = false;

    public int speed = 1;
    public int animTimer = 0;

    public Npc(int startX, int startY, int homeX, int homeY) {
        this.x = startX;
        this.y = startY;
        this.homeX = homeX;
        this.homeY = homeY;
    }

    // ============ MAIN UPDATE ============
    public void update(World w, NpcManager manager, Player k, boolean playerIsVampire) {
        // Door wait
        if (state == DOOR_WAIT) {
            doorWaitTimer--;
            if (doorWaitTimer <= 0) {
                doorOpenRequest = false;
                doorOpened = true;
                // After door opens, finish going in / out
                if (wantsToGoInside) {
                    state = INSIDE;
                    insideTimer = 300 + NpcManager.rnd(600);
                    wantsToGoInside = false;
                    hasTarget = false;
                } else if (wantsToComeOut) {
                    state = IDLE;
                    wantsToComeOut = false;
                    hasTarget = false;
                    pickLifeTarget(w);
                }
            }
            return;
        }

        // Inside timer
        if (state == INSIDE) {
            insideTimer--;
            if (insideTimer <= 0) {
                // Come out - trigger door
                wantsToComeOut = true;
                doorOpenRequest = true;
                doorWaitTimer = 30; // 0.5 sec door open
                state = DOOR_WAIT;
                // Appear at door
                x = homeX;
                y = homeY + 20;
            }
            return;
        }

        // Talking
        if (state == TALKING) {
            // Animate dots: . -> .. -> ...
            talkDotTimer++;
            if (talkDotTimer > 20) {
                talkDotTimer = 0;
                talkDots++;
                if (talkDots > 3) talkDots = 1;
            }
            talkTimer--;
            frame = 0;
            // Face partner
            if (talkPartner != null) {
                if (talkPartner.x < x) dir = 2;
                else if (talkPartner.x > x) dir = 3;
                else if (talkPartner.y < y) dir = 0;
                else dir = 1;
            }
            if (talkTimer <= 0) {
                // Finish talking, now walk around each other
                state = WALK;
                if (talkPartner != null) {
                    talkPartner.state = WALK;
                    talkPartner.talkPartner = null;
                    talkPartner.talkedThisMeeting = true;
                }
                talkPartner = null;
                talkedThisMeeting = true;
                // Walk around: step left / right for a bit
                walkAroundTimer = 30;
                walkAroundDir = NpcManager.rnd(2); // 0 left, 1 right
                hasTarget = false;
            }
            return;
        }

        // Walk around after talking - step aside
        if (walkAroundTimer > 0) {
            walkAroundTimer--;
            int sx = 0, sy = 0;
            if (walkAroundDir == 0) sx = -1;
            else sx = 1;
            int nx = x + sx;
            int ny = y;
            if (free(nx, ny, w) && !w.isRoad(nx, ny)) {
                x = nx;
                y = ny;
            }
            if (walkAroundTimer <= 0) {
                pickLifeTarget(w);
            }
            return;
        }

        // Fear of you
        isAfraid = false;
        if (k != null) {
            int dx = x - k.x;
            int dy = y - k.y;
            int dist = (int)Math.sqrt(dx*dx + dy*dy);
            int fearRange = playerIsVampire ? 120 : 40;
            if (dist < fearRange && dist > 0) {
                isAfraid = true;
                // Step away faster
                int stepX = 0, stepY = 0;
                if (Math.abs(dx) > Math.abs(dy)) stepX = dx > 0 ? 2 : -2;
                else stepY = dy > 0 ? 2 : -2;
                int nx = x + stepX;
                int ny = y + stepY;
                if (free(nx, ny, w) && !w.isRoad(nx, ny)) {
                    x = nx;
                    y = ny;
                    state = WALK;
                    if (stepX > 0) dir = 3;
                    else if (stepX < 0) dir = 2;
                    else if (stepY > 0) dir = 1;
                    else dir = 0;
                    animTimer++;
                    if (animTimer > 8) { animTimer = 0; frame++; if (frame > 3) frame = 0; }
                }
                // Vampire = run inside
                if (playerIsVampire && dist < 70) {
                    wantsToGoInside = true;
                    doorOpenRequest = true;
                    doorWaitTimer = 30;
                    state = DOOR_WAIT;
                }
                return;
            }
        }

        // Idle / standing
        if (idle > 0) {
            idle--;
            frame = 0;
            if (idle == 1 && NpcManager.rnd(3) == 0) dir = NpcManager.rnd(4);
            return;
        }

        if (!hasTarget) {
            pickLifeTarget(w);
            if (!hasTarget) {
                idle = 60 + NpcManager.rnd(120);
                return;
            }
        }

        updateNormal(w, manager, k);
    }

    // ============ WALKING ============
    private void updateNormal(World w, NpcManager manager, Player k) {
        // Check for talking BEFORE walking around each other
        if (!talkedThisMeeting && manager != null) {
            for (Npc other : manager.npcs) {
                if (other == this) continue;
                if (other.state == TALKING || other.state == INSIDE) continue;
                int dx = Math.abs(other.x - x);
                int dy = Math.abs(other.y - y);
                if (dx < 20 && dy < 20) {
                    // 7 out of 10 times talk, 3 times just walk around
                    if (NpcManager.rnd(10) < 7) {
                        state = TALKING;
                        talkPartner = other;
                        talkTimer = 180 + NpcManager.rnd(60); // 3-4 sec
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
        // Reset talked flag when far apart
        if (talkPartner == null) {
            boolean stillClose = false;
            if (manager != null) {
                for (Npc other : manager.npcs) {
                    if (other == this) continue;
                    if (Math.abs(other.x - x) < 40 && Math.abs(other.y - y) < 40) stillClose = true;
                }
            }
            if (!stillClose) talkedThisMeeting = false;
        }

        int dx = targetX - x;
        int dy = targetY - y;

        // Reached target
        if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
            hasTarget = false;
            state = IDLE;
            frame = 0;
            // Go inside - trigger door
            if (lifeMode == 2) {
                wantsToGoInside = true;
                doorOpenRequest = true;
                doorWaitTimer = 30;
                state = DOOR_WAIT;
                return;
            }
            // Stand around home - stand long time
            if (lifeMode == 3) {
                idle = 300 + NpcManager.rnd(400); // 5-10 sec standing
                frame = 0;
                if (NpcManager.rnd(3) == 0) dir = NpcManager.rnd(4);
                return;
            }
            idle = 60 + NpcManager.rnd(180);
            return;
        }

        // Turning back from fence
        if (turningBack) {
            int bdx = turnBackX - x;
            int bdy = turnBackY - y;
            if (Math.abs(bdx) < 10 && Math.abs(bdy) < 10) {
                turningBack = false;
                hasTarget = false;
                pickLifeTarget(w);
                return;
            }
            if (!moveStep(bdx, bdy, w, manager, k)) {
                turningBack = false;
                hasTarget = false;
            }
            return;
        }

        // Normal move to target
        if (!moveStep(dx, dy, w, manager, k)) {
            // Blocked by fence / wall - turn back 120px
            turnBackTries++;
            if (turnBackTries > 2) {
                hasTarget = false;
                turnBackTries = 0;
                pickLifeTarget(w);
                return;
            }
            turningBack = true;
            // Go opposite direction 120px
            int backX = x - (dx > 0 ? 120 : dx < 0 ? -120 : 0);
            int backY = y - (dy > 0 ? 120 : dy < 0 ? -120 : 0);
            if (backX == x && backY == y) backX = x - 120;
            turnBackX = backX;
            turnBackY = backY;
        }
    }

    // ============ MOVE ONE STEP WITH GAP + NO ROAD ============
    private boolean moveStep(int dx, int dy, World w, NpcManager manager, Player k) {
        int stepX = 0, stepY = 0;
        if (Math.abs(dx) > Math.abs(dy)) stepX = dx > 0 ? speed : -speed;
        else stepY = dy > 0 ? speed : -speed;

        int nx = x + stepX;
        int ny = y + stepY;

        // NEVER step on road
        if (w.isRoad(nx, ny)) return false;
        if (!free(nx, ny, w)) return false;

        // Gap from other NPCs - 14px
        if (manager != null) {
            for (Npc other : manager.npcs) {
                if (other == this) continue;
                if (other.state == INSIDE) continue;
                int odx = nx - other.x;
                int ody = ny - other.y;
                if (Math.abs(odx) < 14 && Math.abs(ody) < 14) return false;
            }
        }

        // Gap from you - 16px
        if (k != null) {
            int pdx = nx - k.x;
            int pdy = ny - k.y;
            if (Math.abs(pdx) < 16 && Math.abs(pdy) < 16) return false;
        }

        x = nx;
        y = ny;
        state = WALK;
        if (stepX > 0) dir = 3;
        else if (stepX < 0) dir = 2;
        else if (stepY > 0) dir = 1;
        else dir = 0;
        animTimer++;
        if (animTimer > 8) { animTimer = 0; frame++; if (frame > 3) frame = 0; }
        return true;
    }

    // ============ PICK TARGET - MOST HOME, FEW WALK ============
    private void pickLifeTarget(World w) {
        int roll = NpcManager.rnd(100);

        for (int t = 0; t < 30; t++) {
            int tx, ty;

            if (roll < 50) {
                // 50% - GO INSIDE HOME
                tx = homeX + NpcManager.rnd(40) - 20;
                ty = homeY + NpcManager.rnd(40) - 20;
                lifeMode = 2;
            } else if (roll < 85) {
                // 35% - STAND AROUND HOME
                tx = homeX + NpcManager.rnd(60) - 30;
                ty = homeY + NpcManager.rnd(60) - 30;
                lifeMode = 3;
            } else {
                // Only 15% - WALKING AROUND
                int dist = 100 + NpcManager.rnd(101);
                int angle = NpcManager.rnd(4);
                tx = homeX;
                ty = homeY;
                if (angle == 0) tx += dist;
                else if (angle == 1) tx -= dist;
                else if (angle == 2) ty += dist;
                else ty -= dist;
                lifeMode = 0;
            }

            if (tx < 0 || ty < 0) continue;
            if (!free(tx, ty, w)) continue;
            // NEVER pick target ON road
            if (w.isRoad(tx, ty)) continue;
            if (Math.abs(tx - x) < 60 && Math.abs(ty - y) < 60 && lifeMode != 3) continue;

            targetX = tx;
            targetY = ty;
            hasTarget = true;
            turningBack = false;
            turnBackTries = 0;
            return;
        }
        hasTarget = false;
    }

    // ============ FREE CHECK ============
    private boolean free(int tx, int ty, World w) {
        // Use your World collision here - wall, fence, house wall, etc.
        // Example: return w.isFree(tx, ty);
        // If you already have a method, keep it. This is placeholder:
        return w.isFree(tx, ty);
    }

    // ============ DRAW - INCLUDES TALKING BOX ============
    public void draw(Graphics g, int camX, int camY) {
        int sx = x - camX;
        int sy = y - camY;

        // Don't draw if inside
        if (state == INSIDE) return;

        // Draw body via your sprites - keep your existing draw here
        NpcSprites.draw(g, sx, sy, dir, frame, this);

        // Talking box - small, on top of head, dots animate
        if (state == TALKING) {
            String dots = "";
            if (talkDots == 1) dots = ".";
            else if (talkDots == 2) dots = "..";
            else dots = "...";

            int boxW = 26;
            int boxH = 14;
            int bx = sx - 5;
            int by = sy - 28;

            g.setColor(Color.WHITE);
            g.fillRect(bx, by, boxW, boxH);
            g.setColor(Color.BLACK);
            g.drawRect(bx, by, boxW, boxH);
            g.drawString(dots, bx + 6, by + 11);
        }
    }
}
