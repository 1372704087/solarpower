package com.example.solarpower.client;

import com.example.solarpower.energy.EuTier;
import com.example.solarpower.inventory.SolarPanelContainer;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

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
    private static final String[] EU_UNITS = {"k", "M", "G", "T", "P", "E"};

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
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        SolarTier tier = this.container.getTile().getTier();
        long capacity = tier.capacityEu();
        long stored = Math.min(capacity, this.container.stored);
        int filled = capacity <= 0L ? 0 : (int) (stored * TUBE_HEIGHT / capacity);
        filled = Math.max(0, Math.min(TUBE_HEIGHT, filled));
        if (filled > 0) {
            this.drawTexturedModalRect(this.guiLeft + TUBE_X,
                    this.guiTop + TUBE_Y + TUBE_HEIGHT - filled,
                    FILL_U, FILL_V + TUBE_HEIGHT - filled, 18, filled);
        }

        int ledV = this.container.generating > 0
                ? (this.container.night ? LED_NIGHT_V : LED_LIT_V)
                : LED_DIM_V;
        this.drawTexturedModalRect(this.guiLeft + LED_X, this.guiTop + LED_Y, LED_U, ledV, LED_SIZE, LED_SIZE);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        SolarTier tier = this.container.getTile().getTier();
        this.fontRenderer.drawStringWithShadow(I18n.format(tier.translationKey() + ".name"),
                20, 8, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.generation", formatEu(this.container.generating)),
                ROW_TEXT_X, ROW1_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.voltage", voltageLabel(tier)),
                ROW_TEXT_X, ROW2_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.energy",
                        formatEu(this.container.stored), formatEu(tier.capacityEu())),
                ROW_TEXT_X, ROW3_Y, TEXT_COLOR);
    }

    /** 电压显示，如 {@code LV (32 EU/packet)}；最高档只显示等级名。 */
    private static String voltageLabel(SolarTier tier) {
        EuTier v = tier.voltage();
        if (v.maxVoltage() == Integer.MAX_VALUE) {
            return v.name();
        }
        return v.name() + " (" + v.maxVoltage() + " EU/packet)";
    }

    /** 把大数值压缩成带单位的短串。 */
    private static String formatEu(long value) {
        if (value < 1000L) {
            return Long.toString(value);
        }
        double scaled = value;
        int unit = -1;
        while (scaled >= 1000.0 && unit < EU_UNITS.length - 1) {
            scaled /= 1000.0;
            unit++;
        }
        String pattern = scaled >= 100.0 ? "%.0f" : "%.2f";
        return String.format(Locale.ROOT, pattern, scaled) + EU_UNITS[unit];
    }
}
