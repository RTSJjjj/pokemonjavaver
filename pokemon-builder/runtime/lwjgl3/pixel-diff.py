#!/usr/bin/env python3
"""Pixel-level regression for the T4 perf pass (and any later capture batch).

The probes in ``lwjgl3/MenuCapture`` render fixed points of the game into PNGs
(one directory per mode). This script turns "the optimisation changed nothing
visible" into a re-runnable check: it decodes every PNG and compares pixels, so
a change in PNG compression or metadata cannot masquerade as a difference.

Usage
-----
    python pixel-diff.py stats <dir> [pattern] [--no-recursive] [--quiet]
    python pixel-diff.py diff  <dirA> <dirB> [pattern] [--no-recursive] [--quiet]

    <dir>        capture directory (``X:\\runtime\\lwjgl3\\perf-base`` ...)
    [pattern]    glob for the files to look at, default ``*.png``
    --no-recursive  only look at the directory itself (the default walks
                    subdirectories, i.e. one subtree per capture mode)
    --quiet      diff only: print just the differing files and the summary

Exit code
---------
``stats``: 0. ``diff``: 0 when every file exists on both sides and every pixel
matches, 1 otherwise (missing / extra / resized / differing files), 2 on a usage
or IO error - so ``diff`` can gate a build.

Examples
--------
    python pixel-diff.py stats X:\\runtime\\lwjgl3\\perf-after
    python pixel-diff.py diff X:\\runtime\\lwjgl3\\perf-base X:\\runtime\\lwjgl3\\perf-after
    python pixel-diff.py diff base after "l1-battle-*.png"

Requires Pillow (``pip install Pillow``).
"""

import os
import sys

try:
    from PIL import Image, ImageChops
except ImportError:  # pragma: no cover - environment guard
    sys.stderr.write("pixel-diff.py needs Pillow: pip install Pillow\n")
    sys.exit(2)


def parse_args(argv):
    if len(argv) < 2 or argv[1] not in ("stats", "diff"):
        sys.stderr.write(__doc__)
        sys.exit(2)
    mode = argv[1]
    rest = argv[2:]
    recursive = True
    quiet = False
    positional = []
    for arg in rest:
        if arg == "--no-recursive":
            recursive = False
        elif arg == "--quiet":
            quiet = True
        elif arg.startswith("--"):
            sys.stderr.write("unknown option: %s\n" % arg)
            sys.exit(2)
        else:
            positional.append(arg)
    if mode == "stats":
        if not positional:
            sys.stderr.write(__doc__)
            sys.exit(2)
        return mode, positional[0], None, positional[1] if len(positional) > 1 else "*.png", recursive, quiet
    if len(positional) < 2:
        sys.stderr.write(__doc__)
        sys.exit(2)
    return mode, positional[0], positional[1], positional[2] if len(positional) > 2 else "*.png", recursive, quiet


def collect(root, pattern, recursive):
    """Relative path -> absolute path for every matching file, sorted."""
    import fnmatch

    found = {}
    if recursive:
        for dirpath, _dirnames, filenames in os.walk(root):
            for name in filenames:
                if fnmatch.fnmatch(name, pattern):
                    absolute = os.path.join(dirpath, name)
                    found[os.path.relpath(absolute, root).replace(os.sep, "/")] = absolute
    else:
        for name in os.listdir(root):
            absolute = os.path.join(root, name)
            if os.path.isfile(absolute) and fnmatch.fnmatch(name, pattern):
                found[name] = absolute
    return dict(sorted(found.items()))


def image_stats(path):
    image = Image.open(path).convert("RGBA")
    pixels = list(image.getdata())
    count = len(pixels)
    dark = sum(1 for r, g, b, _a in pixels if r < 8 and g < 8 and b < 8)
    total = sum(r + g + b for r, g, b, _a in pixels)
    opaque = sum(1 for _r, _g, _b, a in pixels if a == 255)
    return image.size, total / (3.0 * count), (count - dark) / count, opaque / count


def stats(root, pattern, recursive, quiet):
    files = collect(root, pattern, recursive)
    if not files:
        sys.stderr.write("no files match %s under %s\n" % (pattern, root))
        return 2
    print("%-46s %-11s %9s %9s %8s" % ("file", "size", "mean", "nonblack", "opaque"))
    for relative, absolute in files.items():
        size, mean, nonblack, opaque = image_stats(absolute)
        print("%-46s %-11s %9.3f %9.4f %8.4f"
              % (relative, "%dx%d" % size, mean, nonblack, opaque))
    print("stats: %d file(s) under %s" % (len(files), root))
    return 0


def diff(dir_a, dir_b, pattern, recursive, quiet):
    files_a = collect(dir_a, pattern, recursive)
    files_b = collect(dir_b, pattern, recursive)
    names = sorted(set(files_a) | set(files_b))
    if not names:
        sys.stderr.write("no files match %s under %s / %s\n" % (pattern, dir_a, dir_b))
        return 2

    differing = 0
    missing = 0
    extra = 0
    identical_bytes = 0
    for name in names:
        if name not in files_b:
            print("%-46s MISSING in B" % name)
            missing += 1
            continue
        if name not in files_a:
            print("%-46s EXTRA in B" % name)
            extra += 1
            continue
        path_a = files_a[name]
        path_b = files_b[name]
        same_bytes = open(path_a, "rb").read() == open(path_b, "rb").read()
        image_a = Image.open(path_a).convert("RGBA")
        image_b = Image.open(path_b).convert("RGBA")
        if image_a.size != image_b.size:
            print("%-46s SIZE %dx%d vs %dx%d" % (name, image_a.size[0], image_a.size[1],
                                                 image_b.size[0], image_b.size[1]))
            differing += 1
            continue
        delta = ImageChops.difference(image_a, image_b)
        # Pillow's getbbox() looks at the alpha channel only when the image has
        # one (alpha_only defaults to True), and two opaque captures always
        # differ by alpha 0 - so ask for the RGB box explicitly.
        bbox = delta.convert("RGB").getbbox()
        if bbox is None:
            identical_bytes += int(same_bytes)
            if not quiet:
                print("%-46s identical (%s)"
                      % (name, "same bytes" if same_bytes else "pixels match, bytes differ"))
            continue
        pixels = sum(1 for pixel in delta.getdata() if pixel != (0, 0, 0, 0))
        print("%-46s diff=%d bbox=%s" % (name, pixels, bbox))
        differing += 1

    print("diff: %d file(s) compared, %d differing, %d missing, %d extra, "
          "%d byte-identical, pattern=%s"
          % (len(names), differing, missing, extra, identical_bytes, pattern))
    if differing or missing or extra:
        print("RESULT: FAIL (pixel regression)")
        return 1
    print("RESULT: OK (0 pixel differences)")
    return 0


def main(argv):
    mode, first, second, pattern, recursive, quiet = parse_args(argv)
    try:
        if mode == "stats":
            return stats(first, pattern, recursive, quiet)
        return diff(first, second, pattern, recursive, quiet)
    except IOError as error:
        sys.stderr.write("IO error: %s\n" % error)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
