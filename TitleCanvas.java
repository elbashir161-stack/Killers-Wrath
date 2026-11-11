import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/** Title screen: a rainy night over the city, Killer on a rooftop with glowing red eyes, and the menu. */
public class TitleCanvas extends Canvas implements Runnable {
    private static final String[] ITEMS = {"Continue", "New Game", "About", "Exit"};

    private final KillersWrath midlet;
    private Thread thread;
    private volatile boolean running = false;
    private int w = 0, h = 0, horizon = 0;
    private int tick = 0, sel = 0, mode = 0; // mode: 0 menu, 1 about, 2 loading, 3 load list, 4 confirm new
    private int loadSel = 0; // 0=autosave/latest, 1=slot1, 2=slot2, 3=slot3, 4=back
    private int confirmSel = 0; // 0=No, 1=Yes
    private int flash = 0;
    private boolean hasSave;
    private final Font fBig = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    private final Font fMed = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    private final Font fSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private int[] starX, starY;
    private int nFar, nNear;
    private int[] farX, farW, farH;
    private int[] nearX, nearW, nearH, nearKind;
    private int[] rainX, rainY;
    private int seed = 4242;

    public TitleCanvas(KillersWrath midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);
        reset();
    }

    public void reset() {
        mode = 0;
        hasSave = SaveManager.hasAny();
        sel = hasSave ? 0 : 1;
    }

    public void showLoading() {
        mode = 2;
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

    public void run() {
        while (running) {
            tick++;
            if (flash > 0) flash--;
            else if (tick % 260 == 120) flash = 3;
            repaint();
            serviceRepaints();
            try {
                Thread.sleep(70);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    // ---------------- input ----------------

    protected void keyPressed(int keyCode) {
        if (mode == 2) return;
        if (mode == 1) {
            mode = 0;
            return;
        }
        if (mode == 3) {
            int ga = 0;
            try { ga = getGameAction(keyCode); } catch (Exception e) {}
            if (ga == UP) loadSel = (loadSel + 4) % 5;
            else if (ga == DOWN) loadSel = (loadSel + 1) % 5;
            else if (ga == FIRE || keyCode == KEY_NUM5) chooseLoad();
            else if (keyCode == KEY_NUM0 || keyCode == KEY_STAR) mode = 0;
            return;
        }
        if (mode == 4) {
            int ga = 0;
            try { ga = getGameAction(keyCode); } catch (Exception e) {}
            if (ga == UP || ga == DOWN || ga == LEFT || ga == RIGHT) confirmSel = 1 - confirmSel;
            else if (ga == FIRE || keyCode == KEY_NUM5) {
                if (confirmSel == 1) midlet.startGame(-1);
                else mode = 0;
            }
            else if (keyCode == KEY_NUM0 || keyCode == KEY_STAR) mode = 0;
            return;
        }
        int ga = 0;
        try {
            ga = getGameAction(keyCode);
        } catch (Exception e) {
            // ignore
        }
        if (ga == UP) sel = (sel + ITEMS.length - 1) % ITEMS.length;
        else if (ga == DOWN) sel = (sel + 1) % ITEMS.length;
        else if (ga == FIRE || keyCode == KEY_NUM5) choose();
    }

    protected void pointerPressed(int x, int y) {
        if (mode == 2) return;
        if (mode == 1) {
            mode = 0;
            return;
        }
        if (mode == 3 || mode == 4) {
            mode = 0;
            return;
        }
        int lineH = fMed.getHeight() + 5;
        int i = (y - itemY(0) + 3) / lineH;
        if (y >= itemY(0) - 3 && i >= 0 && i < ITEMS.length) {
            sel = i;
            choose();
        }
    }

    private void chooseLoad() {
        if (loadSel == 4) { mode = 0; return; }
        int slotToLoad = -2;
        if (loadSel == 0) {
            if (hasSave) slotToLoad = SaveManager.latestSlot();
        } else {
            SaveData d = SaveManager.load(loadSel);
            if (d != null && d.used) slotToLoad = loadSel;
        }
        if (slotToLoad != -2) midlet.startGame(slotToLoad);
    }

    private void choose() {
        if (sel == 0) {
            if (hasSave) { loadSel = 0; mode = 3; }
        } else if (sel == 1) {
            if (hasSave) { confirmSel = 0; mode = 4; }
            else midlet.startGame(-1);
        } else if (sel == 2) {
            mode = 1;
        } else {
            midlet.notifyDestroyed();
        }
    }

    // ---------------- scenery ----------------

    private int rnd(int n) {
        seed = seed * 1103515245 + 12345;
        return ((seed >>> 16) & 0x7FFF) % n;
    }

    private void layout() {
        horizon = h * 56 / 100;
        seed = 4242;
        starX = new int[48];
        starY = new int[48];
        for (int i = 0; i < 48; i++) {
            starX[i] = rnd(w);
            starY[i] = rnd(horizon - 30);
        }
        farX = new int[64]; farW = new int[64]; farH = new int[64];
        nFar = 0;
        int x = -4;
        while (x < w && nFar < 64) {
            int bw = 12 + rnd(16);
            farX[nFar] = x;
            farW[nFar] = bw;
            farH[nFar] = horizon / 5 + rnd(horizon / 4);
            nFar++;
            x += bw - 2 + rnd(4);
        }
        nearX = new int[40]; nearW = new int[40]; nearH = new int[40]; nearKind = new int[40];
        nNear = 0;
        x = -6;
        boolean roof = false, hosp = false, tower = false;
        while (x < w && nNear < 40) {
            int bw = 20 + rnd(22);
            int bh = horizon / 6 + rnd(horizon / 4);
            int kind = 0;
            if (!roof && x > w * 10 / 100) { bw = 36; bh = horizon * 40 / 100; kind = 1; roof = true; }
            else if (!hosp && x > w * 45 / 100) { bw = 44; bh = horizon * 26 / 100; kind = 2; hosp = true; }
            else if (!tower && x > w * 80 / 100) { bw = 16; bh = horizon * 55 / 100; kind = 3; tower = true; }
            nearX[nNear] = x;
            nearW[nNear] = bw;
            nearH[nNear] = bh;
            nearKind[nNear] = kind;
            nNear++;
            x += bw + rnd(3);
        }
        rainX = new int[30];
        rainY = new int[30];
        for (int i = 0; i < 30; i++) {
            rainX[i] = rnd(w + 20);
            rainY[i] = rnd(h);
        }
    }

    // ---------------- drawing ----------------

    protected void paint(Graphics g) {
        int cw = getWidth(), ch = getHeight();
        if (cw != w || ch != h) {
            w = cw;
            h = ch;
            layout();
        }
        sky(g);
        moon(g);
        city(g);
        g.setColor(0x040706);
        g.fillRect(0, horizon, w, h - horizon);
        rain(g);
        if (mode == 1) {
            about(g);
            return;
        }
        title(g);
        if (mode == 2) loading(g);
        else if (mode == 3) loadMenu(g);
        else if (mode == 4) confirmMenu(g);
        else menu(g);
    }

    private void sky(Graphics g) {
        for (int y = 0; y < horizon; y += 4) {
            int t = y * 100 / horizon;
            int r = 0x05 + (0x1B - 0x05) * t / 100;
            int gr = 0x0A + (0x2E - 0x0A) * t / 100;
            int b = 0x09 + (0x24 - 0x09) * t / 100;
            if (flash > 0) { r += 40; gr += 48; b += 44; }
            g.setColor(r << 16 | gr << 8 | b);
            g.fillRect(0, y, w, 4);
        }
        g.setColor(0xB8C4B0);
        for (int i = 0; i < 48; i++) {
            if ((i + tick / 6) % 11 == 0) continue;   // twinkle
            g.fillRect(starX[i], starY[i], 1, 1);
        }
    }

    private void moon(Graphics g) {
        int r = w / 11, cx = w * 76 / 100, cy = horizon * 28 / 100;
        g.setColor(0x24382C);
        g.fillArc(cx - r - 5, cy - r - 5, 2 * r + 10, 2 * r + 10, 0, 360);
        g.setColor(0xD4DBC4);
        g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);
        g.setColor(0xB5BFA6);
        g.fillArc(cx - r / 2, cy - r / 3, r / 2, r / 2, 0, 360);
        g.fillArc(cx + r / 5, cy + r / 4, r / 3, r / 3, 0, 360);
        // drifting cloud
        int cxl = (tick / 3 + 20) % (w + 100) - 50;
        g.setColor(0x13201A);
        g.fillRoundRect(cxl, cy - 4, 70, 11, 11, 11);
        g.fillRoundRect(cxl + 14, cy - 10, 40, 11, 11, 11);
    }

    private void city(Graphics g) {
        for (int i = 0; i < nFar; i++) {
            int top = horizon - farH[i];
            g.setColor(0x0C1612);
            g.fillRect(farX[i], top, farW[i], farH[i]);
            g.setColor(0x2C4A38);
            for (int wy = top + 4; wy < horizon - 2; wy += 6)
                for (int wx = farX[i] + 3; wx < farX[i] + farW[i] - 2; wx += 5)
                    if (((i * 13 + wx * 7 + wy * 3) & 0xFF) % 11 == 0) g.fillRect(wx, wy, 1, 2);
        }
        for (int i = 0; i < nNear; i++) {
            int bx = nearX[i], bw = nearW[i], top = horizon - nearH[i];
            g.setColor(0x040706);
            g.fillRect(bx, top, bw, nearH[i]);
            g.setColor(0x0E1C16);
            g.fillRect(bx, top, bw, 1);
            for (int wy = top + 5; wy < horizon - 6; wy += 8) {
                for (int wx = bx + 4; wx < bx + bw - 5; wx += 6) {
                    int hs = ((i * 31 + wx * 17 + wy * 7) & 0xFF);
                    if (hs % 4 != 0) continue;
                    if (hs % 17 == 0 && (tick / 4 + hs) % 9 < 2) continue;   // flickering light
                    g.setColor(((hs >> 1) & 1) == 0 ? 0xC9D26B : 0x6FB3A0);
                    g.fillRect(wx, wy, 3, 4);
                }
            }
            int kind = nearKind[i];
            if (kind == 1) killer(g, bx + bw / 2, top);
            else if (kind == 2) {
                g.setColor((tick / 10) % 2 == 0 ? 0xD02020 : 0x7A1414);
                int cx = bx + bw / 2, cy = top + 10;
                g.fillRect(cx - 2, cy - 6, 4, 12);
                g.fillRect(cx - 6, cy - 2, 12, 4);
            } else if (kind == 3) {
                g.setColor(0x040706);
                g.fillRect(bx + bw / 2, top - 16, 1, 16);
                if ((tick / 8) % 2 == 0) {
                    g.setColor(0xFF2020);
                    g.fillRect(bx + bw / 2 - 1, top - 18, 3, 3);
                }
            }
        }
    }

    /** Silhouette of Killer: bald, long coat, red eyes that glow and blink. */
    private void killer(Graphics g, int kx, int roofY) {
        g.setColor(0x000000);
        g.fillRect(kx - 3, roofY - 9, 3, 9);
        g.fillRect(kx + 1, roofY - 9, 3, 9);
        g.fillRect(kx - 5, roofY - 21, 10, 13);
        g.fillRect(kx - 7, roofY - 20, 2, 10);
        g.fillRect(kx + 5, roofY - 20, 2, 10);
        g.fillTriangle(kx - 5, roofY - 9, kx + 5, roofY - 9, kx - 1, roofY - 2);
        g.fillArc(kx - 4, roofY - 29, 9, 10, 0, 360);
        g.setColor(0x2C3A32);
        g.fillRect(kx + 4, roofY - 26, 1, 4);                  // moonlight on the head
        if (tick % 70 < 62) {
            g.setColor(0x5A0F0F);
            g.fillRect(kx - 3, roofY - 26, 7, 3);
            g.setColor(0xFF2A2A);
            g.fillRect(kx - 2, roofY - 25, 2, 1);
            g.fillRect(kx + 1, roofY - 25, 2, 1);
        }
    }

    private void rain(Graphics g) {
        g.setColor(0x2E4036);
        for (int i = 0; i < 30; i++) {
            rainX[i] -= 2;
            rainY[i] += 7;
            if (rainY[i] > h || rainX[i] < -4) {
                rainY[i] = -rnd(30);
                rainX[i] = rnd(w + 20);
            }
            g.drawLine(rainX[i], rainY[i], rainX[i] - 2, rainY[i] + 5);
        }
    }

    private void title(Graphics g) {
        g.setFont(fBig);
        String a = "KILLER'S", b = "WRATH";
        int y1 = h * 6 / 100, y2 = y1 + fBig.getHeight() - 2;
        g.setColor(0x2A0404);
        g.drawString(a, w / 2 + 2, y1 + 2, Graphics.TOP | Graphics.HCENTER);
        g.drawString(b, w / 2 + 2, y2 + 2, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xC41818);
        g.drawString(a, w / 2, y1, Graphics.TOP | Graphics.HCENTER);
        g.drawString(b, w / 2, y2, Graphics.TOP | Graphics.HCENTER);
        // blood dripping from the letters
        int bw = fBig.stringWidth(b), bx0 = (w - bw) / 2;
        int baseY = y2 + fBig.getHeight() - 3;
        g.setColor(0x8A1010);
        for (int i = 0; i < 5; i++) {
            int dx = bx0 + 4 + i * (bw - 8) / 4;
            int len = 2 + ((tick / 2 + i * 7) % 14);
            g.fillRect(dx, baseY, 2, len);
            g.fillRect(dx - 1, baseY + len, 4, 2);
        }
        g.setFont(fSmall);
        g.setColor(0xA89F80);
        g.drawString("The city knows his name", w / 2, baseY + 22, Graphics.TOP | Graphics.HCENTER);
    }

    private int itemY(int i) {
        return horizon + h * 5 / 100 + i * (fMed.getHeight() + 5);
    }

    private void menu(Graphics g) {
        g.setFont(fMed);
        for (int i = 0; i < ITEMS.length; i++) {
            boolean disabled = (i == 0 && !hasSave);
            String t = ITEMS[i];
            if (i == sel) {
                g.setColor(disabled ? 0x6A5A20 : 0xE8C23A);
                if ((tick / 8) % 2 == 0) t = "> " + t + " <";
                else t = "  " + t + "  ";
            } else {
                g.setColor(disabled ? 0x2A332E : 0x7A8C80);
            }
            g.drawString(t, w / 2, itemY(i), Graphics.TOP | Graphics.HCENTER);
        }
        g.setFont(fSmall);
        g.setColor(0x3E4C44);
        // save info under Continue
        if (hasSave) {
            g.setColor(0x7A8C80);
            g.drawString("Save: Slot " + SaveManager.latestSlot(), w / 2, itemY(0) + fMed.getHeight() + 1, Graphics.TOP | Graphics.HCENTER);
        } else {
            g.setColor(0x3E4C44);
            g.drawString("No save yet", w / 2, itemY(0) + fMed.getHeight() + 1, Graphics.TOP | Graphics.HCENTER);
        }
        g.setColor(0x3E4C44);
        g.drawString("Up/Down move  5/FIRE select", w / 2, h - fSmall.getHeight() * 2 - 4, Graphics.TOP | Graphics.HCENTER);
        g.drawString("L: Pause  1: Map  F: Action  *: Menu", w / 2, h - fSmall.getHeight() - 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void loadMenu(Graphics g) {
        g.setFont(fMed);
        g.setColor(0xE8C23A);
        g.drawString("LOAD GAME", w / 2, itemY(0) - fMed.getHeight() - 4, Graphics.TOP | Graphics.HCENTER);
        String[] items = new String[5];
        items[0] = "Autosave (Slot " + SaveManager.latestSlot() + ")";
        for (int i = 1; i <= 3; i++) {
            SaveData d = SaveManager.load(i);
            if (d != null && d.used) items[i] = "Slot " + i + " - Saved";
            else items[i] = "Slot " + i + " - Empty";
        }
        items[4] = "Back";
        for (int i = 0; i < 5; i++) {
            boolean empty = (i >= 1 && i <= 3 && items[i].endsWith("Empty"));
            String t = items[i];
            if (i == loadSel) {
                g.setColor(empty ? 0x6A5A20 : 0xE8C23A);
                if ((tick / 8) % 2 == 0) t = "> " + t + " <";
                else t = "  " + t + "  ";
            } else {
                g.setColor(empty ? 0x2A332E : 0x7A8C80);
            }
            g.drawString(t, w / 2, itemY(i + 1), Graphics.TOP | Graphics.HCENTER);
        }
        g.setFont(fSmall);
        g.setColor(0x3E4C44);
        g.drawString("Up/Down move  5/FIRE load  0 back", w / 2, h - fSmall.getHeight() - 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void confirmMenu(Graphics g) {
        g.setFont(fMed);
        g.setColor(0xE8C23A);
        g.drawString("NEW GAME?", w / 2, itemY(0), Graphics.TOP | Graphics.HCENTER);
        g.setFont(fSmall);
        g.setColor(0xB8C0A8);
        g.drawString("This will overwrite your save.", w / 2, itemY(1), Graphics.TOP | Graphics.HCENTER);
        g.drawString("Are you sure?", w / 2, itemY(1) + fSmall.getHeight() + 2, Graphics.TOP | Graphics.HCENTER);
        String[] opts = {"No, keep my save", "Yes, start over"};
        g.setFont(fMed);
        for (int i = 0; i < 2; i++) {
            String t = opts[i];
            if (i == confirmSel) {
                g.setColor(0xE8C23A);
                if ((tick / 8) % 2 == 0) t = "> " + t + " <";
            } else {
                g.setColor(0x7A8C80);
            }
            g.drawString(t, w / 2, itemY(3 + i), Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void loading(Graphics g) {
        g.setFont(fMed);
        g.setColor(0xE8C23A);
        String dots = "";
        for (int i = 0; i < (tick / 5) % 4; i++) dots += ".";
        g.drawString("LOADING" + dots, w / 2, itemY(1), Graphics.TOP | Graphics.HCENTER);
        g.setFont(fSmall);
        g.setColor(0x7A8C80);
        g.drawString("Building the city...", w / 2, itemY(2), Graphics.TOP | Graphics.HCENTER);
    }

    private void about(Graphics g) {
        g.setColor(0x050806);
        g.fillRect(0, 0, w, h);
        g.setFont(fMed);
        g.setColor(0xC41818);
        g.drawString("KILLER'S WRATH", w / 2, 10, Graphics.TOP | Graphics.HCENTER);
        String[] lines = {
            "A 2D open-world crime game.",
            "",
            "Killer is back in the city he",
            "once knew. Everyone knows his",
            "name, and they are afraid.",
            "",
            "CONTROLS",
            "Arrows   walk / drive",
            "FIRE     run (hold)",
            "# or 0   get in / out of a car",
            "Car: Up gas, Down brake/reverse,",
            "     Left/Right steer",
            "*        menu / save game",
            "",
            "Press any key to go back"
        };
        g.setFont(fSmall);
        int y = 10 + fMed.getHeight() + 8;
        for (int i = 0; i < lines.length; i++) {
            g.setColor(i == 6 ? 0xE8C23A : (i == 14 ? 0x7A8C80 : 0xB8C0A8));
            g.drawString(lines[i], 12, y, Graphics.TOP | Graphics.LEFT);
            y += fSmall.getHeight() + 2;
        }
    }
}
