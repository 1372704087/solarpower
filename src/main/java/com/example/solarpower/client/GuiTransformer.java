package com.example.solarpower.client;

import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.inventory.TransformerContainer;
import com.example.solarpower.solar.SolarTier;
import com.example.solarpower.tileentity.TransformerTile;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 变压器界面（自定义布局：低压/高压双池条 + 吞吐与电压桥接行 + 标准玩家背包）。
 * 底图由 texturegen/gen_gui_transformer.py 生成，槽位坐标与
 * {@link TransformerContainer} 的 8+18n 栅格严格一致（ySize=190）。
 */
public class GuiTransformer extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("solarpower", "textures/gui/transformer.png");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 190;

    /** 池条井内可填充区 (10,21)-(166,31) / (10,51)-(166,61)。 */
    private static final int BAR_WIDTH = 156;
    private static final int BAR_HEIGHT = 10;
    private static final int LOW_BAR_X = 10;
    private static final int LOW_BAR_Y = 21;
    private static final int HIGH_BAR_X = 10;
    private static final int HIGH_BAR_Y = 51;

    private static final int ROW_X = 8;
    private static final int ROW_LOW_Y = 37;
    private static final int ROW_HIGH_Y = 67;
    private static final int ROW_RATE_Y = 79;
    private static final int ROW_VOLTAGE_Y = 90;

    private static final int TEXT_COLOR = 0x3F444E;

    /** iuTier → 同档面板主题色（双池条填色用；按档缓存）。 */
    private static final Map<Integer, SolarTier> ACCENTS = new java.util.HashMap<>();

    private final TransformerContainer container;

    public GuiTransformer(TransformerContainer container) {
        super(container);
        this.container = container;
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
    }

    /** 与变压器同 iuTier 的面板档（取首个匹配；两侧能量条各用其主题色）。 */
    private static SolarTier accentTier(int iuTier) {
        SolarTier cached = ACCENTS.get(iuTier);
        if (cached == null) {
            for (SolarTier tier : SolarTier.values()) {
                if (tier.iuTier() == iuTier) {
                    cached = tier;
                    break;
                }
            }
            if (cached == null) {
                cached = SolarTier.BASIC;
            }
            ACCENTS.put(iuTier, cached);
        }
        return cached;
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

    /** 池条悬停：科学计数法约数。 */
    @Nullable
    private List<String> barHoverText(int rx, int ry) {
        TransformerTile tile = this.container.getTile();
        boolean inLow = rx >= LOW_BAR_X - 2 && rx < LOW_BAR_X + BAR_WIDTH + 2
                && ry >= LOW_BAR_Y - 3 && ry < LOW_BAR_Y + BAR_HEIGHT + 3;
        boolean inHigh = rx >= HIGH_BAR_X - 2 && rx < HIGH_BAR_X + BAR_WIDTH + 2
                && ry >= HIGH_BAR_Y - 3 && ry < HIGH_BAR_Y + BAR_HEIGHT + 3;
        if (!inLow && !inHigh) {
            return null;
        }
        BigDecimal value = inLow ? this.container.lowStored : this.container.highStored;
        BigDecimal capacity = mirror(tile.poolCapacity());
        String approx = EuFormat.scientificApprox(value);
        String cap = EuFormat.scientificApprox(capacity);
        if (approx == null && cap == null) {
            return null;
        }
        return Collections.singletonList("\u00a77\u2248 "
                + (approx == null ? "0" : approx) + " / " + (cap == null ? "0" : cap) + " EU");
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, WIDTH, HEIGHT);

        TransformerTile tile = this.container.getTile();
        BigDecimal capacity = mirror(tile.poolCapacity());
        drawBar(LOW_BAR_X, LOW_BAR_Y, accentTier(tile.lowTier().iuTier()).accentColor(),
                this.container.lowStored, capacity);
        drawBar(HIGH_BAR_X, HIGH_BAR_Y, accentTier(tile.highTier().iuTier()).accentColor(),
                this.container.highStored, capacity);
    }

    private void drawBar(int x, int y, int accent, BigDecimal value, BigDecimal capacity) {
        int filled;
        if (capacity.signum() <= 0 || value.signum() <= 0) {
            filled = 0;
        } else if (value.compareTo(capacity) >= 0) {
            filled = BAR_HEIGHT;
        } else {
            double ratio = value.divide(capacity, 4, java.math.RoundingMode.DOWN).doubleValue();
            filled = (int) (ratio * BAR_HEIGHT);
        }
        filled = Math.max(0, Math.min(BAR_HEIGHT, filled));
        if (filled > 0) {
            drawRect(this.guiLeft + x,
                    this.guiTop + y + BAR_HEIGHT - filled,
                    this.guiLeft + x + BAR_WIDTH,
                    this.guiTop + y + BAR_HEIGHT,
                    0xFF000000 | accent);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        TransformerTile tile = this.container.getTile();
        // 标题用低压档主题色
        this.fontRenderer.drawStringWithShadow(
                I18n.format("tile.solarpower." + tile.lowTier().name().toLowerCase() + "_transformer.name"),
                8, 6, 0xFF000000 | accentTier(tile.lowTier().iuTier()).accentColor());
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.transformer.low",
                        tierLabel(tile.lowTier()), EuFormat.formatEu(this.container.lowStored),
                        poolLabel(tile)),
                ROW_X, ROW_LOW_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.transformer.high",
                        tierLabel(tile.highTier()), EuFormat.formatEu(this.container.highStored),
                        poolLabel(tile)),
                ROW_X, ROW_HIGH_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.transformer.rate", EuFormat.formatEu(tile.rate())),
                ROW_X, ROW_RATE_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("gui.solarpower.transformer.voltage",
                        tierLabel(tile.lowTier()), tierLabel(tile.highTier())),
                ROW_X, ROW_VOLTAGE_Y, TEXT_COLOR);
        this.fontRenderer.drawStringWithShadow(
                I18n.format("container.inventory"), ROW_X, 102, TEXT_COLOR);
    }

    /** 池容量显示（吞吐 ×8，formatEu 结果按档位实例缓存）。 */
    private static String poolLabel(TransformerTile tile) {
        String cached = POOL_LABELS.get(tile.lowTier());
        if (cached == null) {
            cached = EuFormat.formatEu(tile.poolCapacity());
            POOL_LABELS.put(tile.lowTier(), cached);
        }
        return cached;
    }

    private static final Map<EuTier, String> POOL_LABELS = new EnumMap<>(EuTier.class);

    /** 电压档短标签，如 {@code LUV T6 (32768)}。 */
    private static String tierLabel(EuTier tier) {
        return tier.name() + " T" + tier.iuTier() + " (" + tier.maxVoltageLabel() + ")";
    }

    /** 池容量镜像（池容量最大 2^136 量级，BigDecimal 直接承载，比例误差可忽略）。 */
    private static BigDecimal mirror(BigInteger capacity) {
        return new BigDecimal(capacity);
    }
}
