import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

/** Game loop, camera, rendering, pause menu, saving, and moving between areas (city <-> forest). */
public class WorldCanvas extends GameCanvas implements Runnable {
    private static final int FRAME_MS = 40; // 25 fps

    private final KillersWrath midlet;
    private final Player player;
    private final SkidMarks skids = new SkidMarks();
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

    private World world, city, forest;
    private VehicleManager vehicles, cityVeh, forestVeh;
    private NpcManager npcs, cityNpcs, forestNpcs;
    private Image mini, cityMini, forestMini;
    private PigeonManager pigeons, cityPigeons, forestPigeons;

    private Thread thread;
    private volatile boolean running = false;
    private volatile boolean interact = false;
    private Vehicle driving = null;
    private String msg = null;
    private int msgT = 0;
    private int ticks = 0;
    private int played = 0;
    private int autoT = 0;
    private int kills = 0;
    private String menuNote = null;
    private boolean noteOk = true;

    private int transArea = -1;
    private int transT = 0;
    private int exitCool = 0;

    // pause menu: 0 none, 1 main, 2 save slots, 3 confirm quit, 4 big map
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
        int area = (save != null) ? save.area : World.CITY;
        World w = new World(area);
        if (area == World.CITY) city = w; else forest = w;
        int sx, sy;
        boolean starters = true;
        if (save != null) {
            int[] p = w.safeSpot(save.px, save.py);
            sx = p[0];
            sy = p[1];
            played = save.playTicks;
            kills = save.kills;
            starters = false;
        } else {
            int[] p = w.startPoint();
            sx = p[0];
            sy = p[1];
        }
        player = new Player(sx, sy);
        attach(w, starters);
        use(w);
    }

    private void attach(World w, boolean starters) {
        boolean isCity = (w.area == World.CITY);
        VehicleManager vm = new VehicleManager(w, player, isCity ? 56 : 0, isCity && starters, isCity ? 14 : 0);
        w.vehicles = vm;
        NpcManager nm = new NpcManager(w, player, isCity ? 36 : 0);
        w.npcs = nm;
        nm.kills = kills;
        Image mm = w.buildMinimap();
        PigeonManager pm = new PigeonManager(w, player, isCity ? 14 : 0);
        if (isCity) { cityVeh = vm; cityNpcs = nm; cityMini = mm; cityPigeons = pm; }
        else { forestVeh = vm; forestNpcs = nm; forestMini = mm; forestPigeons = pm; }
    }

    private void use(World w) {
        world = w;
        boolean isCity = (w.area == World.CITY);
        vehicles = isCity ? cityVeh : forestVeh;
        npcs = isCity ? cityNpcs : forestNpcs;
        mini = isCity ? cityMini : forestMini;
        pigeons = isCity ? cityPigeons : forestPigeons;
        npcs.kills = kills;
        skids.clear();
        district = w.districtName(player.x, player.y);
        bannerT = 90;
    }

    private void switchArea(int target) {
        kills = npcs.kills;
        if (driving != null) {
            driving.driven = false;
            driving.sp = 0;
            driving = null;
        }
        World tw = (target == World.FOREST) ? forest : city;
        boolean created = false;
        if (tw == null) {
            tw = new World(target);
            if (target == World.FOREST) forest = tw; else city = tw;
            created = true;
        }
        int[] e = tw.entryPoint();
        player.x = e[0];
        player.y = e[1];
        player.dir = (target == World.FOREST) ? Sprites.DOWN : Sprites.UP;
        if (created) attach(tw, false);
        use(tw);
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

    // ---------------- input ----------------

    protected void keyPressed(int keyCode) {
        if (menuMode != 0) {
            menuKey(keyCode);
            return;
        }
        if (keyCode == KEY_STAR || keyCode == -6) {
            menuMode = 1;
            menuSel = 0;
            menuNote = null;
            interact = false;
            return;
        }
        if (keyCode == KEY_NUM1) {
            menuMode = 4;
            menuSel = 0;
            interact = false;
            return;
        }
        if (keyCode == KEY_POUND || keyCode == KEY_NUM0) interact = true;
    }

    private void menuKey(int keyCode) {
        if (menuMode == 4) {
            if (keyCode == -6 || keyCode == KEY_STAR || keyCode == KEY_NUM5 || keyCode == KEY_NUM1) {
                menuMode = 0;
            }
            return;
        }
        int ga = 0;
        try {
            ga = getGameAction(keyCode);
        } catch (Exception e) {
        }
        int n = (menuMode == 1) ? 3 : (menuMode == 2) ? SaveManager.SLOTS + 1 : 2;
        if (ga == UP) menuSel = (menuSel + n - 1) % n;
        else if (ga == DOWN) menuSel = (menuSel + 1) % n;
        else if (ga == FIRE || keyCode == KEY_NUM5) menuSelect();
        else if (keyCode == KEY_STAR || keyCode == -6) {
            if (menuMode == 1) menuMode = 0;
            else { menuMode = 1; menuSel = 0; }
        }
    }

    private void menuSelect() {
        if (menuMode == 1) {
            if (menuSel == 0) menuMode = 0;
            else if (menuSel == 1) {
                for (int i = 0; i < SaveManager.SLOTS; i++) slotLabel[i] = slotText(i);
                menuMode = 2;
                menuSel = 0;
            } else { menuMode = 3; menuSel = 1; }
        } else if (menuMode == 2) {
            if (menuSel < SaveManager.SLOTS) doSave(menuSel);
            else { menuMode = 1; menuSel = 1; }
        } else {
            if (menuSel == 0) {
                autoSave();
                menuMode = 0;
                midlet.showTitle();
            } else { menuMode = 1; menuSel = 2; }
        }
    }

    private String slotText(int i) {
        SaveData d = SaveManager.load(i);
        if (!d.used) return "Slot " + (i + 1) + ": empty";
        String where = (d.area == World.FOREST) ? "Blackwood Forest" : World.cityDistrict(d.px, d.py);
        return "Slot " + (i + 1) + ": " + clock(d.playTicks) + "  " + where;
    }

    private static String clock(int tks) {
        int s = tks / 25;
        int h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        String r = (h > 0) ? h + ":" + (m < 10 ? "0" : "") + m : "" + m;
        return r + ":" + (sec < 10 ? "0" : "") + sec;
    }

    private SaveData snapshot() {
        SaveData d = new SaveData();
        d.area = world.area;
        d.px = player.x;
        d.py = player.y;
        d.kills = npcs.kills;
        d.playTicks = played;
        d.health = 100;
        d.money = 0;
        d.stamp = System.currentTimeMillis();
        return d;
    }

    private void autoSave() {
        SaveManager.save(SaveManager.AUTO, snapshot());
    }

    private void doSave(int slot) {
        boolean ok = SaveManager.save(slot, snapshot());
        noteOk = ok;
        menuNote = ok ? "Saved to slot " + (slot + 1) : "Save failed: " + SaveManager.lastError;
        menuMode = 1;
        menuSel = 1;
    }
        // ---------------- main loop ----------------

    public void run() {
        Graphics g = getGraphics();
        while (running) {
            long t0 = System.currentTimeMillis();
            try {
            if (menuMode == 0) {
                if (transT > 0) {
                    transT--;
                    if (transT == 20) {
                        try {
                            switchArea(transArea);
                        } catch (Throwable e) {
                            transT = 0;
                            transArea = -1;
                            exitCool = 150;
                            say("Could not load: " + describe(e), 200);
                        }
                    }
                } else {
                    if (interact) {
                        interact = false;
                        toggleVehicle();
                    }
                    int keys = getKeyStates();
                    if (driving != null) {
                        driving.drive(keys, world, vehicles);
                        player.x = driving.x;
                        player.y = driving.y;
                        skids.update(driving, world);
                    } else {
                        player.update(keys, world);
                    }
                    skids.age();
                    npcs.driven = driving;
                    npcs.playerOnFoot = (driving == null);
                    vehicles.update(player);
                    npcs.update(world, player);
                    if (driving != null) npcs.vehicleHit(driving);
                    vehicles.updateTraffic(npcs, player, driving == null);
                    pigeons.update(player, driving);
                    ticks++;
                    played++;
                    autoT++;
                    if (autoT >= 750) {
                        autoT = 0;
                        autoSave();
                    }
                    if (ticks % 10 == 0) {
                        String dn2 = world.districtName(player.x, player.y);
                        if (!dn2.equals(district)) {
                            district = dn2;
                            bannerT = 75;
                        }
                    }
                    if (bannerT > 0) bannerT--;
                    if (exitCool > 0) exitCool--;
                    int ex = (exitCool > 0) ? -1 : world.exitAt(player.x, player.y);
                    if (ex >= 0) {
                        transArea = ex;
                        transT = 40;
                    }
                }
            }
            if (msgT > 0) msgT--;
            render(g);
            flushGraphics();
            } catch (Throwable e) {
                String why = describe(e);
                try {
                    g.setColor(0x000000);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(0xFF5050);
                    g.setFont(font);
                    g.drawString("Error:", 4, 20, Graphics.TOP | Graphics.LEFT);
                    g.setColor(0xFFFFFF);
                    for (int i = 0; i * 28 < why.length() && i < 6; i++)
                        g.drawString(why.substring(i * 28, Math.min(why.length(), i * 28 + 28)), 4, 40 + i * (font.getHeight() + 2),
                                Graphics.TOP | Graphics.LEFT);
                    flushGraphics();
                } catch (Throwable t2) {
                }
                msgT = 0;
            }
            long wait = FRAME_MS - (System.currentTimeMillis() - t0);
            if (wait < 5) wait = 5;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
            }
        }
    }

    private static String describe(Throwable e) {
        String m = e.getMessage();
        String s = e.getClass().getName() + (m != null ? ": " + m : "");
        return s.length() > 150 ? s.substring(0, 150) : s;
    }

    private void say(String text, int ticksToShow) {
        msg = text;
        msgT = ticksToShow;
    }

    private void toggleVehicle() {
        if (driving != null) {
            int spd = driving.sp < 0 ? -driving.sp : driving.sp;
            if (spd > 48) {
                say("Slow down to get out", 50);
                return;
            }
            int[] spot = exitSpot(driving);
            if (spot == null) {
                say("No room to get out", 50);
                return;
            }
            driving.driven = false;
            driving.sp = 0;
            player.x = spot[0];
            player.y = spot[1];
            driving = null;
        } else {
            Vehicle v = vehicles.nearest(player.x, player.y, 40);
            if (v != null && v.occupants > 0) {
                say("Occupied! Can't enter", 50);
            } else if (v != null) {
                driving = v;
                v.driven = true;
                v.sp = 0;
                player.x = v.x;
                player.y = v.y;
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

    // ---------------- drawing ----------------

    private void add(int key, int type, int idx) {
        if (dn >= dk.length) return;
        dk[dn] = key;
        dt[dn] = type;
        di[dn] = idx;
        dn++;
    }

    private void sortDrawList() {
        for (int i = 1; i < dn; i++) {
            int k = dk[i], t = dt[i], x = di[i];
            int j = i - 1;
            while (j >= 0 && dk[j] > k) {
                dk[j + 1] = dk[j];
                dt[j + 1] = dt[j];
                di[j + 1] = di[j];
                j--;
            }
            dk[j + 1] = k;
            dt[j + 1] = t;
            di[j + 1] = x;
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
        pigeons.drawShadows(g, camX, camY, sw, sh);
        npcs.drawGround(g, camX, camY, sw, sh);

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
        for (int i = 0; i < pigeons.pigeons.length; i++) {
            Pigeon pg = pigeons.pigeons[i];
            if (pg.flying) continue;
            int sx = pg.x - camX, sy = pg.y - camY;
            if (sx < -20 || sx > sw + 20 || sy < -6 || sy > sh + 20) continue;
            add(pg.y, 4, i);
        }
        if (driving == null) add(player.y, 3, 0);
        sortDrawList();
        for (int i = 0; i < dn; i++) {
            switch (dt[i]) {
                case 0: world.drawTree(g, di[i], camX, camY); break;
                case 1: npcs.npcs[di[i]].draw(g, camX, camY); break;
                case 2: vehicles.drawVehicle(g, vehicles.list[di[i]], camX, camY); break;
                case 4: pigeons.drawOne(g, di[i], camX, camY); break;
                default: player.draw(g, camX, camY); break;
            }
        }
        pigeons.drawFlying(g, camX, camY, sw, sh);
        npcs.drawBubbles(g, font, camX, camY);

        g.setFont(font);
        g.setColor(0x000000);
        g.fillRect(0, 0, sw, font.getHeight() + 2);
        if (ticks < 150) {
            g.setColor(0xE01010);
            g.drawString("KILLER'S WRATH", 4, 1, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0xFFFFFF);
            g.drawString("Fleeing: " + npcs.count(Npc.FLEEING) + "  Hiding: " + npcs.count(Npc.INSIDE)
                    + "  Down: " + npcs.kills, 4, 1, Graphics.TOP | Graphics.LEFT);
        }

        String line = null;
        if (msgT > 0) line = msg;
        else if (driving != null) {
            int spd = driving.sp < 0 ? -driving.sp : driving.sp;
            line = (spd * 90 / 256) + " km/h" + (driving.sp < 0 ? " R" : "") + "   # = get out";
        } else if (vehicles.nearest(player.x, player.y, 40) != null)
            line = (vehicles.nearest(player.x, player.y, 40).occupants > 0) ? "Occupied - can't get in" : "# = get in";
        else if (ticks < 150) line = "Arrows: move  FIRE: run  L: menu  1: map";
        if (line != null) {
            g.setColor(0x000000);
            g.fillRect(0, sh - font.getHeight() - 3, sw, font.getHeight() + 3);
            g.setColor(0xFFFFFF);
            g.drawString(line, 4, sh - font.getHeight() - 1, Graphics.TOP | Graphics.LEFT);
        }

        if (bannerT > 0 && menuMode == 0) {
            int bw = font.stringWidth(district) + 16, bh = font.getHeight() + 6;
            int bx = (sw - bw) / 2, by = font.getHeight() + 8;
            g.setColor(0x050806);
            g.fillRect(bx, by, bw, bh);
            g.setColor(0x8A1414);
            g.drawRect(bx, by, bw - 1, bh - 1);
            g.setColor(0xE8C23A);
            g.drawString(district, sw / 2, by + 3, Graphics.TOP | Graphics.HCENTER);
        }

        if (sw >= 160) {
            int mw = mini.getWidth(), mh = mini.getHeight();
            int mx = sw - mw - 3, my = sh - mh - font.getHeight() - 8;
            g.drawImage(mini, mx, my, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xFFFFFF);
            g.drawRect(mx - 1, my - 1, mw + 1, mh + 1);
            g.setColor(0xFF0000);
            g.fillRect(mx + player.x / (World.TS * World.MINI) - 1, my + player.y / (World.TS * World.MINI) - 1, 3, 3);
        }

        if (menuMode != 0 && menuMode != 4) drawMenu(g, sw, sh);
        if (menuMode == 4) drawBigMap(g, sw, sh);

        if (transT > 0) {
            g.setColor(0x000000);
            g.fillRect(0, 0, sw, sh);
            g.setColor(0xC41818);
            g.setFont(font);
            g.drawString(transArea == World.FOREST ? "BLACKWOOD FOREST" : "THE CITY",
                    sw / 2, sh / 2 - font.getHeight() / 2, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0x8A9A8E);
            g.drawString("Loading...", sw / 2, sh / 2 + font.getHeight(), Graphics.TOP | Graphics.HCENTER);
        }
    }
        private void drawBigMap(Graphics g, int sw, int sh) {
        g.setColor(0x000000);
        g.fillRect(0, 0, sw, sh);
        g.setFont(font);
        int mw = mini.getWidth(), mh = mini.getHeight();
        int sc = 1;
        while (mw * (sc + 1) <= sw - 8 && mh * (sc + 1) <= sh - 32) sc++;
        int dw = mw * sc, dh = mh * sc;
        int mx = (sw - dw) / 2, my = (sh - dh) / 2 + 4;
        try {
            int[] src = new int[mw * mh];
            mini.getRGB(src, 0, mw, 0, 0, mw, mh);
            int[] dst = new int[dw * dh];
            for (int y = 0; y < mh; y++) {
                for (int x = 0; x < mw; x++) {
                    int c = src[y * mw + x];
                    for (int dy = 0; dy < sc; dy++) {
                        int row = (y * sc + dy) * dw + x * sc;
                        for (int dx = 0; dx < sc; dx++) dst[row + dx] = c;
                    }
                }
            }
            Image big = Image.createRGBImage(dst, dw, dh, true);
            g.drawImage(big, mx, my, Graphics.TOP | Graphics.LEFT);
        } catch (Throwable e) {
            g.drawImage(mini, mx, my, Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0xFFFFFF);
        g.drawRect(mx - 1, my - 1, dw + 1, dh + 1);
        g.setColor(0xFF0000);
        int dotX = mx + (player.x / (World.TS * World.MINI)) * sc;
        int dotY = my + (player.y / (World.TS * World.MINI)) * sc;
        g.fillRect(dotX - 1, dotY - 1, 3, 3);
        g.setColor(0xE8C23A);
        g.drawString(world.area == World.FOREST ? "BLACKWOOD FOREST - MAP" : "THE CITY - MAP", sw / 2, 2, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xFFFFFF);
        g.drawString(district, sw / 2, sh - font.getHeight() - 2, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xAAAAAA);
        g.drawString("1 / L / * = close", sw / 2, sh - (font.getHeight() + 2) * 2 - 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void drawMenu(Graphics g, int sw, int sh) {
        String head;
        String[] items;
        if (menuMode == 1) {
            head = "PAUSED";
            items = new String[] {"Resume", "Save Game", "Quit to Title"};
        } else if (menuMode == 2) {
            head = "SAVE GAME";
            items = new String[SaveManager.SLOTS + 1];
            for (int i = 0; i < SaveManager.SLOTS; i++) items[i] = slotLabel[i];
            items[SaveManager.SLOTS] = "Back";
        } else {
            head = "QUIT TO TITLE?";
            items = new String[] {"Yes, quit", "No, keep playing"};
        }
        int lineH = font.getHeight() + 7;
        int boxW = sw - 20;
        if (boxW > 230) boxW = 230;
        int extra = (menuMode == 3) ? lineH : 0;
        int noteH = (menuNote != null && menuMode != 3) ? lineH * 2 : 0;
        int boxH = lineH * (items.length + 1) + 14 + extra + noteH;
        int bx = (sw - boxW) / 2, by = (sh - boxH) / 2;
        g.setColor(0x050806);
        g.fillRect(bx, by, boxW, boxH);
        g.setColor(0x8A1414);
        g.drawRect(bx, by, boxW - 1, boxH - 1);
        g.drawRect(bx + 2, by + 2, boxW - 5, boxH - 5);
        g.setFont(font);
        g.setColor(0xC41818);
        g.drawString(head, sw / 2, by + 8, Graphics.TOP | Graphics.HCENTER);
        int y = by + 8 + lineH;
        if (noteH > 0) {
            g.setColor(noteOk ? 0x58C060 : 0xE05050);
            String n1 = menuNote, n2 = null;
            int maxW = boxW - 16;
            if (font.stringWidth(n1) > maxW) {
                int cut = n1.length();
                while (cut > 1 && font.stringWidth(n1.substring(0, cut)) > maxW) cut--;
                n2 = n1.substring(cut);
                n1 = n1.substring(0, cut);
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
                g.setColor(0x3A0A0A);
                g.fillRect(bx + 5, y - 3, boxW - 10, lineH - 1);
                g.setColor(0xE8C23A);
            } else {
                g.setColor(0x8A9A8E);
            }
            g.drawString(items[i], bx + 12, y, Graphics.TOP | Graphics.LEFT);
            y += lineH;
        }
    }
    }
