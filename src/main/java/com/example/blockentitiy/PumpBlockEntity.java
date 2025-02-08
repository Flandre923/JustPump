package com.example.blockentitiy;

import com.example.ExampleMod;
import com.example.helper.CuboidFluidScanner;
import com.example.helper.ClusterFluidScanner;
import com.example.helper.data.FluidResult;
import com.example.menu.PumpMenu;
import com.example.reg.BlockEntityRegister;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PumpBlockEntity extends BlockEntity implements MenuProvider {
    private final Map<Direction, IFluidHandler> directionalHandlers = new EnumMap<>(Direction.class);
    private final Map<Direction, BlockCapabilityCache<IFluidHandler, Direction>> neighborCaches = new EnumMap<>(Direction.class);
    private int pendingFluid = 0;
    private static final int BUCKET_CAPACITY = FluidType.BUCKET_VOLUME;

    // 添加NBT键常量
    private static final String RESULT_DATA_KEY = "ResultData";
    private static final String SCANNING_KEY = "Scanning";
    private static final String SCAN_COMPLETE_KEY = "ScanComplete";

    private PumpMode pumpMode = PumpMode.EXTRACTING_AUTO;
    private CuboidFluidScanner cuboidFluidScanner;
    private ClusterFluidScanner fluidClusterScanner;
    private FluidResult currentResult;

    private boolean scanning = false;
    private boolean scanComplete = false;

    private BlockPos currentArea = new BlockPos(10, 10, 10);
    private BlockPos currentOffset = BlockPos.ZERO;
    private static final int INTERNAL_STORAGE_BUCKETS = 16;
    private final Queue<BlockPos> storedPositions = new ArrayDeque<>(INTERNAL_STORAGE_BUCKETS);
    private Fluid detectedFluidType = Fluids.EMPTY;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntityRegister.PUMP_BE.get(), pos, state);
    }
    public PumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // 方向感知的流体处理器
    private static class DirectionalFluidHandler implements IFluidHandler {
        private static final int INTERNAL_TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;
        private final PumpBlockEntity pump;
        private final Direction direction;
        private FluidStack cachedFluid = FluidStack.EMPTY;

        public DirectionalFluidHandler(PumpBlockEntity pump, Direction direction) {
            this.pump = pump;
            this.direction = direction;
        }

        @Override
        public int getTanks() {
            return 1; // 单槽位设计
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) return FluidStack.EMPTY;
            updateCachedFluid();
            return cachedFluid.copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? INTERNAL_TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && (pump.currentResult == null ||
                    stack.getFluid() == pump.getFluidType());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isValidForInput() || !isFluidValid(0, resource)) return 0;
            //TODO
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!isValidForOutput() || !resource.getFluid().isSame(getFluidType())) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!isValidForOutput() || pump.getFluidType().isSame(Fluids.EMPTY)) {
                return FluidStack.EMPTY;
            }

            // 计算实际可抽取量
            int available = pump.getAvailableFluid();
            if (available <= 0) return FluidStack.EMPTY;

            // 计算实际抽取量（考虑最大抽取限制）
            int drainAmount = Math.min(available, maxDrain);
            FluidStack result = new FluidStack(pump.getFluidType(), drainAmount);

            if (action.execute()) {
                // 实际执行抽取操作
                pump.handleDrainExecution(drainAmount);
            }

            return result;
        }

        private Fluid getFluidType() {
            return pump.currentResult != null ?
                    pump.getFluidType():
                    Fluids.EMPTY;
        }

        private void updateCachedFluid() {
            if (pump.currentResult != null) {
                cachedFluid = new FluidStack(
                        pump.getFluidType(),
                        pump.getAvailableFluid()
                );
            } else {
                cachedFluid = FluidStack.EMPTY;
            }
        }


        private boolean isValidForInput() {
            // 仅在填充模式允许输入，且不来自下方
            return pump.pumpMode == PumpMode.FILLING && direction != Direction.DOWN;
        }

        private boolean isValidForOutput() {
            // 仅在抽取模式允许输出，且不朝向下方向
            return pump.pumpMode.isExtracting() && direction != Direction.DOWN;
        }
    }

    // 在PumpBlockEntity中添加
    public synchronized void handleDrainExecution(int amount) {
        if (currentResult == null) return;

        int remaining = amount;

        // 处理无限流体模式
        if (currentResult.isInfinite()) {
            pendingFluid += amount;
            return; // 无限模式不需要实际处理方块
        }

        // 有限流体模式处理
        int bucketsToRemove = (remaining + pendingFluid) / FluidType.BUCKET_VOLUME;
        pendingFluid = (remaining + pendingFluid) % FluidType.BUCKET_VOLUME;

        // 从存储队列中取出对应数量的位置
        List<BlockPos> positionsToRemove = new ArrayList<>();
        synchronized (storedPositions) {
            while (bucketsToRemove > 0 && !storedPositions.isEmpty()) {
                positionsToRemove.add(storedPositions.poll());
                bucketsToRemove--;
            }
        }

        // 实际移除流体方块
        if (!positionsToRemove.isEmpty() && level instanceof ServerLevel serverLevel) {
            positionsToRemove.forEach(pos -> {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.0);
            });
        }

        // 更新扫描结果
        if (currentResult != null) {
            currentResult.updateAfterDrain(positionsToRemove.size());
        }
    }
    // 修改后的流体可用量计算方法
    public synchronized int getAvailableFluid() {
        if (currentResult == null) return 0;

        // 无限模式直接返回最大值
        if (currentResult.isInfinite()) {
            return Integer.MAX_VALUE;
        }

        // 计算实际可用量：存储队列中的量 + 未处理的残留量
        int storedVolume = storedPositions.size() * FluidType.BUCKET_VOLUME;
        return storedVolume + pendingFluid;
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        return directionalHandlers.computeIfAbsent(direction, dir ->
                new DirectionalFluidHandler(this, dir)
        );
    }

    private void processStorageQueue() {
        if (currentResult == null || storedPositions.size() >= INTERNAL_STORAGE_BUCKETS) return;

        // 获取可用的位置（排除已使用的）
        List<BlockPos> availablePositions = currentResult.getFluidPositions(
                INTERNAL_STORAGE_BUCKETS - storedPositions.size()
        );

        // 过滤有效流体类型
        availablePositions.stream()
                .filter(pos -> isValidFluid(pos))
                .forEach(pos -> {
                    if (storedPositions.size() < INTERNAL_STORAGE_BUCKETS) {
                        storedPositions.add(pos);
                        currentResult.markPositionUsed(pos); // 假设FluidResult添加了标记方法
                    }
                });
    }
    private boolean isValidFluid(BlockPos pos) {
        if (level == null || level.isClientSide) return false;

        // 获取流体状态
        FluidState state = level.getFluidState(pos);

        // 基本有效性检查
        boolean isSource = state.isSource();
        boolean isSameType = state.getType() == detectedFluidType;

        // 存储状态检查
        boolean notStored = !storedPositions.contains(pos);
        return isSource && isSameType && notStored;
    }
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition); // 重要！状态变化时通知能力更新
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
        pump.tick();
    }

    public boolean isScanning() {
        return scanning;
    }

    public boolean isScanComplete() {
        return scanComplete;
    }

    public PumpMode getPumpMode() {
        return pumpMode;
    }

    private void tick() {
        if (level == null || level.isClientSide) return;

        // 新增存储队列处理
        if (scanComplete) {
            processStorageQueue();
        }

        if(scanning)
        {
            switch(pumpMode)
            {
                case EXTRACTING_AUTO:
                    extractAuto();
                    break;
                case EXTRACTING_RANGE:
                    extractInRange();
                    break;
                case FILLING:
                    fillOutput();
                    break;
            }
        }
    }

    private void resetScanners() {
        if (cuboidFluidScanner != null) {
            cuboidFluidScanner.reset();
            cuboidFluidScanner = null;
        }
        if (fluidClusterScanner != null) {
            fluidClusterScanner.reset();
            fluidClusterScanner = null;
        }
    }

    // 统一处理方法
    private void handleScanResult() {
        if (level instanceof ServerLevel serverLevel && currentResult != null) {
            String message = currentResult.debugReport();
            ChatFormatting color = ChatFormatting.AQUA;
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal(message).withStyle(color));
            }
            System.out.println("[Pump Scan] " + message);
        }
    }

    private void extractAuto() {
        // 处理自动模式扫描
        if (pumpMode == PumpMode.EXTRACTING_AUTO && fluidClusterScanner != null) {
            handleAutoModeScan();
        }
    }

    public void handleArea(PumpMode mode, int xRadius, int yExtend, int zRadius,
                           int xOffset, int yOffset, int zOffset)
    {
        updateParameters(xRadius, yExtend, zRadius, xOffset, yOffset, zOffset);
    }
    public void handleScan(PumpMode mode) {
        if (level == null || level.isClientSide) return;

        switch (mode) {
            case EXTRACTING_AUTO -> startAutoScan();
            case EXTRACTING_RANGE -> startRangeScan();
            case FILLING -> startFillScan();
        }

    }
    public void updateParameters(int xr, int ye, int zr, int xo, int yo, int zo) {
        this.currentArea = new BlockPos(xr, ye, zr);
        this.currentOffset = new BlockPos(xo, yo, zo);
        this.markParametersDirty();
        this.setChanged();
    }

    private void startAutoScan() {
        resetScanners();
        BlockPos startPos = getBlockPos();
        if (pumpMode == PumpMode.EXTRACTING_AUTO) {
            fluidClusterScanner = new ClusterFluidScanner(level, startPos);
            fluidClusterScanner.setProgressListener(result -> {
                currentResult = result;
                handleScanResult();
                scanning = false;
                scanComplete = true;
                this.setChanged();
                updateDetectedFluidType();
            });
        }
        this.scanning = true;
        this.scanComplete = false;
        if (fluidClusterScanner != null) {
            fluidClusterScanner.startScan();
        }
        this.setChanged();
    }


    private void startRangeScan() {
        resetScanners(); // 根据参数计算新的扫描范围
        BlockPos basePos = getBlockPos();
        int xo = this.currentOffset.getX();
        int yo = this.currentOffset.getY();
        int zo = this.currentOffset.getZ();
        int xr = this.currentArea.getX();
        int ye = this.currentArea.getY();
        int zr = this.currentArea.getZ();
        BlockPos centerPos = basePos.offset(xo,yo,zo);
        BlockPos start = centerPos.offset(-xr, yo, -zr);
        BlockPos end = centerPos.offset(xr, -ye, zr);
        this.cuboidFluidScanner = new CuboidFluidScanner(level, start, end,this.currentArea,this.currentOffset);
        cuboidFluidScanner.setProgressListener(result -> {
            currentResult = result;
            handleScanResult();
            scanning = false;
            scanComplete = true;
            markParametersDirty();
            this.setChanged();
            updateDetectedFluidType();
        });

        this.scanning = true;
        this.scanComplete = false;
        // 启动新扫描
        cuboidFluidScanner.startScan();
        this.setChanged();
    }

    // 新增方法：从扫描结果更新流体类型
    private void updateDetectedFluidType() {
        if (currentResult == null || currentResult.getAllPositions().isEmpty()) {
            detectedFluidType = Fluids.EMPTY;
            return;
        }
        // 获取第一个有效位置的流体类型
        BlockPos firstPos = currentResult.getAllPositions().get(0);
        if (level != null) {
            FluidState state = level.getFluidState(firstPos);
            if (state.isSource()) {
                detectedFluidType = state.getType();
            }
        }
    }
    // 修改获取流体类型的方法
    public Fluid getFluidType() {
        return detectedFluidType;
    }

    private void startFillScan() {
        // 填充模式扫描逻辑
        this.scanning = true;
        this.scanComplete = false;
        // TODO: 实现具体填充扫描逻辑
    }

    private void handleAutoModeScan() {
        if (fluidClusterScanner != null) {
            if (fluidClusterScanner.isScanning()) {
                fluidClusterScanner.tick();
            }
        }
    }

    public void switchMode() {
        resetScanners();
        this.pumpMode = pumpMode.next();
        this.scanComplete = false;
        this.scanning = false;
        this.setChanged();
    }


    private void extractInRange() {

        if(cuboidFluidScanner != null && !cuboidFluidScanner.isComplete())
        {
            cuboidFluidScanner.tick();
        }
        else
        {
            scanning = false; // 扫描完成时重置状态
            scanComplete = true;
        }
    }
    private void fillOutput() {

    }
    public void markParametersDirty() {
        setChanged();
    }

    public Optional<Pair<BlockPos, BlockPos>> getRangeParameters() {
        if(this.currentArea != null && this.currentOffset!=null)
        {
            return Optional.of(Pair.of(this.currentArea,this.currentOffset));
        }
        return Optional.empty();
    }

        @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ExampleMod.MODID + ".pump");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PumpMenu(i, inventory, this.getBlockPos());
    }

    public void setPumpMode(PumpMode mode) {
        this.pumpMode = mode;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (tag.contains("DetectedFluid")) {
            detectedFluidType = BuiltInRegistries.FLUID.get(
                    ResourceLocation.parse(tag.getString("DetectedFluid"))
            );
        }
        if (tag.contains("ScanParams")) {
            CompoundTag params = tag.getCompound("ScanParams");
            NbtUtils.readBlockPos(params,"Area").ifPresent(pos -> currentArea = pos);
            NbtUtils.readBlockPos(params,"Offset").ifPresent(pos -> currentOffset = pos);
        }
        // 加载扫描状态
        scanning = tag.getBoolean(SCANNING_KEY);
        scanComplete = tag.getBoolean(SCAN_COMPLETE_KEY);

        // 加载结果数据
        if (tag.contains(RESULT_DATA_KEY)) {
            CompoundTag resultTag = tag.getCompound(RESULT_DATA_KEY);
            currentResult = new FluidResult(resultTag);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("DetectedFluid",
                BuiltInRegistries.FLUID.getKey(detectedFluidType).toString());
        CompoundTag params = new CompoundTag();
        params.put("Area",NbtUtils.writeBlockPos(currentArea));
        params.put("Offset",NbtUtils.writeBlockPos(currentOffset));
        tag.put("ScanParams", params);
        // 保存扫描状态
        tag.putBoolean(SCANNING_KEY, scanning);
        tag.putBoolean(SCAN_COMPLETE_KEY, scanComplete);

        // 保存结果数据
        if (currentResult != null) {
            CompoundTag resultTag = new CompoundTag();
            currentResult.save(resultTag);
            tag.put(RESULT_DATA_KEY, resultTag);
        }
    }
}
