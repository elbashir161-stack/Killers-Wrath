import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/** Game loop, camera and rendering. */
public class WorldCanvas extends GameCanvas implements Runnable {
    private static final int FRAME_MS = 40; // 25 fps

    private final KillersWrath midlet;
    private final World world;
    private final Player player;
    private final NpcManager npcs;
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    private Thread thread;
    private volatile boolean running = false;
    private int ticks = 0;

    public WorldCanvas(KillersWrath midlet) {
        super(true);
        this.midlet = midlet;
        setFullScreenMode(true);
        Sprites.init();
        NpcSprites.init();
        world = new World();
        player = new Player(8 * World.TS, 8 * World.TS); // middle of first intersection
        npcs = new NpcManager(world, player, 28);
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
    }

    public void run() {
        Graphics g = getGraphics();
        while (running) {
            long t0 = System.currentTimeMillis();
            player.update(getKeyStates(), world);
            npcs.update(world, player);
            render(g);
            flushGraphics();
            ticks++;
            long wait = FRAME_MS - (System.currentTimeMillis() - t0);
            if (wait < 5) wait = 5;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void render(Graphics g) {
        int sw = getWidth(), sh = getHeight();
        int camX = player.x - sw / 2;
        int camY = player.y - 12 - sh / 2;
        int maxX = World.MW * World.TS - sw, maxY = World.MH * World.TS - sh;
        if (camX > maxX) camX = maxX;
        if (camX < 0) camX = 0;
        if (camY > maxY) camY = maxY;
        if (camY < 0) camY = 0;

        g.setColor(0x000000);
        g.fillRect(0, 0, sw, sh);
        world.draw(g, camX, camY, sw, sh);
        npcs.drawNpcs(g, camX, camY, sw, sh, player.y, false);
        player.draw(g, camX, camY);
        npcs.drawNpcs(g, camX, camY, sw, sh, player.y, true);
        npcs.drawBubbles(g, font, camX, camY);

        g.setFont(font);
        g.setColor(0x000000);
        g.fillRect(0, 0, sw, font.getHeight() + 2);
        if (ticks < 150) {
            g.setColor(0xE01010);
            g.drawString("KILLER'S WRATH", 4, 1, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xFFFFFF);
            g.drawString("Arrows: move   FIRE: run   *: quit", 4, sh - font.getHeight() - 1,
                    Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0xFFFFFF);
            g.drawString("Fleeing: " + npcs.count(Npc.FLEEING) + "  Hiding: " + npcs.count(Npc.INSIDE),
                    4, 1, Graphics.TOP | Graphics.LEFT);
        }
    }
}
