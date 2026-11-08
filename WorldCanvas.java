import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

/** Game loop, camera, rendering, pause menu and saving. */
public class WorldCanvas extends GameCanvas implements Runnable {
    private static final int FRAME_MS = 40; // 25 fps

    private final KillersWrath midlet;
    private World world;
    private final Player player;
    private VehicleManager vehicles;
    private NpcManager npcs;
    private PigeonManager pigeons;
    private final SkidMarks skids = new SkidMarks();
    private Image mini;
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private Thread thread;
    private volatile boolean running = false;
    private volatile boolean interact = false;
    private Vehicle driving = null;
    private String msg = null;
    private int msgT = 0;
    private int ticks = 0;
    private int played = 0;
    private int autoT = 0;
    private String menuNote = null;
    private boolean noteOk = true;

    private volatile int menuMode = 0;
    private int menuSel = 0;
    private final String[] slotLabel = new String[SaveManager.SLOTS];

    private String district = "";
    private int bannerT = 0;

    private final int[] dk = new int[1024];
    private final int[] dt = new int[1024];
    private final int[] di = new int[1024];
    private int dn = 0;

    public WorldCanvas(KillersWrath midlet, SaveData save) {
        super(true);
        this.midlet = midlet;
        setFullScreenMode(true);
        Sprites.init();
        NpcSprites.init();
        world = new World(0);
        mini = world.buildMinimap();
        int sx = 380 * World.TS + 8, sy = 120 * World.TS + 8;
        boolean starters = true;
        if (save != null) {
            int[] p = world.safeSpot(save.px, save.py);
            sx = p[0];
            sy = p[1];
            played = save.playTicks;
            starters = false;
        }
        player = new Player(sx, sy);
        vehicles = new VehicleManager(world, player, 56, starters);
        world.vehicles = vehicles;
        npcs = new NpcManager(world, player, 36);
        world.npcs = npcs;
        pigeons = new PigeonManager(world, 24);
        if (save != null) npcs.kills = save.kills;
        district = world.districtName(sx, sy);
        bannerT = 90;
    }

    private void switchWorld(int newArea) {
        int sx, sy;
        if (newArea == World.FOREST) {
            sx = 160 * World.TS;
            sy = 230 * World.TS;
        } else {
            sx = 540 * World.TS;
            sy = 10 * World.TS;
        }
        world = new World(newArea);
        mini = world.buildMinimap();
        int[] p = world.safeSpot(sx, sy);
        player.x = p[0];
        player.y = p[1];
        driving = null;
        vehicles = new VehicleManager(world, player, (newArea == World.FOREST) ? 0 : 56, false);
        world.vehicles = vehicles;
        npcs = new NpcManager(world, player, (newArea == World.FOREST) ? 10 : 36);
        world.npcs = npcs;
        pigeons = new PigeonManager(world, (newArea == World.FOREST) ? 12 : 24);
        district = world.districtName(player.x, player.y);
        bannerT = 90;
        say(newArea == World.FOREST ? "Entered the Forest" : "Back in the City", 80);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
    }

    protected void showNotify() { start(); }
    protected void hideNotify() { stop(); }

    protected void keyPressed(int keyCode) {
        if (menuMode != 0) {
            menuKey(keyCode);
            return;
        }
        if (keyCode == KEY_STAR) {
            menuMode = 1;
            menuSel = 0;
            menuNote = null;
            interact = false;
            return;
        }
        if (keyCode == KEY_POUND || keyCode == KEY_NUM0) interact = true;
    }

    private void menuKey(int keyCode) {
        int ga = 0;
        try { ga = getGameAction(keyCode); } catch (Exception e) {}
        int n = (menuMode == 1) ? 3 : (menuMode == 2) ? SaveManager.SLOTS + 1 : 2;
        if (ga == UP) menuSel = (menuSel + n - 1) % n;
        else if (ga == DOWN) menuSel = (menuSel + 1) % n;
        else if (ga == FIRE || keyCode == KEY_NUM5) menuSelect();
        else if (keyCode == KEY_STAR) {
            if (menuMode == 1) menuMode = 0;
            else { menuMode = 1; menuSel = 0; }
        }
    }

    private void menuSelect() {
        if (menuMode == 1) {
            if (menuSel == 0) menuMode = 0;
            else if (menuSel == 1) {
                for (int i = 0; i < SaveManager.SLOTS; i++) slotLabel[i] = slotText(i);
                menuMode = 2; menuSel = 0;
            } else { menuMode = 3; menuSel = 1; }
        } else if (menuMode == 2) {
            if (menuSel < SaveManager.SLOTS) doSave(menuSel);
            else { menuMode = 1; menuSel = 1; }
        } else {
            if (menuSel == 0) { autoSave(); menuMode = 0; midlet.showTitle(); }
            else { menuMode = 1; menuSel = 2; }
        }
    }

    private String slotText(int i) {
        SaveData d = SaveManager.load(i);
        if (!d.used) return "Slot " + (i + 1) + ": empty";
        return "Slot " + (i + 1) + ": " + clock(d.playTicks) + "  " + world.districtName(d.px, d.py);
    }

    private static String clock(int tks) {
        int s = tks / 25;
        int h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        String r = (h > 0) ? h + ":" + (m < 10 ? "0" : "") + m : "" + m;
        return r + ":" + (sec < 10 ? "0" : "") + sec;
    }

    private SaveData snapshot() {
        SaveData d = new SaveData();
        d.px = player.x; d.py = player.y;
        d.kills = npcs.kills; d.playTicks = played;
        d.health = 100; d.money = 0;
        d.stamp = System.currentTimeMillis();
        return d;
    }

    private void autoSave() { SaveManager.save(SaveManager.AUTO, snapshot()); }

    private void doSave(int slot) {
        boolean ok = SaveManager.save(slot, snapshot());
        noteOk = ok;
        menuNote = ok ? "Saved to slot " + (slot + 1) : "Save failed: " + SaveManager.lastError;
        menuMode = 1; menuSel = 1;
    }

    public void run() {
        Graphics g = getGraphics();
        while (running) {
            long t0 = System.currentTimeMillis();
            if (menuMode == 0) {
                if (interact) { interact = false; toggleVehicle(); }
                int keys = getKeyStates();
                if (driving != null) {
                    driving.drive(keys, world, vehicles);
                    player.x = driving.x; player.y = driving.y;
                    skids.update(driving, world);
                } else {
                    player.update(keys, world);
                }
                if (world.area == World.CITY && player.y < 20 && player.x > 500 * World.TS) {
                    switchWorld(World.FOREST);
                } else if (world.area == World.FOREST && player.y > world.mh * World.TS - 20) {
                    switchWorld(World.CITY);
                }
                skids.age();
                npcs.driven = driving;
                npcs.playerOnFoot = (driving == null);
                vehicles.update(player);
                npcs.update(world, player);
                pigeons.update(world, player);
                if (driving != null) npcs.vehicleHit(driving);
                ticks++; played++; autoT++;
                if (autoT >= 750) { autoT = 0; autoSave(); }
                if (ticks % 10 == 0) {
                    String dn2 = world.districtName(player.x, player.y);
                    if (!dn2.equals(district)) { district = dn2; bannerT = 75; }
                }
                if (bannerT > 0) bannerT--;
            }
            if (msgT > 0) msgT--;
            render(g);
            flushGraphics();
            long wait = FRAME_MS - (System.currentTimeMillis() - t0);
            if (wait < 5) wait = 5;
            try { Thread.sleep(wait); } catch (InterruptedException e) {}
        }
    }

    private void say(String text, int ticksToShow) { msg = text; msgT = ticksToShow; }

    private void toggleVehicle() {
        if (driving != null) {
            int spd = driving.sp < 0 ? -driving.sp : driving.sp;
            if (spd > 48) { say("Slow down to get out", 50); return; }
            int[] spot = exitSpot(driving);
            if (spot == null) { say("No room to get out", 50); return; }
            driving.driven = false; driving.sp = 0;
            player.x = spot[0]; player.y = spot[1];
            driving = null;
        } else {
            Vehicle v = vehicles.nearest(player.x, player.y, 40);
            if (v != null) {
                driving = v; v.driven = true; v.sp = 0;
                player.x = v.x; player.y = v.y;
                say("Up = gas   Down = brake / reverse   Left / Right = steer", 120);
            }
        }
    }

    private int[] exitSpot(Vehicle v) {
        int c = VehicleKit.COS[v.h], s = VehicleKit.SIN[v.h];
        int ds = v.wid() / 2 + 10, df = v.len() / 2 + 10;
        int[][] cand = {
            {v.x + s * ds / 256, v.y - c * ds / 256},
            {v.x - s * ds / 256, v.y + c * ds / 256},
            {v.x + c * df / 256, v.y + s * df / 256},
            {v.x - c * df / 256, v.y - s * df / 256}
        };
        for (int i = 0; i < cand.length; i++)
            if (player.freeAt(cand[i][0], cand[i][1], world)) return cand[i];
        return null;
    }

    private void add(int key, int type, int idx) {
        if (dn >= dk.length) return;
        dk[dn] = key; dt[dn] = type; di[dn] = idx; dn++;
    }

    private void sortDrawList() {
        for (int i = 1; i < dn; i++) {
            int k = dk[i], t = dt[i], x = di[i];
            int j = i - 1;
            while (j >= 0 && dk[j] > k) {
                dk[j + 1] = dk[j]; dt[j + 1] = dt[j]; di[j + 1] = di[j]; j--;
            }
            dk[j + 1] = k; dt[j + 1] = t; di[j + 1] = x;
        }
    }

    private void render(Graphics g) {
        int sw = getWidth(), sh = getHeight();
        int camX = player.x - sw / 2;
        int camY = player.y - 14 - sh / 2;
        int maxX = world.mw * World.TS - sw, maxY = world.mh * World.TS - sh;
        if (camX > maxX) camX = maxX;
        if (camX < 0) camX = 0;
        if (camY > maxY) camY = maxY;
        if (camY < 0) camY = 0;
        g.setColor(0x000000);
        g.fillRect(0, 0, sw, sh);
        world.draw(g, camX, camY, sw, sh);
        skids.draw(g, camX, camY, sw, sh);
        npcs.drawGround(g, camX, camY, sw, sh);
        pigeons.draw(g, camX, camY, sw, sh);
        dn = 0;
        int tc = world.collectTrees(camX, camY, sw, sh);
        for (int i = 0; i < tc; i++) add(world.treeBaseY(world.treeBuf[i]), 0, world.treeBuf[i]);
        for (int i = 0; i < npcs.npcs.length; i++) {
            Npc n = npcs.npcs[i];
            if (n.state == Npc.INSIDE || n.state == Npc.DEAD) continue;
            int sx = n.x - camX, sy = n.y - camY;
            if (sx < -24 || sx > sw + 24 || sy < -8 || sy > sh + 36) continue;
            add(n.y, 1, i);
        }
        for (int i = 0; i < vehicles.list.length; i++) {
            Vehicle v = vehicles.list[i];
            int sx = v.x - camX, sy = v.y - camY;
            if (sx < -70 || sx > sw + 70 || sy < -70 || sy > sh + 70) continue;
            add(v.y + v.extY(), 2, i);
        }
        if (driving == null) add(player.y, 3, 0);
        sortDrawList();
        for (int i = 0; i < dn; i++) {
            switch (dt[i]) {
                case 0: world.drawTree(g, di[i], camX, camY); break;
                case 1: npcs.npcs[di[i]].draw(g, camX, camY); break;
                case 2: vehicles.drawVehicle(g, vehicles.list[di[i]], camX, camY); break;
                default: player.draw(g, camX, camY); break;
            }
        }
        npcs.drawBubbles(g, font, camX, camY);
        g.setFont(font);
        g.setColor(0x000000);
        g.fillRect(0, 0, sw, font.getHeight() + 2);
        if (ticks < 150) {
            g.setColor(0xE01010);
            g.drawString("KILLER'S WRATH", 4, 1, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0xFFFFFF);
            g.drawString("Fleeing: " + npcs.count(Npc.FLEEING) + "  Hiding: " + npcs.count(Npc.INSIDE) + "  Down: " + npcs.kills, 4, 1, Graphics.TOP | Graphics.LEFT);
        }
        String line = null;
        if (msgT > 0) line = msg;
        else if (driving != null) {
            int spd = driving.sp < 0 ? -driving.sp : driving.sp;
            line = (spd * 90 / 256) + " km/h" + (driving.sp < 0 ? " R" : "") + "   # = get out";
        } else if (vehicles.nearest(player.x, player.y, 40) != null) line = "# = get in";
        else if (ticks < 150) line = "Arrows: move  FIRE: run  *: menu";
        if (line != null) {
            g.setColor(0x000000);
            g.fillRect(0, sh - font.getHeight() - 3, sw, font.getHeight() + 3);
            g.setColor(0xFFFFFF);
            g.drawString(line, 4, sh - font.getHeight() - 1, Graphics.TOP | Graphics.LEFT);
        }
        if (bannerT > 0 && menuMode == 0) {
            int bw = font.stringWidth(district) + 16, bh = font.getHeight() + 6;
            int bx = (sw - bw) / 2, by = font.getHeight() + 8;
            g.setColor(0x050806); g.fillRect(bx, by, bw, bh);
            g.setColor(0x8A1414); g.drawRect(bx, by, bw - 1, bh - 1);
            g.setColor(0xE8C23A);
            g.drawString(district, sw / 2, by + 3, Graphics.TOP | Graphics.HCENTER);
        }
        if (sw >= 160) {
            int mw = mini.getWidth(), mh = mini.getHeight();
            int mx = sw - mw - 3, my = sh - mh - font.getHeight() - 8;
            g.drawImage(mini, mx, my, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xFFFFFF); g.drawRect(mx - 1, my - 1, mw + 1, mh + 1);
            g.setColor(0xFF0000);
            g.fillRect(mx + player.x / (World.TS * 8) - 1, my + player.y / (World.TS * 8) - 1, 3, 3);
        }
        if (menuMode != 0) drawMenu(g, sw, sh);
    }

    private void drawMenu(Graphics g, int sw, int sh) {
        String head; String[] items;
        if (menuMode == 1) { head = "PAUSED"; items = new String[] {"Resume", "Save Game", "Quit to Title"}; }
        else if (menuMode == 2) {
            head = "SAVE GAME"; items = new String[SaveManager.SLOTS + 1];
            for (int i = 0; i < SaveManager.SLOTS; i++) items[i] = slotLabel[i];
            items[SaveManager.SLOTS] = "Back";
        } else { head = "QUIT TO TITLE?"; items = new String[] {"Yes, quit", "No, keep playing"}; }
        int lineH = font.getHeight() + 7;
        int boxW = sw - 20; if (boxW > 230) boxW = 230;
        int extra = (menuMode == 3) ? lineH : 0;
        int noteH = (menuNote != null && menuMode != 3) ? lineH * 2 : 0;
        int boxH = lineH * (items.length + 1) + 14 + extra + noteH;
        int bx = (sw - boxW) / 2, by = (sh - boxH) / 2;
        g.setColor(0x050806); g.fillRect(bx, by, boxW, boxH);
        g.setColor(0x8A1414); g.drawRect(bx, by, boxW - 1, boxH - 1);
        g.drawRect(bx + 2, by + 2, boxW - 5, boxH - 5);
        g.setFont(font); g.setColor(0xC41818);
        g.drawString(head, sw / 2, by + 8, Graphics.TOP | Graphics.HCENTER);
        int y = by + 8 + lineH;
        if (noteH > 0) {
            g.setColor(noteOk ? 0x58C060 : 0xE05050);
            String n1 = menuNote, n2 = null;
            int maxW = boxW - 16;
            if (font.stringWidth(n1) > maxW) {
                int cut = n1.length();
                while (cut > 1 && font.stringWidth(n1.substring(0, cut)) > maxW) cut--;
                n2 = n1.substring(cut); n1 = n1.substring(0, cut);
            }
            g.drawString(n1, bx + 8, y - 4, Graphics.TOP | Graphics.LEFT);
            if (n2 != null) g.drawString(n2, bx + 8, y - 4 + font.getHeight(), Graphics.TOP | Graphics.LEFT);
            y += lineH * 2;
        }
        if (menuMode == 3) {
            g.setColor(0x9AA890);
            g.drawString("Your progress is autosaved", sw / 2, y - 4, Graphics.TOP | Graphics.HCENTER);
            y += lineH;
        }
        for (int i = 0; i < items.length; i++) {
            if (i == menuSel) {
                g.setColor(0x3A0A0A); g.fillRect(bx + 5, y - 3, boxW - 10, lineH - 1);
                g.setColor(0xE8C23A);
            } else { g.setColor(0x8A9A8E); }
            g.drawString(items[i], bx + 12, y, Graphics.TOP | Graphics.LEFT);
            y += lineH;
        }
    }
    }
