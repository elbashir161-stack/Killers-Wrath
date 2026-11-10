import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** One pedestrian with a personality, simplified to walk normally without fleeing or shouting. */
public class Npc {
    public static final int NORMAL = 0, RECOGNISING = 1, FLEEING = 2, INSIDE = 3, STAND = 4, DEAD = 5;
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
    }

    // ---------------- main update ----------------

    public void update(NpcManager m, World w, Player k) {
        mgr = m;
        bubble = null; // Ensure speech bubbles never show up
        bubbleT = 0;
        if (state == DEAD) return;
        
        // Force everyone to stay in normal walking behavior
        state = NORMAL;
        updateNormal(w, k);
    }

    private void updateNormal(World w, Player k) {
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

    public void startRecognise(int sx, int sy, boolean heard, int delay) {
        // Disabled: Pedestrians won't recognize danger or shout
    }

    public void hurt(int dmg, NpcManager m, int sx, int sy) {
        if (state == DEAD || state == INSIDE) return;
        hp -= dmg;
        m.addBlood(x, y - 6, dmg >= 60 ? 2 : (dmg >= 30 ? 1 : 0));
        if (hp <= 0) {
            state = DEAD;
            frame = 0;
            bubble = null;
            m.kills++;
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

    // ---------------- drawing ----------------

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
        // Disabled speech bubble rendering completely
    }
    }
    
