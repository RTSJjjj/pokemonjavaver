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
  assert.deepEqual(result.ir, { command: "GIVE_ITEM", item: "ORANBERRY", amount: 1 });
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
  const partner = compileBlock(essentials("pbCryFile",
      'cry = pbCryFile(1001)\npbSEPlay(cry) if cry\npbRegisterPartner(:TAPUKOKO, "卡璞·鸣鸣")'));
  assert.equal(partner.status, "JAVA_HANDLER_REQUIRED");
  assert.match(partner.reason, /stage 3/);
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
  // A stage 3 domain API instead moves to the Java-handler bucket (section 25).
  const domain = compileBlock(essentials("pbGetKeyItem", "pbGetKeyItem(PBItems::TOWNMAP)"));
  assert.equal(domain.status, "JAVA_HANDLER_REQUIRED");
  assert.equal(domain.ir, undefined);
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

test("P0c: a Pokemon script with a loop stays in the handler bucket", () => {
  const result = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\ncount=$Trainer.pokemonCount\nfor i in 0...count\npbAddPokemon(p,1)\nend"));
  assert.equal(result.status, "JAVA_HANDLER_REQUIRED");
});

test("P1: pbSetPokemonCenter / pbStoreItem / myAddEgg become IR", () => {
  assert.deepEqual(
    compileBlock(essentials("pbSetPokemonCenter", "pbSetPokemonCenter")).ir,
    { command: "SET_POKEMON_CENTER" },
  );
  assert.deepEqual(
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
  const free = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\np.makeShiny\npbFreeWildBattle(p)"));
  assert.equal(free.status, "TRANSLATED");
  assert.deepEqual(free.ir.steps[2], { command: "FREE_WILD_BATTLE", local: "p" });

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

test("P3: p.giveRibbon becomes a POKEMON_CALL", () => {
  const result = compileBlock(essentials("pbGenPkmn",
      "p=pbGenPkmn(:PIKACHU,5)\np.giveRibbon(:EFFORT)"));
  assert.equal(result.status, "TRANSLATED");
  assert.deepEqual(result.ir.steps[1],
      { command: "POKEMON_CALL", local: "p", action: "giveRibbon", args: ["EFFORT"] });
});




