package pokemon.runtime.pokemon;

import java.util.ArrayList;
import java.util.List;

/**
 * 段 Pokemon_Storage (Scripts.rxdata #204, 389 行；原文见
 * {@code __r20-ref/ruby/Pokemon_Storage.rb}): {@code $PokemonStorage}.
 *
 * <p>{@code PokemonBox}(:1-46) 是 30 个固定槽(含 nil)，带名字和壁纸；
 * {@code PokemonStorage}(:50-252) 有 {@code NUM_STORAGE_BOXES} 个盒子，盒号 -1 代表队伍
 * ({@code self.party = $Trainer.party})。盒子按需创建(原文一开始就建 200 个，
 * 名字「盒子 N」、壁纸 {@code i%42}，懒创建对外不可见)。
 * {@code RegionalStorage}(:259-358) 的按地区取库由 {@link TrainerState#storageForRegion} 承担。</p>
 *
 * <p>登记: {@code pkmn.formTime} 没有建模；{@code pkmn.form = 0 if SHAYMIN} 对应
 * {@code form = null}。{@code pbEachPokemon}(:377-384) 没有 Java 调用方，未转。</p>
 */
public final class Storage {

    /** Settings::NUM_STORAGE_BOXES (the original project uses 200). */
    public static final int BOXES = 200;
    public static final int SLOTS = 30;
    /** :54 PokemonStorage::BASICWALLPAPERQTY */
    public static final int BASICWALLPAPERQTY = 42;

    /** :70-84 allWallpapers */
    private static final String[] WALLPAPERS = {
        "森林", "城市", "沙漠", "草原",
        "峭壁", "火山", "雪地", "洞穴",
        "海滩", "海底", "河流", "天空",
        "精灵中心", "机器", "方格", "简约",
        "莱希拉姆1", "捷克罗姆1", "黑白", "银河队1",
        "樱花", "索罗亚克", "合众地图", "大赛",
        "方块", "银河队2", "宝莱坞", "冠军",
        "暗黑酋雷姆", "焰白酋雷姆", "莱希拉姆2", "捷克罗姆2",
        "心灵之金", "灵魂之银", "型男", "全能竞技赛",
        "城都初始", "刺耳皮丘", "和服少女", "火箭队",
        "合众小可爱", "黑白酋雷姆"
    };

    /** :1-46 PokemonBox */
    public static final class Box {
        public String name;
        public int background;
        private final Pokemon[] pokemon;

        /** :6-13 initialize(name,maxPokemon) */
        Box(String name, int maxPokemon) {
            this.name = name;
            this.background = 0;
            this.pokemon = new Pokemon[maxPokemon];
        }

        /** :15-17 */
        public int length() {
            return pokemon.length;
        }

        /** :19-21 */
        public int nitems() {
            int count = 0;
            for (Pokemon p : pokemon) {
                if (p != null) count++;
            }
            return count;
        }

        /** :23-25 */
        public boolean full() {
            return nitems() == length();
        }

        /** :27-29 */
        public boolean empty() {
            return nitems() == 0;
        }

        /** :31-33 */
        public Pokemon get(int i) {
            return i >= 0 && i < pokemon.length ? pokemon[i] : null;
        }

        /** :35-37 */
        public void set(int i, Pokemon value) {
            if (i >= 0 && i < pokemon.length) pokemon[i] = value;
        }

        /** B2W2 PC:3-5 {@code PokemonBox#pokemon} */
        public Pokemon[] pokemon() {
            return pokemon;
        }

        /** B2W2 PC:7-9 {@code PokemonBox#pokemon=}: 「换位」交换两个盒子的内容(槽位数组)。 */
        public void pokemon(Pokemon[] value) {
            System.arraycopy(value, 0, pokemon, 0, Math.min(value.length, pokemon.length));
        }

        /** :43-45 (Ruby 的 clear 把数组清空；这里保持 30 个槽，等价于全部置 nil) */
        public void clear() {
            java.util.Arrays.fill(pokemon, null);
        }
    }

    private final Party party;
    private final Box[] boxes = new Box[BOXES];
    private final boolean[] unlockedWallpapers = new boolean[WALLPAPERS.length];
    /** :52 currentBox */
    public int currentBox;

    /** Standalone storage (tests): its own empty party. */
    public Storage() {
        this(new Party());
    }

    /** :109-111 {@code def party; $Trainer.party; end} */
    public Storage(Party party) {
        this.party = party;
    }

    /** :56-68 initialize: box i is named 「盒子 i+1」 with wallpaper i%42. */
    private Box boxAt(int index) {
        if (index < 0 || index >= BOXES) return null;
        if (boxes[index] == null) {
            boxes[index] = new Box("盒子 " + (index + 1), SLOTS);   // :59
            boxes[index].background = index % BASICWALLPAPERQTY;   // :60
        }
        return boxes[index];
    }

    /** :145-148 {@code self[x]}: box -1 is the party, so this is only for boxes. */
    public Box box(int index) {
        Box box = boxAt(index);
        if (box == null) throw new IllegalArgumentException("Invalid box: " + index);
        return box;
    }

    /** :70-84 */
    public String[] allWallpapers() {
        return WALLPAPERS.clone();
    }

    /** :86-89 */
    public boolean[] unlockedWallpapers() {
        return unlockedWallpapers;
    }

    /** :91-96 */
    public boolean isAvailableWallpaper(int i) {
        if (i < BASICWALLPAPERQTY) return true;
        return i >= 0 && i < unlockedWallpapers.length && unlockedWallpapers[i];
    }

    /** :98-107 availableWallpapers: names and ids. */
    public List<Object[]> availableWallpapers() {
        List<Object[]> ret = new ArrayList<>();
        for (int i = 0; i < WALLPAPERS.length; i++) {
            if (!isAvailableWallpaper(i)) continue;
            ret.add(new Object[] {WALLPAPERS[i], i});
        }
        return ret;
    }

    /** :365-367 pbUnlockWallpaper */
    public void unlockWallpaper(int index) {
        if (index >= 0 && index < unlockedWallpapers.length) unlockedWallpapers[index] = true;
    }

    /** :109-111 */
    public Party party() {
        return party;
    }

    /** :117-119 */
    public int maxBoxes() {
        return BOXES;
    }

    /** :121-124 */
    public int maxPokemon(int box) {
        if (box >= maxBoxes()) return 0;
        return box < 0 ? 6 : SLOTS;
    }

    /** :126-131 */
    public boolean full() {
        for (int i = 0; i < maxBoxes(); i++) {
            if (!box(i).full()) return false;
        }
        return true;
    }

    /** :133-143 */
    public int pbFirstFreePos(int box) {
        if (box == -1) {
            int ret = party.size();                            // :135 party.nitems
            return ret == 6 ? -1 : ret;
        }
        for (int i = 0; i < maxPokemon(box); i++) {
            if (get(box, i) == null) return i;
        }
        return -1;
    }

    /** :145-154 {@code self[x,y]} */
    public Pokemon get(int box, int index) {
        if (box == -1) return party.get(index);
        Box b = boxAt(box);
        return b == null ? null : b.get(index);
    }

    /** :156-162 {@code self[x,y]=value}；队伍没有 nil 槽，写 nil 即移除(原文随后 compact!)。 */
    public void set(int box, int index, Pokemon value) {
        if (box == -1) {
            com.badlogic.gdx.utils.Array<Pokemon> members = party.members();
            if (value == null) {
                if (index >= 0 && index < members.size) members.removeIndex(index);
            } else if (index >= 0 && index < members.size) {
                members.set(index, value);
            } else {
                party.add(value);                              // party[party.length] = value
            }
            return;
        }
        Box b = boxAt(box);
        if (b != null) b.set(index, value);
    }

    /**
     * 204_Pokemon_Storage:377-384 {@code pbEachPokemon}: every Pokemon in the
     * party (box -1) and in all boxes, in that order.
     */
    public void eachPokemon(java.util.function.Consumer<Pokemon> action) {
        for (int i = -1; i < maxBoxes(); i++) {                // :378
            for (int j = 0; j < maxPokemon(i); j++) {          // :379
                Pokemon pkmn = get(i, j);                      // :380
                if (pkmn != null) {                            // :381
                    action.accept(pkmn);
                }
            }
        }
    }

    /** 他段: PokeBattle_Pokemon:769-774 heal (egg 不动)。 */
    public static void heal(Pokemon p) {
        if (p == null || p.egg) return;
        p.hp = p.maxHp();                                      // healHP
        p.status = "";                                         // healStatus
        for (Pokemon.MoveSlot move : p.moves) move.pp = move.maxPp;   // healPP
    }

    /** :182-183 {@code pkmn.form = 0 if pkmn.isSpecies?(:SHAYMIN)} (formTime 未建模) */
    public static void resetForm(Pokemon p) {
        if (p != null && p.species != null && "SHAYMIN".equals(p.species.internalName)) {
            p.form = null;
            p.internalName = p.species.internalName;
        }
    }

    /** :164-188 pbCopy */
    public boolean pbCopy(int boxDst, int indexDst, int boxSrc, int indexSrc) {
        if (indexDst < 0 && boxDst < maxBoxes()) {              // :165
            boolean found = false;
            for (int i = 0; i < maxPokemon(boxDst); i++) {      // :167
                if (get(boxDst, i) != null) continue;           // :168
                found = true;
                indexDst = i;
                break;
            }
            if (!found) return false;                           // :173
        }
        if (boxDst == -1) {                                     // :175 Copying into party
            if (party.size() >= 6) return false;                // :176
            party.add(get(boxSrc, indexSrc));                   // :177-178
        } else {                                                // Copying into box
            Pokemon pkmn = get(boxSrc, indexSrc);               // :180
            if (pkmn == null) throw new IllegalStateException("Trying to copy nil to storage");   // :181
            resetForm(pkmn);                                    // :182-183
            heal(pkmn);                                         // :184
            set(boxDst, indexDst, pkmn);                        // :185
        }
        return true;
    }

    /** :190-194 pbMove */
    public boolean pbMove(int boxDst, int indexDst, int boxSrc, int indexSrc) {
        if (!pbCopy(boxDst, indexDst, boxSrc, indexSrc)) return false;
        pbDelete(boxSrc, indexSrc);
        return true;
    }

    /** :196-199 pbMoveCaughtToParty */
    public boolean pbMoveCaughtToParty(Pokemon pkmn) {
        if (party.size() >= 6) return false;
        return party.add(pkmn);
    }

    /** :201-214 pbMoveCaughtToBox */
    public boolean pbMoveCaughtToBox(Pokemon pkmn, int box) {
        for (int i = 0; i < maxPokemon(box); i++) {
            if (get(box, i) == null) {
                if (box >= 0) {
                    resetForm(pkmn);                            // :205-206
                    heal(pkmn);                                 // :207
                }
                set(box, i, pkmn);                              // :209
                return true;
            }
        }
        return false;
    }

    /** :216-238 pbStoreCaught: the box number it landed in, or -1. */
    public int pbStoreCaught(Pokemon pkmn) {
        if (currentBox >= 0) {                                  // :217
            resetForm(pkmn);                                    // :218-219
            heal(pkmn);                                         // :220
        }
        for (int i = 0; i < maxPokemon(currentBox); i++) {      // :222
            if (get(currentBox, i) == null) {
                set(currentBox, i, pkmn);
                return currentBox;                              // :225
            }
        }
        for (int j = 0; j < maxBoxes(); j++) {                  // :228
            for (int i = 0; i < maxPokemon(j); i++) {
                if (get(j, i) == null) {
                    set(j, i, pkmn);
                    currentBox = j;                             // :232
                    return currentBox;
                }
            }
        }
        return -1;                                              // :237
    }

    /** :240-245 pbDelete (队伍的 compact! 由 {@link #set} 的移除完成) */
    public void pbDelete(int box, int index) {
        if (get(box, index) != null) {
            set(box, index, null);
        }
    }

    /** :247-251 clear */
    public void clear() {
        for (int i = 0; i < maxBoxes(); i++) {
            if (boxes[i] != null) boxes[i].clear();
        }
    }

    // ---- 非原文接口: 事件胶水/存档需要 ------------------------------------

    /** {@code pbStoreCaught(pkmn) >= 0}: the overflow path of pbAddPokemon. */
    public boolean store(Pokemon pokemon) {
        return pokemon != null && pbStoreCaught(pokemon) >= 0;
    }

    /** Boxes created so far (null = never touched, saved as default). */
    public Box boxIfCreated(int index) {
        return index >= 0 && index < BOXES ? boxes[index] : null;
    }

    /** Pokemon in all boxes. */
    public int count() {
        int total = 0;
        for (Box box : boxes) {
            if (box != null) total += box.nitems();
        }
        return total;
    }

    /** How many boxes currently hold at least one Pokemon. */
    public int usedBoxes() {
        int used = 0;
        for (Box box : boxes) {
            if (box != null && !box.empty()) used++;
        }
        return used;
    }
}
