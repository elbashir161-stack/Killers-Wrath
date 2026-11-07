import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.rms.RecordStore;

/** Three save slots kept in the phone's record store (they survive closing the game). */
public class SaveManager {
    public static final int SLOTS = 3;
    private static final String STORE = "KillersWrathSaves";

    private static RecordStore open() throws Exception {
        RecordStore rs = RecordStore.openRecordStore(STORE, true);
        while (rs.getNumRecords() < SLOTS) rs.addRecord(new byte[0], 0, 0);
        return rs;
    }

    public static SaveData load(int slot) {
        SaveData d = new SaveData();
        RecordStore rs = null;
        try {
            rs = open();
            byte[] b = rs.getRecord(slot + 1);
            if (b != null && b.length > 0) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
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
            }
        } catch (Exception e) {
            d.used = false;
        } finally {
            close(rs);
        }
        return d;
    }

    public static boolean save(int slot, SaveData d) {
        RecordStore rs = null;
        try {
            rs = open();
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
            byte[] b = bo.toByteArray();
            rs.setRecord(slot + 1, b, 0, b.length);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            close(rs);
        }
    }

    /** Slot with the newest save, or -1 if there is none. */
    public static int latestSlot() {
        int best = -1;
        long bestStamp = -1;
        for (int i = 0; i < SLOTS; i++) {
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
