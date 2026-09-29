#!/usr/bin/env python3
"""Generates exorcist.png (64x64 diffuse) and exorcist_glow.png (emissive)
for the face-from-the-abyss exorcist model.

Box-UV layout (verified in-game): for texOffs(u,v) and box (w,h,d) the -z face
(the side the player meets) samples (u+2d+w, v+d, w, h); the +z face samples
(u+d, v+d, w, h). Backgrounds are opaque: transparent texels bleed into mip
levels at distance and cutout rendering fades the model out.
"""
import os
import random
from PIL import Image

random.seed(7)

W = H = 64
img = Image.new("RGBA", (W, H), (18, 13, 26, 255))
glow = Image.new("RGBA", (W, H), (0, 0, 0, 255))  # additive pass: black = dark
px = img.load()
gx = glow.load()

GOLD = (196, 156, 62, 255)
GOLD_D = (138, 104, 38, 255)
GOLD_L = (255, 224, 138, 255)
BONE = (228, 220, 198, 255)
BONE_D = (182, 172, 148, 255)
BONE_DD = (122, 112, 94, 255)
ABYSS = (36, 22, 52, 255)
ABYSS_D = (22, 13, 33, 255)
CRACK = (178, 92, 255, 255)
CRACK_GLOW = (220, 150, 255, 255)
FEATHER = (46, 32, 66, 255)
FEATHER_D = (30, 20, 44, 255)
FEATHER_TIP = (122, 70, 178, 255)
EYE = (255, 208, 64, 255)
EYE_CORE = (255, 244, 200, 255)
SOCKET = (40, 10, 52, 255)
THIRD = (190, 90, 250, 255)
THIRD_GLOW = (235, 170, 255, 255)


def fill(p, x0, y0, x1, y1, color):
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            if 0 <= x < W and 0 <= y < H:
                p[x, y] = color


def noise(p, x0, y0, x1, y1, base, dark, amount=0.3):
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            if 0 <= x < W and 0 <= y < H:
                p[x, y] = dark if random.random() < amount else base


# ---------------- FACE: texOffs(0,0), box 8x10x2, d=2 ----------------
# player-facing (-z) face: x12-20, y2-12
noise(px, 0, 0, 20, 12, BONE_D, BONE_DD, 0.15)
fill(px, 12, 2, 20, 12, BONE)
for x in range(12, 20):                 # gold brow band
    px[x, 2] = GOLD
    px[x, 3] = GOLD_D
fill(px, 13, 5, 15, 8, SOCKET)          # left socket
fill(px, 17, 5, 19, 8, SOCKET)          # right socket
px[15, 4] = THIRD                        # third eye setting
px[16, 4] = THIRD
px[16, 3] = BONE_DD                      # forehead crack
px[15, 3] = BONE_DD
px[14, 4] = BONE_DD
for x in range(12, 20):                 # chin shading
    px[x, 10] = BONE_D
    px[x, 11] = BONE_D
px[12, 2] = GOLD_L                       # brow highlights
px[19, 2] = GOLD_L

# ---------------- HORNS: texOffs(24,0), box 1.5x6x1.5 ----------------
fill(px, 24, 0, 30, 8, GOLD)
for y in range(0, 8):
    px[25, y] = GOLD_L if y % 2 == 0 else GOLD
    px[27, y] = GOLD_D

# ---------------- EYES: texOffs(32,0), box 1.8x1.8x0.6 ----------------
fill(px, 32, 0, 37, 3, EYE)
px[33, 1] = EYE_CORE
px[34, 1] = EYE_CORE
fill(gx, 32, 0, 37, 3, EYE)
gx[33, 1] = EYE_CORE
gx[34, 1] = EYE_CORE

# ---------------- CORE (ex-third-eye): texOffs(48,0), box 2.2x2.6x0.8 ----------------
fill(px, 48, 0, 55, 4, THIRD)
px[49, 1] = (255, 220, 255, 255)  # hot center
px[50, 1] = (255, 220, 255, 255)
fill(gx, 48, 0, 55, 4, THIRD_GLOW)
gx[49, 1] = (255, 240, 255, 255)
gx[50, 1] = (255, 240, 255, 255)

# ---------------- WINGS: texOffs(0,16), feather box up to 14x3x1 ----------------
# bounding x0-30, y16-20
noise(px, 0, 16, 30, 20, FEATHER, FEATHER_D, 0.35)
for x in range(26, 30):                  # violet feather tips
    for y in range(16, 20):
        px[x, y] = FEATHER_TIP
for x in range(0, 30, 2):                # quill lines
    px[x, 18] = FEATHER_D

# ---------------- TENDRILS: texOffs(40,16), box 1x7x1 ----------------
fill(px, 40, 16, 44, 24, ABYSS)
for y in range(16, 24):
    px[41, y] = CRACK if y % 2 == 0 else ABYSS_D
    if y % 2 == 0:
        gx[41, y] = CRACK_GLOW

# ---------------- MOUND: texOffs(0,24), box 12x8x8, d=8 ----------------
# bounding x0-40, y24-40; player-facing (-z) face x28-40, y32-40
noise(px, 0, 24, 40, 40, ABYSS, ABYSS_D, 0.4)
# glowing cracks crawling over the player-facing side of the mound
for (cx, cy) in [(29, 33), (30, 34), (29, 35), (31, 35), (32, 36), (30, 37), (33, 37),
                 (32, 38), (35, 34), (36, 35), (37, 34), (38, 36), (35, 38), (39, 38)]:
    px[cx, cy] = CRACK
    gx[cx, cy] = CRACK_GLOW
# and a few on the far side
for (cx, cy) in [(9, 33), (10, 34), (11, 35), (10, 36)]:
    px[cx, cy] = CRACK
    gx[cx, cy] = CRACK_GLOW

out = os.path.join(os.path.dirname(__file__), "src/main/resources/assets/rumblefruit/textures/entity")
img.save(os.path.join(out, "exorcist.png"))
glow.save(os.path.join(out, "exorcist_glow.png"))
print("written")
