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
    /** 296_Follower_Config:44 {@code TOGGLEFOLLOWERKEY = :CTRL}: the following Pokemon on / off. */
    TOGGLE_FOLLOWER
}