import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** Entry point (MIDP 2.0 MIDlet): title screen first, then the game. */
public class KillersWrath extends MIDlet {
    private TitleCanvas title;
    private WorldCanvas game;

    protected void startApp() {
        Display d = Display.getDisplay(this);
        if (game != null) {
            d.setCurrent(game);
            game.start();
            return;
        }
        if (title == null) title = new TitleCanvas(this);
        d.setCurrent(title);
        title.start();
    }

    protected void pauseApp() {
        if (game != null) game.stop();
        if (title != null) title.stop();
    }

    protected void destroyApp(boolean unconditional) {
        if (game != null) game.stop();
        if (title != null) title.stop();
    }

    /** slot -1 = new game, otherwise load that slot. The city takes a moment to build, so do it off-screen. */
    public void startGame(int slot) {
        title.showLoading();
        new Thread(new Loader(this, slot)).start();
    }

    void gameReady(WorldCanvas c) {
        game = c;
        title.stop();
        Display.getDisplay(this).setCurrent(c);
        c.start();
    }

    public void showTitle() {
        if (game != null) {
            game.stop();
            game = null;
        }
        title.reset();
        Display.getDisplay(this).setCurrent(title);
        title.start();
    }

    private static class Loader implements Runnable {
        private final KillersWrath m;
        private final int slot;

        Loader(KillersWrath m, int slot) {
            this.m = m;
            this.slot = slot;
        }

        public void run() {
            SaveData d = (slot >= 0) ? SaveManager.load(slot) : null;
            if (d != null && !d.used) d = null;
            m.gameReady(new WorldCanvas(m, d));
        }
    }
}
