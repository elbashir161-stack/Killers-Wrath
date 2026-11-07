import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

/** Game loop, camera and rendering. */
public class WorldCanvas extends GameCanvas implements Runnable {
    private static final int FRAME_MS = 40; // 25 fps

    private final KillersWrath midlet;
    private final World world;
    private final Player player;
    private final VehicleManager vehicles;
    private final NpcManager npcs;
    private final Image mini;
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private Thread thread;
    private volatile boolean running = false;
    private volatile boolean interact = false;
    private Vehicle driving = null;
    private String msg = null;
    private int msgT = 0;
    private int ticks = 0;

    // everything that stands in the world is drawn back-to-front (sorted by its feet / base line)
    private final int[] dk = new int[1024];
    private final int[] dt = new int[1024];
    private final int[] di = new int[1024];
    private int dn = 0;

    public WorldCanvas(KillersWrath midlet) {
        super(true);
        this.midlet = midlet;
        setFullScreenMode(true);
        Sprites.init();
        NpcSprites.init();
        world = new World();
        mini = world.buildMinimap();
        // start on the pavement beside the central avenue, downtown
        player = new Player(380 * World.TS + 8, 120 * World.TS + 8);
        vehicles = new VehicleManager(world, player, 56);
        world.vehicles = vehicles;
        npcs = new NpcManager(world, player, 36);
        world.npcs = npcs;
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
        if (keyCode == KEY_STAR) midlet.notifyDestroyed();
        if (keyCode == KEY_POUND || keyCode == KEY_NUM0) interact = true;
    }

    public void run() {
        Graphics g = getGraphics();
        while (running) {
            long t0 = System.currentTimeMillis();
            if (interact) {
                interact = false;
                toggleVehicle();
            }
            int keys = getKeyStates();
            if (driving != null) {
                driving.drive(keys, world, vehicles);
                player.x = driving.x;
                player.y = driving.y;
            } else {
                player.update(keys, world);
            }
            npcs.driven = driving;
            npcs.playerOnFoot = (driving == null);
            vehicles.update(player);
            npcs.update(world, player);
            if (driving != null) npcs.vehicleHit(driving);
            render(g);
            flushGraphics();
            ticks++;
            if (msgT > 0) msgT--;
            long wait = FRAME_MS - (System.currentTimeMillis() - t0);
            if (wait < 5) wait = 5;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                // ignore
            }
        }
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
            if (v != null) {
                driving = v;
                v.driven = true;
                v.sp = 0;
                player.x = v.x;
                player.y = v.y;
                say("Up = gas   Down = brake / reverse   Left / Right = steer", 120);
            }
        }
    }

    /** A free place beside the vehicle (its sides first, then front and back) for Killer to stand. */
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
        int maxX = World.MW * World.TS - sw, maxY = World.MH * World.TS - sh;
        if (camX > maxX) camX = maxX;
        if (camX < 0) camX = 0;
        if (camY > maxY) camY = maxY;
        if (camY < 0) camY = 0;

        g.setColor(0x000000);
        g.fillRect(0, 0, sw, sh);
        world.draw(g, camX, camY, sw, sh);
        npcs.drawGround(g, camX, camY, sw, sh);

        // collect everything that stands up and draw it back to front
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

        // HUD
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
        } else if (vehicles.nearest(player.x, player.y, 40) != null) line = "# = get in";
        else if (ticks < 150) line = "Arrows: move  FIRE: run  *: quit";
        if (line != null) {
            g.setColor(0x000000);
            g.fillRect(0, sh - font.getHeight() - 3, sw, font.getHeight() + 3);
            g.setColor(0xFFFFFF);
            g.drawString(line, 4, sh - font.getHeight() - 1, Graphics.TOP | Graphics.LEFT);
        }

        // minimap
        if (sw >= 160) {
            int mw = mini.getWidth(), mh = mini.getHeight();
            int mx = sw - mw - 3, my = font.getHeight() + 5;
            g.drawImage(mini, mx, my, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xFFFFFF);
            g.drawRect(mx - 1, my - 1, mw + 1, mh + 1);
            g.setColor(0xFF0000);
            g.fillRect(mx + player.x / (World.TS * 8) - 1, my + player.y / (World.TS * 8) - 1, 3, 3);
        }
    }
}
