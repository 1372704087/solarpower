# -*- coding: utf-8 -*-
# Patch solar-sim.html: drop embedded base64 art, load textures from the asset
# directory at runtime (fetch under a static server, or a directory picker on
# Chromium when opened from file://).
import re, io, sys

path = r'G:\solarpower\solar-sim.html'
html = io.open(path, encoding='utf-8').read()
orig_len = len(html)

zh2tex = {
    '基础太阳能': 'solar_panel', '高级太阳能': 'advanced_solar_panel',
    '融合太阳能': 'hybrid_solar_panel', '完美融合太阳能': 'perfect_solar_panel',
    '量子太阳能': 'quantum_solar_panel', '光谱太阳能': 'spectral_solar_panel',
    '质子太阳能': 'proton_solar_panel', '奇异太阳能': 'singular_solar_panel',
    '衍射太阳能': 'diffraction_solar_panel', '光子太阳能': 'photonic_solar_panel',
    '中子太阳能': 'neutron_solar_panel', '重子太阳能': 'baryon_solar_panel',
    '强子太阳能': 'hadron_solar_panel', '引力太阳能': 'graviton_solar_panel',
    '夸克太阳能': 'quark_solar_panel',
}

# 1) CSS: strip the three giant base64 background-image declarations
html, n_css = re.subn(r'background-image:url\("data:image/png;base64,[A-Za-z0-9+/=]+"\);', '', html)
assert n_css == 3, n_css

# 2) cubeTop img: drop embedded src
html, n_img = re.subn(r'<img class="f top" id="cubeTop" alt="solar panel top"\s+src="data:image/png;base64,[A-Za-z0-9+/=]+">',
                      '<img class="f top" id="cubeTop" alt="solar panel top">', html)
assert n_img == 1, n_img

# 3) TIERS array -> compact entries with texture keys
m = re.search(r'var TIERS = \[.*?\n  \];', html, re.S)
assert m, 'TIERS block not found'
entries = re.findall(r'zh: "([^"]+)", iu: (\d+), gen: (\d+), cap: (\d+)', html[m.start():m.end()])
assert len(entries) == 15, len(entries)
lines = ['  // 与 SolarTier.java 一一对应：15 档常规太阳能板（不含日光/月光机制）',
         '  // top/side 贴图按 tex 名从素材目录加载（block/<tex>_top.png / _side.png）',
         '  var TIERS = [']
for zh, iu, gen, cap in entries:
    tex = zh2tex[zh]
    lines.append('    {{ zh: "{}", iu: {}, gen: {}, cap: {}, tex: "{}" }},'.format(zh, iu, gen, cap, tex))
lines.append('  ];')
html = html[:m.start()] + '\n'.join(lines) + html[m.end():]

# 4) applyTierLook reads object URLs instead of data URIs
html = html.replace('$("cubeTop").src = cur().top;', '$("cubeTop").src = cur().topURL;')
html = html.replace('var img = "url(\\"" + cur().side + "\\")";',
                    'var img = "url(\\"" + cur().sideURL + "\\")";')

# 5) banner overlay markup (before footer)
banner = '''<div id="assetBanner" hidden>
  <div class="ab-card">
    <h3>无法读取素材目录</h3>
    <p class="ab-msg" id="abMsg"></p>
    <p>方式一：在本目录运行 <code>start-solar-sim.bat</code>（等价于 <code>py -m http.server 8765</code>），然后刷新页面，贴图会自动从素材目录加载。</p>
    <p>方式二：<button class="btn" id="abPick">选择 textures 目录</button>（Chrome / Edge，选择
    <code>src/main/resources/assets/solarpower/textures</code> 目录即可，需同时包含 <code>block</code> 与 <code>gui</code>）</p>
  </div>
</div>
'''
html = html.replace('<p class="foot">', banner + '\n<p class="foot">', 1)

# 6) banner CSS (before </style>)
banner_css = '''  /* ---------- 素材加载提示 ---------- */
  #assetBanner{position:fixed;inset:0;background:#0d1116ee;display:flex;align-items:center;justify-content:center;z-index:99}
  #assetBanner[hidden]{display:none}
  #assetBanner .ab-card{max-width:480px;background:var(--panel);border:1px solid var(--line);border-radius:12px;padding:20px 22px;font-size:13px;line-height:1.9}
  #assetBanner h3{margin:0 0 8px;font-size:15px}
  #assetBanner .ab-msg{color:var(--warn);font-size:12px;margin:0 0 6px}
  #assetBanner code{background:#00000055;border:1px solid var(--line);border-radius:5px;padding:1px 6px;font-size:11.5px}
  #assetBanner p{margin:6px 0}
'''
html = html.replace('</style>', banner_css + '</style>', 1)

# 7) asset loader + boot, replacing the old startup block
old_boot = '''  // ---------- 启动 ----------
  buildTierSelect();
  applyTierLook();
  buildRules();
  history.push(0);
  requestAnimationFrame(loop);'''
new_boot = '''  // ---------- 素材加载：直接读取目录 ----------
  var TEX_BASE = "src/main/resources/assets/solarpower/textures/";
  var guiURL = null;

  function fetchTex(path) {
    return fetch(path).then(function (res) {
      if (!res.ok) throw new Error(path + " -> HTTP " + res.status);
      return res.blob();
    }).then(function (b) { return URL.createObjectURL(b); });
  }
  function fetchAll() {
    var jobs = [fetchTex(TEX_BASE + "gui/solar_panel.png").then(function (u) { guiURL = u; })];
    TIERS.forEach(function (t) {
      jobs.push(fetchTex(TEX_BASE + "block/" + t.tex + "_top.png").then(function (u) { t.topURL = u; }));
      jobs.push(fetchTex(TEX_BASE + "block/" + t.tex + "_side.png").then(function (u) { t.sideURL = u; }));
    });
    return Promise.all(jobs);
  }
  async function pickAndLoad() {
    if (!window.showDirectoryPicker) throw new Error("当前浏览器不支持目录选择，请用 Chrome / Edge，或改用方式一。");
    var dir = await window.showDirectoryPicker({ mode: "read" });
    var block = dir, guiDir = null;
    try {
      block = await dir.getDirectoryHandle("block");
      guiDir = await dir.getDirectoryHandle("gui");
    } catch (e) { /* 可能直接选的就是 block 目录 */ }
    async function file(dirHandle, name) {
      return URL.createObjectURL(await (await dirHandle.getFileHandle(name)).getFile());
    }
    try {
      for (var t of TIERS) {
        t.topURL = await file(block, t.tex + "_top.png");
        t.sideURL = await file(block, t.tex + "_side.png");
      }
      guiURL = await file(guiDir, "solar_panel.png");
    } catch (e) {
      throw new Error("目录里找不到所需贴图：请选择包含 block/ 与 gui/ 的 textures 目录。(" + e.message + ")");
    }
  }
  function showBanner(msg) {
    $("abMsg").textContent = msg || "";
    $("assetBanner").hidden = false;
  }
  function applyGuiTexture() {
    var u = 'url("' + guiURL + '")';
    $("gui").style.backgroundImage = u;
    elBar.style.backgroundImage = u;
    elLed.style.backgroundImage = u;
  }
  function start() {
    applyGuiTexture();
    buildTierSelect();
    applyTierLook();
    buildRules();
    history.push(0);
    requestAnimationFrame(loop);
  }
  async function boot() {
    try { await fetchAll(); }
    catch (e) { showBanner("通过本地服务器加载失败：" + e.message); return; }
    start();
  }
  $("abPick").addEventListener("click", async function () {
    try { await pickAndLoad(); $("assetBanner").hidden = true; start(); }
    catch (e) { showBanner(e.message); }
  });

  boot();'''
assert old_boot in html
html = html.replace(old_boot, new_boot, 1)

# 8) header note text
html = html.replace(
    '页面使用 mod 里的真实贴图渲染；',
    '贴图直接从素材目录读取（<code>textures/block</code> 与 <code>textures/gui</code>，'
    '建议经 <code>start-solar-sim.bat</code> 打开；直接双击时可在提示框中选择 textures 目录）；', 1)

io.open(path, 'w', encoding='utf-8', newline='\n').write(html)
print('patched:', path, orig_len, '->', len(html), 'bytes')
print('remaining base64 refs:', html.count('base64'))
