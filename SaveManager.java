import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

/**
 * Save slots kept in the phone's record store. All slots live in ONE record
 * (3 manual slots + 1 autosave), so it works the same on every phone.
 */
public class SaveManager {
    public static final int SLOTS = 3;   // manual slots 0..2
    public static final int AUTO = 3;    // autosave slot
    public static final int TOTAL = 4;
    private static final String STORE = "KWSaves";
    private static final int BLOCK = 36; // 7 ints + 1 long

    /** Why the last load/save failed (shown on screen). */
    public static String lastError = "";

    private static int recordId(RecordStore rs) throws Exception {
        RecordEnumeration en = rs.enumerateRecords(null, null, false);
        try {
            if (en.hasNextElement()) return en.nextRecordId();
        } finally {
            en.destroy();
        }
        byte[] blank = new byte[BLOCK * TOTAL];
        return rs.addRecord(blank, 0, blank.length);
    }

    private static byte[] readAll(RecordStore rs, int id) throws Exception {
        byte[] all = rs.getRecord(id);
        if (all == null || all.length < BLOCK * TOTAL) {
            byte[] fresh = new byte[BLOCK * TOTAL];
            if (all != null) System.arraycopy(all, 0, fresh, 0, all.length);
            all = fresh;
        }
        return all;
    }

    public static SaveData load(int slot) {
        SaveData d = new SaveData();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            byte[] all = readAll(rs, recordId(rs));
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(all, slot * BLOCK, BLOCK));
            if (in.readInt() == 1) {
                d.px = in.readInt();
                d.py = in.readInt();
                d.kills = in.readInt();
                d.playTicks = in.readInt();
                d.health = in.readInt();
                d.money = in.readInt();
                d.stamp = in.readLong();
                d.used = true;
            }
        } catch (Exception e) {
            lastError = describe(e);
            d.used = false;
        } finally {
            close(rs);
        }
        return d;
    }

    public static boolean save(int slot, SaveData d) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            int id = recordId(rs);
            byte[] all = readAll(rs, id);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bo);
            out.writeInt(1);
            out.writeInt(d.px);
            out.writeInt(d.py);
            out.writeInt(d.kills);
            out.writeInt(d.playTicks);
            out.writeInt(d.health);
            out.writeInt(d.money);
            out.writeLong(d.stamp);
            out.flush();
            System.arraycopy(bo.toByteArray(), 0, all, slot * BLOCK, BLOCK);
            rs.setRecord(id, all, 0, all.length);
            return true;
        } catch (Exception e) {
            lastError = describe(e);
            return false;
        } finally {
            close(rs);
        }
    }

    /** Slot (manual or auto) with the newest save, or -1 if there is none. */
    public static int latestSlot() {
        int best = -1;
        long bestStamp = -1;
        for (int i = 0; i < TOTAL; i++) {
            SaveData d = load(i);
            if (d.used && d.stamp >= bestStamp) {
                best = i;
                bestStamp = d.stamp;
            }
        }
        return best;
    }

    public static boolean hasAny() {
        return latestSlot() >= 0;
    }

    private static String describe(Exception e) {
        String m = e.getMessage();
        String s = e.getClass().getName() + (m != null ? ": " + m : "");
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    private static void close(RecordStore rs) {
        if (rs != null) {
            try {
                rs.closeRecordStore();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
