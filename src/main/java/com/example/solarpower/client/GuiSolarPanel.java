package com.example.solarpower.client;

import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.inventory.SolarPanelContainer;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/** 太阳能板界面（1:1 复刻 NeoForge 版布局：能量管 + 指示灯 + 三行数据）。 */
public class GuiSolarPanel extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("solarpower", "textures/gui/solar_panel.png");

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 166;

    private static final int TUBE_X = 176;
    private static final int TUBE_Y = 34;
    private static final int TUBE_HEIGHT = 108;
    private static final int FILL_U = 200;
    private static final int FILL_V = 0;

    private static final int LED_X = 181;
    private static final int LED_Y = 21;
    private static final int LED_SIZE = 10;
    private static final int LED_U = 220;
    private static final int LED_LIT_V = 0;
    private static final int LED_NIGHT_V = 24;
    private static final int LED_DIM_V = 12;

    private static final int ROW_TEXT_X = 16;
    private static final int ROW1_Y = 33;
    private static final int ROW2_Y = 53;
    private static final int ROW3_Y = 73;

    private static final int TEXT_COLOR = 0xDFEAF4;

    private final SolarPanelContainer container;

    public GuiSolarPanel(SolarPanelContainer container) {
        super(container);
        this.container = container;
        this.xSize = PANEL_WIDTH;
        this.ySize = PANEL_HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        // 数据行悬停：显示科学计数法约数（词头不好反推大小时用）
        long t0 = System.nanoTime();
        List<String> hover = this.rowHoverText(mouseX - this.guiLeft, mouseY - this.guiTop);
        long t1 = System.nanoTime();
        if (hover != null) {
            this.drawHoveringText(hover, mouseX, mouseY, this.fontRenderer);
        }
        long d2 = System.nanoTime() - t1;
        if (d2 > perfHoverMax) perfHoverMax = d2;
        perfHoverNanos += t1 - t0;
        if (++perfHoverCount >= 300) {
            System.out.println("[SolarPerf] rowHover calc avg=" + perfHoverNanos / perfHoverCount / 1000
                    + "us | tooltipDraw max=" + perfHoverMax / 1000 + "us");
            perfHoverCount = 0; perfHoverNanos = 0; perfHoverMax = 0;
        }
    }

    private static long perfHoverNanos;
    private static int perfHoverCount;
    private static long perfHoverMax;

    /** 三行数据行的悬停科学计数法约数；鼠标不在行上或数值太小时返回 null。 */
    @Nullable
    private List<String> rowHoverText(int rx, int ry) {
        // 先在最小代价下排除：鼠标不在数据行区域就不做任何大数格式化
        boolean inRow = rx >= ROW_TEXT_X - 4 && rx < 172;
        if (!inRow || ry < ROW1_Y - 2 || ry >= ROW3_Y + 18) {
            return null;
        }
        SolarTier tier = this.container.getTile().getTier();
        if (ry < ROW2_Y - 2) {
            String gen = EuFormat.scientificApprox(this.container.generating);
            return gen == null ? null : Collections.singletonList("\u00a77\u2248 " + gen + " EU/t");
        }
        if (ry < ROW3_Y - 2) {
            String volt = tier.voltage().maxVoltageScientific();
            return volt == null ? null : Collections.singletonList("\u00a77\u2248 " + volt + " EU/packet");
        }
        // 档位常量走 SolarTier 的惰性缓存；只有实时存量每次都真的变了
        String stored = EuFormat.scientificApprox(this.container.stored);
        String cap = tier.capacityScientific();
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
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        SolarTier tier = this.container.getTile().getTier();
        BigDecimal capacity = new BigDecimal(tier.capacityEu());
        int filled;
        if (capacity.signum() <= 0 || this.container.stored.signum() <= 0) {
            filled = 0;
        } else if (this.container.stored.compareTo(capacity) >= 0) {
            filled = TUBE_HEIGHT;
        } else {
            // 定点除法终止，比值恒在 [0,1)，不会出现 Infinity/NaN
            double ratio = this.container.stored.divide(capacity, 4, RoundingMode.DOWN).doubleValue();
            filled = (int) (ratio * TUBE_HEIGHT);
        }
        filled = Math.max(0, Math.min(TUBE_HEIGHT, filled));
        if (filled > 0) {
            this.drawTexturedModalRect(this.guiLeft + TUBE_X,
                    this.guiTop + TUBE_Y + TUBE_HEIGHT - filled,
                    FILL_U, FILL_V + TUBE_HEIGHT - filled, 18, filled);
        }

        int ledV = this.container.generating.signum() > 0
                ? (this.container.night ? LED_NIGHT_V : LED_LIT_V)
                : LED_DIM_V;
        this.drawTexturedModalRect(this.guiLeft + LED_X, this.guiTop + LED_Y, LED_U, ledV, LED_SIZE, LED_SIZE);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        SolarTier tier = this.container.getTile().getTier();
        // 标题用档位主题色的精确 RGB（GUI 字体不受原版 16 色板限制）
        this.fontRenderer.drawStringWithShadow(I18n.format(tier.translationKey() + ".name"),
                20, 8, 0xFF000000 | tier.accentColor());
        // 发电量每 tick 变，走容器同步的显示值；容量是档位常量，用 SolarTier 的惰性缓存
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.generation", EuFormat.formatEu(this.container.generating)),
                ROW_TEXT_X, ROW1_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.voltage", voltageLabel(tier)),
                ROW_TEXT_X, ROW2_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.energy",
                        EuFormat.formatEu(this.container.stored), tier.capacityLabel()),
                ROW_TEXT_X, ROW3_Y, TEXT_COLOR);
    }

    /** 电压显示，如 {@code LV (32 EU/packet)}。 */
    private static String voltageLabel(SolarTier tier) {
        EuTier v = tier.voltage();
        return v.name() + " (" + v.maxVoltageLabel() + " EU/packet)";
    }
}