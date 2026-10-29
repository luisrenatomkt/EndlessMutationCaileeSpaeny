#!/usr/bin/env python3
"""Gera as texturas do mod (originais, desenhadas por codigo): entidade 64x64 e icone 16x16.
Os retangulos UV abaixo DEVEM coincidir com ModelHuntress.java. Execute: python3 tools/generate_textures.py"""
import random, os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "endlessmutation", "textures")
SKIN=(236,196,176,255); SKIN_D=(222,178,158,255); HAIR=(98,63,42,255); HAIR_D=(78,48,32,255); HAIR_L=(122,82,56,255)
PINK=(233,30,120,255); PINK_D=(200,20,100,255); PINK_L=(246,160,200,255); PINK_LL=(250,190,215,255)
EYE_W=(245,245,245,255); EYE_I=(120,150,175,255); EYE_P=(30,30,40,255); LIP=(205,105,120,255); BLUSH=(232,160,160,255)
SHOE=(214,170,150,255); BONE=(236,230,214,255); BONE_D=(200,192,170,255); CLEAR=(0,0,0,0)

img = Image.new("RGBA", (64, 64), CLEAR)
px = img.load()
rects = []

def faces(u, v, w, h, d):
    return {"top": (u+d, v, w, d), "bottom": (u+d+w, v, w, d), "right": (u, v+d, d, h),
            "front": (u+d, v+d, w, h), "left": (u+d+w, v+d, d, h), "back": (u+2*d+w, v+d, w, h)}

def claim(name, u, v, w, h, d):
    box = (u, v, u+2*(d+w), v+d+h)
    for n, b in rects:
        if box[0] < b[2] and b[0] < box[2] and box[1] < b[3] and b[1] < box[3]:
            raise SystemExit("Sobreposicao de UV: %s x %s" % (name, n))
    if box[2] > 64 or box[3] > 64: raise SystemExit("UV fora da textura: " + name)
    rects.append((name, box))
    return faces(u, v, w, h, d)

def fill(rect, color, jitter=0, rnd=None):
    x, y, w, h = rect
    for j in range(h):
        for i in range(w):
            c = color
            if jitter and rnd:
                k = rnd.randint(-jitter, jitter); c = tuple(max(0, min(255, c[n]+k)) for n in range(3)) + (c[3],)
            px[x+i, y+j] = c

def put(rect, i, j, c):
    x, y, w, h = rect
    if 0 <= i < w and 0 <= j < h: px[x+i, y+j] = c

rnd = random.Random(7)

# ---------------- cabeca (0,0) 8x8x8
F = claim("head", 0, 0, 8, 8, 8)
for k in F: fill(F[k], SKIN)
fill(F["top"], HAIR, 10, rnd); fill(F["back"], HAIR, 10, rnd); fill(F["bottom"], SKIN_D)
for j in range(0, 4):
    for i in range(8): put(F["right"], i, j, HAIR); put(F["left"], i, j, HAIR)
fr = F["front"]
for i in range(8):                       # franja
    for j in range(2): put(fr, i, j, HAIR)
for i in (0, 1, 6, 7):
    for j in range(2, 5): put(fr, i, j, HAIR)
for (ex) in (1, 5):                      # olhos
    put(fr, ex+0, 4, EYE_W); put(fr, ex+1, 4, EYE_I); put(fr, ex+2, 4, EYE_P if False else EYE_W)
    put(fr, ex+1, 4, EYE_I); put(fr, ex, 3, HAIR_D); put(fr, ex+1, 3, HAIR_D); put(fr, ex+2, 3, HAIR_D)
put(fr, 2, 4, EYE_P); put(fr, 6, 4, EYE_P)
put(fr, 3, 5, SKIN_D); put(fr, 4, 5, SKIN_D)                     # nariz
put(fr, 3, 6, LIP); put(fr, 4, 6, LIP)                           # labios
put(fr, 1, 5, BLUSH); put(fr, 6, 5, BLUSH)

# ---------------- cabelo (camada sobreposta, 32,0)
H = claim("hair", 32, 0, 8, 8, 8)
fill(H["top"], HAIR, 12, rnd); fill(H["back"], HAIR, 12, rnd)
for side in ("right", "left"):
    r = H[side]
    for j in range(8):
        for i in range(8):
            front_edge = (i == 7) if side == "right" else (i == 0)
            if j <= 4 or (j > 4 and not front_edge and abs((i if side == "right" else 7-i) - 0) <= 5):
                put(r, i, j, tuple(max(0, min(255, HAIR[n]+rnd.randint(-10, 10))) for n in range(3))+(255,))
r = H["front"]
for i in range(8):
    for j in range(3): put(r, i, j, HAIR_L if (i+j) % 3 == 0 else HAIR)
for i in (0, 1, 6, 7):
    for j in range(3, 8): put(r, i, j, HAIR)
put(r, 3, 2, CLEAR); put(r, 4, 2, CLEAR)                         # repartido da franja

# ---------------- cabelo longo (24,16) 8x12x2
L = claim("hairLong", 24, 16, 8, 12, 2)
for k in L: fill(L[k], HAIR, 14, rnd)
for j in range(12):
    for i in range(0, 8, 2): put(L["back"], i, j, HAIR_L if (j+i) % 5 == 0 else HAIR_D)

# ---------------- pescoco (52,46)
N = claim("neck", 52, 46, 3, 3, 3)
for k in N: fill(N[k], SKIN)

# ---------------- torso (0,16) 8x12x4 - vestido tomara-que-caia
B = claim("body", 0, 16, 8, 12, 4)
fill(B["top"], SKIN); fill(B["bottom"], PINK_D)
for k in ("front", "back", "right", "left"):
    fill(B[k], PINK, 6, rnd)
    for i in range(B[k][2]):
        for j in range(2): put(B[k], i, j, SKIN)
for i in range(8): put(B["front"], i, 2, PINK_L)                # barra do decote
for j in range(3, 12): put(B["front"], 3, j, PINK_D); put(B["front"], 4, j, PINK_D)  # costura central
for i in range(8): put(B["back"], i, 2, PINK_L)
for i in (3, 4):
    for j in range(3, 7): put(B["back"], i, j, PINK_LL)         # laco nas costas

# peito (32,56) 7x4x3 -> tecido do vestido
P = claim("pec", 32, 56, 7, 4, 3)
for k in P: fill(P[k], PINK, 6, rnd)
# costas/dorsal (0,56) 3x6x2 -> pele
Lt = claim("lat", 0, 56, 3, 6, 2)
for k in Lt: fill(Lt[k], SKIN_D)
# espinho (24,56) 2x4x2 -> osso
Sp = claim("spike", 24, 56, 2, 4, 2)
for k in Sp:
    x, y, w, h = Sp[k]
    for j in range(h):
        for i in range(w): px[x+i, y+j] = BONE if j < h-1 else BONE_D
# trapezio (44,36) 6x3x4, ombro (24,36) 5x5x5 -> pele
T = claim("trap", 44, 36, 6, 3, 4)
for k in T: fill(T[k], SKIN)
Sh = claim("shoulder", 24, 36, 5, 5, 5)
for k in Sh: fill(Sh[k], SKIN)

# saia (0,32) 8x8x4 -> tule rosa claro
Sk = claim("skirt", 0, 32, 8, 8, 4)
for k in Sk: fill(Sk[k], PINK_L, 8, rnd)
for k in ("front", "back", "right", "left"):
    for i in range(Sk[k][2]): put(Sk[k], i, 7, PINK_LL)

# bracos (pele)
UA = claim("upperArm", 44, 16, 4, 6, 4)
for k in UA: fill(UA[k], SKIN)
FA = claim("foreArm", 44, 26, 4, 5, 4)
for k in FA: fill(FA[k], SKIN)
Hd = claim("hand", 24, 30, 3, 3, 3)
for k in Hd: fill(Hd[k], SKIN)
for k in ("front", "back", "right", "left"):
    put(Hd[k], 0, 2, PINK_LL); put(Hd[k], 1, 2, PINK_LL); put(Hd[k], 2, 2, PINK_LL)   # unhas

# pernas: coxa vestido, panturrilha tule + pele, pe sapato
Th = claim("thigh", 0, 44, 4, 6, 4)
for k in Th: fill(Th[k], PINK, 6, rnd)
Ca = claim("calf", 16, 46, 4, 6, 4)
for k in Ca:
    fill(Ca[k], PINK_L, 6, rnd)
    for i in range(Ca[k][2]):
        for j in range(4, Ca[k][3]): put(Ca[k], i, j, SKIN)
Ft = claim("foot", 32, 46, 4, 2, 5)
for k in Ft: fill(Ft[k], SHOE)

out = os.path.join(ROOT, "entity"); os.makedirs(out, exist_ok=True)
img.save(os.path.join(out, "huntress.png"))

# ---------------- icone do amuleto 16x16
ic = Image.new("RGBA", (16, 16), CLEAR); ip = ic.load()
cx, cy = 7.5, 8.5
for y in range(16):
    for x in range(16):
        d = ((x-cx)**2 + (y-cy)**2) ** 0.5
        if d <= 5.2: ip[x, y] = PINK if d > 3.2 else PINK_L
        if d <= 1.6: ip[x, y] = (255, 255, 255, 255)
        if 5.2 < d <= 6.2: ip[x, y] = (80, 20, 50, 255)
for y in range(0, 4): ip[7, y] = (150, 150, 150, 255); ip[8, y] = (190, 190, 190, 255)
os.makedirs(os.path.join(ROOT, "items"), exist_ok=True)
ic.save(os.path.join(ROOT, "items", "summoner.png"))
print("OK: texturas geradas; %d regioes UV sem sobreposicao" % len(rects))
