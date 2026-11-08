import java.util.Random;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Calm city population: families, playing kids, people going somewhere. No panic chain. */
public class NpcManager {
    private static final Random R = new Random();

    public static int rnd(int n) {
        return (R.nextInt() & 0x7FFFFFFF) % n;
    }

    public final Npc[] npcs;
    public final int[] doorX;
    public final int[] doorY;
    public final int[] doorUse;
    public Vehicle driven;
    public boolean playerOnFoot = true;
    public int kills = 0;

    private final World world;
    private final Player player;
    private final int[] bloodX = new int[64];
    private final int[] bloodY = new int[64];
    private final int[] bloodS = new int[64];
    private int bloodN = 0, bloodHead = 0;
    private final Image[] bloodImg = new Image[3];
    private int tick = 0;

    public NpcManager(World w, Player k, int count) {
        world = w;
        player = k;
        doorX = w.doorX;
        doorY = w.doorY;
        doorUse = new int[World.MAXDOORS];
        bloodImg[0] = makeBlood(12);
        bloodImg[1] = makeBlood(20);
        bloodImg[2] = makeBlood(30);
        npcs = new Npc[count];
        for (int i = 0; i < count; i++) {
            int[] p = placeAround(k, 100, 560);
            int px = (p != null) ? p[0] : k.x + 120;
            int py = (p != null) ? p[1] : k.y;
            int r = rnd(100);
            int pers = (r < 10) ? Npc.P_BRAVE : (r < 25) ? Npc.P_COWARD : (r < 40) ? Npc.P_FROZEN
                    : (r < 55) ? Npc.P_PANIC : Npc.P_NORMAL;
            npcs[i] = new Npc(px, py, rnd(NpcSprites.LOOKS), pers);
        }
        // Wire families / playgrounds / destinations AFTER everyone exists
        wireRelationships();
    }

    // ---------------- families / playgrounds / destinations ----------------

    private void wireRelationships() {
        // 1. Collect adults who can be parents
        // 2. For each K_WITH_PARENT kid, pick a nearby adult as parent
        for (int i = 0; i < npcs.length; i++) {
            Npc kid = npcs[i];
            if (kid.kind != Npc.KID || kid.kidRole != Npc.K_WITH_PARENT) continue;
            Npc bestParent = null;
            int bestD = 999999;
            for (int j = 0; j < npcs.length; j++) {
                Npc a = npcs[j];
                if (a.kind != Npc.ADULT) continue;
                // Don't let a parent have more than 2 kids
                int kids = 0;
                for (int t = 0; t < npcs.length; t++) {
                    if (npcs[t].parent == a) kids++;
                }
                if (kids >= 2) continue;
                int dx = a.x - kid.x, dy = a.y - kid.y;
                int d = dx * dx + dy * dy;
                if (d < bestD) { bestD = d; bestParent = a; }
            }
            if (bestParent != null) {
                kid.parent = bestParent;
                // Teleport kid next to parent so they start together
                kid.x = bestParent.x + 10;
                kid.y = bestParent.y + 8;
                kid.homeX = bestParent.x;
                kid.homeY = bestParent.y;
            } else {
                // No parent found, make them a home kid instead
                kid.kidRole = Npc.K_HOME;
                kid.homeX = kid.x;
                kid.homeY = kid.y;
            }
        }

        // 3. Playing kids: group them into playgrounds of 2-3
        // Pick a few playground spots near doors / sidewalks
        int playgrounds = 3;
        int[] pgX = new int[playgrounds];
        int[] pgY = new int[playgrounds];
        for (int p = 0; p < playgrounds; p++) {
            if (world.doorCount > 0) {
                int d = rnd(world.doorCount);
                pgX[p] = doorX[d];
                pgY[p] = doorY[d] + 30;
            } else {
                pgX[p] = player.x + (p - 1) * 120;
                pgY[p] = player.y + 80;
            }
        }
        int pgIndex = 0;
        for (int i = 0; i < npcs.length; i++) {
            Npc kid = npcs[i];
            if (kid.kind != Npc.KID || kid.kidRole != Npc.K_PLAYING) continue;
            // Assign to a playground, 2-3 kids per playground
            kid.playCX = pgX[pgIndex % playgrounds];
            kid.playCY = pgY[pgIndex % playgrounds];
            kid.x = kid.playCX + rnd(30) - 15;
            kid.y = kid.playCY + rnd(20) - 10;
            kid.destX = kid.playCX;
            kid.destY = kid.playCY;
            // Every 3 kids, move to next playground
            if (i % 3 == 2) pgIndex++;
        }

        // 4. Home kids: near a door
        for (int i = 0; i < npcs.length; i++) {
            Npc kid = npcs[i];
            if (kid.kind != Npc.KID || kid.kidRole != Npc.K_HOME) continue;
            if (world.doorCount > 0) {
                int d = rnd(world.doorCount);
                kid.homeX = doorX[d];
                kid.homeY = doorY[d] + 20;
                kid.x = kid.homeX + rnd(20) - 10;
                kid.y = kid.homeY + rnd(10) - 5;
            } else {
                kid.homeX = kid.x;
                kid.homeY = kid.y;
            }
        }

        // 5. GOING adults: pick a real destination (a door)
        for (int i = 0; i < npcs.length; i++) {
            Npc a = npcs[i];
            if (a.kind != Npc.ADULT || a.role != Npc.R_GOING) continue;
            if (world.doorCount > 0) {
                int d = rnd(world.doorCount);
                a.destX = doorX[d];
                a.destY = doorY[d] + 15;
            } else {
                a.destX = a.x + 150;
                a.destY = a.y;
            }
        }
    }

    // Re-wire when an NPC is recycled far away - so new kid also gets family/playground
    private void rewireOne(Npc n) {
        if (n.kind == Npc.KID && n.kidRole == Npc.K_WITH_PARENT) {
            // Find new parent near new position
            Npc best = null; int bestD = 999999;
            for (int j = 0; j < npcs.length; j++) {
                Npc a = npcs[j];
                if (a.kind != Npc.ADULT) continue;
                int dx = a.x - n.x, dy = a.y - n.y;
                int d = dx * dx + dy * dy;
                if (d < bestD) { bestD = d; best = a; }
            }
            if (best != null) {
                n.parent = best;
                n.x = best.x + 10; n.y = best.y + 8;
            } else {
                n.kidRole = Npc.K_HOME; n.homeX = n.x; n.homeY = n.y;
            }
        } else if (n.kind == Npc.KID && n.kidRole == Npc.K_PLAYING) {
            if (world.doorCount > 0) {
                int d = rnd(world.doorCount);
                n.playCX = doorX[d]; n.playCY = doorY[d] + 30;
                n.x = n.playCX; n.y = n.playCY;
            }
        } else if (n.kind == Npc.KID && n.kidRole == Npc.K_HOME) {
            if (world.doorCount > 0) {
                int d = rnd(world.doorCount);
                n.homeX = doorX[d]; n.homeY = doorY[d] + 20;
                n.x = n.homeX; n.y = n.homeY;
            } else { n.homeX = n.x; n.homeY = n.y; }
        } else if (n.kind == Npc.ADULT && n.role == Npc.R_GOING) {
            if (world.doorCount > 0) {
                int d = rnd(world.doorCount);
                n.destX = doorX[d]; n.destY = doorY[d] + 15;
            }
        }
    }

    // ---------------- placement ----------------

    private int[] placeAround(Player k, int minD, int maxD) {
        int rad = maxD / World.TS + 1;
        for (int i = 0; i
