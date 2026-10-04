#!/usr/bin/env python3
"""Читаем heightmap чанков из .mca (anvil) — земная правда о генерации."""
import struct, sys, zlib

def read_chunk(path, cx, cz):
    with open(path, 'rb') as f:
        data = f.read()
    lx, lz = cx & 31, cz & 31
    off = (lx + lz * 32) * 4
    loc = struct.unpack('>I', data[off:off+4])[0]
    if loc == 0:
        return None
    sector = loc >> 8
    pos = sector * 4096
    length = struct.unpack('>I', data[pos:pos+4])[0]
    comp = data[pos+5:pos+4+length]
    return zlib.decompress(comp)

class Nbt:
    def __init__(self, b):
        self.b = b; self.i = 0
    def u1(self): v = self.b[self.i]; self.i += 1; return v
    def i2(self): v = struct.unpack('>h', self.b[self.i:self.i+2])[0]; self.i += 2; return v
    def i4(self): v = struct.unpack('>i', self.b[self.i:self.i+4])[0]; self.i += 4; return v
    def i8(self): v = struct.unpack('>q', self.b[self.i:self.i+8])[0]; self.i += 8; return v
    def f8(self): v = struct.unpack('>d', self.b[self.i:self.i+8])[0]; self.i += 8; return v
    def name(self):
        n = struct.unpack('>H', self.b[self.i:self.i+2])[0]; self.i += 2
        s = self.b[self.i:self.i+n].decode(); self.i += n; return s
    def skip(self, t):
        if t == 0: return
        if t == 1: self.i += 1
        elif t == 2: self.i += 2
        elif t == 3: self.i += 4
        elif t == 4: self.i += 8
        elif t == 5: self.i += 4
        elif t == 6: self.i += 8
        elif t == 7:
            n = self.i4(); self.i += n
        elif t == 8:
            n = self.u1() << 8 | self.u1(); self.i += n - 2
        elif t == 9:
            et = self.u1(); n = self.i4()
            for _ in range(n): self.skip(et)
        elif t == 10:
            while True:
                et = self.u1()
                if et == 0: break
                self.name(); self.skip(et)
        elif t == 11:
            n = self.i4(); self.i += 4 * n
        elif t == 12:
            n = self.i4(); self.i += 8 * n
    def read_long_array(self):
        """Ищем тег типа 12 с заданным именем на верхнем уровне рекурсивно."""
        raise NotImplementedError

def find_heightmap(b, key):
    """Ищем long-array с именем тега 'MOTION_BLOCKING': 16x16 высот по 9 бит = 36 long."""
    needle = key.encode()
    idx = 0
    while True:
        idx = b.find(needle, idx)
        if idx < 0:
            return None
        p = idx + len(needle)
        n = struct.unpack('>i', b[p:p+4])[0]
        if 30 <= n <= 40 and p + 4 + 8 * n <= len(b):
            return struct.unpack('>%dq' % n, b[p+4:p+4+8*n])
        idx += 1

def unpack_heightmap(arr):
    """Высоты 16x16, по 9 бит на значение (256..) в 1.21: ceil((384+1)/64)... стандартно 9 бит."""
    vals = []
    bits = 9
    per_long = 64 // bits  # 7
    mask = (1 << bits) - 1
    for l in arr:
        l &= (1 << 64) - 1
        for k in range(per_long):
            vals.append((l >> (k * bits)) & mask)
    return vals[:256]

def chunk_surface(path, cx, cz):
    raw = read_chunk(path, cx, cz)
    if raw is None:
        return None
    arr = find_heightmap(raw, 'MOTION_BLOCKING')
    if arr is None:
        return None
    vals = unpack_heightmap(arr)
    # высоты хранятся со смещением minY? В 1.18+ значения абсолютные (y+1), minY=-64 кодируется отрицательно?
    return vals

spots = [
    ('Рим', 3749, -12568),
    ('Афины', 7118, -11395),
    ('Монблан', 2060, -13750),
    ('Олимп', 6708, -12026),
    ('Атлас', -2375, -9318),
    ('Сахара', 3000, -9200),
]
import os
base = 'run/world/dimensions/rumblefruit/earth/region'
for name, x, z in spots:
    cx, cz = x >> 4, z >> 4
    rx, rz = cx >> 5, cz >> 5
    path = f'{base}/r.{rx}.{rz}.mca'
    if not os.path.exists(path):
        print(f'{name}: региона {path} нет')
        continue
    vals = chunk_surface(path, cx, cz)
    if vals is None:
        print(f'{name}: чанк не сохранён')
        continue
    lx, lz = x & 15, z & 15
    # heightmap хранит (y - minY + 1), minY = -64 → y = val - 65
    h = vals[lz * 16 + lx] - 65
    print(f'{name}: поверхность y ≈ {h}')
