import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** Calm city person - walks, looks, kids play / follow parents, bikers. NO KILLER shouting. */
public class Npc {
    // States - only calm ones
    public static final int NORMAL = 0, RECOGNISING = 1, FLEEING = 2, INSIDE = 3, STAND = 4, DEAD = 5;
    // Personalities kept for compatibility, all act calm now
    public static final int P_NORMAL = 0, P_COWARD = 1, P_FROZEN = 2, P_BRAVE = 3, P_PANIC = 4;
    // Kinds
    public static final int ADULT = 0, KID = 1, BIKER = 2;
    // Adult roles
    public static final int R_NORMAL = 0, R_LOOKER = 1, R_GOING = 2;
    // Kid roles
    public static final int K_ALONE = 0, K_WITH_PARENT = 1, K_PLAYING = 2, K_HOME = 3;

    private static final int[] CYCLE = {1, 0, 2, 0};

    public int x, y, dir, frame, state, personality, look;
    public int kind = ADULT;
    public int role = R_NORMAL;
    public int kidRole = K_ALONE;
    public int fx, fy;
    public Npc parent; // for K_WITH_PARENT kids
    public int homeX, homeY; // for K_HOME kids
    public int destX, destY; // for R_GOING adults + K_PLAYING kids target
    public int playCX, playCY; // center of playground for K_PLAYING
    private int animTick, timer, idle, walkT, wdx, wdy, sayCool, bubbleT;
    private int playT = 0;
    private String bubble;
    public int hp = 100;
    private NpcManager mgr;

    public Npc(int x, int y, int look, int personality) {
        this.x = x;
        this.y = y;
        this.look = look;
        this.personality = personality;
        this.dir = NpcManager.rnd(4);
        this.state = NORMAL;
        this.idle = NpcManager.rnd(40);

        homeX = x; homeY = y;
        playCX = x; playCY = y;
        destX = x + 100; destY = y;

        int r = NpcManager.rnd(100);
        if (r < 15) {
            kind = KID;
            int kr = NpcManager.rnd(3);
            if (kr == 0) kidRole = K_WITH_PARENT;
            else if (kr == 1) kidRole = K_PLAYING;
            else kidRole = K_HOME;
        } else if (r < 25) {
            kind = BIKER;
        } else {
            kind = ADULT;
            int ar = NpcManager.rnd(100);
            if (ar < 10) role = R_LOOKER;
            else if (ar < 20) role = R_GOING;
            else role = R_NORMAL;
        }
    }

    public void reset(int nx, int ny) {
        x = nx; y = ny;
        state = NORMAL;
        idle = NpcManager.rnd(40);
        walkT = 0; frame = 0;
        bubble = null; bubbleT = 0;
        hp = 100;
        homeX = nx; homeY = ny;
        playCX = nx; playCY = ny;
    }

    // ---------------- main update ----------------

    public void update(NpcManager m, World w, Player k) {
        mgr = m;
        if (bubbleT > 0) {
            bubbleT--;
            if (bubbleT == 0) bubble = null;
        }
        if (sayCool > 0) sayCool--;
        if (state == DEAD) return;
        if (m.driven != null) dodge(m.driven);

        if (state == STAND) {
            updateStand(k);
        } else {
            updateNormal(w, k);
        }
    }

    private void updateNormal(World w, Player k) {
        // --- LOOKERS: turn to look when player passes close ---
        if (role == R_LOOKER && kind == ADULT) {
            int d2 = dist2(k.x, k.y);
            if (d2 < 90 * 90 && d2 > 20 * 20) {
                faceTo(k.x, k.y);
                frame = 0;
                if (timer <= 0) timer = 60;
                timer--;
                if (timer <= 0) {
                    role = R_NORMAL;
                    idle = 10;
                    timer = 0;
                }
                return;
            }
        }

        // --- KIDS WITH PARENT: follow parent ---
        if (kind == KID && kidRole == K_WITH_PARENT && parent != null) {
            if (parent.state == DEAD) parent = null;
            else {
                int dx = parent.x - x, dy = parent.y - y;
                int d2 = dx * dx + dy * dy;
                if (d2 > 16 * 16) {
                    walk(w, clamp(dx, 1), clamp(dy, 1));
                    animTick++; frame = CYCLE[(animTick / 6) % 4];
                } else {
                    frame = 0;
                }
                return;
            }
        }

        // --- KIDS PLAYING: chase each other in a circle ---
        if (kind == KID && kidRole == K_PLAYING) {
            playT--;
            if (playT <= 0) {
                playT = 20 + NpcManager.rnd(30);
                destX = playCX + NpcManager.rnd(40) - 20;
                destY = playCY + NpcManager.rnd(30) - 15;
            }
            int dx = destX - x, dy = destY - y;
            if (abs(dx) > 3 || abs(dy) > 3) {
                walk(w, clamp(dx, 1), clamp(dy, 1));
                animTick++; frame = CYCLE[(animTick / 4) % 4];
            } else {
                frame = 0;
                if (sayCool == 0 && NpcManager.rnd(100) < 3) {
                    say("Ha!", 30);
                    sayCool = 150;
                }
            }
            return;
        }

        // --- KIDS AT HOME: stay near home door ---
        if (kind == KID && kidRole == K_HOME) {
            int dx = homeX - x, dy = homeY - y;
            int d2 = dx * dx + dy * dy;
            if (d2 > 25 * 25) {
                walk(w, clamp(dx, 1), clamp(dy, 1));
                animTick++; frame = CYCLE[(animTick / 6) % 4];
            } else {
                if (idle > 0) { idle--; frame = 0; return; }
                if (walkT <= 0) {
                    int d = NpcManager.rnd(4);
                    wdx = (d == 0) ? -1 : (d == 1 ? 1 : 0);
                    wdy = (d == 2) ? -1 : (d == 3 ? 1 : 0);
                    walkT = 10 + NpcManager.rnd(20);
                }
                walkT--;
                walk(w, wdx, wdy);
                animTick++; frame = CYCLE[(animTick / 6) % 4];
            }
            return;
        }

        // --- ADULTS GOING SOMEWHERE: walk TO a door ---
        if (kind == ADULT && role == R_GOING) {
            int dx = destX - x, dy = destY - y;
            if (abs(dx) <= 4 && abs(dy) <= 4) {
                state = STAND;
                timer = 150 + NpcManager.rnd(150);
                role = R_NORMAL;
                return;
            }
            if (walk(w, clamp(dx, 1), clamp(dy, 1))) {
                animTick++; frame = CYCLE[(animTick / 6) % 4];
            } else {
                role = R_NORMAL;
            }
            return;
        }

        // --- NORMAL WALK ---
        if (idle > 0) { idle--; frame = 0; return; }
        if (walkT <= 0) {
            if (NpcManager.rnd(6) == 0) { idle = 20 + NpcManager.rnd(60); return; }
            if (NpcManager.rnd(100) < 12) {
                state = STAND; timer = 50 + NpcManager.rnd(100); frame = 0; return;
            }
            int d = NpcManager.rnd(4);
            wdx = (d == 0) ? -1 : (d == 1 ? 1 : 0);
            wdy = (d == 2) ? -1 : (d == 3 ? 1 : 0);
            walkT = 30 + NpcManager.rnd(60);
        }
        walkT--;
        int speed = getSpeed();
        if (walk(w, wdx * speed, wdy * speed)) {
            animTick++;
            int div = (kind == BIKER) ? 3 : 6;
            frame = CYCLE[(animTick / div) % 4];
        } else {
            walkT = 0; idle = 8; frame = 0;
        }
    }

    private int getSpeed() {
        if (kind == KID) return 1;
        if (kind == BIKER) return 2;
        return 1;
    }

    private void updateStand(Player k) {
        frame = 0;
        timer--;
        if (timer <= 0) {
            state = NORMAL;
            idle = 10;
            walkT = 0;
            return;
        }
        if (sayCool == 0) {
            int d2 = dist2(k.x, k.y);
            if (d2 < 50 * 50 && NpcManager.rnd(100) < 5) {
                say("Hi!", 40);
                sayCool = 200;
            }
        }
    }

    // ---------------- cars / dodge ----------------

    private void dodge(Vehicle v) {
        if (state != NORMAL && state != STAND) return;
        int spd = v.sp < 0 ? -v.sp : v.sp;
        if (spd < 24) return;
        int c = VehicleKit.COS[v.h], s = VehicleKit.SIN[v.h];
        int rx = x - v.x, ry = y - v.y;
        int fwd = (rx * c + ry * s) / 256;
        int lat = (-rx * s + ry * c) / 256;
        if (v.sp < 0) fwd = -fwd;
        int look = 40 + (spd / 16) * 14;
        int half = VehicleKit.WID[v.type] / 2 + 20;
        if (fwd < -10 || fwd > look || lat > half || lat < -half) return;
        int side = (lat >= 0) ? 1 : -1;
        int sx = sign(-s * side), sy = sign(c * side);
        x += sx * 2;
        y += sy * 2;
        if (bubble == null) say("!", 25);
    }

    public void hurt(int dmg, NpcManager m, int sx, int sy) {
        if (state == DEAD) return;
        hp -= dmg;
        m.addBlood(x, y - 6, dmg >= 60 ? 2 : (dmg >= 30 ? 1 : 0));
        if (hp <= 0) {
            state = DEAD;
            frame = 0;
            bubble = null; bubbleT = 0;
            m.kills++;
        } else {
            say("Ouch!", 40);
            state = STAND;
            timer = 40;
        }
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
                x = nx; y = ny; moved = true;
            } else break;
        }
        return moved;
    }

    private boolean free(int nx, int ny, World w) {
        if (w == null) return true;
        boolean inside = w.vehicles != null && w.vehicles.hitParked(x, y - 2);
        if (blockedAt(nx - 4, ny - 5, w, inside) || blockedAt(nx + 3, ny - 5, w, inside)
                || blockedAt(nx - 4, ny - 1, w, inside) || blockedAt(nx + 3, ny - 1, w, inside)) return false;
        return mgr == null || !mgr.blocks(nx, ny, x, y, this);
    }

    private boolean blockedAt(int px, int py, World w, boolean ignoreVehicles) {
        return ignoreVehicles ? w.solidTerrain(px, py) : w.solidAt(px, py);
    }

    private void faceTo(int px, int py) {
        int ddx = px - x, ddy = py - y;
        if (abs(ddx) > abs(ddy)) dir = (ddx < 0) ? Sprites.LEFT : Sprites.RIGHT;
        else dir = (ddy < 0) ? Sprites.UP : Sprites.DOWN;
    }

    private static int abs(int v) { return v < 0 ? -v : v; }
    private static int sign(int v) { return v < 0 ? -1 : (v > 0 ? 1 : 0); }
    private static int clamp(int v, int m) { return v > m ? m : (v < -m ? -m : v); }
    private int dist2(int px, int py) {
        int a = px - x, b = py - y;
        return a * a + b * b;
    }

    // ---------------- drawing ----------------

    private void say(String text, int ticks) {
        bubble = text;
        bubbleT = ticks;
    }

    public void draw(Graphics g, int camX, int camY) {
        if (state == DEAD) return;
        if (kind == KID) {
            g.drawImage(NpcSprites.looks[look][dir][frame], x - NpcSprites.W / 2 - camX, y - (NpcSprites.H - 2) - camY + 3,
                    Graphics.TOP | Graphics.LEFT);
        } else if (kind == BIKER) {
            g.drawImage(NpcSprites.looks[look][dir][frame], x - NpcSprites.W / 2 - camX, y - (NpcSprites.H - 2) - camY,
                    Graphics.TOP | Graphics.LEFT);
            g.setColor(0x222222);
            g.drawArc(x - 6 - camX, y - 4 - camY, 6, 6, 0, 360);
            g.drawArc(x + 1 - camX, y - 4 - camY, 6, 6, 0, 360);
        } else {
            g.drawImage(NpcSprites.looks[look][dir][frame], x - NpcSprites.W / 2 - camX, y - (NpcSprites.H - 2) - camY,
                    Graphics.TOP | Graphics.LEFT);
        }
    }

    public void drawBubble(Graphics g, Font f, int camX, int camY) {
        if (bubble == null) return;
        int sx = x - camX, sy = y - camY;
        int w = f.stringWidth(bubble) + 6;
        int h = f.getHeight() + 2;
        int bx = sx - w / 2;
        int by = sy - (NpcSprites.H + 3) - h;
        g.setColor(0xFFFFFF);
        g.fillRoundRect(bx, by, w, h, 6, 6);
        g.setColor(0x000000);
        g.drawRoundRect(bx, by, w, h, 6, 6);
        g.fillTriangle(sx - 2, by + h, sx + 2, by + h, sx, by + h + 3);
        g.setColor(0x000000);
        g.drawString(bubble, bx + 3, by + 1, Graphics.TOP | Graphics.LEFT);
    }

    // Compatibility stubs so old NpcManager code still compiles
    public void startRecognise(int sx, int sy, boolean heard, int delay) { }
                                        }
