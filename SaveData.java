/** What a save slot remembers. health and money are reserved for the gameplay stage. */
public class SaveData {
    public boolean used = false;
    public int area = 0;      // 0 city, 1 forest
    public int px, py;
    public int kills = 0;
    public int playTicks = 0;
    public int health = 100;
    public int money = 0;
    public long stamp = 0;
}
