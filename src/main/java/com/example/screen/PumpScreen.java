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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class PumpScreen extends AbstractContainerScreen<PumpMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/pump.png");
    private final PumpBlockEntity blockEntity;
    private Component modeText;
    private Component statusText;

    // 输入框组件
    private EditBox xRadius;
    private EditBox yExtend;
    private EditBox zRadius;
    private EditBox xOffset;
    private EditBox yOffset;
    private EditBox zOffset;
    private Button showRangeButton;


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
        int leftX = this.leftPos + 10;
        int buttonWidth = 25;
        // 模式切换按钮
        addRenderableWidget(Button.builder(Component.translatable("button.mode"), button -> cycleMode())
                .bounds(leftX, this.topPos + 35, buttonWidth, 15)
                .build());
        // 开始扫描按钮
        addRenderableWidget(Button.builder(Component.translatable("button.scan"), button -> onScanClicked())
                .bounds(leftX, this.topPos + 65, buttonWidth, 15)
                .build());
        // 右侧区域：输入框
        int rightX = this.leftPos + 130;
        int inputWidth = 50;
        int inputHeight = 20;
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
        showRangeButton = Button.builder(Component.translatable("button.show_range"), button -> {
                    PacketDistributor.sendToServer(new ToggleRangePayload(this.blockEntity.getBlockPos(),ToggleRangePayload.TOGGLE));
                })
                .bounds(rightX, this.topPos + 80, 130, 20)
                .build();

        addRenderableWidget(showRangeButton);
        updateComponentVisibility();
        updateStatusText();
    }

    private void createInputFields() {
        int rightX = this.leftPos + 130;
        int inputWidth = 50;
        int inputHeight = 20;

        // 前三个（范围输入框）- 仅允许非负整数
        xRadius = createNumberInput(rightX, this.topPos + 20, inputWidth, "X半径", false);
        yExtend = createNumberInput(rightX + 60, this.topPos + 20, inputWidth, "Y延伸", false);
        zRadius = createNumberInput(rightX + 120, this.topPos + 20, inputWidth, "Z半径", false);

        // 后三个（偏移输入框）- 允许负数
        xOffset = createNumberInput(rightX, this.topPos + 50, inputWidth, "X偏移", true);
        yOffset = createNumberInput(rightX + 60, this.topPos + 50, inputWidth, "Y偏移", true);
        zOffset = createNumberInput(rightX + 120, this.topPos + 50, inputWidth, "Z偏移", true);
    }
    // 修改后的创建方法（添加allowNegative参数）
    private EditBox createNumberInput(int x, int y, int width, String label, boolean allowNegative) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.literal(label));
        box.setMaxLength(allowNegative ? 4 : 3); // 负数多一位符号位

        // 根据参数设置不同过滤规则
        if(allowNegative) {
            box.setFilter(s -> s.matches("^-?\\d*$")); // 允许负号
        } else {
            box.setFilter(s -> s.matches("^\\d*$"));     // 仅数字
        }

        box.setResponder(input -> {
            if (input.isEmpty() || (allowNegative && input.equals("-"))) return;

            try {
                int value = Integer.parseInt(input);

                // 非负数输入框自动校正
                if(!allowNegative && value < 0) {
                    box.setValue("0");
                    return;
                }

                // 通用范围限制
                int max = allowNegative ? 999 : 999;
                if(Math.abs(value) > max) {
                    box.setValue(String.valueOf(max * (value < 0 ? -1 : 1)));
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
            statusText = Component.translatable("status.scanning").withStyle(ChatFormatting.GOLD);
        } else if(blockEntity.isScanComplete()) {
            statusText = Component.translatable("status.complete").withStyle(ChatFormatting.GREEN);
        } else {
            statusText = Component.translatable("status.ready").withStyle(ChatFormatting.GRAY);
        }
    }

    private void onScanClicked() {
        if (minecraft == null || minecraft.player == null) return;
        BlockPos pos = blockEntity.getBlockPos();
        PumpMode mode = blockEntity.getPumpMode();
        sendParametersToServer();
        PacketDistributor.sendToServer(new ScanStartPayload(mode,pos));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
//        super.renderLabels(guiGraphics, mouseX, mouseY);
        // 绘制标题和状态文字
        guiGraphics.drawString(
                this.font,
                this.title,
                this.titleLabelX,
                this.titleLabelY,
                0xFFFFFF, // 灰色文本颜色
                false // 不使用阴影
        );

        // 模式状态（居中绘制）
        guiGraphics.drawCenteredString(
                this.font,
                modeText,
                10,
                13 ,
                0xFFFFFF // 白色
        );

        // 扫描状态（居中绘制）
        guiGraphics.drawCenteredString(
                this.font,
                statusText,
                10,
                 18,
                0xFFFFFF // 白色
        );
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
                x, y,
                0, 0,
                imageWidth, imageHeight
        );
    }
    private boolean isEditing = false;

    public void updateInputFields(BlockPos area, BlockPos offset) {
        if (getFocused() instanceof EditBox) {
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
        // 检测到点击输入框时标记编辑状态
        if (getFocused() instanceof EditBox) {
            isEditing = true;
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 当用户输入时重置同步标记
        if (getFocused() instanceof EditBox) {
            isEditing = true;
//            menu.hasSentData = false;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


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
