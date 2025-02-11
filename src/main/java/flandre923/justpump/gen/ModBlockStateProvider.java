package flandre923.justpump.gen;

import flandre923.justpump.reg.BlockRegister;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, modid, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // 生成方块模型
        ModelFile translucentModel = models().cubeAll(
                BlockRegister.VISIABLE_BLOCK.getId().getPath(),
                blockTexture(BlockRegister.VISIABLE_BLOCK.get())
        ).renderType("translucent"); // 关键修改点
        // 生成方块状态
        simpleBlock(BlockRegister.VISIABLE_BLOCK.get(), translucentModel);


        ResourceLocation topTexture = modLoc("block/pump_top");
        ResourceLocation sideTexture = modLoc("block/pump_side");

        // 创建cube_bottom_top模型（参数顺序：名称，侧面纹理，底面纹理，顶面纹理）
        ModelFile pumpModel = models().cubeBottomTop(
                "pump_block",
                sideTexture, // 所有侧面使用相同纹理
                topTexture,  // 底面使用顶部纹理（如果需要不同底面可单独指定）
                topTexture    // 顶面使用顶部纹理
        );

        // 注册简单方块状态（无方向属性）
        simpleBlock(BlockRegister.PUMP_BLOCK.get(), pumpModel);
        simpleBlockItem(BlockRegister.PUMP_BLOCK.get(), pumpModel);

    }
}
