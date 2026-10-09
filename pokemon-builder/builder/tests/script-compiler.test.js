// R7.1 tests: Essentials script -> IR (project3 sections 4, 21-23, 78, 79).
// Run with: node --test builder/tests

import test from "node:test";
import assert from "node:assert/strict";

import {
  compileBlock,
  compileBlocks,
  evaluateReleasePolicy,
  splitArguments,
  parseValue,
} from "../src/script-compiler.js";

const essentials = (name, rubySource) => ({
  id: `map2/event22/page1/cmd0`,
  category: "ESSENTIALS_API",
  apis: [name],
  calls: [{ name }],
  rubySource,
});

test("P4 PC and trade selection have explicit menu IR", () => {
  assert.deepEqual(compileBlock(essentials("pbPokeCenterPC", "pbPokeCenterPC")).ir, { command: "OPEN_PC" });
  assert.deepEqual(compileBlock(essentials("pbChoosePokemonForTrade", "pbChoosePokemonForTrade(1,2,:PIKACHU)")).ir,
    { command: "CHOOSE_TRADE", variable: 1, nameVariable: 2, wanted: "PIKACHU" });
});
test("P4 trade retains local Pokemon and variable slot references", () => {
  const result = compileBlock(essentials("pbGenPkmn", 'p=pbGenPkmn(:PIKACHU,20)\np.makeShiny\npbStartTrade(pbGet(1),p,_I("礼物"),_I("训练家"),0,906)'));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir.steps.at(-1), { command: "START_TRADE", index: { variable: 1 }, offered: { local: "p" }, nickname: "礼物", trainerName: "训练家" });
});
test("P4 unsupported dynamic trade arguments never become translated", () => {
  const result = compileBlock(essentials("pbStartTrade", 'pbStartTrade(random_slot(),:PIKACHU,"A","B")'));
  assert.notEqual(result.status, "TRANSLATED");
});

test("splitArguments keeps nested arrays and quoted commas intact", () => {
  assert.deepEqual(splitArguments(':A, 1, [1,2], "x,y"'), [":A", "1", "[1,2]", '"x,y"']);
});

test("parseValue maps Ruby literals to JSON and marks scripts", () => {
  assert.equal(parseValue(":ORANBERRY"), "ORANBERRY");
  assert.equal(parseValue("1"), 1);
  assert.equal(parseValue("true"), true);
  assert.equal(parseValue('"你好"'), "你好");
  assert.deepEqual(parseValue("[:A,:B]"), ["A", "B"]);
  assert.deepEqual(parseValue("$game_variables[1]"), { script: "$game_variables[1]" });
});

test("pbItemBall becomes GIVE_ITEM IR without any Ruby text", () => {
  const result = compileBlock(essentials("pbItemBall", "pbItemBall(:ORANBERRY,1)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "GIVE_ITEM", item: "ORANBERRY", amount: 1, mode: "ball" });
  const text = JSON.stringify(result.ir);
  assert.ok(!text.includes("pbItemBall") && !text.includes(":ORANBERRY"), "IR must not carry Ruby");
});

test("handler batch 1: pbSEPlay / pbWait / pbCaveEntrance / pbCaveExit (R6.26)", () => {
  assert.deepEqual(
    compileBlock(essentials("pbSEPlay", 'pbSEPlay("Door enter")')).ir,
    { command: "PLAY_SE", name: "Door enter", volume: 100, pitch: 100 },
  );
  assert.deepEqual(
    compileBlock(essentials("pbSEPlay", 'pbSEPlay("Door enter",80,120)')).ir,
    { command: "PLAY_SE", name: "Door enter", volume: 80, pitch: 120 },
  );
  assert.deepEqual(
    compileBlock(essentials("pbWait", "pbWait(20)")).ir,
    { command: "WAIT", frames: 20 },
  );
  assert.deepEqual(
    compileBlock(essentials("pbCaveEntrance", "pbCaveEntrance")).ir,
    { command: "CAVE_ENTRANCE", exiting: false },
  );
  assert.deepEqual(
    compileBlock(essentials("pbCaveExit", "pbCaveExit")).ir,
    { command: "CAVE_ENTRANCE", exiting: true },
  );
  // A cave script that also messages: both statements become one SEQUENCE.
  const sequence = compileBlock(essentials("pbCaveEntrance",
      'pbCaveEntrance\npbMessage("天黑了")'));
  assert.equal(sequence.status, "TRANSLATED");
  assert.equal(sequence.ir.command, "SEQUENCE");
  assert.deepEqual(sequence.ir.steps[0], { command: "CAVE_ENTRANCE", exiting: false });
  assert.equal(sequence.ir.steps[1].command, "SHOW_TEXT");
});

test("handler batch 2: cry pair / pbExclaim / boulder / floating plates (R6.30)", () => {
  // The project's cry pair compiles into one PLAY_CRY step.
  assert.deepEqual(
    compileBlock(essentials("pbCryFile", "cry = pbCryFile(251)\npbSEPlay(cry) if cry")).ir,
    { command: "PLAY_CRY", species: 251 },
  );
  // A trailing statement keeps the pair as the first SEQUENCE step...
  const withSwitch = compileBlock(essentials("pbCryFile",
      'cry = pbCryFile(483)\npbSEPlay(cry) if cry\npbSetSelfSwitch(2,"A",true)'));
  assert.deepEqual(withSwitch.ir.steps, [
    { command: "PLAY_CRY", species: 483 },
    { command: "SET_SELF_SWITCH", eventId: 2, channel: "A", value: true },
  ]);
  // ... and two cries in one block stay two cries.
  const twice = compileBlock(essentials("pbCryFile",
      "cry = pbCryFile(483)\npbSEPlay(cry) if cry\ncry = pbCryFile(484)\npbSEPlay(cry) if cry"));
  assert.deepEqual(twice.ir.steps, [
    { command: "PLAY_CRY", species: 483 },
    { command: "PLAY_CRY", species: 484 },
  ]);
  // A lone assignment is not a sound; it must not be translated as one.
  const lone = compileBlock(essentials("pbCryFile", "cry = pbCryFile(251)"));
  assert.equal(lone.status, "UNSUPPORTED");

  // pbExclaim resolves get_character(N) and keeps the animation defaults.
  assert.deepEqual(
    compileBlock(essentials("pbExclaim", "pbExclaim(get_character(2))")).ir,
    { command: "EXCLAIM", character: 2, animationId: 3, tinting: false },
  );
  const unknownTarget = compileBlock(essentials("pbExclaim", "pbExclaim(candidate)"));
  assert.equal(unknownTarget.status, "UNSUPPORTED");
  assert.match(unknownTarget.reason, /literal/);

  // L6: pbNoticePlayer - the 324 block pattern is pbTrainerIntro + notice.
  assert.deepEqual(
    compileBlock(essentials("pbNoticePlayer", "pbNoticePlayer(get_character(0))")).ir,
    { command: "NOTICE_PLAYER", character: 0 },
  );
  const trainerNotice = compileBlock({
    id: "map2/event9/page1/cmd4",
    category: "ESSENTIALS_API",
    apis: ["pbTrainerIntro", "pbNoticePlayer"],
    calls: [{ name: "pbTrainerIntro" }, { name: "pbNoticePlayer" }],
    rubySource: 'pbTrainerIntro(:YOUNGSTER)\npbNoticePlayer(get_character(0))',
  });
  assert.equal(trainerNotice.status, "TRANSLATED", JSON.stringify(trainerNotice));
  assert.equal(trainerNotice.ir.steps.length, 2);
  assert.deepEqual(trainerNotice.ir.steps[1], { command: "NOTICE_PLAYER", character: 0 });

  // L6: pbSave - the project's quiet save (no UI).
  assert.deepEqual(
    compileBlock(essentials("pbSave", "pbSave")).ir,
    { command: "SAVE_GAME" },
  );

  assert.deepEqual(
    compileBlock(essentials("pbPushThisBoulder", "pbPushThisBoulder")).ir,
    { command: "PUSH_BOULDER" },
  );
  assert.deepEqual(
    compileBlock({
      id: "map60/event37/page1/cmd0",
      category: "PLUGIN_API",
      apis: ["toggle_liefeng_switches"],
      calls: [{ name: "toggle_liefeng_switches" }],
      rubySource: "toggle_liefeng_switches",
    }).ir,
    { command: "TOGGLE_PLATE_SWITCHES" },
  );

  // A cry block that then needs the Pokemon runtime stays handler-required.
  const domain = compileBlock(essentials("pbCryFile",
      'cry = pbCryFile(1001)\npbSEPlay(cry) if cry\npbCrystalWarp'));
  assert.equal(domain.status, "JAVA_HANDLER_REQUIRED");
  assert.match(domain.reason, /stage 3/);
});

test("self switch and temp switch commands carry their channel", () => {
  assert.deepEqual(
    compileBlock(essentials("pbSetSelfSwitch", 'pbSetSelfSwitch(10,"A",true)')).ir,
    { command: "SET_SELF_SWITCH", eventId: 10, channel: "A", value: true },
  );
  assert.deepEqual(
    compileBlock(essentials("setTempSwitchOn", 'setTempSwitchOn("A")')).ir,
    { command: "SET_TEMP_SWITCH", channel: "A", value: true },
  );
});

test("pbSetSelfSwitch keeps the target event and an optional map (R6.19)", () => {
  assert.deepEqual(
    compileBlock(essentials("pbSetSelfSwitch", 'pbSetSelfSwitch(10,"B",false,5)')).ir,
    { command: "SET_SELF_SWITCH", eventId: 10, channel: "B", value: false, mapId: 5 },
  );
});

test("several statements become one SEQUENCE step list (R6.19)", () => {
  const result = compileBlock(essentials("pbSetSelfSwitch",
      'pbSetSelfSwitch(23,"A",true) \npbSetSelfSwitch(24,"B",false)'));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, {
    command: "SEQUENCE",
    steps: [
      { command: "SET_SELF_SWITCH", eventId: 23, channel: "A", value: true },
      { command: "SET_SELF_SWITCH", eventId: 24, channel: "B", value: false },
    ],
  });
});

test("quest chains translate into a SEQUENCE (R6.19)", () => {
  const result = compileBlock(essentials("completeQuest",
      "completeQuest(:Quest5)\nactivateQuest(:Quest6)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir.steps, [
    { command: "COMPLETE_QUEST", quest: "Quest5" },
    { command: "ACTIVATE_QUEST", quest: "Quest6" },
  ]);
});

test("a non-call statement is reported, not mis-translated (R6.19)", () => {
  const result = compileBlock(essentials("pbSetSelfSwitch",
      'pbSetSelfSwitch(23,"A",true)\n$game_switches[5] = true'));
  assert.equal(result.status, "UNSUPPORTED");
  assert.match(result.reason, /simple API call/);
  assert.equal(result.ir, undefined, "no broken IR may be emitted");
});

test("non literal handler arguments are reported instead of leaking Ruby (R6.19)", () => {
  // A non-domain API cannot resolve a script argument: it stays unsupported.
  const result = compileBlock(essentials("pbSEPlay", "pbSEPlay(seName)"));
  assert.equal(result.status, "UNSUPPORTED");
  assert.match(result.reason, /literal/);
  assert.equal(result.ir, undefined);
  // A stage 3 domain API whose argument is still Ruby moves to the
  // Java-handler bucket (section 25).
  const domain = compileBlock(essentials("pbGetKeyItem", "pbGetKeyItem(keyItemName(pbGet(3)))"));
  assert.equal(domain.status, "JAVA_HANDLER_REQUIRED");
  assert.equal(domain.ir, undefined);
});

test("R8: PBItems constants resolve to the internal name", () => {
  assert.equal(parseValue("PBItems::TOWNMAP"), "TOWNMAP");
  assert.equal(parseValue("PBSpecies::PIKACHU"), "PIKACHU");
  const result = compileBlock(essentials("pbGetKeyItem", "pbGetKeyItem(PBItems::TOWNMAP,1)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "GIVE_KEY_ITEM", item: "TOWNMAP", amount: 1 });
});

test("R8: $Trainer badges / pokedex / pokepc assignments become IR", () => {
  const badge = compileBlock(essentials("pbGetKeyItem",
    'pbGetKeyItem("itemBadge0Key")\n$Trainer.badges[1] = true'));
  assert.equal(badge.status, "TRANSLATED");
  assert.deepEqual(badge.ir, {
    command: "SEQUENCE",
    steps: [
      { command: "GIVE_KEY_ITEM", item: "itemBadge0Key", amount: 1 },
      { command: "SET_BADGE", badge: 1, value: true },
    ],
  });
  const dex = compileBlock(essentials("pbGetKeyItem",
    '$Trainer.pokedex=true\npbGetKeyItem("itemDexFemaleKey")'));
  assert.equal(dex.status, "TRANSLATED");
  assert.deepEqual(dex.ir.steps[0], { command: "SET_TRAINER_FLAG", flag: "pokedex", value: true });
  const pc = compileBlock(essentials("pbGetKeyItem", '$Trainer.pokepc = true\npbGetKeyItem("itemPCKey")'));
  assert.equal(pc.status, "TRANSLATED");
  assert.deepEqual(pc.ir.steps[0], { command: "SET_TRAINER_FLAG", flag: "pokepc", value: true });
});

test("R8: pbSet reads the $Trainer dex counters", () => {
  const result = compileBlock(essentials("pbSet",
    "pbSet(1,$Trainer.pokedexSeen)\npbSet(2,$Trainer.pokedexOwned)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, {
    command: "SEQUENCE",
    steps: [
      { command: "SET_VARIABLE", id: 1, value: { trainerStat: "pokedexSeen" } },
      { command: "SET_VARIABLE", id: 2, value: { trainerStat: "pokedexOwned" } },
    ],
  });
});

test("R8: boss_reward / BossRewards calls become their IR commands", () => {
  assert.deepEqual(compileBlock(essentials("boss_reward", "boss_reward(5)")).ir,
    { command: "BOSS_REWARD", rank: 5 });
  assert.deepEqual(compileBlock(essentials("pokemon_reward", "BossRewards.pokemon_reward")).ir,
    { command: "BOSS_POKEMON_REWARD" });
  assert.deepEqual(compileBlock(essentials("gholdengo_money", "BossRewards.gholdengo_money")).ir,
    { command: "BOSS_GHOLDENGO_MONEY" });
  assert.deepEqual(compileBlock(essentials("blissey", "BossRewards.blissey")).ir,
    { command: "BOSS_BLISSEY" });
});

test("R8: the gift scripts set trainerID / otgender / ballused", () => {
  const result = compileBlock(essentials("pbGenPkmn",
    "p=pbGenPkmn(:VOLTCAT,15)\np.iv=[31,31,31,31,31,31]\np.ballused=26\npbAddPokemon(p,1)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir.steps[2],
    { command: "POKEMON_SET", local: "p", property: "ballused", value: 26 });
});

test("R8: partner registration becomes its IR commands (PField_Field:1399-1440)", () => {
  assert.deepEqual(compileBlock(essentials("pbRegisterPartner", 'pbRegisterPartner(:CYAN,"阿青")')).ir,
    { command: "REGISTER_PARTNER", trainerType: "CYAN", name: "阿青", partyId: 0 });
  assert.deepEqual(compileBlock(essentials("pbRegisterPartner", 'pbRegisterPartner(:LEADER_Dragon,"未明",2)')).ir,
    { command: "REGISTER_PARTNER", trainerType: "LEADER_Dragon", name: "未明", partyId: 2 });
  assert.deepEqual(compileBlock(essentials("pbDeregisterPartner", "pbDeregisterPartner")).ir,
    { command: "DEREGISTER_PARTNER" });
});

test("R8: the single-call pbAddPokemon becomes GIVE_POKEMON", () => {
  assert.deepEqual(compileBlock(essentials("pbAddPokemon", "pbAddPokemon(:TURTWIG,5)")).ir,
    { command: "GIVE_POKEMON", species: "TURTWIG", level: 5 });
  // The Pokemon construction form still uses the local + PARTY_ADD.
  const local = compileBlock(essentials("pbGenPkmn",
    "p=pbGenPkmn(:VOLTCAT,15)\npbAddPokemon(p,1)"));
  assert.equal(local.status, "TRANSLATED");
  assert.deepEqual(local.ir.steps.at(-1), { command: "PARTY_ADD", local: "p", silent: false });
});

test("no-parentheses calls work (pbTrainerEnd, pbBridgeOn)", () => {
  assert.deepEqual(compileBlock(essentials("pbTrainerEnd", "pbTrainerEnd")).ir, { command: "TRAINER_END" });
  assert.deepEqual(compileBlock(essentials("pbBridgeOn", "pbBridgeOn")).ir, { command: "SET_BRIDGE", on: true });
});

test("a multiline mart list becomes one OPEN_MART command", () => {
  const ruby = "pbPokemonMart([\n:POKEBALL,:GREATBALL,\n:POTION\n])";
  const result = compileBlock(essentials("pbPokemonMart", ruby));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "OPEN_MART", items: ["POKEBALL", "GREATBALL", "POTION"] });
});

test("_INTL(...) texts are unwrapped so SHOW_TEXT carries plain text", () => {
  assert.equal(parseValue('_INTL("你好")'), "你好");
  assert.equal(parseValue('_INTL("数量：", 3)'), '数量：'); // non literal part stays out
  const result = compileBlock(essentials("pbMessage", 'pbMessage(_INTL("拿到了树果！"))'));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "SHOW_TEXT", text: "拿到了树果！" });
});

test("unsupported blocks carry a source location and fail a release build", () => {
  const { coverage } = compileBlocks([
    {
      id: "map70/event4/page1/cmd2",
      category: "ESSENTIALS_API",
      apis: ["some_unknown_ruby_magic"],
      calls: [{ name: "some_unknown_ruby_magic" }],
      rubySource: "some_unknown_ruby_magic(1)",
      mapId: 70,
      eventId: 4,
      page: 1,
      commandIndex: 2,
    },
  ]);
  assert.equal(coverage.problems.unsupported.length, 1);
  const problem = coverage.problems.unsupported[0];
  assert.equal(problem.source.mapId, 70);
  assert.equal(problem.source.eventId, 4);
  assert.equal(problem.source.commandIndex, 2);
  assert.match(problem.rubySource, /some_unknown_ruby_magic\(1\)/, "reports keep the original Ruby");

  const release = evaluateReleasePolicy(coverage.problems);
  assert.equal(release.exitCode, 1);
  assert.equal(release.blocked, 1);
  assert.equal(release.first[0].id, "map70/event4/page1/cmd2");

  const development = evaluateReleasePolicy(coverage.problems, { allowUnsupported: true });
  assert.equal(development.exitCode, 0);
  assert.equal(development.allowed, 1);
});

test("a clean project passes the release policy", () => {
  const { coverage } = compileBlocks([essentials("pbItemBall", "pbItemBall(:POTION,1)")]);
  assert.deepEqual(evaluateReleasePolicy(coverage.problems), { exitCode: 0, blocked: 0, allowed: 0 });
});

test("plugin APIs are JAVA_HANDLER_REQUIRED and unknown APIs unsupported", () => {
  const plugin = compileBlock({
    id: "map2/event1/page1/cmd1",
    category: "PLUGIN_API",
    apis: ["pbSomePluginCall"],
    calls: [{ name: "pbSomePluginCall" }],
    rubySource: "pbSomePluginCall(:X)",
  });
  assert.equal(plugin.status, "JAVA_HANDLER_REQUIRED");

  // R6.19: a plugin API the Builder knows does translate (quest batch 1).
  const handled = compileBlock({
    id: "map2/event1/page1/cmd2",
    category: "PLUGIN_API",
    apis: ["advanceQuestToStage"],
    calls: [{ name: "advanceQuestToStage" }],
    rubySource: "advanceQuestToStage(:Quest1, 3)",
  });
  assert.equal(handled.status, "TRANSLATED");
  assert.deepEqual(handled.ir, { command: "ADVANCE_QUEST_TO_STAGE", quest: "Quest1", stage: 3 });

  const unknown = compileBlock(essentials("pbTotallyUnknown", "pbTotallyUnknown(:X)"));
  assert.equal(unknown.status, "UNSUPPORTED");
  assert.match(unknown.reason, /no handler/);

  const domain = compileBlock(essentials("pbGenPkmn", "pbGenPkmn(:RATTATA,5)"));
  assert.equal(domain.status, "JAVA_HANDLER_REQUIRED");
  assert.match(domain.reason, /stage 3/);
});

test("coverage counts every block exactly once", () => {
  const { ir, coverage } = compileBlocks([
    essentials("pbItemBall", "pbItemBall(:POTION,1)"),
    essentials("pbTotallyUnknown", "pbTotallyUnknown()"),
    { id: "x", category: "PLUGIN_API", apis: ["quest"], calls: [{ name: "quest" }], rubySource: "quest(1)" },
    { id: "y", category: "SIMPLE_EXPRESSION", apis: [], calls: [], rubySource: "$game_switches[3]" },
  ]);
  assert.equal(coverage.blocks, 4);
  assert.equal(coverage.translated, 2);
  assert.equal(coverage.unsupported, 1);
  assert.equal(coverage.javaHandlerRequired, 1);
  assert.equal(ir.length, 1);
  assert.equal(coverage.byApi.pbItemBall.translated, 1);
});

test("P0c: a pbGenPkmn construction script becomes local-variable IR", () => {
  const result = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PANSAGE,15)\np.iv=[31,31,31,31,31,31]\np.setAbility(2)\npbAddPokemon(p,1)"));
  assert.equal(result.status, "TRANSLATED");
  assert.equal(result.ir.command, "SEQUENCE");
  assert.deepEqual(result.ir.steps, [
    { command: "POKEMON_CREATE", local: "p", species: "PANSAGE", level: 15 },
    { command: "POKEMON_SET", local: "p", property: "iv", value: [31, 31, 31, 31, 31, 31] },
    { command: "POKEMON_CALL", local: "p", action: "setAbility", args: [2] },
    { command: "PARTY_ADD", local: "p", silent: false },
  ]);
  assert.ok(!JSON.stringify(result.ir).includes("pbGenPkmn"), "IR must not carry Ruby");
});

test("P0c: Pokemon methods, properties and mixed handler statements translate", () => {
  const result = compileBlock(essentials("pbGenPkmn",
      'p=PokeBattle_Pokemon.new(:GOOMY,5,$Trainer)\np.makeShiny\np.ot="阿辽"\np.form=1\n'
      + "p.setNature(:LONELY)\np.setItem(:LEFTOVERS)\np.pbLearnMove(:TACKLE)\np.calcStats\n"
      + "pbAddPokemonSilent(p)\npbSEPlay(\"Door enter\")\npbSet(4,p.name)"));
  assert.equal(result.status, "TRANSLATED");
  const steps = result.ir.steps;
  assert.deepEqual(steps[0], { command: "POKEMON_CREATE", local: "p", species: "GOOMY", level: 5 });
  assert.deepEqual(steps[1], { command: "POKEMON_CALL", local: "p", action: "makeShiny", args: [] });
  assert.deepEqual(steps[2], { command: "POKEMON_SET", local: "p", property: "ot", value: "阿辽" });
  assert.deepEqual(steps[3], { command: "POKEMON_SET", local: "p", property: "form", value: 1 });
  assert.deepEqual(steps[4], { command: "POKEMON_CALL", local: "p", action: "setNature", args: ["LONELY"] });
  assert.deepEqual(steps[8], { command: "PARTY_ADD", local: "p", silent: true });
  assert.equal(steps[9].command, "PLAY_SE");
  assert.deepEqual(steps[10], {
    command: "SET_VARIABLE",
    id: 4,
    value: { local: "p", property: "name" },
  });
});

test("P0c: pbGet / $game_variables become typed variable values", () => {
  const result = compileBlock(essentials("pbSet", "pbSet(10, pbGet(3))"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "SET_VARIABLE", id: 10, value: { variable: 3 } });
  const direct = compileBlock(essentials("pbSet", "pbSet(10, $game_variables[4])"));
  assert.deepEqual(direct.ir, { command: "SET_VARIABLE", id: 10, value: { variable: 4 } });
});

test("P0c/P2: a battle terminator now translates, a trade one stays pending", () => {
  const wild = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\np.makeShiny\npbFreeWildBattle(p)"));
  assert.equal(wild.status, "TRANSLATED");
  assert.deepEqual(wild.ir.steps[2], { command: "FREE_WILD_BATTLE", local: "p" });

  const trade = compileBlock(essentials("pbGenPkmn",
      'p=pbGenPkmn(:PIKACHU,5)\np.makeShiny\npbStartTrade(pbGet(1),p,"x")'));
  assert.equal(trade.status, "JAVA_HANDLER_REQUIRED");
});

test("P0d: a Pokemon script loop compiles to a REPEAT step", () => {
  const result = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\ncount=$Trainer.pokemonCount\nfor i in 0...count\npbAddPokemon(p,1)\nend"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir.steps[2], {
    command: "REPEAT",
    local: "i",
    from: 0,
    to: { local: "count" },
    exclusive: true,
    steps: [{ command: "PARTY_ADD", local: "p", silent: false }],
  });
});

test("P0d: the catch ball-shake glue loop becomes a REPEAT sequence", () => {
  const result = compileBlock(essentials("pbSet",
      'count=$Trainer.pokemonCount\nfor i in 1..count\n  pbSet(6,i)\n  pbSEPlay("Battle ball shake")\n  pbWait(16)\nend'));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, {
    command: "SEQUENCE",
    steps: [
      { command: "LOCAL_SET", local: "count", value: { trainerPokemonCount: true } },
      {
        command: "REPEAT",
        local: "i",
        from: 1,
        to: { local: "count" },
        steps: [
          { command: "SET_VARIABLE", id: 6, value: { local: "i" } },
          { command: "PLAY_SE", name: "Battle ball shake", volume: 100, pitch: 100 },
          { command: "WAIT", frames: 16 },
        ],
      },
    ],
  });
  assert.ok(!JSON.stringify(result.ir).includes("$Trainer"), "IR must not carry Ruby");

  const inline = compileBlock(essentials("pbSet",
      'for i in 1..$Trainer.pokemonCount\n  pbSet(6,i)\n  pbSEPlay("Battle ball shake")\n  pbWait(16)\nend'));
  assert.equal(inline.status, "TRANSLATED");
  assert.equal(inline.ir.command, "REPEAT");
  assert.deepEqual(inline.ir.from, 1);
  assert.deepEqual(inline.ir.to, { trainerPokemonCount: true });
  assert.equal(inline.ir.exclusive, undefined);
});

test("P0d: a glue loop with a non-range iterator stays in the handler bucket", () => {
  const result = compileBlock(essentials("pbSet",
      "for pkmn in $Trainer.pokemonParty\npbSet(1,pkmn.name)\nend"));
  assert.equal(result.status, "JAVA_HANDLER_REQUIRED");
});

test("P0d: pbShowMap becomes SHOW_MAP with the plugin defaults (PScreen_RegionMap:431)", () => {
  assert.deepEqual(
    compileBlock(essentials("pbShowMap", "pbShowMap")).ir,
    { command: "SHOW_MAP", region: -1, wallmap: true },
  );
  assert.deepEqual(
    compileBlock(essentials("pbShowMap", "pbShowMap(2,false)")).ir,
    { command: "SHOW_MAP", region: 2, wallmap: false },
  );
  const dynamic = compileBlock(essentials("pbShowMap", "pbShowMap(pbGet(1))"));
  assert.notEqual(dynamic.status, "TRANSLATED", "a dynamic region stays for the runtime");
});

test("P2: setBattleRule records battle rules for the next battle", () => {
  assert.deepEqual(
    compileBlock(essentials("setBattleRule", 'setBattleRule("double")')).ir,
    { command: "BATTLE_RULE", rules: [{ rule: "double" }] },
  );
  assert.deepEqual(
    compileBlock(essentials("setBattleRule", 'setBattleRule("outcomeVar", 2)')).ir,
    { command: "BATTLE_RULE", rules: [{ rule: "outcomeVar", value: 2 }] },
  );
  const chain = compileBlock(essentials("pbTrainerIntro",
      'pbTrainerIntro(:BLACKBELT)\nsetBattleRule("double")'));
  assert.equal(chain.status, "TRANSLATED");
  assert.equal(chain.ir.command, "SEQUENCE");
  assert.deepEqual(chain.ir.steps[0], { command: "TRAINER_INTRO", trainerType: "BLACKBELT" });
  assert.deepEqual(chain.ir.steps[1], { command: "BATTLE_RULE", rules: [{ rule: "double" }] });
});

test("P1: pbSetPokemonCenter / pbStoreItem / myAddEgg become IR", () => {
  assert.deepEqual(
    compileBlock(essentials("pbSetPokemonCenter", "pbSetPokemonCenter")).ir,
    { command: "SET_POKEMON_CENTER" },
  );  assert.deepEqual(
    compileBlock(essentials("pbStoreItem", "pbStoreItem(:POTION,2)")).ir,
    { command: "GIVE_ITEM", item: "POTION", amount: 2 },
  );
  assert.deepEqual(
    compileBlock(essentials("myAddEgg", 'myAddEgg(:PICHU,"一只蛋")')).ir,
    { command: "ADD_EGG", species: "PICHU", text: "一只蛋" },
  );
  assert.deepEqual(
    compileBlock(essentials("myAddEgg", "myAddEgg(:PICHU)")).ir,
    { command: "ADD_EGG", species: "PICHU", text: null },
  );
});

test("P1: myAddEgg with a non literal species stays in the handler bucket", () => {
  const result = compileBlock(essentials("myAddEgg", "myAddEgg(pbGet(1))"));
  assert.equal(result.status, "JAVA_HANDLER_REQUIRED");
  assert.equal(result.ir, undefined);
});

test("P1: $PokemonBag.pbStoreItem / pbDeleteItem drop the receiver", () => {
  assert.deepEqual(
    compileBlock(essentials("pbStoreItem", "$PokemonBag.pbStoreItem(:FRESHWATER)")).ir,
    { command: "GIVE_ITEM", item: "FRESHWATER", amount: 1 },
  );
  assert.deepEqual(
    compileBlock(essentials("pbDeleteItem", "$PokemonBag.pbDeleteItem(:CREDENTIALS)")).ir,
    { command: "REMOVE_ITEM", item: "CREDENTIALS", amount: 1 },
  );
});

test("P2: wild / trainer battle scripts become battle IR", () => {
  assert.deepEqual(
    compileBlock(essentials("pbWildBattle", "pbWildBattle(:PIKACHU, 12)")).ir,
    { command: "WILD_BATTLE", species: "PIKACHU", level: 12 },
  );
  assert.deepEqual(
    compileBlock(essentials("pbWildBattle", "pbWildBattle(:PIKACHU)")).ir,
    { command: "WILD_BATTLE", species: "PIKACHU", level: 5 },
  );
  assert.deepEqual(
    compileBlock(essentials("pbTrainerBattle", 'pbTrainerBattle(:POKEMONTRAINER_Red,"Blue",1)')).ir,
    { command: "TRAINER_BATTLE", trainerType: "POKEMONTRAINER_Red", trainerName: "Blue", version: 1, partner: false },
  );
  // PField_Battles:582-596: pbDoubleTrainerBattle keeps both trainers and the double rule.
  assert.deepEqual(
    compileBlock(essentials("pbDoubleTrainerBattle",
        'pbDoubleTrainerBattle(:HIKER,"Al",0,"a",:LASS,"Bo",2,"b",true)')).ir,
    { command: "TRAINER_BATTLE", trainerType: "HIKER", trainerName: "Al", version: 0, partner: false,
      second: { trainerType: "LASS", trainerName: "Bo", version: 2 }, double: true, canLose: true },
  );
  // PField_Battles:598-614: pbTripleTrainerBattle keeps all three trainers and the triple rule.
  assert.deepEqual(
    compileBlock(essentials("pbTripleTrainerBattle",
        'pbTripleTrainerBattle(:HIKER,"Al",0,"a",:LASS,"Bo",2,"b",:BUGCATCHER,"Cy",1,"c",true,5)')).ir,
    { command: "TRAINER_BATTLE", trainerType: "HIKER", trainerName: "Al", version: 0, partner: false,
      second: { trainerType: "LASS", trainerName: "Bo", version: 2 },
      third: { trainerType: "BUGCATCHER", trainerName: "Cy", version: 1 }, triple: true, canLose: true, outcomeVar: 5 },
  );
  const free = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\np.makeShiny\npbFreeWildBattle(p)"));
  assert.equal(free.status, "TRANSLATED");
  assert.deepEqual(free.ir.steps[2], { command: "FREE_WILD_BATTLE", local: "p" });

  // R14 / PField_Battles:359-361: the three optional arguments are battle rules
  // and must survive into the IR.
  assert.deepEqual(
    compileBlock(essentials("pbWildBattle", "pbWildBattle(:PIKACHU, 12, 7)")).ir,
    { command: "WILD_BATTLE", species: "PIKACHU", level: 12, outcomeVar: 7 },
  );
  assert.deepEqual(
    compileBlock(essentials("pbWildBattle", "pbWildBattle(:PIKACHU, 12, 1, false)")).ir,
    { command: "WILD_BATTLE", species: "PIKACHU", level: 12, canRun: false },
  );
  assert.deepEqual(
    compileBlock(essentials("pbWildBattle", "pbWildBattle(:PIKACHU, 12, 1, true, true)")).ir,
    { command: "WILD_BATTLE", species: "PIKACHU", level: 12, canLose: true },
  );
  assert.deepEqual(
    compileBlock(essentials("pbWildBattle", "pbWildBattle(:PIKACHU, 12, 3, false, true)")).ir,
    { command: "WILD_BATTLE", species: "PIKACHU", level: 12, outcomeVar: 3, canRun: false, canLose: true },
  );
  const freeOutcome = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\npbFreeWildBattle(p, 9)"));
  assert.deepEqual(freeOutcome.ir.steps[1],
      { command: "FREE_WILD_BATTLE", local: "p", outcomeVar: 9 });
  assert.deepEqual(
    compileBlock(essentials("pbWildBattle", "pbWildBattle(:PIKACHU, 12, 1, false)")).ir,
    { command: "WILD_BATTLE", species: "PIKACHU", level: 12, canRun: false },
    "the default outcome variable is not written out",
  );

  assert.deepEqual(
    compileBlock(essentials("pbRockSmashRandomEncounter", "pbRockSmashRandomEncounter")).ir,
    { command: "ROCK_SMASH_ENCOUNTER" },
  );
});

test("P-select: pbGenderSelector / pbChangePlayer become player IR", () => {
  assert.deepEqual(
    compileBlock(essentials("pbGenderSelector", "pbGenderSelector")).ir,
    { command: "GENDER_SELECTOR" },
  );
  assert.deepEqual(
    compileBlock(essentials("pbChangePlayer", "pbChangePlayer(1)")).ir,
    { command: "CHANGE_PLAYER", playerId: 1 },
  );
});

test("P3 berry: pbBerryPlant / pbPickBerry become BERRY_PLANT / BERRY_PICK", () => {
  // pbBerryPlant is called without parentheses in the project's events.
  assert.deepEqual(
    compileBlock(essentials("pbBerryPlant", "pbBerryPlant")).ir,
    { command: "BERRY_PLANT" },
  );
  assert.deepEqual(
    compileBlock(essentials("pbPickBerry", "pbPickBerry(:CHERIBERRY,2)")).ir,
    { command: "BERRY_PICK", berry: "CHERIBERRY", qty: 2 },
  );
  // The plugin's qty defaults to 1 (PField_BerryPlants:555).
  assert.deepEqual(
    compileBlock(essentials("pbPickBerry", "pbPickBerry(:ORANBERRY)")).ir,
    { command: "BERRY_PICK", berry: "ORANBERRY", qty: 1 },
  );
});

test("P3: p.giveRibbon becomes a POKEMON_CALL", () => {
  const result = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\np.giveRibbon(:EFFORT)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir.steps[1],
      { command: "POKEMON_CALL", local: "p", action: "giveRibbon", args: ["EFFORT"] });
});




test("stage 1: $game_screen.weather(...) compiles to SET_WEATHER with the PBFieldWeather constant resolved", () => {
  const result = compileBlock(essentials("weather", "$game_screen.weather(\n    \n  PBFieldWeather::Sun,3,20)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "SET_WEATHER", type: 7, power: 3, duration: 20 });
  const heavy = compileBlock(essentials("weather", "$game_screen.weather(\n PBFieldWeather::HeavyRain,6,20)"));
  assert.deepEqual(heavy.ir, { command: "SET_WEATHER", type: 6, power: 6, duration: 20 });
  const none = compileBlock(essentials("weather", "$game_screen.weather(\n   PBFieldWeather::None,0.0,10)"));
  assert.equal(none.status, "TRANSLATED");
  assert.deepEqual(none.ir, { command: "SET_WEATHER", type: 0, power: 0, duration: 10 });
});

test("stage 1: get_character(n).setTempSwitchOn(c) targets event n; get_character(0) is this event", () => {
  const other = compileBlock(essentials("get_character", 'get_character(1).setTempSwitchOn("A")'));
  assert.equal(other.status, "TRANSLATED");
  assert.deepEqual(other.ir, { command: "SET_TEMP_SWITCH", channel: "A", value: true, eventId: 1 });
  const off = compileBlock(essentials("get_character", 'get_character(3).setTempSwitchOff("B")'));
  assert.deepEqual(off.ir, { command: "SET_TEMP_SWITCH", channel: "B", value: false, eventId: 3 });
  const self = compileBlock(essentials("get_character", 'get_character(0).setTempSwitchOn("A")'));
  assert.deepEqual(self.ir, { command: "SET_TEMP_SWITCH", channel: "A", value: true });
});

test("stage 1: pbToneChangeAll(Tone.new(...), n) compiles to TONE_CHANGE_ALL", () => {
  const result = compileBlock(essentials("pbToneChangeAll", "pbToneChangeAll(Tone.new(-255,-255,-255,0),20)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "TONE_CHANGE_ALL", red: -255, green: -255, blue: -255, gray: 0, duration: 20 });
});

test("stage 6: pbPokemonMart carries speech and cantsell; setPrice / setSellPrice become SET_MART_PRICE", () => {
  const plain = compileBlock(essentials("pbPokemonMart", "pbPokemonMart([" + '\n' + " :POTION,:ANTIDOTE," + '\n' + "])"));
  assert.deepEqual(plain.ir, { command: "OPEN_MART", items: ["POTION", "ANTIDOTE"] });
  const full = compileBlock(essentials("pbPokemonMart", 'pbPokemonMart([:POTION],_I("欢迎"),true)'));
  assert.deepEqual(full.ir, { command: "OPEN_MART", items: ["POTION"], speech: "欢迎", cantSell: true });
  const priced = compileBlock(essentials("setPrice", "setPrice(:GOLDBOTTLECAP,80000,0)" + '\n' + "pbPokemonMart([:GOLDBOTTLECAP])"));
  assert.equal(priced.status, "TRANSLATED");
  assert.deepEqual(priced.ir.steps[0], { command: "SET_MART_PRICE", item: "GOLDBOTTLECAP", buy: 80000, sell: 0 });
  assert.equal(priced.ir.steps[1].command, "OPEN_MART");
  const sell = compileBlock(essentials("setSellPrice", "setSellPrice(:NUGGET,5000)"));
  assert.deepEqual(sell.ir, { command: "SET_MART_PRICE", item: "NUGGET", buy: -1, sell: 5000 });
});

test("stage 8.4: DiegoWTsStarterSelection.new(a,b,c) compiles to STARTER_SELECTION", () => {
  const result = compileBlock(essentials("new", "DiegoWTsStarterSelection.new(152,255,728)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir, { command: "STARTER_SELECTION", dex: [152, 255, 728] });
});

test("the Hall of Fame ribbon loop is one pbGiveRibbonToParty (197_PokeBattle_Pokemon:571-576)", () => {
  const block = essentials("giveRibbon", "for i in $Trainer.pokemonParty\n  i.giveRibbon(:CHAMPION)\nend");
  const ir = compileBlock(block);
  assert.match(JSON.stringify(ir), /GIVE_RIBBON_PARTY/);
  assert.match(JSON.stringify(ir), /CHAMPION/);
});

test("the Move Relearner's party choice has its own IR", () => {
  assert.deepEqual(compileBlock(essentials("pbChoosePokemon", "pbChoosePokemon(1,3,proc{|p|\n pbHasRelearnableMove?(p)\n},true)\n")).ir,
    { command: "CHOOSE_POKEMON", variable: 1, nameVariable: 3, proc: "relearnable", allowIneligible: true });
});
