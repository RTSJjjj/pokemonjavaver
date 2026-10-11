# 开场动画

游戏每次启动都会先播开场动画（`OpeningScreen`），按任意键或点屏幕跳过，播完进入标题画面。

- 运行时：`runtime/core/.../ui/menu/OpeningScreen.java`。它是离线合成脚本 `render_opening_v2.py` 的 Java 移植版，时间轴相同，也用同样的 1280×720 设计坐标，画面上下留黑边。画面时钟跟随音乐播放位置。
- 素材：`pokemon-builder/assets/opening/`。时间表（`opening.json`）、图层和 `bgm.mp3` 都在这里。构建时这个目录作为 `Graphics/Opening` 打进游戏：
  - PC 版复制到 `runtime-data/`
  - 安卓版打进数据包
  
  两种都和其他素材一样加密（见 `builder/src/data-pack.js`）。
- 找不到 `Graphics/Opening/opening.json`（比如直接用源工程运行）时，会跳过开场，直接进入标题。

## 改了开场之后重新生成素材

开场是先用离线合成工程（`929/opening/ref/`）做出来的：先跑 `prepare_assets.py` 准备素材，再用 `render_opening_v2.py` 渲染成视频。改完文字、角色或时间点，按下面的步骤更新：

```
cd 929/opening/ref
python prepare_assets.py
python render_opening_v2.py --preview          # 先看分镜
python <pokemon-builder>/tools/opening/export_assets.py .
```

`export_assets.py` 会清空 `assets/opening/` 再重新写入。所有用到滤镜的效果都在这一步烘焙成图片，包括去饱和、压暗立绘、名牌、水印、模糊阴影和标题画面。运行时只做位移、缩放、旋转、透明度和纯色遮罩。需要 Python 和 Pillow、numpy。

## 验证

- 逐帧对比：`POKEMON_CAPTURE_MAIN=pokemon.runtime.lwjgl3.OpeningCapture`，执行 `scripts/jdk-capture.ps1 <assets/opening> <输出目录> <t1,t2,...>`，按指定时间点输出 Java 版画面，和 `render_opening_v2.frame(t)` 的结果对比。
- 实际播放：`pokemon.runtime.lwjgl3.OpeningLiveCheck <含 Graphics/Opening 的目录> <dataRoot> [秒]`。它会在隐藏窗口里静音播放，检查音乐在播、时钟跟得上、跳过后能切到下一个界面。classpath 要带上 `jlayer-1.0.1-gdx.jar`；`jdk-capture.ps1` 默认没带这个包，因为截帧时会关掉音频。
