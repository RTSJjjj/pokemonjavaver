package pokemon.runtime.field;

import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.state.FieldGlobals;
import pokemon.runtime.state.GameState;

/**
 * The bicycle half of the vehicles: 025_Game_Player:448-475 ({@code pbCancelVehicles}, {@code pbCanUseBike?},
 * {@code pbMountBike}, {@code pbDismountBike}), 188_PItem_Items:724-747 ({@code pbBikeCheck}) and the forced mount /
 * dismount of {@code Events.onMapChange} (170_PField_Field:603-608). Surfing is {@link FieldMoves}' and the map
 * screen's: it needs the map.
 *
 * <p>The graphic and the music follow the flags: the map screen watches {@code surfing} / {@code bicycle}.</p>
 */
public final class Vehicles {
    private final PbsData pbs;
    private final GameState state;

    public Vehicles(PbsData pbs, GameState state) {
        this.pbs = pbs;
        this.state = state;
    }

    private FieldGlobals g() {
        return state.fieldGlobals();
    }

    private PbsData.Metadata metadata(int mapId) {
        return pbs == null ? null : pbs.mapMetadata(mapId);
    }

    /** {@code pbGetMetadata(mapid, MetadataBicycleAlways)}. */
    public boolean bicycleAlways(int mapId) {
        PbsData.Metadata meta = metadata(mapId);
        return meta != null && Boolean.TRUE.equals(meta.bicycleAlways);
    }

    /** {@code pbCanUseBike?(mapid)} (025:455-460): BicycleAlways, else Bicycle, else the map's Outdoor flag. */
    public boolean canUseBike(int mapId) {
        if (bicycleAlways(mapId)) {
            return true;
        }
        PbsData.Metadata meta = metadata(mapId);
        Boolean value = meta == null ? null : meta.bicycle;
        if (value == null && g().outdoorOf != null) {
            value = g().outdoorOf.apply(mapId);
        }
        return Boolean.TRUE.equals(value);
    }

    /** {@code pbCancelVehicles(destination=nil)} (025:448-453). */
    public void cancelVehicles(Integer destination) {
        g().surfing = false;
        g().diving = false;
        if (destination == null || !canUseBike(destination)) {
            g().bicycle = false;
        }
    }

    /** {@code pbMountBike} (025:462-468); the music follows in the map screen. */
    public void mountBike() {
        g().bicycle = true;
    }

    /** {@code pbDismountBike} (025:470-475). */
    public void dismountBike() {
        g().bicycle = false;
    }

    /** {@code Events.onMapChange}'s tail (170:603-608): force cycling on BicycleAlways maps, walking where there is no bicycle. */
    public void onMapChange(int mapId) {
        if (bicycleAlways(mapId)) {
            mountBike();
        } else if (!canUseBike(mapId)) {
            dismountBike();
        }
    }

    /**
     * {@code pbBikeCheck} (188_PItem_Items:724-747): whether the bicycle can be mounted or dismounted here; says why not.
     * 登记: {@code PBTerrain.onlyWalk?} is not on this project's tags (it holds for none of them), and there are no
     * dependent events, so the "with someone" line never applies.
     */
    public boolean bikeCheck(int mapId, java.util.function.Consumer<String> say) {
        if (g().surfing) {
            say.accept("这里不能使用。");                                      // :725-728
            return false;
        }
        if (g().bicycle) {                                                        // :734
            if (bicycleAlways(mapId)) {
                say.accept("你不能在这里下车。");                              // :736
                return false;
            }
            return true;
        }
        if (!canUseBike(mapId)) {                                                 // :741-745
            say.accept("这里不能使用。");
            return false;
        }
        return true;
    }
}
