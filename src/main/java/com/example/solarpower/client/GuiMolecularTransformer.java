package com.example.solarpower.client;

import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.inventory.MolecularTransformerContainer;
import com.example.solarpower.solar.MtRecipes;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.math.BigInteger;

import javax.annotation.Nullable;

/**
 * 分子重组仪界面：1:1 采用 ASP 原版布局（guidef/molecular_transformer.xml）。
 * 窗口 220x193；进度条为竖向液柱 (23,48)，填充取自贴图 (221,7) 10x15 向下；
 * 五排文本（标签右对齐至 x=107，值起于 x=112）：输入/输出/能耗/已注入/进度。
 * 输出名称客户端可由输入物品反查配方得出（配方表两侧一致）。
 */
public class GuiMolecularTransformer extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("solarpower", "textures/gui/molecular_transformer.png");

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 193;

    /** 进度液柱：显示区 (23,48)，填充源 (221,7) 10x15，向下填充（ASP ProgressBars 定义）。 */
    private static final int GAUGE_X = 23;
    private static final int GAUGE_Y = 48;
    private static final int GAUGE_WIDTH = 10;
    private static final int GAUGE_HEIGHT = 15;
    private static final int GAUGE_FILL_U = 221;
    private static final int GAUGE_FILL_V = 7;

    private static final int LABEL_RIGHT_X = 107;
    private static final int VALUE_X = 112;
    private static final int ROW0_Y = 26;
    private static final int ROW_STEP = 12;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private final MolecularTransformerContainer container;

    public GuiMolecularTransformer(MolecularTransformerContainer container) {
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
        @Nullable
        String hover = this.hoverText(mouseX - this.guiLeft, mouseY - this.guiTop);
        if (hover != null) {
            this.drawHoveringText(java.util.Collections.singletonList(hover),
                    mouseX, mouseY, this.fontRenderer);
        }
    }

    /** 进度液柱悬停：显示精确的已注入/所需 EU。 */
    @Nullable
    private String hoverText(int rx, int ry) {
        if (rx >= GAUGE_X - 2 && rx < GAUGE_X + GAUGE_WIDTH + 2
                && ry >= GAUGE_Y - 2 && ry < GAUGE_Y + GAUGE_HEIGHT + 2
                && this.container.displayedTotal() > 0) {
            return "\u00a77" + EuFormat.annotated(BigInteger.valueOf(this.container.displayedUsed()))
                    + " / " + EuFormat.annotated(BigInteger.valueOf(this.container.displayedTotal())) + " EU";
        }
        return null;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        int total = this.container.displayedTotal();
        int filled = total <= 0 ? 0
                : (int) ((long) this.container.displayedUsed() * GAUGE_HEIGHT / total);
        filled = Math.max(0, Math.min(GAUGE_HEIGHT, filled));
        if (filled > 0) {
            // Down 方向：从液柱顶部向下填充
            this.drawTexturedModalRect(this.guiLeft + GAUGE_X, this.guiTop + GAUGE_Y,
                    GAUGE_FILL_U, GAUGE_FILL_V, GAUGE_WIDTH, filled);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题居中（ASP: <text y="9" align="center">%name%</text>）
        String title = I18n.format("tile.solarpower.molecular_transformer.name");
        this.fontRenderer.drawStringWithShadow(title,
                (PANEL_WIDTH - this.fontRenderer.getStringWidth(title)) / 2, 9, TEXT_COLOR);

        ItemStack input = this.container.getSlot(0).getStack();
        String inputName = input.isEmpty() ? "" : input.getDisplayName();
        String outputName = "";
        if (!input.isEmpty()) {
            MtRecipes.MtRecipe recipe = MtRecipes.find(input);
            if (recipe != null) {
                outputName = recipe.output.getDisplayName();
            }
        }
        int total = this.container.displayedTotal();
        int used = this.container.displayedUsed();
        String percent = total <= 0 ? "" : ((int) ((long) used * 100 / total)) + "%";

        // 标签右对齐至 x=107；值起于 x=112（ASP XML 的五排布局）
        this.label(I18n.format("gui.solarpower.mt.input"), ROW0_Y);
        this.label(I18n.format("gui.solarpower.mt.output"), ROW0_Y + ROW_STEP);
        this.label(I18n.format("gui.solarpower.mt.energy_per_op"), ROW0_Y + ROW_STEP * 2);
        this.label(I18n.format("gui.solarpower.mt.energy_used"), ROW0_Y + ROW_STEP * 3);
        this.label(I18n.format("gui.solarpower.mt.progress"), ROW0_Y + ROW_STEP * 4);

        this.value(inputName, ROW0_Y);
        this.value(outputName, ROW0_Y + ROW_STEP);
        this.value(total <= 0 ? "" : EuFormat.formatEu(BigInteger.valueOf(total)) + " EU",
                ROW0_Y + ROW_STEP * 2);
        this.value(used <= 0 ? "" : EuFormat.formatEu(BigInteger.valueOf(used)) + " EU",
                ROW0_Y + ROW_STEP * 3);
        this.value(percent, ROW0_Y + ROW_STEP * 4);
    }

    private void label(String text, int y) {
        this.fontRenderer.drawStringWithShadow(text,
                LABEL_RIGHT_X - this.fontRenderer.getStringWidth(text), y, TEXT_COLOR);
    }

    private void value(String text, int y) {
        this.fontRenderer.drawStringWithShadow(text, VALUE_X, y, TEXT_COLOR);
    }
}
