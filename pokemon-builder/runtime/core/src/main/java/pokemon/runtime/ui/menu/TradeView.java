package pokemon.runtime.ui.menu;

import pokemon.runtime.app.RuntimeContext;
import pokemon.runtime.event.MenuService;
import pokemon.runtime.input.*;
import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.ui.WindowSkin;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.*;

public final class TradeView {
    private final RuntimeContext context;
    private final MenuService.Request request;
    private final PartyModel party;
    private final MenuListModel confirm = new MenuListModel(2);
    private String notice = "";
    public TradeView(RuntimeContext context, MenuService.Request request) {
        this.context = context; this.request = request;
        party = new PartyModel(context.gameState().trainer().party, context.pbsData());
        confirm.size(2);
    }
    public boolean update(InputManager input) {
        boolean choose = request.kind == MenuService.Kind.CHOOSE_TRADE;
        MenuListModel cursor = choose ? party.cursor : confirm;
        if (input.wasPressed(GameAction.CANCEL) || input.wasPressed(GameAction.MENU)) { request.complete(-1, ""); return true; }
        if (input.wasPressed(GameAction.UP)) cursor.move(-1);
        if (input.wasPressed(GameAction.DOWN)) cursor.move(1);
        if (input.wasPressed(GameAction.CONFIRM)) {
            if (choose) {
                if (!TradeModel.eligible(party.selected(), request.wanted)) notice = "这只宝可梦不符合交易条件。";
                else { request.complete(party.cursor.index(), party.selected().name); return true; }
            } else if (confirm.index() == 0) { request.complete(-1, ""); return true; }
            else if (TradeModel.trade(context.gameState().trainer(), request.index, request.offered,
                    request.nickname, request.trainerName, context.pbsData())) { request.complete(1, ""); return true; }
            else notice = "交易条件已变化，无法交换。";
        }
        return false;
    }
    public void render(SpriteBatch b, MenuAssets a, MenuFont f, WindowSkin skin) {
        boolean choose = request.kind == MenuService.Kind.CHOOSE_TRADE;
        List<String> details = new ArrayList<>();
        if (choose) {
            pokemon.runtime.pokemon.PbsData.Species wanted = context.pbsData().species(request.wanted);
            details.add("对方想要：" + (wanted == null ? request.wanted : wanted.name));
            details.addAll(party.details());
        } else {
            Pokemon mine = context.gameState().trainer().party.get(request.index);
            details.add("送出：" + (mine == null ? "—" : mine.name));
            details.add("获得：" + (request.offered == null ? "—" : request.offered.name));
            details.add("昵称：" + request.nickname); details.add("训练家：" + request.trainerName);
            details.add("确认后立即交换。");
        }
        MenuPanel.draw(b, a, f, skin, "宝可梦交换", choose ? party.rows() : Arrays.asList("取消", "确认交换"),
                choose ? party.cursor : confirm, details, notice.isEmpty() ? "↑↓ 选择  Z 确认  X 取消" : notice);
    }
}
