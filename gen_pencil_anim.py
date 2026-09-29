#!/usr/bin/env python3
"""Generates the pencil-sketch cast animation flipbook:
assets/.../textures/pencil/cast_0..4.png  — grayscale hand-drawn frames
assets/.../textures/pencil/glow_0..4.png  — ONLY the energy (tinted at runtime)

The figure is a sketchy humanoid drawn with jittered strokes so it reads as
hand-penciled. Frames: 0 stance, 1 coil, 2 thrust, 3 release, 4 cooldown.
"""
import math
import os
import random
from PIL import Image, ImageDraw, ImageFilter

random.seed(42)

W, H = 640, 360
FRAMES = 5
OUT = os.path.join(os.path.dirname(__file__),
                   "src/main/resources/assets/rumblefruit/textures/pencil")
os.makedirs(OUT, exist_ok=True)

INK = (24, 20, 26, 255)
PAPER = (236, 231, 218, 255)
PAPER_D = (210, 204, 190, 255)


def jittered_line(dr, pts, width, ink=INK, jit=1.6):
    """a hand-drawn stroke: redraw the polyline a few times with jitter"""
    for _ in range(3):
        jpts = [(x + random.uniform(-jit, jit), y + random.uniform(-jit, jit)) for x, y in pts]
        dr.line(jpts, fill=ink, width=width, joint="curve")


def sketch_ellipse(dr, cx, cy, rx, ry, width, ink=INK):
    for _ in range(3):
        j = random.uniform(-1.5, 1.5)
        dr.ellipse([cx - rx + j, cy - ry - j, cx + rx - j, cy + ry + j], outline=ink, width=width)


def speed_lines(dr, cx, cy, count, r0, r1, spread=math.pi * 2, base_angle=0.0):
    for i in range(count):
        a = base_angle + (i / max(1, count - 1) - 0.5) * spread + random.uniform(-0.08, 0.08)
        l0 = r0 + random.uniform(-8, 8)
        l1 = r1 + random.uniform(-15, 25)
        dr.line([(cx + math.cos(a) * l0, cy + math.sin(a) * l0),
                 (cx + math.cos(a) * l1, cy + math.sin(a) * l1)],
                fill=INK, width=random.choice([1, 2, 2, 3]))


def paper_bg(img):
    dr = ImageDraw.Draw(img)
    dr.rectangle([0, 0, W, H], fill=PAPER)
    # sketchy border + paper grain
    for _ in range(220):
        x, y = random.uniform(0, W), random.uniform(0, H)
        dr.point([(x, y)], fill=PAPER_D)
    for i in range(4):
        j = random.uniform(-3, 3)
        dr.rectangle([6 + j, 6 - j, W - 6 + j, H - 6 - j], outline=INK, width=2)
    # corner slashes
    speed_lines(dr, 30, 30, 6, 20, 70, spread=0.9, base_angle=0.6)
    speed_lines(dr, W - 30, H - 30, 6, 20, 70, spread=0.9, base_angle=math.pi + 0.6)


# pose description: joint positions for the figure per frame
# (hx,hy) head center; (sx,sy) shoulder; (ex,ey) elbows; (wx,wy) hands; hips; knees; feet
def pose(frame):
    cx, cy = 240, 250  # hip base
    if frame == 0:      # stance: tall, arms swept back
        return dict(head=(cx, cy - 150), shoulder=(cx, cy - 110),
                    elbR=(cx + 45, cy - 90), hndR=(cx + 65, cy - 105),
                    elbL=(cx - 45, cy - 90), hndL=(cx - 65, cy - 105),
                    kneeR=(cx + 25, cy - 45), ftR=(cx + 35, cy),
                    kneeL=(cx - 25, cy - 45), ftL=(cx - 35, cy))
    if frame == 1:      # coil: crouched, arms back, head down
        return dict(head=(cx + 10, cy - 120), shoulder=(cx + 5, cy - 85),
                    elbR=(cx + 55, cy - 70), hndR=(cx + 85, cy - 75),
                    elbL=(cx - 40, cy - 65), hndL=(cx - 60, cy - 70),
                    kneeR=(cx + 30, cy - 35), ftR=(cx + 45, cy),
                    kneeL=(cx - 25, cy - 40), ftL=(cx - 45, cy - 5))
    if frame == 2:      # thrust: right arm shoots forward
        return dict(head=(cx + 20, cy - 145), shoulder=(cx + 15, cy - 105),
                    elbR=(cx + 75, cy - 105), hndR=(cx + 135, cy - 105),
                    elbL=(cx - 30, cy - 80), hndL=(cx - 55, cy - 85),
                    kneeR=(cx + 35, cy - 45), ftR=(cx + 55, cy),
                    kneeL=(cx - 20, cy - 40), ftL=(cx - 35, cy - 2))
    if frame == 3:      # release: arm forward, body open
        return dict(head=(cx + 25, cy - 150), shoulder=(cx + 18, cy - 110),
                    elbR=(cx + 80, cy - 108), hndR=(cx + 145, cy - 108),
                    elbL=(cx - 35, cy - 95), hndL=(cx - 70, cy - 100),
                    kneeR=(cx + 35, cy - 45), ftR=(cx + 60, cy),
                    kneeL=(cx - 20, cy - 42), ftL=(cx - 38, cy - 2))
    # frame 4: cooldown — arm lowers, stance settles
    return dict(head=(cx + 10, cy - 148), shoulder=(cx + 5, cy - 108),
                elbR=(cx + 55, cy - 85), hndR=(cx + 90, cy - 70),
                elbL=(cx - 40, cy - 90), hndL=(cx - 60, cy - 100),
                kneeR=(cx + 28, cy - 45), ftR=(cx + 40, cy),
                kneeL=(cx - 25, cy - 43), ftL=(cx - 36, cy))


def draw_figure(dr, p, line=5):
    hx, hy = p["head"]
    sx, sy = p["shoulder"]
    # torso: shoulder -> hip-ish (midpoint of knees)
    hipx = (p["kneeR"][0] + p["kneeL"][0]) / 2
    hipy = (p["kneeR"][1] + p["kneeL"][1]) / 2 - 10
    # head
    sketch_ellipse(dr, hx, hy, 17, 20, line - 2)
    # messy hair strokes
    for i in range(5):
        a = -0.6 + i * 0.3
        jittered_line(dr, [(hx + math.cos(a) * 10, hy - 14 + math.sin(a) * 8),
                           (hx + math.cos(a) * 20, hy - 30 + math.sin(a) * 14)], 2)
    # neck + torso (two strokes for volume)
    jittered_line(dr, [(hx, hy + 18), (sx, sy)], line - 1)
    jittered_line(dr, [(sx, sy), (hipx, hipy)], line + 2)
    jittered_line(dr, [(sx - 8, sy + 4), (hipx - 8, hipy)], 2)
    # arms
    jittered_line(dr, [(sx, sy), p["elbR"]], line)
    jittered_line(dr, [p["elbR"], p["hndR"]], line - 1)
    jittered_line(dr, [(sx, sy), p["elbL"]], line)
    jittered_line(dr, [p["elbL"], p["hndL"]], line - 1)
    # hands
    for hnd in (p["hndR"], p["hndL"]):
        sketch_ellipse(dr, hnd[0], hnd[1], 7, 7, line - 3)
    # legs
    jittered_line(dr, [(hipx, hipy), p["kneeR"]], line)
    jittered_line(dr, [p["kneeR"], p["ftR"]], line - 1)
    jittered_line(dr, [(hipx, hipy), p["kneeL"]], line)
    jittered_line(dr, [p["kneeL"], p["ftL"]], line - 1)
    # feet
    jittered_line(dr, [(p["ftR"][0] - 8, p["ftR"][1]), (p["ftR"][0] + 8, p["ftR"][1])], 3)
    jittered_line(dr, [(p["ftL"][0] - 8, p["ftL"][1]), (p["ftL"][0] + 8, p["ftL"][1])], 3)


def draw_glow(dr, p, frame):
    hx, hy = p["hndR"]
    if frame == 2:      # the orb ignites
        for r, a in [(26, 70), (16, 120), (8, 220)]:
            dr.ellipse([hx - r, hy - r, hx + r, hy + r], fill=(255, 255, 255, a))
    if frame == 3:      # the burst: orb + lightning fork + beam
        for r, a in [(60, 60), (40, 100), (22, 170), (10, 255)]:
            dr.ellipse([hx - r, hy - r, hx + r, hy + r], fill=(255, 255, 255, a))
        # lightning fork forward
        pts = [(hx + 10, hy)]
        x, y = hx + 10, hy
        for i in range(7):
            x += random.uniform(28, 48)
            y += random.uniform(-26, 26)
            pts.append((x, y))
        dr.line(pts, fill=(255, 255, 255, 235), width=10, joint="curve")
        dr.line(pts, fill=(255, 255, 255, 255), width=4, joint="curve")
        for bpt in (pts[2], pts[4]):
            bx, by = bpt
            branch = [(bx, by), (bx + 30, by + random.uniform(-30, 30)),
                      (bx + 60, by + random.uniform(-40, 40))]
            dr.line(branch, fill=(255, 255, 255, 190), width=4, joint="curve")
    if frame == 4:      # embers
        for i in range(8):
            ex = hx + random.uniform(10, 130)
            ey = hy + random.uniform(-60, 60)
            r = random.uniform(2, 5)
            dr.ellipse([ex - r, ey - r, ex + r, ey + r], fill=(255, 255, 255, 160))


for f in range(FRAMES):
    base = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    paper_bg(base)
    dr = ImageDraw.Draw(base)
    p = pose(f)
    # ground scribble
    jittered_line(dr, [(150, 255), (330, 252)], 3)
    jittered_line(dr, [(170, 260), (310, 258)], 2)
    draw_figure(dr, p)
    if f == 1:
        speed_lines(dr, p["head"][0], p["head"][1] - 60, 7, 30, 90, spread=1.6, base_angle=-math.pi / 2)
    if f == 2:
        speed_lines(dr, p["hndR"][0], p["hndR"][1], 8, 40, 110, spread=1.2, base_angle=0.0)
    if f == 3:
        speed_lines(dr, p["hndR"][0], p["hndR"][1], 14, 70, 200, spread=1.6, base_angle=0.0)
    if f == 4:
        dr = ImageDraw.Draw(base)
        jittered_line(dr, [(p["head"][0] + 24, p["head"][1] - 10),
                           (p["head"][0] + 40, p["head"][1] - 22)], 2)
    base = base.filter(ImageFilter.GaussianBlur(0.4))
    base.save(os.path.join(OUT, f"cast_{f}.png"))

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dg = ImageDraw.Draw(glow)
    draw_glow(dg, p, f)
    glow = glow.filter(ImageFilter.GaussianBlur(1.2))
    glow.save(os.path.join(OUT, f"glow_{f}.png"))

print("frames written:", sorted(os.listdir(OUT)))
