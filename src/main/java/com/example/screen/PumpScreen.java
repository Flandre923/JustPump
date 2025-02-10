package com.example.screen;

import com.example.ExampleMod;
import com.example.blockentitiy.PumpBlockEntity;
import com.example.blockentitiy.PumpMode;
import com.example.menu.PumpMenu;
import com.example.network.ModeUpdatePayload;
import com.example.network.ScanAreaPayload;
import com.example.network.ScanStartPayload;
import com.example.network.ToggleRangePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class PumpScreen extends AbstractContainerScreen<PumpMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/pump_gui.png");
    private final PumpBlockEntity blockEntity;
    private Component modeText;
    private Component statusText;

    // 输入框组件
    private TextureEditBox xRadius;
    private TextureEditBox yExtend;
    private TextureEditBox zRadius;
    private TextureEditBox xOffset;
    private TextureEditBox yOffset;
    private TextureEditBox zOffset;
    private TextureButton showRangeButton;


    public PumpScreen(PumpMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 300;  // 加宽界面
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        // 左侧区域：按钮和状态
        int buttonWidth = 31;
        int buttonHeight = 15;
        // 模式切换按钮
        addRenderableWidget(TextureButton.builder(Component.translatable("button.mode"), button -> cycleMode())
                .texture(TEXTURE)
                .textureCoords(16,32)
                .sourceSize(31,15)
                .textureTotalSize(300,200)
                .bounds(this.leftPos + 16, this.topPos + 32, buttonWidth, buttonHeight)
                .build());
        // 开始扫描按钮
        addRenderableWidget(TextureButton.builder(Component.translatable("button.scan"), button -> onScanClicked())
                .texture(TEXTURE)
                .textureCoords(16,32)
                .sourceSize(31,15)
                .textureTotalSize(300,200)
                .bounds(this.leftPos + 16, this.topPos + 50, buttonWidth, buttonHeight)
                .build());

        // 范围输入框
        createInputFields();
        // 设置默认值
        xRadius.setValue("10");
        yExtend.setValue("10");
        zRadius.setValue("10");
        xOffset.setValue("0");
        yOffset.setValue("0");
        zOffset.setValue("0");

        // 显示范围按钮
        showRangeButton = TextureButton.builder(Component.translatable("button.show_range"), button -> {
                    PacketDistributor.sendToServer(new ToggleRangePayload(this.blockEntity.getBlockPos(),ToggleRangePayload.TOGGLE));
                })
                .texture(TEXTURE)
                .textureCoords(16,32)
                .sourceSize(31,15)
                .textureTotalSize(300,200)
                .bounds(this.leftPos+240, this.topPos + 115, 47, 15)
                .build();

        addRenderableWidget(showRangeButton);
        updateComponentVisibility();
        updateStatusText();
    }

    private void createInputFields() {
        int rightX = this.leftPos + 112;
        int offsetRightX = this.leftPos + 208;
        int inputWidth = 79;

        // 范围输入框（X半径0-64，Y延伸0-100，Z半径0-64）
        xRadius = createNumberInput(rightX,  this.topPos + 32, inputWidth, "label.x_radius", false, 64);
        yExtend = createNumberInput(rightX , this.topPos + 64, inputWidth, "label.y_extend", false, 100);
        zRadius = createNumberInput(rightX,  this.topPos + 96, inputWidth, "label..z_radius", false, 64);

        // 偏移输入框（允许负数，不做额外限制）
        xOffset = createNumberInput(offsetRightX, this.topPos + 32, inputWidth, "label.x_offset", true, Integer.MAX_VALUE);
        yOffset = createNumberInput(offsetRightX, this.topPos + 64, inputWidth, "label.y_offset", true, Integer.MAX_VALUE);
        zOffset = createNumberInput(offsetRightX, this.topPos + 96, inputWidth, "label.z_offset", true, Integer.MAX_VALUE);
    }
    // 修改后的创建方法（添加allowNegative参数）
    private TextureEditBox createNumberInput(int x, int y, int width, String label,
                                      boolean allowNegative, int maxValue) {
        TextureEditBox box = new TextureEditBox(this.font, x, y, width, 15, Component.literal(label));
        box.setMaxLength(allowNegative ? 4 : String.valueOf(maxValue).length());

        // 根据参数设置不同过滤规则
        box.setFilter(s -> {
            if (allowNegative) {
                return s.matches("^-?\\d*$");
            } else {
                return s.matches("^\\d*$");
            }
        });

        box.setResponder(input -> {
            if (input.isEmpty() || (allowNegative && input.equals("-"))) return;

            try {
                int value = Integer.parseInt(input);

                // 非负数输入框自动校正
                if (!allowNegative) {
                    value = Math.max(value, 0);
                }

                // 应用不同范围限制
                int clampedValue = allowNegative ?
                        Math.min(Math.abs(value), 999) * (value < 0 ? -1 : 1) :
                        Math.min(value, maxValue);

                if (value != clampedValue) {
                    box.setValue(String.valueOf(clampedValue));
                }
            } catch (NumberFormatException e) {
                box.setValue(allowNegative ? "-0" : "0");
            }
        });

        addRenderableWidget(box);
        return box;
    }

    private void updateComponentVisibility() {
        PumpMode mode = blockEntity.getPumpMode();
        boolean showFields = mode == PumpMode.EXTRACTING_RANGE || mode == PumpMode.FILLING;

        xRadius.setVisible(showFields);
        yExtend.setVisible(showFields);
        zRadius.setVisible(showFields);
        xOffset.setVisible(showFields);
        yOffset.setVisible(showFields);
        zOffset.setVisible(showFields);
        showRangeButton.visible = showFields;
    }

    private int parseInt(String value, boolean allowNegative) {
        try {
            int num = Integer.parseInt(value.isEmpty() ? "0" : value);
            return allowNegative ? num : Math.max(num, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void updateDisplay() {
        this.init(minecraft, width, height);
    }


    private void cycleMode() {
        if (minecraft != null && minecraft.player != null) {
            BlockPos pos = menu.getBlockEntity().getBlockPos();
            PumpMode mode = menu.getBlockEntity().getPumpMode();
            PacketDistributor.sendToServer(new ModeUpdatePayload(pos,mode));
        }
        updateStatusText();
    }

    private void updateStatusText() {
        // 模式状态文本
        switch(blockEntity.getPumpMode()) {
            case EXTRACTING_AUTO -> modeText = Component.translatable("mode.auto").withStyle(ChatFormatting.BLUE);
            case EXTRACTING_RANGE -> modeText = Component.translatable("mode.range").withStyle(ChatFormatting.GREEN);
            case FILLING -> modeText = Component.translatable("mode.fill").withStyle(ChatFormatting.YELLOW);
        }

        // 扫描状态文本
        if(blockEntity.isScanning()) {
            statusText = Component.translatable("status.scanning").withStyle(ChatFormatting.RED);
        } else if(blockEntity.isScanComplete()) {
            statusText = Component.translatable("status.complete").withStyle(ChatFormatting.GREEN);
        } else {
            statusText = Component.translatable("status.ready").withStyle(ChatFormatting.WHITE);
        }
    }

    private void onScanClicked() {
        if (minecraft == null || minecraft.player == null) return;
        BlockPos pos = blockEntity.getBlockPos();
        PumpMode mode = blockEntity.getPumpMode();
        sendParametersToServer();
        PacketDistributor.sendToServer(new ScanStartPayload(mode,pos));
    }

    private void renderText(GuiGraphics gui, Component text, int x, int y, int color, boolean shadow) {
        gui.drawString(
                this.font,      // 使用当前GUI的字体
                text,           // 要渲染的文本组件
                x, y,           // 屏幕坐标
                color,          // 颜色（ARGB格式）
                shadow          // 是否显示阴影
        );
    }
    private void renderText(GuiGraphics gui, Component text, int x, int y,
                            int color, boolean shadow, float scale) {
        gui.pose().pushPose();
        gui.pose().translate(x, y, 0);
        gui.pose().scale(scale, scale, 1.0f);
        gui.drawString(
                this.font,
                text,
                0, 0,      // 坐标已通过translate处理
                color,
                shadow
        );
        gui.pose().popPose();
    }



    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderAreaTips(guiGraphics,mouseX,mouseY);
        renderText(guiGraphics,
                Component.translatable("label.mode"),
                64, 24,
                0xFFFFFF, false,0.75f
        );

        // 绘制状态标签
        renderText(guiGraphics,
                Component.translatable("label.status"),
                64, 56,
                0xFFFFFF, false,0.75f
        );

        // 绘制标题
        renderText(guiGraphics,
                this.title,
                this.titleLabelX, this.titleLabelY,
                0xFFFFFF, false
        );

        // 绘制模式状态
        renderText(guiGraphics,
                modeText,
                64, 32,
                0xFFFFFF, false
        );

        // 绘制扫描状态
        renderText(guiGraphics,
                statusText,
                64, 64,
                0xFFFFFF, false
        );
    }

    public void renderAreaTips(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        if(this.blockEntity.getPumpMode() == PumpMode.EXTRACTING_RANGE || this.blockEntity.getPumpMode() == PumpMode.FILLING)
        {
            // 输入框标签组 - 右侧
            renderText(guiGraphics, Component.translatable("label.x_radius"), 112, 25, 0xFFFFFF, false,0.75f);
            renderText(guiGraphics, Component.translatable("label.y_extend"), 112, 56, 0xFFFFFF, false,0.75f);
            renderText(guiGraphics, Component.translatable("label.z_radius"), 112, 90, 0xFFFFFF, false,0.75f);
            // 偏移量标签组 - 更右侧
            renderText(guiGraphics, Component.translatable("label.x_offset"), 208, 25, 0xFFFFFF, false,0.75f);
            renderText(guiGraphics, Component.translatable("label.y_offset"), 208, 56, 0xFFFFFF, false,0.75f);
            renderText(guiGraphics, Component.translatable("label.z_offset"), 208, 90, 0xFFFFFF, false,0.75f);

        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 先渲染背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        // 调用父类渲染
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染工具提示
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        // 动态更新组件可见性
        updateComponentVisibility();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 绘制GUI背景纹理（需要根据实际纹理路径调整）
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 绘制主背景
        guiGraphics.blit(
                TEXTURE, // 你的纹理资源位置
                leftPos, topPos,
                300,144,
                0, 0,
                300, 144,
                300,200
        );
    }

    public void updateInputFields(BlockPos area, BlockPos offset) {
        if (getFocused() instanceof TextureEditBox) {
            return;
        }
        xRadius.setValue(String.valueOf(area.getX()));
        yExtend.setValue(String.valueOf(area.getY()));
        zRadius.setValue(String.valueOf(area.getZ()));
        xOffset.setValue(String.valueOf(offset.getX()));
        yOffset.setValue(String.valueOf(offset.getY()));
        zOffset.setValue(String.valueOf(offset.getZ()));
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    public void onClose() {
        // 在关闭界面时同步数据
        sendParametersToServer();
        super.onClose();
    }

    private void sendParametersToServer() {
        BlockPos pos = blockEntity.getBlockPos();
        // 范围参数使用非负解析
        int xr = parseInt(xRadius.getValue(), false);
        int ye = parseInt(yExtend.getValue(), false);
        int zr = parseInt(zRadius.getValue(), false);

        // 偏移参数允许负数
        int xo = parseInt(xOffset.getValue(), true);
        int yo = parseInt(yOffset.getValue(), true);
        int zo = parseInt(zOffset.getValue(), true);
        PacketDistributor.sendToServer(new ScanAreaPayload(
                pos, PumpMode.EXTRACTING_RANGE, xr, ye, zr, xo, yo, zo
        ));
    }


}
