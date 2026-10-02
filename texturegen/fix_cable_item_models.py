# -*- coding: utf-8 -*-
"""把 1.12.2 / modern 的电缆物品模型改成扁平 layer0（IU 风格图标）。"""
import json
import os

DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}

CABLES = [
    "glass_cable", "glass_cable_2", "ultimate_hybrid_glass_cable", "glass_cable_4",
    "glass_cable_5", "glass_cable_6", "glass_cable_7", "glass_cable_8",
    "glass_cable_9", "glass_cable_10", "glass_cable_11",
]

ROOTS = [
    r"G:\solarpower",
    r"G:\solarpower\forge-1.12.2",
]

for root in ROOTS:
    item_dir = os.path.join(root, "src", "main", "resources", "assets", "solarpower", "models", "item")
    os.makedirs(item_dir, exist_ok=True)
    for tid in CABLES:
        data = {
            "parent": "builtin/generated",
            "textures": {
                "layer0": "solarpower:item/" + tid
            },
            "display": DISPLAY,
        }
        path = os.path.join(item_dir, tid + ".json")
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print("item models updated in", root)
