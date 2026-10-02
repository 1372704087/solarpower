# -*- coding: utf-8 -*-
"""1.12.2 资源加固：真实模型 + 双路径 shim，兼容 block/block 误拼。"""
import json
import os

ROOT = r"G:\solarpower\forge-1.12.2\src\main\resources\assets\solarpower"
BS = os.path.join(ROOT, "blockstates")
MODELS = os.path.join(ROOT, "models")
ITEM = os.path.join(MODELS, "item")
BLOCK = os.path.join(MODELS, "block")
BLOCK_BLOCK = os.path.join(MODELS, "block", "block")
FLAT = MODELS  # models/<name>.json

PANELS = [
    "solar_panel", "advanced_solar_panel", "hybrid_solar_panel", "perfect_solar_panel",
    "quantum_solar_panel", "spectral_solar_panel", "proton_solar_panel", "singular_solar_panel",
    "diffraction_solar_panel", "photonic_solar_panel", "neutron_solar_panel", "baryon_solar_panel",
    "hadron_solar_panel", "graviton_solar_panel", "quark_solar_panel", "fusion_solar_panel",
    "antimatter_solar_panel", "zero_point_solar_panel", "dark_energy_solar_panel", "cosmic_string_solar_panel",
    "hyperdimensional_solar_panel", "omniversal_solar_panel", "void_solar_panel", "singularity_solar_panel",
    "star_core_solar_panel", "genesis_solar_panel", "vacuum_decay_solar_panel", "superstring_solar_panel",
    "brane_solar_panel", "heat_death_solar_panel", "zeroing_solar_panel", "paradox_solar_panel",
    "eternity_solar_panel", "transcendent_solar_panel", "myriad_solar_panel", "demiurge_solar_panel",
    "apotheosis_solar_panel", "boundless_solar_panel", "absolute_infinity_solar_panel", "abyss_solar_panel",
    "annihilation_solar_panel", "chaos_solar_panel", "order_solar_panel", "sands_of_time_solar_panel",
    "void_rift_solar_panel", "arbiter_solar_panel", "primordial_solar_panel", "final_law_solar_panel",
    "convergence_solar_panel", "supreme_one_solar_panel", "godhead_solar_panel", "negentropy_solar_panel",
    "endgame_solar_panel", "the_absolute_solar_panel", "supreme_void_solar_panel", "awakening_solar_panel",
    "creator_solar_panel", "star_sea_solar_panel", "supreme_principle_solar_panel", "finale_solar_panel", "finality_solar_panel", "divine_light_solar_panel", "firmament_solar_panel", "pantheon_solar_panel", "mandate_of_heaven_solar_panel", "apocalypse_solar_panel", "heavens_end_solar_panel",
]
CABLES = [
    "glass_cable", "glass_cable_2", "ultimate_hybrid_glass_cable", "glass_cable_4",
    "glass_cable_5", "glass_cable_6", "glass_cable_7", "glass_cable_8",
    "glass_cable_9", "glass_cable_10", "glass_cable_11", "glass_cable_12",
    "glass_cable_13", "glass_cable_14", "glass_cable_15", "glass_cable_16",
    "glass_cable_17", "glass_cable_18", "glass_cable_19", "glass_cable_20",
    "glass_cable_21", "glass_cable_22", "glass_cable_23", "glass_cable_24", "glass_cable_25", "glass_cable_26", "glass_cable_27", "glass_cable_28", "glass_cable_29", "glass_cable_30", "glass_cable_31", "glass_cable_32",
]
DIRECTIONS = ["north", "south", "east", "west", "up", "down"]

DEFAULT_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def shim(parent):
    return {"parent": parent}


def load_display(path):
    if not os.path.exists(path):
        return dict(DEFAULT_DISPLAY)
    try:
        d = json.load(open(path, encoding="utf-8"))
    except Exception:
        return dict(DEFAULT_DISPLAY)
    return d.get("display", dict(DEFAULT_DISPLAY))


for pid in PANELS:
    # blockstate：标准 1.12.2 写法 + normal 变体
    write_json(os.path.join(BS, pid + ".json"), {
        "variants": {
            "": {"model": "solarpower:block/" + pid},
            "normal": {"model": "solarpower:block/" + pid},
        }
    })
    # 真实方块模型：原版 cube parent
    write_json(os.path.join(BLOCK, pid + ".json"), {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": "solarpower:block/" + pid + "_top",
            "down": "solarpower:block/solar_panel_bottom",
            "up": "solarpower:block/" + pid + "_top",
            "north": "solarpower:block/" + pid + "_side",
            "south": "solarpower:block/" + pid + "_side",
            "east": "solarpower:block/" + pid + "_side",
            "west": "solarpower:block/" + pid + "_side",
        },
    })
    # 双路径 shim：兼容被拼成 models/block/block/ 或 models/
    write_json(os.path.join(BLOCK_BLOCK, pid + ".json"),
               shim("solarpower:block/" + pid))
    write_json(os.path.join(FLAT, pid + ".json"),
               shim("solarpower:block/" + pid))
    write_json(os.path.join(ITEM, pid + ".json"), {
        "parent": "solarpower:block/" + pid,
        "display": load_display(os.path.join(ITEM, pid + ".json")),
    })

for cid in CABLES:
    write_json(os.path.join(BS, cid + ".json"), {
        "variants": {
            "": {"model": "solarpower:block/" + cid + "_core"},
            "normal": {"model": "solarpower:block/" + cid + "_core"},
        }
    })
    tex = "solarpower:block/" + cid
    write_json(os.path.join(BLOCK, cid + "_core.json"), {
        "textures": {"particle": tex, "tex": tex},
        "elements": [{
            "from": [6, 6, 6], "to": [10, 10, 10],
            "faces": {d: {"texture": "#tex"} for d in DIRECTIONS},
        }],
        "display": DEFAULT_DISPLAY,
    })
    arms = {
        "north": ([6, 6, 0], [10, 10, 6]),
        "south": ([6, 6, 10], [10, 10, 16]),
        "east": ([10, 6, 6], [16, 10, 10]),
        "west": ([0, 6, 6], [6, 10, 10]),
        "up": ([6, 10, 6], [10, 16, 10]),
        "down": ([6, 0, 6], [10, 6, 10]),
    }
    for d, (frm, to) in arms.items():
        write_json(os.path.join(BLOCK, cid + "_arm_" + d + ".json"), {
            "textures": {"particle": tex, "tex": tex},
            "elements": [{
                "from": frm, "to": to,
                "faces": {x: {"texture": "#tex"} for x in DIRECTIONS},
            }],
            "display": DEFAULT_DISPLAY,
        })
        write_json(os.path.join(BLOCK_BLOCK, cid + "_arm_" + d + ".json"),
                   shim("solarpower:block/" + cid + "_arm_" + d))
    # core 双路径
    write_json(os.path.join(BLOCK_BLOCK, cid + "_core.json"),
               shim("solarpower:block/" + cid + "_core"))
    write_json(os.path.join(FLAT, cid + "_core.json"),
               shim("solarpower:block/" + cid + "_core"))
    # 也给不带 _core 的名字做 shim（部分加载器会丢后缀）
    write_json(os.path.join(BLOCK_BLOCK, cid + ".json"),
               shim("solarpower:block/" + cid + "_core"))
    write_json(os.path.join(FLAT, cid + ".json"),
               shim("solarpower:block/" + cid + "_core"))
    # 物品模型：现有文件是 builtin/generated + item 贴图（textures/item/*.png），
    # 不能覆盖成方块模型；只给缺失的档位生成兜底。
    item_path = os.path.join(ITEM, cid + ".json")
    if not os.path.exists(item_path):
        write_json(item_path, {
            "parent": "builtin/generated",
            "textures": {"layer0": "solarpower:item/" + cid},
        })

print("panels", len(PANELS), "cables", len(CABLES))
