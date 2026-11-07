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
                driving.drive(keys, world, vehicles, npcs);
                player.x = driving.x;
                player.y = driving.y;
                player.dir = driving.dir;
            } else {
                player.update(keys, world);
            }
            vehicles.update(player);
            npcs.update(world, player);
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

    private void say(String text) {
        msg = text;
        msgT = 50;
    }

    private void toggleVehicle() {
        if (driving != null) {
            if (driving.sp > 48) {
                say("Slow down to get out");
                return;
            }
            int[] spot = exitSpot(driving);
            if (spot == null) {
                say("No room to get out");
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
                player.dir = v.dir;
            }
        }
    }

    private int[] exitSpot(Vehicle v) {
        int hx = v.hx(), hy = v.hy();
        int[][] c;
        if (v.dir >= 2) {
            c = new int[][] {{v.x, v.y - hy - 2}, {v.x, v.y + hy + 8}, {v.x - hx - 8, v.y}, {v.x + hx + 8, v.y}};
        } else {
            c = new int[][] {{v.x - hx - 8, v.y}, {v.x + hx + 8, v.y}, {v.x, v.y - hy - 2}, {v.x, v.y + hy + 8}};
        }
        for (int i = 0; i < c.length; i++)
            if (player.freeAt(c[i][0], c[i][1], world)) return c[i];
        return null;
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
        vehicles.drawVehicles(g, camX, camY, sw, sh, player.y, false);
        npcs.drawNpcs(g, camX, camY, sw, sh, player.y, false);
        if (driving == null) player.draw(g, camX, camY);
        vehicles.drawVehicles(g, camX, camY, sw, sh, player.y, true);
        npcs.drawNpcs(g, camX, camY, sw, sh, player.y, true);
        world.drawCrowns(g, camX, camY, sw, sh);
        npcs.drawBubbles(g, font, camX, camY);

        g.setFont(font);
        g.setColor(0x000000);
        g.fillRect(0, 0, sw, font.getHeight() + 2);
        if (ticks < 150) {
            g.setColor(0xE01010);
            g.drawString("KILLER'S WRATH", 4, 1, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0xFFFFFF);
            g.drawString("Fleeing: " + npcs.count(Npc.FLEEING) + "  Hiding: " + npcs.count(Npc.INSIDE),
                    4, 1, Graphics.TOP | Graphics.LEFT);
        }

        // bottom line: messages, hints, speed
        String line = null;
        if (msgT > 0) line = msg;
        else if (driving != null) line = (driving.sp * 90 / 256) + " km/h   # = get out";
        else if (vehicles.nearest(player.x, player.y, 40) != null) line = "# = get in";
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
