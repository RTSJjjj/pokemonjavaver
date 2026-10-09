package pokemon.runtime.field;

import pokemon.runtime.pokemon.Pokemon;
import pokemon.runtime.state.ScreenWeather;

import java.util.Random;

/**
 * 296_Follower_Config:132-901 {@code Events.OnTalkToFollower}: what the following Pokemon does and says when the player
 * talks to it ({@code pbTalkToFollower}, 297_Follower_Main:68-82). The handlers run in order and the first that answers
 * true ends the talk; the texts are the plugin's own (only the {1} / {2} placeholders are filled in).
 */
public final class FollowerTalk {

    /** What the handler wants played: the emote, the wait, the follower's route, then the line. */
    public static final class Result {
        /** {@code $scene.spriteset.addUserAnimation(Emo_*,x,y)}; 0 = none. */
        public int animation;
        /** {@code pbWait(n)} in 40 fps frames after the emote. */
        public int waitFrames;
        /** {@code pbMoveRoute($game_player,[Wait,n])}: frames-by-5 of the player's wait, 0 = no route. */
        public int playerWait;
        /** The follower's move route in the DSL of {@link #route}; null = none. */
        public String route;
        /** The line (placeholders filled). */
        public String message;
        /** True for the item found by {@code pbPokemonFound}: the caller runs its messages instead of {@link #message}. */
        public boolean foundItem;
        public String foundItemName;
        public int foundQuantity;
        public String foundMessage;
    }

    /** What the handlers read. */
    public interface Env {
        String mapName();

        String trainerName();

        int weather();

        /** {@code $PokemonGlobal.followerHoldItem}. */
        boolean holdItem();

        /** An item the follower may find: internal name for a numeric id of {@code pbPokemonFound(rand(...))}. */
        String itemNameById(int id);
    }

    private FollowerTalk() {
    }

    private static final String[] ITEMS_BATTLE = {"POKEBALL", "POKEBALL", "POKEBALL", "GREATBALL", "GREATBALL", "ULTRABALL"};   // :185

    /** 296_Follower_Config:211 (3 lines). */
    static final String[] M0 = {
        "{1}似乎对树木很感兴趣",   // :212
        "{1}似乎很喜欢虫宝可梦的嗡嗡声。",   // :213
        "{1}在森林里不安地跳来跳去。",   // :214
    };

    /** 296_Follower_Config:226 (3 lines). */
    static final String[] M1 = {
        "{1}正在触摸某种开关。",   // :227
        "{1}嘴里叼着一根绳子！",   // :228
        "{1}似乎想触摸机器。",   // :229
    };

    /** 296_Follower_Config:241 (3 lines). */
    static final String[] M2 = {
        "{1}正在房间里四处嗅探。",   // :242
        "{1}注意到{2}的妈妈在附近。",   // :243
        "{1}似乎想在家里安顿下来。",   // :244
    };

    /** 296_Follower_Config:256 (9 lines). */
    static final String[] M3 = {
        "{1}很高兴见到护士。",   // :257
        "{1}在宝可梦中心看起来更好一些。",   // :258
        "{1}似乎对治疗机器很感兴趣。”,",   // :259
        "{1}看起来想小睡一会儿。",   // :260
        "{1}对护士轻声问候。",   // :261
        "{1}正用一种俏皮的目光注视着{2}。",   // :262
        "{1}似乎完全自在。",   // :263
        "{1}完全放松了。",   // :264
        "{1}的脸上有一种满足的表情。",   // :265
    };

    /** 296_Follower_Config:277 (13 lines). */
    static final String[] M4 = {
        "{1}似乎对树木非常感兴趣。",   // :278
        "{1}似乎喜欢宝可梦的嗡嗡声。",   // :279
        "{1}在森林中不安地跳来跳去。",   // :280
        "{1}在到处徘徊，聆听不同的声音。",   // :281
        "{1}在草地上发呆。",   // :282
        "{1}到处奔跑，欣赏森林风光。",   // :283
        "{1}在草地上玩耍，拔拔草。",   // :284
        "{1}凝视着穿过树木的光。",   // :285
        "{1}正在玩一片叶子！",   // :286
        "{1}似乎正在听沙沙作响的声音。",   // :287
        "{1}一动不动，可能在扮演棵树。",   // :288
        "{1}被树枝缠住了，差点摔倒了！",   // :289
        "{1}感到惊讶！",   // :290
    };

    /** 296_Follower_Config:302 (10 lines). */
    static final String[] M5 = {
        "{1}看起来渴望战斗！",   // :303
        "{1}正用坚定的眼神看着{2}。",   // :304
        "{1}试图恐吓其他训练家。",   // :305
        "{1}相信{2}会提出制胜策略。",   // :306
        "{1}正在关注健身房的领导者。",   // :307
        "{1}已准备好与某人打架。",   // :308
        "{1}看起来可能正在准备一场大决战！",   // :309
        "{1}想炫耀它有多强大！",   // :310
        "{1}正在……做热身运动？",   // :311
        "{1}正在沉思中低声咆哮……",   // :312
    };

    /** 296_Follower_Config:324 (11 lines). */
    static final String[] M6 = {
        "{1}似乎很享受风景。",   // :325
        "{1}似乎很享受海浪拍打沙滩的声音。",   // :326
        "{1}看起来它想游泳！",   // :327
        "{1}几乎无法将目光移开海洋。",   // :328
        "{1}正渴望地盯着水面。",   // :329
        "{1}一直试图将{2}推向水面。",   // :330
        "{1}很高兴看到大海！",   // :331
        "{1}正在快乐地看海浪！",   // :332
        "{1}正在沙滩上玩耍！",   // :333
        "{1}正盯着{2}在沙滩上的脚印。",   // :334
        "{1}正在沙滩上打滚。",   // :335
    };

    /** 296_Follower_Config:348 (7 lines). */
    static final String[] M7 = {
        "{1}似乎很不舒服。",   // :349
        "{1}在发抖……",   // :350
        "{1}似乎不喜欢全身湿透……",   // :351
        "{1}一直试图让自己变干...",   // :352
        "{1}靠近{2}以获得舒适感。",   // :353
        "{1}抬头看着天空，皱着眉头。",   // :354
        "{1}似乎很难移动它的身体。",   // :355
    };

    /** 296_Follower_Config:360 (7 lines). */
    static final String[] M8 = {
        "{1}似乎很享受天气。",   // :361
        "{1}似乎对下雨很高兴！",   // :362
        "{1}似乎很惊讶下雨了！",   // :363
        "{1}在{2}身边开心地笑了！",   // :364
        "{1}正凝视着雨云。",   // :365
        "雨滴不断落在{1}头上。",   // :366
        "{1}张着嘴抬头仰望。",   // :367
    };

    /** 296_Follower_Config:372 (6 lines). */
    static final String[] M9 = {
        "{1}正在仰望天空。",   // :373
        "{1}看到下雨看起来有点惊讶。",   // :374
        "{1}一直试图让自己变干。",   // :375
        "下雨似乎不太打扰{1}。",   // :376
        "{1}在水坑里玩！",   // :377
        "{1}在水中滑了一下，差点摔倒！",   // :378
    };

    /** 296_Follower_Config:392 (6 lines). */
    static final String[] M10 = {
        "{1}正在仰望天空。",   // :393
        "风暴似乎让{1}兴奋不已。",   // :394
        "{1}抬头望天，大声喊道！",   // :395
        "风暴似乎只是在给 {1} 注入活力！",   // :396
        "{1}高兴地跳着跳圈！",   // :397
        "闪电根本不会打扰{1}。",   // :398
    };

    /** 296_Follower_Config:403 (6 lines). */
    static final String[] M11 = {
        "{1}正在仰望天空。",   // :404
        "风暴似乎让{1}有点紧张。",   // :405
        "闪电惊了{1}！",   // :406
        "下雨似乎不太打扰{1}。",   // :407
        "天气似乎让{1}处于紧张状态。",   // :408
        "{1}被闪电吓了一跳，依偎在{2}身边！",   // :409
    };

    /** 296_Follower_Config:423 (5 lines). */
    static final String[] M12 = {
        "{1}正在看着雪落。",   // :424
        "{1}被雪惊呆了！",   // :425
        "{1}微笑着仰望天空。",   // :426
        "雪似乎让{1}心情愉快。",   // :427
        "{1}因为冷而开朗！",   // :428
    };

    /** 296_Follower_Config:433 (7 lines). */
    static final String[] M13 = {
        "{1}嘴里叼着雪花。",   // :434
        "{1}正在看着雪落。",   // :435
        "{1}正在捕捉飘落的雪花。",   // :436
        "{1}想在它的嘴里接一片雪花。",   // :437
        "{1}被雪迷住了。",   // :438
        "{1}的牙齿在打颤！",   // :439
        "{1}因为寒冷使身体稍微变小了……",   // :440
    };

    /** 296_Follower_Config:454 (5 lines). */
    static final String[] M14 = {
        "{1}正在观看冰雹。",   // :455
        "{1}完全不受冰雹的困扰。",   // :456
        "{1}微笑着仰望天空。",   // :457
        "冰雹似乎让{1}心情愉快。",   // :458
        "{1}正在啃一块冰雹。",   // :459
    };

    /** 296_Follower_Config:464 (5 lines). */
    static final String[] M15 = {
        "{1}被冰雹击中！",   // :465
        "{1}想避开冰雹。",   // :466
        "冰雹正在击中{1}。",   // :467
        "{1}看起来不高兴。",   // :468
        "{1}像树叶一样颤抖！",   // :469
    };

    /** 296_Follower_Config:483 (4 lines). */
    static final String[] M16 = {
        "{1}被沙子覆盖。",   // :484
        "天气似乎根本不影响{1}！",   // :485
        "沙子似乎不能让{1}慢下来！",   // :486
        "{1}正在享受天气。",   // :487
    };

    /** 296_Follower_Config:492 (4 lines). */
    static final String[] M17 = {
        "{1}被沙子覆盖，但似乎并不介意。",   // :493
        "{1}似乎不受沙尘暴的困扰。",   // :494
        "沙子不会减慢{1}的速度。",   // :495
        "{1}似乎并不介意天气。",   // :496
    };

    /** 296_Follower_Config:501 (4 lines). */
    static final String[] M18 = {
        "{1}被沙子覆盖...",   // :502
        "{1}吐了一口沙子！",   // :503
        "{1}在沙尘暴中眯着眼睛。",   // :504
        "沙子似乎困扰着{1}。",   // :505
    };

    /** 296_Follower_Config:519 (6 lines). */
    static final String[] M19 = {
        "{1}似乎很高兴在阳光下。",   // :520
        "{1}正在沐浴阳光。",   // :521
        "明亮的阳光似乎根本不会打扰{1}。",   // :522
        "{1}向空中发射了一团环状孢子云！",   // :523
        "{1}伸展着身体，在阳光下放松。",   // :524
        "{1}散发出花香。",   // :525
    };

    /** 296_Follower_Config:530 (6 lines). */
    static final String[] M20 = {
        "{1}似乎对好天气很高兴！",   // :531
        "明亮的阳光似乎根本不会打扰{1}。",   // :532
        "{1}看着阳光很激动！",   // :533
        "{1}吹出一个火球。",   // :534
        "{1}正在吐火！",   // :535
        "{1}又热又开朗！",   // :536
    };

    /** 296_Follower_Config:541 (6 lines). */
    static final String[] M21 = {
        "{1}正在抬头看着天空",   // :542
        "{1}似乎感觉被阳光冒犯了。",   // :543
        "明媚的阳光似乎困扰着{1}。",   // :544
        "{1}出于某种原因看起来很沮丧。",   // :545
        "{1}正试图留在{2}的影子中。",   // :546
        "{1}一直在寻找避光处。",   // :547
    };

    /** 296_Follower_Config:552 (6 lines). */
    static final String[] M22 = {
        "{1}在明媚的阳光下眯着眼睛。",   // :553
        "{1}开始出汗了。",   // :554
        "{1}在这种天气下似乎有点不舒服。",   // :555
        "{1}看起来有点过热。",   // :556
        "{1}看起来很热...",   // :557
        "{1}挡住了它的视线，挡住了闪烁的光芒！",   // :558
    };

    /** 296_Follower_Config:571 (28 lines). */
    static final String[] M23 = {
        "{1}似乎想和{2}一起玩。",   // :572
        "{1}在轻微小声哼哼，似乎是在\n唱歌。",   // :573
        "{1}十分高兴的抬头看着{2}。",   // :574
        "{1}随其摇摆而跳舞。",   // :575
        "{1}正在无忧无虑地跳来跳去！",   // :576
        "{1}正在展示其敏捷性！",   // :577
        "{1}正在愉快地移动！",   // :578
        "哇！{1}在十分快乐的跳舞！！",   // :579
        "{1}稳步跟上{2}!",   // :580
        "{1}很高兴的跳来跳去。",   // :581
        "{1}正在嬉戏地走来走去。",   // :582
        "{1}正在嬉戏地抓住{2}的脚。",   // :583
        "{1}非常接近{2}！",   // :584
        "{1}转过身来，看着{2}。",   // :585
        "{1}正在努力炫耀其强大的\n力量！",   // :586
        "{1}到处在跑来跑去！",   // :587
        "{1}到处游荡欣赏风景。",   // :588
        "{1}似乎很喜欢这里！",   // :589
        "{1}很高兴！",   // :590
        "{1}似乎在唱歌？",   // :591
        "{1}正在快乐地跳舞！",   // :592
        "{1}跳着活泼的舞蹈十分\n开心！",   // :593
        "{1}十分高兴，正在唱歌！",   // :594
        "{1}抬起头大叫！",   // :595
        "{1}的心情似乎很乐观。",   // :596
        "看起来{1}好像在跳舞！",   // :597
        "{1}突然开始唱歌！感觉\n很棒。",   // :598
        "看来{1}想和{2}跳舞！",   // :599
    };

    /** 296_Follower_Config:656 (10 lines). */
    static final String[] M24 = {
        "{1}发出一声怒吼！",   // :657
        "{1}做了个生气的表情！",   // :658
        "{1}似乎出于某种原因生气了。",   // :659
        "{1}踩住了{2}的脚。",   // :660
        "{1}把脸凑过来，露出挑衅的表情。",   // :661
        "{1}正试图恐吓{2}的敌人！",   // :662
        "{1}想挑架！",   // :663
        "{1}正在准备战斗！",   // :664
        "看起来{1}现在几乎会与任何人战斗！",   // :665
        "{1}的咆哮声听起来几乎像说话……",   // :666
    };

    /** 296_Follower_Config:686 (33 lines). */
    static final String[] M25 = {
        "{1}一直在往下看。",   // :687
        "{1}正在四处嗅探。",   // :688
        "{1}正在集中注意力。",   // :689
        "{1}面对{2}点了点头。",   // :690
        "{1}直视{2}的眼睛。",   // :691
        "{1}正在调查该区域。",   // :692
        "{1}用锐利的目光聚焦！",   // :693
        "{1}心不在焉地环顾四周。",   // :694
        "{1}打哈欠的声音很大！",   // :695
        "{1}正在舒适地放松。",   // :696
        "{1}将注意力集中在{2}。",   // :697
        "{1}无所事事地盯着周围。",   // :698
        "{1}正在集中注意力。",   // :699
        "{1}面对{2}点了点头。",   // :700
        "{1}正在查看 {2} 的脚印。",   // :701
        "{1}似乎想玩，并期待地注视着{2}。",   // :702
        "{1}似乎在深入思考某件事。",   // :703
        "{1}没有关注{2}... 它似乎在考虑其他事情。",   // :704
        "{1}似乎很严肃。",   // :705
        "{1}似乎不感兴趣",   // :706
        "{1}的心思似乎在别处。",   // :707
        "{1}似乎在观察周围环境，而不是看着{2}。",   // :708
        "{1}看起来有点无聊。",   // :709
        "{1}的表情很严肃。",   // :710
        "{1}正盯着远方。",   // :711
        "{1}似乎在仔细检查{2}的脸。",   // :712
        "{1}似乎试图用它的眼睛交流。",   // :713
        "...{1}好像打喷嚏了！",   // :714
        "...{1}注意到{2}的鞋子有点脏。",   // :715
        "{1}好像吃了什么奇怪的东西，脸色很奇怪……",   // :716
        "{1}似乎闻到什么了，看起来很香。",   // :717
        "{1}注意到{2}的背包上有一点污垢...",   // :718
        "...... ...... ...... ...... ...... ...... ...... ...... ...... ...... ...... {1}默默点头。",   // :719
    };

    /** 296_Follower_Config:742 (28 lines). */
    static final String[] M26 = {
        "{1}开始戳 {2}。",   // :743
        "{1}看起来很开心。",   // :744
        "{1}高兴地拥抱了{2}。",   // :745
        "{1}高兴得停不下来。",   // :746
        "{1}看起来想要领先！",   // :747
        "{1}很开心。",   // :748
        "{1}和{2}一起走路似乎感觉很棒！",   // :749
        "{1}健康焕发。",   // :750
        "{1}看起来很开心。",   // :751
        "{1}为{2}付出了额外的努力！",   // :752
        "{1}正在闻周围空气的气味。”",   // :753
        "{1}高兴得跳了起来！",   // :754
        "{1}仍然感觉很棒！”",   // :755
        "{1}伸展着身体，正在放松。",   // :756
        "{1}正在尽最大努力跟上 {2}。",   // :757
        "{1}很高兴地拥抱 {2}！”",   // :758
        "{1}充满活力！",   // :759
        "{1}高兴得停不下来！",   // :760
        "{1}四处游荡，聆听不同的声音。",   // :761
        "{1}给了{2}一个快乐的表情和微笑。",   // :762
        "{1}兴奋地开始用鼻子粗鲁地呼吸！",   // :763
        "{1}急得发抖！",   // :764
        "{1}非常高兴，它开始四处游荡。",   // :765
        "{1}看到{2}的关注看起来很兴奋。",   // :766
        "{1}似乎很高兴{2}注意到了这一点！",   // :767
        "{1}开始兴奋地扭动整个身体！",   // :768
        "似乎{1}几乎无法阻止自己拥抱{2}！",   // :769
        "{1}靠近{2}的脚。",   // :770
    };

    /** 296_Follower_Config:799 (22 lines). */
    static final String[] M27 = {
        "{1}突然开始走近{2}。",   // :800
        "哇哦！{1}突然抱住了{2}。",   // :801
        "{1}与{2}擦肩而过。",   // :802
        "{1}接近{2}.",   // :803
        "{1}脸红了。",   // :804
        "{1}喜欢和{2}一起度过一天！",   // :805
        "{1}突然玩了起来！！",   // :806
        "{1}正在摩擦{2}的腿！",   // :807
        "{1}对{2}表示崇拜！",   // :808
        "{1}似乎想要从{2}那里得到一些夸奖。",   // :809
        "{1}似乎希望得到{2}的关注。",   // :810
        "{1}与{2}一起旅行似乎很开心。",   // :811
        "{1}似乎对{2}深情注视。",   // :812
        "{1}正用慈爱的眼睛看着{2}。",   // :813
        "{1}看起来想要{2}的款待。",   // :814
        "{1}看起来想要{2}的抚摸。",   // :815
        "{1}深情地摩擦着{2}。",   // :816
        "{1}的头轻轻地撞在了{2}的手上。",   // :817
        "{1}翻了个身，期待地看着{2}。",   // :818
        "{1}正用信任的眼神看着{2}。",   // :819
        "{1}似乎在向{2}求情！",   // :820
        "{1}模仿了{2}！",   // :821
    };

    /** 296_Follower_Config:838 (24 lines). */
    static final String[] M28 = {
        "{1}对你转了一圈！",   // :839
        "{1}发出战斗声。",   // :840
        "{1}正在监视中！",   // :841
        "{1}耐心地站着。",   // :842
        "{1}不安地环顾四周。",   // :843
        "{1}在四处游荡。",   // :844
        "{1}大声打哈欠！",   // :845
        "{1}稳定地站在{2}脚周围的地面上。",   // :846
        "{1}看着{2}并可爱的笑着。",   // :847
        "{1}凝视着远方。",   // :848
        "{1}跟上{2}。",   // :849
        "{1}看起来很满意。",   // :850
        "{1}装着很强大！",   // :851
        "{1}跟随者{2}的步伐。",   // :852
        "{1}开始绕圈旋转。",   // :853
        "{1}满怀期待地看着{2}。",   // :854
        "{1}摔倒了，看上去有些尴尬。",   // :855
        "{1}正在等待查看{2}会做什么。",   // :856
        "{1}正在默默地观看{2}。",   // :857
        "{1}正在寻找{2}的某种提示。",   // :858
        "{1}留在原地，等待{2}采取行动。",   // :859
        "{1}乖乖地坐在{2}的脚下。",   // :860
        "{1}被吓到了一下！",   // :861
        "{1}跳了一下！",   // :862
    };

    private static String fill(String text, Pokemon pkmn, Env env) {
        return text.replace("{1}", pkmn.name).replace("{2}", env.trainerName());
    }

    private static Result result(int animation, int wait, String text, Pokemon pkmn, Env env) {
        Result result = new Result();
        result.animation = animation;
        result.waitFrames = wait;
        result.message = fill(text, pkmn, env);
        return result;
    }

    private static Result found(Pokemon pkmn, Env env, String item, int quantity, String message) {
        if (!env.holdItem()) {
            return null;                                                            // :96 return false if !followerHoldItem
        }
        Result result = new Result();
        result.foundItem = true;
        result.foundItemName = item;
        result.foundQuantity = quantity;
        result.foundMessage = fill(message == null || message.isEmpty() ? "{1}似乎正在持有某物..." : message, pkmn, env);   // :98
        return result;
    }

    private static String pick(String[] messages, Random random) {
        return messages[random.nextInt(messages.length)];
    }

    /**
     * Runs the handlers for one talk. {@code randomVal} is {@code rand(6)} (297_Follower_Main:79); {@code random} draws
     * the lines. Null when no handler answers (the plugin then just turns the follower toward the player).
     */
    public static Result choose(Pokemon pkmn, Env env, int randomVal, Random random) {
        String map = env.mapName() == null ? "" : env.mapName();
        int weather = env.weather();
        // :145-178 a status
        String status = pkmn.status == null ? "" : pkmn.status;
        if (!status.isEmpty()) {
            switch (status) {
                case "POISON":     return result(FollowerRules.EMO_POISON, 120, "{1}因为中毒而颤抖...", pkmn, env);
                case "BURN":       return result(FollowerRules.EMO_HATE, 70, "{1}因为烧伤在颤抖", pkmn, env);
                case "FROZEN":     return result(FollowerRules.EMO_NORMAL, 100, "{1}被冰封了。", pkmn, env);
                case "SLEEP":      return result(FollowerRules.EMO_NORMAL, 100, "{1}看起来真的很累。", pkmn, env);
                case "PARALYSIS":  return result(FollowerRules.EMO_NORMAL, 100, "{1}站立不动并抽搐。", pkmn, env);
                case "FROSTBITE":  return result(FollowerRules.EMO_HATE, 70, "{1}的冻伤看起来很痛。", pkmn, env);
                case "DROWSY":     return result(FollowerRules.EMO_NORMAL, 100, "{1}感到困倦……", pkmn, env);
                default:           return new Result();                              // :177 next true with no message
            }
        }
        // :182-189 a map with "Battle" in its name
        if (map.contains("Battle")) {
            Result found = found(pkmn, env, ITEMS_BATTLE[random.nextInt(ITEMS_BATTLE.length)], 2, "{1}正拿着一个圆形物体……");
            if (found != null) return found;
        }
        // :192-204 the follower holds something
        if (env.holdItem()) {
            String item = env.itemNameById(random.nextInt(26));                    // :202 pbPokemonFound(rand(items.length)): an item NUMBER
            Result found = found(pkmn, env, item, 1, null);
            if (found != null) return found;
        }
        // :206-219
        if ("Route 3".equals(map) && FollowerRules.hasType(pkmn, "BUG")) {
            return result(FollowerRules.EMO_SING, 50, pick(M0, random), pkmn, env);
        }
        if ("Pokémon Lab".equals(map)) {                                           // :222
            return result(FollowerRules.EMO_NORMAL, 100, pick(M1, random), pkmn, env);
        }
        if (map.contains(env.trainerName())) {                                     // :237
            return result(FollowerRules.EMO_HAPPY, 70, pick(M2, random), pkmn, env);
        }
        if (map.contains("Poké Center") || map.contains("Pokémon Center")) {       // :252
            return result(FollowerRules.EMO_HAPPY, 70, pick(M3, random), pkmn, env);
        }
        if (map.contains("Forest")) {                                              // :273
            return result(FollowerRules.EMO_SING, 50, pick(M4, random), pkmn, env);
        }
        if (map.contains("Gym")) {                                                 // :298
            return result(FollowerRules.EMO_HATE, 70, pick(M5, random), pkmn, env);
        }
        if (map.contains("Beach")) {                                               // :320
            return result(FollowerRules.EMO_HAPPY, 70, pick(M6, random), pkmn, env);
        }
        if (weather == ScreenWeather.RAIN || weather == ScreenWeather.HEAVY_RAIN) {   // :343
            if (FollowerRules.hasType(pkmn, "FIRE") || FollowerRules.hasType(pkmn, "GROUND") || FollowerRules.hasType(pkmn, "ROCK")) {
                return result(FollowerRules.EMO_HATE, 70, pick(M7, random), pkmn, env);
            } else if (FollowerRules.hasType(pkmn, "WATER") || FollowerRules.hasType(pkmn, "GRASS")) {
                return result(FollowerRules.EMO_HAPPY, 70, pick(M8, random), pkmn, env);
            }
            return result(FollowerRules.EMO_NORMAL, 100, pick(M9, random), pkmn, env);
        }
        if (weather == ScreenWeather.STORM) {                                      // :387
            if (FollowerRules.hasType(pkmn, "ELECTRIC")) {
                return result(FollowerRules.EMO_HAPPY, 70, pick(M10, random), pkmn, env);
            }
            return result(FollowerRules.EMO_NORMAL, 100, pick(M11, random), pkmn, env);
        }
        if (weather == ScreenWeather.SNOW) {                                       // :418
            if (FollowerRules.hasType(pkmn, "ICE")) {
                return result(FollowerRules.EMO_HAPPY, 70, pick(M12, random), pkmn, env);
            }
            return result(FollowerRules.EMO_NORMAL, 100, pick(M13, random), pkmn, env);
        }
        if (weather == ScreenWeather.BLIZZARD) {                                   // :449
            if (FollowerRules.hasType(pkmn, "ICE")) {
                return result(FollowerRules.EMO_HAPPY, 70, pick(M14, random), pkmn, env);
            }
            return result(FollowerRules.EMO_HATE, 70, pick(M15, random), pkmn, env);
        }
        if (weather == ScreenWeather.SANDSTORM) {                                  // :478
            if (FollowerRules.hasType(pkmn, "ROCK") || FollowerRules.hasType(pkmn, "GROUND")) {
                return result(FollowerRules.EMO_HAPPY, 70, pick(M16, random), pkmn, env);
            } else if (FollowerRules.hasType(pkmn, "STEEL")) {
                return result(FollowerRules.EMO_NORMAL, 100, pick(M17, random), pkmn, env);
            }
            return result(FollowerRules.EMO_HATE, 70, pick(M18, random), pkmn, env);
        }
        if (weather == ScreenWeather.SUN) {                                        // :514
            if (FollowerRules.hasType(pkmn, "GRASS")) {
                return result(FollowerRules.EMO_HAPPY, 70, pick(M19, random), pkmn, env);
            } else if (FollowerRules.hasType(pkmn, "FIRE")) {
                return result(FollowerRules.EMO_HAPPY, 70, pick(M20, random), pkmn, env);
            } else if (FollowerRules.hasType(pkmn, "DARK")) {
                return result(FollowerRules.EMO_HATE, 70, pick(M21, random), pkmn, env);
            }
            return result(FollowerRules.EMO_NORMAL, 100, pick(M22, random), pkmn, env);
        }
        switch (randomVal) {
            case 0: {                                                              // :567 the music note
                int value = random.nextInt(M23.length);
                Result result = result(FollowerRules.EMO_SING, 50, M23[value], pkmn, env);
                switch (value) {
                    case 3: case 9:
                        result.playerWait = 65;
                        result.route = "TR W4 J W10 TU W4 J W10 TL W4 J W10 TD W4 J";
                        break;
                    case 4: case 5:
                        result.playerWait = 40;
                        result.route = "J W10 J W10 J";
                        break;
                    case 6: case 17:
                        result.playerWait = 20;
                        result.route = "TR W4 TD W4 TL W4 TU";
                        break;
                    case 7: case 28:
                        result.playerWait = 60;
                        result.route = "TR W4 TU W4 TL W4 TD W4 TR W4 TU W4 TL W4 TD W4 J W10 J";
                        break;
                    case 21: case 22:
                        result.playerWait = 50;
                        result.route = "TR W4 TU W4 TL W4 TD W4 J W10 J";
                        break;
                    default:
                        break;
                }
                return result;
            }
            case 1: {                                                              // :652 angry
                int value = random.nextInt(M24.length);
                Result result = result(FollowerRules.EMO_HATE, 70, M24[value], pkmn, env);
                if (value == 6 || value == 7 || value == 8) {
                    result.playerWait = 25;
                    result.route = "J W10 J";
                }
                return result;
            }
            case 2: {                                                              // :682 neutral
                int value = random.nextInt(M25.length);
                Result result = result(FollowerRules.EMO_NORMAL, 100, M25[value], pkmn, env);
                if (value == 1 || value == 5 || value == 7 || value == 20 || value == 21) {
                    result.playerWait = 35;
                    result.route = "TR W10 TU W10 TL W10 TD";
                }
                return result;
            }
            case 3: {                                                              // :738 happy
                int value = random.nextInt(M26.length);
                Result result = result(FollowerRules.EMO_HAPPY, 70, M26[value], pkmn, env);
                if (value == 3) {
                    result.playerWait = 45;
                    result.route = "TR W4 TU W4 TL W4 TD W4 J W10 J";
                } else if (value == 11 || value == 16 || value == 17 || value == 24) {
                    result.playerWait = 40;
                    result.route = "J W10 J W10 J";
                }
                return result;
            }
            case 4: {                                                              // :795 heart
                int value = random.nextInt(M27.length);
                Result result = result(FollowerRules.EMO_LOVE, 70, M27[value], pkmn, env);
                if (value == 1 || value == 6) {                                    // :825 "when 1, 6," (a trailing comma in the plugin)
                    result.playerWait = 10;
                    result.route = "J";
                }
                return result;
            }
            case 5: {                                                              // :836 no animation
                int value = random.nextInt(M28.length);
                Result result = result(0, 0, M28[value], pkmn, env);
                if (value == 0) {
                    result.playerWait = 15;
                    result.route = "TR W4 TU W4 TL W4 TD";
                } else if (value == 2 || value == 4) {
                    result.playerWait = 35;
                    result.route = "TR W10 TU W10 TL W10 TD";
                } else if (value == 14) {
                    result.playerWait = 50;
                    result.route = "TR W4 TU W4 TL W4 TD W4 TR W4 TU W4 TL W4 TD W4 TR W4 TU W4 TL W4 TD";
                } else if (value == 22 || value == 23) {
                    result.playerWait = 10;
                    result.route = "J";
                }
                return result;
            }
            default:
                return null;
        }
    }
}
