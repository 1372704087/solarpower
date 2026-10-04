package com.example.solarpower.client;

import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.inventory.StorageBoxContainer;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * 储电盒界面（自定义布局：宽能量条 + 三行数据 + 标准玩家背包）。
 * 底图由 texturegen/gen_gui_storage.py 生成，槽位坐标与
 * {@link StorageBoxContainer} 的 8+18n 栅格严格一致。
 */
public class GuiStorageBox extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("solarpower", "textures/gui/storage_box.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;

    /** 能量条井内可填充区 (10,22)-(166,34)。 */
    private static final int BAR_X = 10;
    private static final int BAR_Y = 22;
    private static final int BAR_WIDTH = 156;
    private static final int BAR_HEIGHT = 12;

    private static final int ROW_X = 8;
    private static final int ROW_STORED_Y = 42;
    private static final int ROW_CAPACITY_Y = 53;
    private static final int ROW_OUTPUT_Y = 64;

    private static final int TEXT_COLOR = 0x3F444E;

    private final StorageBoxContainer container;

    public GuiStorageBox(StorageBoxContainer container) {
        super(container);
        this.container = container;
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        List<String> hover = barHoverText(mouseX - this.guiLeft, mouseY - this.guiTop);
        if (hover != null) {
            this.drawHoveringText(hover, mouseX, mouseY, this.fontRenderer);
        }
    }

    /** 能量条悬停：科学计数法约数（4 位有效数字的显示值太小，词头不好反推大小时用）。 */
    @Nullable
    private List<String> barHoverText(int rx, int ry) {
        boolean inBar = rx >= BAR_X - 2 && rx < BAR_X + BAR_WIDTH + 2
                && ry >= BAR_Y - 3 && ry < BAR_Y + BAR_HEIGHT + 3;
        if (!inBar) {
            return null;
        }
        SolarTier tier = this.container.getTile().getSolarTier();
        String stored = EuFormat.scientificApprox(this.container.stored);
        String cap = EuFormat.scientificApprox(capacityMirror(tier));
        if (stored == null && cap == null) {
            return null;
        }
        return Collections.singletonList("\u00a77\u2248 "
                + (stored == null ? "0" : stored) + " / " + (cap == null ? "0" : cap) + " EU");
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, WIDTH, HEIGHT);

        // 能量条填充：档位主题色（容量镜像 ×64 = 储电盒容量，见 capacityMirror）
        SolarTier tier = this.container.getTile().getSolarTier();
        BigDecimal capacity = capacityMirror(tier);
        int filled;
        if (capacity.signum() <= 0 || this.container.stored.signum() <= 0) {
            filled = 0;
        } else if (this.container.stored.compareTo(capacity) >= 0) {
            filled = BAR_HEIGHT;
        } else {
            double ratio = this.container.stored.divide(capacity, 4, java.math.RoundingMode.DOWN).doubleValue();
            filled = (int) (ratio * BAR_HEIGHT);
        }
        filled = Math.max(0, Math.min(BAR_HEIGHT, filled));
        if (filled > 0) {
            drawRect(this.guiLeft + BAR_X,
                    this.guiTop + BAR_Y + BAR_HEIGHT - filled,
                    this.guiLeft + BAR_X + BAR_WIDTH,
                    this.guiTop + BAR_Y + BAR_HEIGHT,
                    0xFF000000 | tier.accentColor());
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        SolarTier tier = this.container.getTile().getSolarTier();
        // 标题用档位主题色的精确 RGB（GUI 字体不受原版 16 色板限制）
        this.fontRenderer.drawStringWithShadow(
                I18n.format("tile.solarpower." + tier.id() + "_storage_box.name"),
                8, 6, 0xFF000000 | tier.accentColor());
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.storage.energy",
                        EuFormat.formatEu(this.container.stored), capacityLabel(tier)),
                ROW_X, ROW_STORED_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.storage.capacity", capacityLabel(tier)),
                ROW_X, ROW_CAPACITY_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.storage.output",
                        tier.maxOutputAnnotated(), voltageLabel(tier)),
                ROW_X, ROW_OUTPUT_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("container.inventory"), ROW_X, 74, TEXT_COLOR);
    }

    /** 储电盒容量的「紧凑镜像」（面板镜像 ×64，18 位有效数字，比例误差 < 1e-15）。 */
    private static BigDecimal capacityMirror(SolarTier tier) {
        return tier.capacityMirror().multiply(BigDecimal.valueOf(64));
    }

    /** 储电盒容量显示（formatEu 结果按档位实例缓存：顶档容量也是百万位级大数）。 */
    private static String capacityLabel(SolarTier tier) {
        String cached = CAPACITY_LABELS.get(tier);
        if (cached == null) {
            cached = EuFormat.formatEu(tier.capacityEu().shiftLeft(6));
            CAPACITY_LABELS.put(tier, cached);
        }
        return cached;
    }

    private static final java.util.Map<SolarTier, String> CAPACITY_LABELS =
            new java.util.EnumMap<>(SolarTier.class);

    /** 电压显示，如 {@code EV (8192 EU/packet)}。 */
    private static String voltageLabel(SolarTier tier) {
        EuTier v = tier.voltage();
        return v.name() + " (" + v.maxVoltageLabel() + ")";
    }
}
