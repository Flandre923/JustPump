package flandre923.justpump.screen;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TextureButton extends AbstractButton {

    private final ResourceLocation texture;
    private final int texX;
    private final int texY;
    private final int texWidth;
    private final int texHeight;
    private final int textureTotalWidth;
    private final int textureTotalHeight;
    private final int hoverOffset;
    private final int disabledOffset;
    private final OnPress onPress;

    public TextureButton(int x, int y, int width, int height,
                         ResourceLocation texture, int texX, int texY,
                         int srcWidth, int srcHeight, // 纹理源区域尺寸
                         int textureTotalWidth, int textureTotalHeight, // 纹理总尺寸
                         int hoverOffset, int disabledOffset,
                         Component message, OnPress onPress) {
        super(x, y, width, height, message);
        // 新增纹理尺寸参数
        this.textureTotalWidth = textureTotalWidth;
        this.textureTotalHeight = textureTotalHeight;
        // 保留原有参数
        this.texture = texture;
        this.texX = texX;
        this.texY = texY;
        this.texWidth = srcWidth;    // 修改为源宽度
        this.texHeight = srcHeight; // 修改为源高度
        this.hoverOffset = hoverOffset;
        this.disabledOffset = disabledOffset;
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        if (this.active) {
            this.onPress.onPress(this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(TextureButton TextureButton);
    }


    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 计算纹理Y偏移
        int yOffset = 0;
        if (!this.active) {
            yOffset = disabledOffset;
        } else if (this.isHoveredOrFocused()) {
            yOffset = hoverOffset;
        }

        // 绘制按钮纹理
        guiGraphics.blit(
                texture,
                getX(), getY(),
                this.width, this.height, // 目标尺寸（按钮实际大小）
                texX, texY + yOffset,
                texWidth, texHeight,  // 纹理源区域尺寸
                textureTotalWidth, textureTotalHeight // 纹理总尺寸
        );

        // 绘制按钮文本
        this.renderString(guiGraphics, Minecraft.getInstance().font,
                getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    public static TexturedButtonBuilder builder(Component message,TextureButton.OnPress onPress)
    {
        return new TexturedButtonBuilder(message,onPress);
    }


    public static class TexturedButtonBuilder {
        private int x;
        private int y;
        private int width;
        private int height;
        private ResourceLocation texture;
        private int texX;
        private int texY;
        private int texWidth;
        private int texHeight;
        private int hoverOffset = 0;
        private int disabledOffset = 0;
        private Component message;
        private OnPress onPress;
        private Tooltip tooltip;

        private int srcWidth;
        private int srcHeight;
        private int textureTotalWidth;
        private int textureTotalHeight;

        public TexturedButtonBuilder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public TexturedButtonBuilder texture(ResourceLocation texture) {
            this.texture = texture;
            return this;
        }

        public TexturedButtonBuilder sourceSize(int width, int height) {
            this.srcWidth = width;
            this.srcHeight = height;
            return this;
        }
        public TexturedButtonBuilder textureTotalSize(int width, int height) {
            this.textureTotalWidth = width;
            this.textureTotalHeight = height;
            return this;
        }

        public TexturedButtonBuilder textureCoords(int x, int y) {
            this.texX = x;
            this.texY = y;
            return this;
        }


        public TexturedButtonBuilder offsets(int hover, int disabled) {
            this.hoverOffset = hover;
            this.disabledOffset = disabled;
            return this;
        }

        public TexturedButtonBuilder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public TexturedButtonBuilder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public TextureButton build() {
            TextureButton button = new TextureButton(
                    x, y, width, height,
                    texture, texX, texY,
                    srcWidth, srcHeight,    // 源尺寸
                    textureTotalWidth, textureTotalHeight, // 总尺寸
                    hoverOffset, disabledOffset,
                    message, onPress
            );
            if (tooltip != null) {
                button.setTooltip(tooltip);
            }
            return button;
        }
    }
}
