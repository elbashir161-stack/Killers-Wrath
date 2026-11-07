import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** Entry point (MIDP 2.0 MIDlet). */
public class KillersWrath extends MIDlet {
    private WorldCanvas canvas;

    protected void startApp() {
        if (canvas == null) {
            canvas = new WorldCanvas(this);
        }
        Display.getDisplay(this).setCurrent(canvas);
        canvas.start();
    }

    protected void pauseApp() {
        if (canvas != null) canvas.stop();
    }

    protected void destroyApp(boolean unconditional) {
        if (canvas != null) canvas.stop();
    }
}
