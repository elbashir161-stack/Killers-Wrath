import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/** Killer. (x, y) is the point between his feet in world pixels. */
public class Player {
    private static final int[] CYCLE = {1, 0, 2, 0};

    public int x, y;
    public int dir = Sprites.DOWN;
    public boolean running = false;
    private int frame = 0;
    private int animTick = 0;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update(int keys, World w) {
        int dx = 0, dy = 0;
        if ((keys & GameCanvas.LEFT_PRESSED) != 0) dx = -1;
        if ((keys & GameCanvas.RIGHT_PRESSED) != 0) dx = 1;
        if ((keys & GameCanvas.UP_PRESSED) != 0) dy = -1;
        if ((keys & GameCanvas.DOWN_PRESSED) != 0) dy = 1;
        running = (keys & GameCanvas.FIRE_PRESSED) != 0;

        if (dx == 0 && dy == 0) {
            frame = 0;
            animTick = 0;
            return;
        }
        if (dx != 0) dir = (dx < 0) ? Sprites.LEFT : Sprites.RIGHT;
        else dir = (dy < 0) ? Sprites.UP : Sprites.DOWN;

        int speed = running ? 4 : 2;
        move(dx, 0, speed * (dx < 0 ? -dx : dx), w);
        move(0, dy, speed * (dy < 0 ? -dy : dy), w);

        animTick++;
        int rate = running ? 2 : 4;
        frame = CYCLE[(animTick / rate) % 4];
    }

    private void move(int dx, int dy, int steps, World w) {
        for (int i = 0; i < steps; i++) {
            if (free(x + dx, y + dy, w)) {
                x += dx;
                y += dy;
            } else {
                return;
            }
        }
    }

    /** Feet hitbox: 8 wide, 5 tall. */
    private boolean free(int nx, int ny, World w) {
        return !w.solidAt(nx - 4, ny - 5) && !w.solidAt(nx + 3, ny - 5)
                && !w.solidAt(nx - 4, ny - 1) && !w.solidAt(nx + 3, ny - 1);
    }

    public void draw(Graphics g, int camX, int camY) {
        g.drawImage(Sprites.killer[dir][frame], x - 8 - camX, y - 23 - camY,
                Graphics.TOP | Graphics.LEFT);
    }
}
