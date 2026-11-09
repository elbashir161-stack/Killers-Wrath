import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class Npc {

    public static final int P_NORMAL = 0;
    public static final int P_BRAVE = 1;
    public static final int P_COWARD = 2;
    public static final int P_FROZEN = 3;
    public static final int P_PANIC = 4;

    public static final int ADULT = 0;
    public static final int KID = 1;

    public static final int K_HOME = 0;
    public static final int K_WITH_PARENT = 1;
    public static final int K_PLAYING = 2;

    public static final int R_IDLE = 0;
    public static final int R_GOING = 1;
    public static final int R_SITTING = 2;

    public static final int NORMAL = 0;
    public static final int DEAD = 1;
    public static final int INSIDE = 2;

    public int x, y;
    public int look;
    public int pers;
    public int kind;
    public int kidRole;
    public int role;
    public int state = NORMAL;

    public int destX, destY;
    public int homeX, homeY;
    public int playCX, playCY;
    public Npc parent;

    public int hp = 100;
    public int dir = 2;
    public int anim = 0;
    public int waitT = 0;
    public int speed = 1;
    public int bubbleT = 0;
    public String bubble = null;
    public int lookAtT = 0;

    private int step = 0;
    private int thinkT = 0;

    public Npc(int px, int py, int lookId, int persId) {
        x = px;
        y = py;
        look = lookId;
        pers = persId;
        int r = NpcManager.rnd(100);
        if (r < 25) {
            kind = KID;
            int kr = NpcManager.rnd(100);
            if (kr < 35) kidRole = K_WITH_PARENT;
            else if (kr < 65) kidRole = K_PLAYING;
            else kidRole = K_HOME;
            speed = 1;
        } else {
            kind = ADULT;
            int ar = NpcManager.rnd(100);
            if (ar < 45) role = R_GOING;
            else if (ar < 60) role = R_SITTING;
            else role = R_IDLE;
            speed = 1;
        }
        homeX = x;
        homeY = y;
        destX = x;
        destY = y;
        playCX = x;
        playCY = y;
        waitT = NpcManager.rnd(60);
    }

    public void reset(int px, int py) {
        x = px;
        y = py;
        state = NORMAL;
        hp = 100;
        bubble = null;
        bubbleT = 0;
        waitT = NpcManager.rnd(40);
        homeX = px;
        homeY = py;
        destX = px;
        destY = py;
    }

    public void update(NpcManager m, World w, Player k) {
        if (state == DEAD || state == INSIDE) {
            if (bubbleT > 0) bubbleT--;
            return;
        }
        if (bubbleT > 0) bubbleT--;
        if (lookAtT > 0) lookAtT--;
        int dxp = k.x - x;
        int dyp = k.y - y;
        int d2p = dxp * dxp + dyp * dyp;
        if (d2p < 50 * 50 && lookAtT == 0 && NpcManager.rnd(200) == 0) {
            lookAtT = 40;
            if (Math.abs(dxp) > Math.abs(dyp)) dir = (dxp > 0) ? 3 : 2;
            else dir = (dyp > 0) ? 1 : 0;
        }
        if (lookAtT > 0) return;
        thinkT--;
        if (thinkT <= 0) {
            thinkT = 20 + NpcManager.rnd(40);
            decide(m, w, k);
        }
        if (waitT > 0) {
            waitT--;
            return;
        }
        if (kind == KID) updateKid(m, w);
        else updateAdult(m, w);
        step++;
        if (step % 8 == 0) anim = (anim + 1) & 1;
    }

    private void decide(NpcManager m, World w, Player k) {
        if (kind == KID) {
            if (kidRole == K_PLAYING) {
                destX = playCX + NpcManager.rnd(41) - 20;
                destY = playCY + NpcManager.rnd(31) - 15;
                if (NpcManager.rnd(100) < 15) say("Yay!");
            } else if (kidRole == K_HOME) {
                destX = homeX + NpcManager.rnd(31) - 15;
                destY = homeY + NpcManager.rnd(21) - 10;
            } else if (kidRole == K_WITH_PARENT) {
                if (parent == null || parent.state != NORMAL) {
                    kidRole = K_HOME;
                    homeX = x;
                    homeY = y;
                }
            }
        } else {
            if (role == R_GOING) {
                int dx = destX - x, dy = destY - y;
                if (dx * dx + dy * dy < 20 * 20) {
                    if (NpcManager.rnd(100) < 50) {
                        int d = m.findDoor(x, y);
                        if (d >= 0) {
                            destX = m.doorX[d];
                            destY = m.doorY[d] + 15;
                        } else {
                            destX = x + NpcManager.rnd(201) - 100;
                            destY = y + NpcManager.rnd(121) - 60;
                        }
                    } else {
                        waitT = 60 + NpcManager.rnd(120);
                    }
                }
            } else if (role == R_IDLE) {
                if (NpcManager.rnd(100) < 20) {
                    destX = x + NpcManager.rnd(81) - 40;
                    destY = y + NpcManager.rnd(61) - 30;
                } else {
                    waitT = 40 + NpcManager.rnd(80);
                }
            } else if (role == R_SITTING) {
                waitT = 100;
            }
        }
    }

    private void updateKid(NpcManager m, World w) {
        int tx, ty;
        if (kidRole == K_WITH_PARENT && parent != null && parent.state == NORMAL) {
            int dx = parent.x - x, dy = parent.y - y;
            int d2 = dx * dx + dy * dy;
            if (d2 < 18 * 18) {
                waitT = 10;
                return;
            }
            if (d2 > 120 * 120) {
                x = parent.x + 10;
                y = parent.y + 8;
                return;
            }
            tx = parent.x + 10;
            ty = parent.y + 8;
        } else if (kidRole == K_PLAYING || kidRole == K_HOME) {
            tx = destX;
            ty = destY;
            int dx = tx - x, dy = ty - y;
            if (dx * dx + dy * dy < 6 * 6) {
                waitT = 20 + NpcManager.rnd(40);
                return;
            }
        } else {
            return;
        }
        walkToward(m, w, tx, ty);
    }

    private void updateAdult(NpcManager m, World w) {
        if (role == R_SITTING) return;
        int dx = destX - x, dy = destY - y;
        if (dx * dx + dy * dy < 6 * 6) {
            waitT = 20;
            return;
        }
        walkToward(m, w, destX, destY);
    }

    private void walkToward(NpcManager m, World w, int tx, int ty) {
        int nx = x, ny = y;
        int dx = tx - x, dy = ty - y;
        if (Math.abs(dx) > Math.abs(dy)) {
            nx += (dx > 0) ? speed : -speed;
            dir = (dx > 0) ? 3 : 2;
        } else {
            ny += (dy > 0) ? speed : -speed;
            dir = (dy > 0) ? 1 : 0;
        }
        int txTile = nx / World.TS;
        int tyTile = ny / World.TS;
        if (!w.isSidewalk(txTile, tyTile) && !w.isRoad(txTile, tyTile)) {
            if (Math.abs(dx) > Math.abs(dy)) {
                nx = x;
                ny = y + ((dy > 0) ? speed : -speed);
                dir = (dy > 0) ? 1 : 0;
            } else {
                ny = y;
                nx = x + ((dx > 0) ? speed : -speed);
                dir = (dx > 0) ? 3 : 2;
            }
            txTile = nx / World.TS;
            tyTile = ny / World.TS;
            if (!w.isSidewalk(txTile, tyTile) && !w.isRoad(txTile, tyTile)) {
                waitT = 30;
                return;
            }
        }
        if (m.blocks(nx, ny, x, y, this)) {
            waitT = 15;
            return;
        }
        x = nx;
        y = ny;
    }

    public void say(String s) {
        bubble = s;
        bubbleT = 90;
    }

    public void hurt(int dmg, NpcManager m, int fromX, int fromY) {
        if (state != NORMAL) return;
        hp -= dmg;
        m.addBlood(x, y, 1);
        if (hp <= 0) {
            state = DEAD;
            m.kills++;
            bubble = null;
            bubbleT = 0;
        }
    }

    public void drawBubble(Graphics g, Font f, int camX, int camY) {
        if (bubble == null || bubbleT <= 0 || state != NORMAL) return;
        int sx = x - camX;
        int sy = y - 28 - camY;
        int bw = f.stringWidth(bubble) + 8;
        int bh = f.getHeight() + 4;
        g.setColor(0xFFFFFF);
        g.fillRoundRect(sx - bw / 2, sy - bh, bw, bh, 6, 6);
        g.setColor(0x000000);
        g.drawRoundRect(sx - bw / 2, sy - bh, bw, bh, 6, 6);
        g.setColor(0x000000);
        g.drawString(bubble, sx, sy - bh + 2, Graphics.HCENTER | Graphics.TOP);
    }
}
