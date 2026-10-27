"""Slice the 5x4 expression sheet into 20 transparent PNGs (512x512, centred).
The source sheet is RGB (checkerboard baked in), so alpha is rebuilt:
 * ink mask -> feature groups; each group is assigned to a cell by its centroid
 * sclera (eye white) is recovered only for eyes that have a blue iris:
   convex hull of the thick lid/iris pixels inside a box around that iris
 * outer edge: alpha from darkness vs white + white-fringe removal"""
import sys, os
import numpy as np
from PIL import Image
from scipy import ndimage as ndi
import cv2

src, outdir = sys.argv[1], sys.argv[2]
os.makedirs(outdir, exist_ok=True)
im = np.array(Image.open(src).convert("RGB"))
H, W = im.shape[:2]
COLS, ROWS = 5, 4
cw, rh = W / COLS, H / ROWS
ell = lambda n: cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (n, n))

R_, G_, B_ = [im[..., i].astype(int) for i in range(3)]
mn = im.min(axis=2).astype(int); mx = im.max(axis=2).astype(int); sat = mx - mn
ink = (mn < 205) | (sat > 40)
lab, n = ndi.label(ink)
sizes = ndi.sum(ink, lab, range(1, n + 1))
ink = np.isin(lab, [i + 1 for i, s in enumerate(sizes) if s >= 60])

grouped, ng = ndi.label(cv2.dilate(ink.astype(np.uint8), ell(11)) > 0)
group_of = np.where(ink, grouped, 0)

# --- iris blobs (mid-blue, bright enough to exclude navy sunglasses lenses)
blue = (B_ - R_ > 60) & (B_ > 110) & (sat > 70) & (mx > 110)
bl, bn = ndi.label(cv2.morphologyEx(blue.astype(np.uint8), cv2.MORPH_CLOSE, ell(5)) > 0)
irises = []
for i in range(1, bn + 1):
    ys, xs = np.where(bl == i)
    if not (500 <= len(ys) <= 7000): continue
    w, h = xs.max()-xs.min()+1, ys.max()-ys.min()+1
    if max(w, h) > 2.2 * min(w, h): continue  # far from round -> not an iris (lid may clip it)
    irises.append((xs.mean(), ys.mean(), max(w, h) / 2.0))

thick_all = cv2.morphologyEx(ink.astype(np.uint8), cv2.MORPH_OPEN, ell(9)) > 0
solid = ndi.binary_fill_holes(ink) | ink
region_extra = np.zeros(ink.shape, bool)   # hull areas (belong to the iris' group)
hull_group = {}
iris_groups = {}   # gid -> [(cx, cy, R)] in sheet coordinates (used for blink metadata)
for (cx, cy, R) in irises:
    x0, x1 = int(cx - 2.4*R), int(cx + 2.4*R)
    y0, y1 = int(cy - 1.5*R), int(cy + 1.6*R)
    x0, y0 = max(x0, 0), max(y0, 0); x1, y1 = min(x1, W), min(y1, H)
    # dominant feature-group around the iris (the centre pixel may be a white highlight -> group 0)
    patch = group_of[max(int(cy-R),0):int(cy+R)+1, max(int(cx-R),0):int(cx+R)+1].ravel()
    patch = patch[patch > 0]
    gid = int(np.bincount(patch).argmax()) if len(patch) else 0
    box = np.zeros(ink.shape, bool); box[y0:y1, x0:x1] = True
    same = (group_of == gid) if gid else np.ones(ink.shape, bool)
    # thick strokes anywhere in the box + thin outline pixels below the lash line
    low = np.zeros(ink.shape, bool); low[max(int(cy - 1.15*R), 0):y1, x0:x1] = True
    pts_m = (thick_all & box & same) | (ink & low & same)
    ty, tx = np.where(pts_m)
    if len(ty) < 100: continue
    hull = cv2.convexHull(np.stack([tx, ty], 1).astype(np.int32))
    hm = np.zeros(ink.shape, np.uint8); cv2.fillConvexPoly(hm, hull, 1)
    iris_groups.setdefault(gid, []).append((cx, cy, R))
    hmask = hm > 0
    # follow the true (curved) lower rim: near-white pixels that carry a slight tint
    # (the baked-in checkerboard is perfectly neutral R=G=B, the sclera is not)
    zone = cv2.dilate(hm, ell(int(0.9 * R) | 1)) > 0
    cand = (mn >= 200) & (sat >= 2) & zone & ~ink
    ext = cv2.morphologyEx(cand.astype(np.uint8), cv2.MORPH_CLOSE, ell(9)) > 0
    ext = ndi.binary_fill_holes(ext | hmask) & zone
    # keep only the part connected to the hull so stray tinted specks are ignored
    lab_e, _ = ndi.label(ext)
    hit = np.unique(lab_e[hmask & ext]); hit = hit[hit > 0]
    ext = np.isin(lab_e, hit) | hmask
    zone2 = cv2.dilate(hm, ell(int(1.1 * R) | 1)) > 0
    ext = (ndi.binary_fill_holes(cv2.morphologyEx(ext.astype(np.uint8), cv2.MORPH_CLOSE, ell(21)) > 0)) & zone2
    ext |= hmask
    solid |= ext; region_extra |= ext
    hull_group.setdefault(gid, []).append(ext)

# --- alpha (global)
interior = ndi.binary_erosion(solid, iterations=3)
outside = ~ndi.binary_dilation(solid, iterations=2)
band = ~interior & ~outside
soft = np.clip((255 - mn - 12) / 140.0, 0, 1)
alpha = np.where(interior, 1.0, np.where(band, soft, 0.0))
sclera_edge = band & solid & (mn >= 205) & (sat <= 40)
alpha = np.where(sclera_edge, cv2.GaussianBlur(solid.astype(np.float32), (0, 0), 0.9), alpha)
alpha = np.clip(alpha, 0, 1).astype(np.float32)
rgb = im.astype(np.float32)
a3 = np.maximum(alpha, 1e-3)[..., None]
partial = (alpha < 0.98) & (alpha > 0) & ~sclera_edge
rgb = np.where(partial[..., None], np.clip((rgb - 255.0*(1-a3)) / a3, 0, 255), rgb)
rgba_full = np.dstack([rgb.astype(np.uint8), (alpha * 255).astype(np.uint8)])

# --- assign groups to cells by centroid
cell_groups = {}
for g in range(1, ng + 1):
    gm = group_of == g
    if gm.sum() < 60: continue
    ys, xs = np.where(gm)
    c = min(int(xs.mean() // cw), COLS-1); r = min(int(ys.mean() // rh), ROWS-1)
    cell_groups.setdefault(r*COLS + c, []).append(g)

report = []
meta = {}
for k in range(COLS * ROWS):
    region = np.zeros(ink.shape, bool)
    for g in cell_groups.get(k, []):
        region |= cv2.dilate((group_of == g).astype(np.uint8), ell(7)) > 0
        for hm in hull_group.get(g, []): region |= hm
    a = np.where(region, rgba_full[..., 3], 0).astype(np.uint8)
    out = rgba_full.copy(); out[..., 3] = a
    ys, xs = np.where(a > 12)
    pad = 8
    y0, y1 = max(ys.min()-pad, 0), min(ys.max()+pad, H)
    x0, x1 = max(xs.min()-pad, 0), min(xs.max()+pad, W)
    crop = out[y0:y1, x0:x1]
    h, w = crop.shape[:2]
    s = 480 / max(h, w)
    ci = Image.fromarray(crop, "RGBA").resize((max(1, int(w*s)), max(1, int(h*s))), Image.LANCZOS)
    canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    canvas.paste(ci, ((512-ci.width)//2, (512-ci.height)//2), ci)
    canvas.save(os.path.join(outdir, f"face_{k+1:02d}.png"), optimize=True)
    offx, offy = (512 - ci.width) // 2, (512 - ci.height) // 2
    irs = [i for g in cell_groups.get(k, []) for i in iris_groups.get(g, [])]
    # only round, eye-sized irises (skip e.g. tear drops): radius ~20-30px in sheet coordinates
    irs = [i for i in irs if 18 <= i[2] <= 32]
    if irs:
        to_c = lambda cx, cy: (((cx - x0) * s + offx) / 512.0, ((cy - y0) * s + offy) / 512.0)
        eye_cy = float(np.mean([to_c(i[0], i[1])[1] for i in irs]))
        eye_bottom = float(max(to_c(i[0], i[1] + 1.55 * i[2])[1] for i in irs))
        meta[f"face_{k+1:02d}"] = {"eyeCenterY": round(eye_cy, 3), "splitY": round(min(eye_bottom + 0.02, 0.95), 3), "eyes": len(irs)}
    else:
        meta[f"face_{k+1:02d}"] = {"eyeCenterY": None, "splitY": None, "eyes": 0}
    report.append((k+1, f"{w}x{h}", f"groups={len(cell_groups.get(k, []))}"))
print("irises found:", len(irises)); print(report)

import json
json.dump(meta, open(os.path.join(os.path.dirname(os.path.abspath(outdir)), 'face_meta.json'), 'w'), indent=1)
for k_, v_ in meta.items(): print(k_, v_)
