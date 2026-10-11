"""开场动画素材导出：把离线合成版（参考脚本 render_opening_v2.py + prepare_assets.py 生成的 assets/）
里运行时要用到的图层，烘焙成游戏用的素材，写进 pokemon-builder/assets/opening/。

    python export_assets.py <ref 目录（含 render_opening_v2.py 和 assets/）> [输出目录]

需要 Pillow + numpy。运行时（OpeningScreen.java）只做位移 / 缩放 / 透明度 / 遮罩，
所以凡是 Python 里用到滤镜的部分（去饱和、压暗、名牌、水印、模糊阴影、标题画面）都在这里先烘焙好。
文件名全部用 ASCII，避免压缩包和安卓文件系统的编码问题。
"""
import json, os, shutil, sys
from pathlib import Path
from PIL import Image, ImageEnhance

REF = Path(sys.argv[1]).resolve()
OUT = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else Path(__file__).resolve().parents[2] / 'assets' / 'opening'
sys.path.insert(0, str(REF))
os.chdir(REF)
import render_opening as b          # noqa: E402  参考脚本（基础合成器）
import render_opening_v2 as v       # noqa: E402  参考脚本（第二版时间轴）

IDS = {'南晓': 'nanxiao', '索伽': 'suojia', '伊娟': 'yijuan', '朱羽': 'zhuyu',
       '心悦': 'xinyue', '沁奏': 'qinzou', '清勉': 'qingmian', '松铭': 'songming'}
A = REF / 'assets'

def save(im, name):
    im.save(OUT / name, optimize=True)

def unscale(im):
    """准备脚本把像素图按整数倍最近邻放大过；找出倍数还原成 1 倍，运行时再按需放大。"""
    for k in range(40, 1, -1):
        if im.width % k or im.height % k:
            continue
        small = im.resize((im.width // k, im.height // k), Image.Resampling.NEAREST)
        if small.resize(im.size, Image.Resampling.NEAREST).tobytes() == im.tobytes():
            return small
    return im

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.iterdir():
        if old.is_file():
            old.unlink()

    # 胶卷条（参考里裁掉透明边后平铺）
    save(b.asset('胶卷.png', True), 'film.png')
    # 开场背景：'dark' 色调 = 饱和度 .5 再亮度 .19；亮度在运行时用着色乘上去
    bg = Image.open(A / '死魂场地.jpg').convert('RGB')
    bg = bg.resize((bg.width // 3, bg.height // 3), Image.Resampling.NEAREST)  # 准备脚本放大了 3 倍，还原
    save(ImageEnhance.Color(bg).enhance(.5), 'intro_bg.png')
    for i in range(1, 5):
        save(b.asset(f'文字{i}.png', True), f'story{i}.png')
    # 暗角：INK 颜色 + 参考的 alpha 曲线
    vig = b.VIGNETTE.resize((480, 270), Image.Resampling.BILINEAR)
    plate = Image.new('RGBA', vig.size, b.INK + (0,)); plate.putalpha(vig)
    save(plate, 'vignette.png')

    chars = []
    for c in v.CHARS:
        cid = IDS[c['name']]
        body = b.asset(c['body'], True)
        save(body, f'body_{cid}.png')
        # 卡图阶段空侧的压暗立绘（亮度 .42、饱和度 .6、不透明度 .85）
        rgb = ImageEnhance.Color(ImageEnhance.Brightness(body.convert('RGB')).enhance(.42)).enhance(.6)
        ghost = rgb.convert('RGBA'); ghost.putalpha(body.getchannel('A').point(lambda a: round(a * .85)))
        save(ghost, f'ghost_{cid}.png')
        back = Image.open(A / f"{c['name']}卡图·背景.png").convert('RGB')
        back = back.resize((back.width // 3, back.height // 3), Image.Resampling.NEAREST)
        save(ImageEnhance.Color(back).enhance(.88), f'back_{cid}.png')
        save(v.nameplate(c['en']), f'plate_{cid}.png')          # 1080p 像素尺寸，运行时 /1.5 换成设计单位
        save(v.watermark(c['en']), f'mark_{cid}.png')
        save(unscale(b.asset(c['name'] + '像素.png', True)), f'pixel_{cid}.png')
        sh = v.pix_shadow(c['name'])                            # 模糊阴影，半分辨率足够
        save(sh.resize((sh.width // 2, sh.height // 2), Image.Resampling.BILINEAR), f'shadow_{cid}.png')
        chars.append(dict(id=cid, en=c['en'], start=c['start'], end=c['end'], close=c['close'], card=c['card'],
                          side=c['card_side'], face=list(c['face']), panY=c['pan_y'],
                          plateSize=list(v.nameplate(c['en']).size), markSize=list(v.watermark(c['en']).size),
                          shadowSize=list(sh.size)))

    # 记忆拼图（2480 方形画布 → 1024）
    k = 1024 / 2480
    for src, name in [('死者的记忆A部分.png', 'memory_a.png'), ('死者的记忆B部分.png', 'memory_b.png')]:
        im = Image.open(A / src).convert('RGBA')
        save(im.resize((round(im.width * k), round(im.height * k)), Image.Resampling.LANCZOS), name)
    full = Image.open(A / '死者的记忆.jpg').convert('RGB').resize((1024, 1024), Image.Resampling.LANCZOS)
    full.save(OUT / 'memory_full.jpg', quality=92)
    memory_a_width = Image.open(A / '死者的记忆A部分.png').width / 2480   # 相对方形画布的宽度

    # 快切：还原成原尺寸（准备脚本放大了 3 倍），按参考的帧表播放
    montage = []
    for i, name in enumerate(v.MONTAGE):
        im = Image.open(A / name).convert('RGB')
        im = im.resize((im.width // 3, im.height // 3), Image.Resampling.LANCZOS)
        fn = f'shot_{i + 1:02d}.png'
        save(im, fn)
        montage.append(dict(file=fn, start=int(v.STARTS[i]), frames=int(v.LENGTHS[i])))
    flashes = [dict(frame=int(f), color=list(c)) for f, c in sorted(v.FLASHES.items())]

    # 标题：参考的 ending() 是 79s 那一帧的静止画面再做淡入淡出
    v.title_plate().resize((672, 378), Image.Resampling.LANCZOS).save(OUT / 'title.png', optimize=True)
    shutil.copyfile(A / 'bgm.mp3', OUT / 'bgm.mp3')

    data = dict(
        version=1, design=[1280, 720], fps=v.FPS, duration=v.DURATION, start=v.START,
        memory=dict(start=32 + v.OFFSET, end=34.5 + v.OFFSET, aWidth=memory_a_width),
        montageStart=v.MON_START, montageEnd=v.MON_END, titleFade=93.0, musicFadeStart=98.68,
        ink=list(b.INK), characters=chars, montage=montage, flashes=flashes,
        story=[dict(file=f'story{i}.png', start=a, end=e, k=6) for a, e, i in [(0, 5.65, 1), (5.65, 11.3, 2), (11.3, 16.15, 3), (16.15, 21.2, 4)]],
    )
    (OUT / 'opening.json').write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding='utf-8')
    total = sum(p.stat().st_size for p in OUT.iterdir())
    print(f'EXPORTED {len(list(OUT.iterdir()))} files, {total / 1048576:.1f} MB -> {OUT}')

if __name__ == '__main__':
    main()
