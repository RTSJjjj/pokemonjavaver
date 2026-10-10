package pokemon.runtime.input;

/**
 * Logical game actions (project3 section 14). The runtime never looks at raw
 * keys: platform input sources translate keys / touches into these actions.
 */
public enum GameAction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    CONFIRM,
    CANCEL,
    MENU,
    RUN,
    /** RMXP {@code Input::A} - the battle fight menu's Mega Evolution toggle. */
    SPECIAL,
    /**
     * RMXP {@code Input::L} / {@code Input::R} (PSystem_Controls:113-114:
     * A/Q/PageUp and S/PageDown) - the region map's region switching and other
     * menu pages (PScreen_RegionMap:337/349).
     */
    SHOULDER_LEFT,
    SHOULDER_RIGHT,
    /**
     * RMXP {@code Input::F5} (PSystem_Controls:124: F, F5, Tab) - the party
     * screen's "[F]:寄存系统" (PScreen_Party:918).
     */
    F5,
    /**
     * RMXP {@code Input::F8} (049_Scene_Map:200-201): the {@code goldFinger} cheat menu (371_goldFinger). Outside the
     * cheat switches it only offers to refresh the habitat list.
     */
    F8,
    /** 296_Follower_Config:44 {@code TOGGLEFOLLOWERKEY = :CTRL}: the following Pokemon on / off. */
    TOGGLE_FOLLOWER,
    /** 338_004_ESMM_Overwrite:213-217 {@code Input::MAP = 97}: the M key. */
    MAP_KEY,
    /** {@code Input::FANGDA = 98}: the = / + key (the mini map's size / zoom up). */
    ZOOM_IN,
    /** {@code Input::SUOXIAO = 99}: the - / _ key (the mini map's size / zoom down). */
    ZOOM_OUT
}