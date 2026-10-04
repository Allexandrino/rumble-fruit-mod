#!/usr/bin/env python3
"""Пакетный замер высот древнего мира через одну RCON-сессию."""
from rcon import Rcon

r = Rcon()
r.login()

def q(cmd):
    return r.cmd(cmd)

def find_surface(x, z, y_from, y_to):
    """Первый воздух снизу вверх → поверхность = y-1."""
    for y in range(y_from, y_to):
        if q(f"execute in rumblefruit:earth if block {x} {y} {z} minecraft:air") == "Test passed":
            return y - 1
    return None

spots = [
    ("Рим", 3749, -12568, 58, 90),
    ("Афины", 7118, -11395, 58, 100),
    ("Монблан", 2060, -13750, 250, 315),
    ("Олимп", 6708, -12026, 150, 260),
    ("Атлас", -2375, -9318, 150, 260),
    ("Сахара", 3000, -9200, 58, 110),
]
for name, x, z, a, b in spots:
    s = find_surface(x, z, a, b)
    if s is None:
        print(f"{name}: воздух не найден в диапазоне {a}..{b}")
        continue
    top = "?"
    for blk in ["grass_block", "sand", "stone", "snow_block", "gravel", "water"]:
        if q(f"execute in rumblefruit:earth if block {x} {s} {z} {blk}") == "Test passed":
            top = blk
            break
    print(f"{name}: поверхность y={s}, блок={top}")

# море между Сицилией и Карфагеном (lon 11, lat 37.5)
print("море y=62:", q("execute in rumblefruit:earth if block 3300 62 -11250 minecraft:water"))
print("море y=45 (дно? воздух нет):", q("execute in rumblefruit:earth if block 3300 45 -11250 minecraft:water"))
print("дно y=40:", q("execute in rumblefruit:earth if block 3300 40 -11250 minecraft:sand"))
