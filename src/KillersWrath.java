
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

public class KillersWrath extends MIDlet {

    private City city;

    public void startApp() {
        if (city == null) {
            city = new City();
        }
        Display.getDisplay(this).setCurrent(city);
    }

    public void pauseApp() {}

    public void destroyApp(boolean b) {}

    class City extends Canvas {

        int playerX = 400;
        int playerY = 300;
        int cameraX, cameraY;

        boolean up, down, left, right;

        public City() {
            setFullScreenMode(true);
        }

        protected void keyPressed(int key) {
            int k = getGameAction(key);

            if (k == UP) up = true;
            if (k == DOWN) down = true;
            if (k == LEFT) left = true;
            if (k == RIGHT) right = true;
        }

        protected void keyReleased(int key) {
            int k = getGameAction(key);

            if (k == UP) up = false;
            if (k == DOWN) down = false;
            if (k == LEFT) left = false;
            if (k == RIGHT) right = false;
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            if (up) playerY -= 3;
            if (down) playerY += 3;
            if (left) playerX -= 3;
            if (right) playerX += 3;

            cameraX = playerX - w / 2;
            cameraY = playerY - h / 2;

            // City ground
            g.setColor(75, 83, 75);
            g.fillRect(0, 0, w, h);

            // Main road
            g.setColor(45, 45, 47);
            g.fillRect(-cameraX, 180-cameraY, 1200, 180);

            // Road markings
            g.setColor(220, 210, 170);
            for (int x = 0; x < 1200; x += 100) {
                g.fillRect(x-cameraX, 268-cameraY, 45, 3);
            }

            // Buildings
            drawBuilding(g, 80, 50, 180, 100);
            drawBuilding(g, 400, 50, 200, 100);
            drawBuilding(g, 750, 50, 180, 100);
            drawBuilding(g, 80, 400, 200, 140);
            drawBuilding(g, 450, 400, 200, 140);
            drawBuilding(g, 800, 400, 180, 140);

            // Player
            g.setColor(0, 0, 0);
            g.fillArc(w/2-8, h/2-8, 16, 16, 0, 360);

            g.setColor(180, 20, 25);
            g.fillArc(w/2-5, h/2-5, 10, 10, 0, 360);

            // Interface
            g.setColor(255, 255, 255);
            g.drawString("KILLER'S WRATH", 5, 5, Graphics.TOP | Graphics.LEFT);
            g.drawString("GWAGWALADA", 5, 22, Graphics.TOP | Graphics.LEFT);
            g.drawString("HEALTH: 100", 5, 39, Graphics.TOP | Graphics.LEFT);

            repaint();
        }

        private void drawBuilding(Graphics g, int x, int y,
                                  int width, int height) {
            x -= cameraX;
            y -= cameraY;

            g.setColor(30, 30, 32);
            g.fillRect(x+5, y+7, width, height);

            g.setColor(105, 100, 91);
            g.fillRect(x, y, width, height);

            g.setColor(145, 135, 120);
            g.drawRect(x, y, width, height);

            g.setColor(45, 45, 48);
            g.fillRect(x+width/2-12, y+height-25, 24, 25);
        }
    }
}
