import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** One pedestrian with a personality and a simple state machine. */
public class Npc {
    public static final int NORMAL = 0, RECOGNISING = 1, FLEEING = 2, INSIDE = 3, STAND = 4;
    public static final int P_NORMAL = 0, P_COWARD = 1, P_FROZEN = 2, P_BRAVE = 3, P_PANIC = 4;
    private static final int S_AWAY = 0, S_HOME = 1, S_RANDOM = 2;
    private static final int[] CYCLE = {1, 0, 2, 0};

    public int x, y, dir, frame, state, personality, look;
    public int fx, fy; // where the danger (Killer) was last known to be
    private int animTick, timer, idle, walkT, wdx, wdy, style, stuck, rT, rdx, rdy, sayCool, bubbleT;
    private int door = -1;
    private boolean heardOnly;
    private String bubble;

    public Npc(int x, int y, int look, int personality) {
        this.x = x;
        this.y = y;
        this.look = look;
        this.personality = personality;
        this.dir = NpcManager.rnd(4);
        this.state = NORMAL;
        this.idle = NpcManager.rnd(40);
    }

    /** Put this pedestrian somewhere else, calm again. */
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
    }

    // ---------------- main update ----------------

    public void update(NpcManager m, World w, Player k) {
        if (bubbleT > 0) {
            bubbleT--;
            if (bubbleT == 0) bubble = null;
        }
        if (sayCool > 0) sayCool--;
        switch (state) {
            case NORMAL: updateNormal(w, k); break;
            case RECOGNISING: updateRecognising(m, w, k); break;
            case FLEEING: updateFleeing(m, w, k); break;
            case INSIDE: updateInside(m, k); break;
            case STAND: updateStand(k); break;
        }
    }

    private void updateNormal(World w, Player k) {
        if (canSee(w, k)) {
            if (personality == P_BRAVE) {
                state = STAND;
                faceTo(k.x, k.y);
                return;
            }
            startRecognise(k.x, k.y, false, reactionDelay());
            return;
        }
        if (idle > 0) {
            idle--;
            frame = 0;
            return;
        }
        if (walkT <= 0) {
            if (NpcManager.rnd(6) == 0) {
                idle = 20 + NpcManager.rnd(60);
                return;
            }
            int d = NpcManager.rnd(4);
            wdx = (d == 0) ? -1 : (d == 1 ? 1 : 0);
            wdy = (d == 2) ? -1 : (d == 3 ? 1 : 0);
            walkT = 30 + NpcManager.rnd(60);
        }
        walkT--;
        if (walk(w, wdx, wdy)) {
            animTick++;
            frame = CYCLE[(animTick / 6) % 4];
        } else {
            walkT = 0;
            idle = 8;
            frame = 0;
        }
    }

    private int reactionDelay() {
        switch (personality) {
            case P_COWARD: return 3;
            case P_FROZEN: return 28;   // freezes before running
            case P_PANIC: return 8;
            default: return 10 + NpcManager.rnd(8);
        }
    }

    /** Called by the manager when another pedestrian shouts nearby. */
    public void startRecognise(int sx, int sy, boolean heard, int delay) {
        state = RECOGNISING;
        timer = delay;
        fx = sx;
        fy = sy;
        heardOnly = heard;
        frame = 0;
        say(heard ? "?!" : "?", delay);
    }

    private void updateRecognising(NpcManager m, World w, Player k) {
        frame = 0;
        if (canSee(w, k)) {
            fx = k.x;
            fy = k.y;
        }
        faceTo(fx, fy);
        timer--;
        if (timer > 0) return;
        boolean shout = !heardOnly || NpcManager.rnd(100) < 50;
        if (shout) {
            say(heardOnly ? "KILLER!" : "KILLER! KILLER!", 50);
            m.hear(this);
        } else {
            say("!", 30);
        }
        beginFlee(m);
    }

    private void beginFlee(NpcManager m) {
        int r = NpcManager.rnd(100);
        if (personality == P_PANIC) style = S_RANDOM;
        else if (personality == P_COWARD) style = (r < 60) ? S_HOME : S_AWAY;
        else if (personality == P_FROZEN) style = S_AWAY;
        else style = (r < 45) ? S_HOME : S_AWAY;
        if (style == S_HOME) {
            door = m.findDoor(x, y);
            if (door < 0) style = S_AWAY;
        }
        state = FLEEING;
        timer = 150 + NpcManager.rnd(150);
        stuck = 0;
        rT = 0;
    }

    private void updateFleeing(NpcManager m, World w, Player k) {
        if (canSee(w, k)) {
            fx = k.x;
            fy = k.y;
        }
        boolean mv = false;
        if (style == S_HOME) {
            int ax = m.doorX[door], ay = m.doorY[door];
            int ddx = ax - x, ddy = ay - y;
            if (abs(ddx) <= 5 && abs(ddy) <= 6) {
                if (m.doorUse[door] < 2) {
                    m.doorUse[door]++;
                    state = INSIDE;
                    timer = 300 + NpcManager.rnd(300);
                    frame = 0;
                    bubble = null;
                    return;
                }
                style = S_AWAY; // door already taken
                return;
            }
            mv = walk(w, clamp(ddx, 3), clamp(ddy, 3));
            if (!mv) {
                stuck++;
                if (stuck > 20) style = S_AWAY;
            }
        } else if (style == S_AWAY) {
            if (rT > 0) {
                rT--;
                mv = walk(w, rdx * 3, rdy * 3);
            } else {
                int sx = sign(x - fx), sy = sign(y - fy);
                if (sx == 0 && sy == 0) sx = 1;
                mv = walk(w, sx * 3, sy * 3);
                if (!mv) {
                    stuck++;
                    if (stuck > 3) {
                        stuck = 0;
                        rT = 20;
                        int d = NpcManager.rnd(4);
                        rdx = (d == 0) ? -1 : (d == 1 ? 1 : 0);
                        rdy = (d == 2) ? -1 : (d == 3 ? 1 : 0);
                    }
                } else {
                    stuck = 0;
                }
            }
        } else { // S_RANDOM: panics in unpredictable directions
            if (rT <= 0) {
                rT = 12 + NpcManager.rnd(15);
                rdx = NpcManager.rnd(3) - 1;
                rdy = NpcManager.rnd(3) - 1;
                if (rdx == 0 && rdy == 0) rdx = 1;
            }
            rT--;
            mv = walk(w, rdx * 3, rdy * 3);
            if (!mv) rT = 0;
        }
        if (mv) {
            animTick++;
            frame = CYCLE[(animTick / 2) % 4];
        } else {
            frame = 0;
        }
        if (timer > 0) timer--;
        if (timer <= 0 && dist2(k.x, k.y) > 260 * 260) {
            state = NORMAL;
            idle = 30;
            walkT = 0;
        }
    }

    private void updateInside(NpcManager m, Player k) {
        timer--;
        if (timer > 0) return;
        int ddx = k.x - m.doorX[door], ddy = k.y - m.doorY[door];
        if (ddx * ddx + ddy * ddy > 220 * 220) {
            m.doorUse[door]--;
            x = m.doorX[door];
            y = m.doorY[door];
            door = -1;
            dir = Sprites.DOWN;
            state = NORMAL;
            idle = 20;
            walkT = 0;
        } else {
            timer = 60; // danger still near, stay hidden
        }
    }

    private void updateStand(Player k) {
        frame = 0;
        faceTo(k.x, k.y);
        int d2 = dist2(k.x, k.y);
        if (d2 > 220 * 220) {
            state = NORMAL;
            idle = 10;
            return;
        }
        if (d2 < 120 * 120 && sayCool == 0) {
            say("Back off!", 40);
            sayCool = 120;
        }
    }

    // ---------------- senses ----------------

    private boolean canSee(World w, Player k) {
        int d2 = dist2(k.x, k.y);
        if (d2 > 200 * 200) return false;
        if (d2 >= 70 * 70) {
            int fxd = (dir == Sprites.LEFT) ? -1 : (dir == Sprites.RIGHT ? 1 : 0);
            int fyd = (dir == Sprites.UP) ? -1 : (dir == Sprites.DOWN ? 1 : 0);
            if ((k.x - x) * fxd + (k.y - y) * fyd <= 0) return false;
        }
        return clear(w, x, y, k.x, k.y);
    }

    private boolean clear(World w, int x1, int y1, int x2, int y2) {
        for (int i = 1; i < 8; i++) {
            int px = x1 + (x2 - x1) * i / 8;
            int py = y1 + (y2 - y1) * i / 8 - 2;
            if (w.solidAt(px, py)) return false;
        }
        return true;
    }

    private void faceTo(int px, int py) {
        int ddx = px - x, ddy = py - y;
        if (abs(ddx) > abs(ddy)) dir = (ddx < 0) ? Sprites.LEFT : Sprites.RIGHT;
        else dir = (ddy < 0) ? Sprites.UP : Sprites.DOWN;
    }

    private int dist2(int px, int py) {
        int a = px - x, b = py - y;
        return a * a + b * b;
    }

    // ---------------- movement ----------------

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
        return !w.solidAt(nx - 4, ny - 5) && !w.solidAt(nx + 3, ny - 5)
                && !w.solidAt(nx - 4, ny - 1) && !w.solidAt(nx + 3, ny - 1);
    }

    private static int abs(int v) { return v < 0 ? -v : v; }
    private static int sign(int v) { return v < 0 ? -1 : (v > 0 ? 1 : 0); }
    private static int clamp(int v, int m) { return v > m ? m : (v < -m ? -m : v); }

    // ---------------- drawing ----------------

    private void say(String text, int ticks) {
        bubble = text;
        bubbleT = ticks;
    }

    public void draw(Graphics g, int camX, int camY) {
        if (state == INSIDE) return;
        g.drawImage(NpcSprites.looks[look][dir][frame], x - NpcSprites.W / 2 - camX, y - 28 - camY,
                Graphics.TOP | Graphics.LEFT);
    }

    public void drawBubble(Graphics g, Font f, int camX, int camY) {
        if (bubble == null || state == INSIDE) return;
        int sx = x - camX, sy = y - camY;
        int w = f.stringWidth(bubble) + 6;
        int h = f.getHeight() + 2;
        int bx = sx - w / 2;
        int by = sy - 32 - h;
        g.setColor(0xFFFFFF);
        g.fillRoundRect(bx, by, w, h, 6, 6);
        g.setColor(0x000000);
        g.drawRoundRect(bx, by, w, h, 6, 6);
        g.fillTriangle(sx - 2, by + h, sx + 2, by + h, sx, by + h + 3);
        g.setColor(bubble.charAt(0) == 'K' ? 0xC01010 : 0x000000);
        g.drawString(bubble, bx + 3, by + 1, Graphics.TOP | Graphics.LEFT);
    }
}
