#!/usr/bin/env python3
"""Поблочная проверка древнего мира: города, стены, ворота, дороги, дамбы."""
from rcon import Rcon

r = Rcon()
r.login()

def q(cmd):
    return r.cmd(cmd).strip()

def is_block(x, y, z, blk):
    return q(f"execute in rumblefruit:earth if block {x} {y} {z} minecraft:{blk}") == "Test passed"

def surface(x, z, y_from=58, y_to=320):
    for y in range(y_from, y_to):
        if is_block(x, y, z, "air"):
            return y - 1
    return None

def top_block(x, z, y_from=58, y_to=320):
    s = surface(x, z, y_from, y_to)
    if s is None:
        return None, None
    for blk in ["stone_bricks", "cracked_stone_bricks", "gravel", "grass_block",
                "sand", "sandstone", "cut_sandstone", "stone", "snow_block",
                "water", "bricks", "oak_planks", "mud_bricks", "quartz_block",
                "white_wool", "terracotta"]:
        if is_block(x, s, z, blk):
            return s, blk
    return s, "?"

def citydebug(x, z):
    return q(f"rumblefruit citydebug {x} {z}")

def roaddebug(x, z):
    return q(f"rumblefruit roaddebug {x} {z}")

results = []
def check(name, ok, detail=""):
    results.append((name, ok, detail))
    print(("PASS " if ok else "FAIL ") + name + (" — " + detail if detail else ""))

# ---- Рим (центр 3749,-12568, r=120) ----
info = citydebug(3749, -12568)
check("Рим: центр=форум", "kind=2" in info, info)

s, blk = top_block(3749, -12568)
check("Рим: пол форума каменный", blk in ("stone_bricks", "cracked_stone_bricks", "gravel"), f"y={s} {blk}")

# улица: сетка 24 от центра
street_found = None
for off in (24, 48, 72, 96):
    info = citydebug(3749 + off, -12568)
    if "kind=1" in info:
        street_found = off
        break
check("Рим: улица по сетке 24", street_found is not None, f"off={street_found}")

# стена по +x
wall = None
for d in range(100, 145):
    info = citydebug(3749 + d, -12568)
    if "kind=3" in info:
        wall = d
        break
check("Рим: кольцо стены", wall is not None, f"d={wall}")
if wall:
    s = surface(3749 + wall, -12568)
    above = is_block(3749 + wall, s + 1, -12568, "air") if s else True
    check("Рим: стена выше рельефа", s is not None and not above, f"верх y={s}")

# ворота: дорога на Неаполь пересекает кольцо (направление 0.86, 0.51)
gx, gz = round(3749 + 0.86 * 118), round(-12568 + 0.51 * 118)
gate_info = citydebug(gx, gz)
road_info = roaddebug(gx, gz)
check("Рим: ворота на Неаполь (дорога у стены)", "dist=" in road_info and "dist=-1" not in road_info,
      f"({gx},{gz}) {road_info}")

# дома между улицами
house = None
for dx in range(8, 100, 4):
    for dz in range(8, 100, 4):
        if "kind=4" in citydebug(3749 + dx, -12568 + dz):
            house = (dx, dz)
            break
    if house:
        break
check("Рим: жилые дома", house is not None, f"{house}")

# ---- Афины (7118,-11395) и Фивы (7002,-11496) — монументы ----
for name, x, z in [("Афины", 7118, -11395), ("Фивы", 7002, -11496)]:
    info = citydebug(x, z)
    s, blk = top_block(x, z)
    check(f"{name}: монумент в центре", "kind=" in info and "kind=0" not in info, f"{info} top={blk}")

# ---- Гиза: пирамида ----
pyr = None
for x in range(9296, 9409, 16):
    for z in range(-9040, -8913, 16):
        s = surface(x, z, 64, 200)
        if s and s > 100:
            pyr = (x, z, s)
            break
    if pyr:
        break
check("Гиза: пирамида возвышается", pyr is not None, f"{pyr}")

# ---- природа ----
s, blk = top_block(3000, -9200)
check("Сахара: песок", blk == "sand", f"y={s} {blk}")
s, blk = top_block(2060, -13750, 200, 315)
check("Альпы: снег", blk == "snow_block", f"y={s} {blk}")
check("Море: вода на y=62", is_block(3300, 62, -11250, "water"), "")

# ---- дорога Рим—Неаполь: покрытие коридора ----
road_ok = None
for off in range(-60, 61, 5):
    x = round(4015 - 0.51 * off)
    z = round(-12414 + 0.86 * off)
    info = roaddebug(x, z)
    if "dist=" in info and "dist=-1" not in info:
        d = int(info.split("dist=")[1].split()[0])
        if d <= 2:
            road_ok = (x, z, d)
            break
if road_ok:
    s, blk = top_block(road_ok[0], road_ok[1])
    check("Дорога Рим—Неаполь: каменное полотно",
          blk in ("stone_bricks", "cracked_stone_bricks", "gravel"), f"{road_ok} y={s} {blk}")
else:
    check("Дорога Рим—Неаполь: каменное полотно", False, "коридор не найден")

# ---- дамба Александрия—Каир через лагуну дельты ----
# середина (9173,-9187); ищем коридор перпендикулярно
dam_ok = None
for off in range(-60, 61, 5):
    x = round(9173 - 0.75 * off)   # перпендикуляр к (0.66,0.75)
    z = round(-9187 + 0.66 * off)
    info = roaddebug(x, z)
    if "dist=" in info and "dist=-1" not in info:
        d = int(info.split("dist=")[1].split()[0])
        if d <= 2:
            dam_ok = (x, z)
            break
if dam_ok:
    s = surface(dam_ok[0], dam_ok[1])
    blk_ok = is_block(dam_ok[0], s, dam_ok[1], "stone_bricks") or \
             is_block(dam_ok[0], s, dam_ok[1], "gravel") or \
             is_block(dam_ok[0], s, dam_ok[1], "stone")
    check("Дамба Александрия—Каир над лагуной", blk_ok and 63 <= s <= 66,
          f"{dam_ok} y={s}")
else:
    check("Дамба Александрия—Каир над лагуной", False, "коридор не найден")

print()
fails = [n for n, ok, _ in results if not ok]
print(f"ИТОГ: {len(results) - len(fails)}/{len(results)} PASS")
if fails:
    print("FAIL:", ", ".join(fails))
