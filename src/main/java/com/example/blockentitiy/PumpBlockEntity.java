package com.example.blockentitiy;

import com.example.ExampleMod;
import com.example.block.Pump;
import com.example.blockentitiy.help.SimpleSwitch;
import com.example.blockentitiy.visiable.VisiableHelper;
import com.example.helper.CuboidFluidScanner;
import com.example.helper.ClusterFluidScanner;
import com.example.helper.FluidFillScanner;
import com.example.helper.Helper;
import com.example.helper.data.FluidResult;
import com.example.menu.PumpMenu;
import com.example.reg.BlockEntityRegister;
import com.example.reg.BlockRegister;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.CaretListener;
import java.util.*;

public class PumpBlockEntity extends BlockEntity implements MenuProvider {
    private final Map<Direction, IFluidHandler> directionalHandlers = new EnumMap<>(Direction.class);
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
    private FluidFillScanner fluidFillScanner;


    public PumpBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntityRegister.PUMP_BE.get(), pos, state);
    }
    public PumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public boolean isRangeVisible() {
        return this.getAreaDisplay().isActive();
    }

    private static class FluidPosition {
        final Fluid type;
        final BlockPos pos;

        FluidPosition(Fluid type, BlockPos pos) {
            this.type = type;
            this.pos = pos;
        }
    }

    private class FluidTank {
        private static final int CAPACITY = FluidType.BUCKET_VOLUME * 16; // 16000
        private Fluid fluidType = Fluids.EMPTY;
        private int amount = 0;
        private static final String FLUID_TYPE_KEY = "FluidType";
        private static final String AMOUNT_KEY = "Amount";
        boolean isEmpty() {
            return amount <= 0;
        }

        boolean isFull() {
            return amount >= CAPACITY;
        }

        int getRemainingSpace() {
            return CAPACITY - amount;
        }

        boolean canAccept(Fluid fluid) {
            return isEmpty() ||  fluid.isSame(fluidType);
        }

        int fill(Fluid fluid, int quantity) {
            if (!canAccept(fluid)) return 0;

            if (isEmpty()) {
                fluidType = fluid;
            }

            int actualFill = Math.min(quantity, getRemainingSpace());
            amount += actualFill;
            PumpBlockEntity.this.setChanged();
            return actualFill;
        }

        FluidStack drain(int maxDrain) {
            int drained = Math.min(maxDrain, amount);
            amount -= drained;
            FluidStack stack = new FluidStack(fluidType, drained);

            if (isEmpty()) {
                fluidType = Fluids.EMPTY;
            }
            PumpBlockEntity.this.setChanged();
            return stack;
        }

        public void clear()
        {
            amount = 0;
            fluidType = Fluids.EMPTY;
        }


        public CompoundTag writeToNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString(FLUID_TYPE_KEY, BuiltInRegistries.FLUID.getKey(fluidType).toString());
            tag.putInt(AMOUNT_KEY, amount);
            return tag;
        }

        public void readFromNBT(CompoundTag tag) {
            ResourceLocation fluidId = ResourceLocation.parse(tag.getString(FLUID_TYPE_KEY));
            fluidType = BuiltInRegistries.FLUID.get(fluidId);
            if (fluidType == null) fluidType = Fluids.EMPTY;

            amount = tag.getInt(AMOUNT_KEY);
            // 确保数值合法
            if (amount < 0) amount = 0;
            if (amount > CAPACITY) amount = CAPACITY;
        }
    }
    private final FluidTank tank = new FluidTank();
    private final Queue<FluidPosition> pendingPositions = new LinkedList<>();

    // 范围显示
    public SimpleSwitch areaDisplay = new SimpleSwitch();

    public SimpleSwitch getAreaDisplay()
    {
        return areaDisplay;
    }

    public void switchAreaDisplay(){
        areaDisplay.toggle();
        setChanged();
        Pair<BlockPos,BlockPos> area = Helper.calculateScanRange(this.getBlockPos(),this.currentArea,this.currentOffset);
        if(areaDisplay.isActive())
        {
            VisiableHelper.placeHollowCube(level, BlockRegister.VISIABLE_BLOCK.get(),area.first(),area.second());
        }
        else
        {
            VisiableHelper.removeHollowCube(level,BlockRegister.VISIABLE_BLOCK.get(),area.first(),area.second());
        }
    }

    public void closeDisplay(){
        if(this.areaDisplay.isActive())
        {
            Pair<BlockPos,BlockPos> area = Helper.calculateScanRange(this.getBlockPos(),this.currentArea,this.currentOffset);
            this.areaDisplay.toggle();
            VisiableHelper.removeHollowCube(level,BlockRegister.VISIABLE_BLOCK.get(),area.first(),area.second());
        }
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
            return pump.currentResult != null && (stack.getFluid() == pump.getFluidType() || pump.getFluidType() == Fluids.EMPTY);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isValidForInput() || !isFluidValid(0, resource)) return 0;

            synchronized (pump.tank)
            {
                // 检查流体类型兼容性
                if (!pump.tank.canAccept(resource.getFluid())) {
                    return 0;
                }
                // 计算实际可填充量
                int transferable = Math.min(
                        resource.getAmount(),
                        pump.tank.getRemainingSpace()
                );

                if (transferable <= 0) return 0;

                // 执行实际填充
                if (action.execute()) {
                    int actualFilled = pump.tank.fill(
                            resource.getFluid(),
                            transferable
                    );

                    return actualFilled;
                }
                return transferable; // 模拟时返回可填充量
            }
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
    // 修改抽取执行逻辑
    public synchronized void handleDrainExecution(int amount) {
        FluidStack drained = tank.drain(amount);
    }

    // 修改可用量计算
    public synchronized int getAvailableFluid() {
        return tank.amount;
    }
    public IFluidHandler getFluidHandler(Direction direction) {
        return directionalHandlers.computeIfAbsent(direction, dir ->
                new DirectionalFluidHandler(this, dir)
        );
    }

    private boolean isPositionQueued(BlockPos pos) {
        return pendingPositions.stream().anyMatch(e -> e.pos.equals(pos));
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

    private void removeFluidBlock(BlockPos pos) {
        if (level == null || level.isClientSide()) return;

        // 安全移除流体方块
        if (level.getBlockState(pos).getFluidState().isSource()) {
            ServerLevel serverLevel = (ServerLevel) level;

            // 设置方块为空气并更新
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_NEIGHBORS);

            // 生成粒子效果
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    5, // 粒子数量
                    0.2, 0.2, 0.2, // 偏移量
                    0.0); // 速度
        }
    }
    private void tick() {
        if (level == null || level.isClientSide) return;

        // 修改后的处理Tank填充逻辑
        if (this.pumpMode.isExtracting() && !tank.isFull() && !pendingPositions.isEmpty()) {
            synchronized (pendingPositions) { // 增加同步块保证线程安全
                FluidPosition next = pendingPositions.peek();
                if (tank.canAccept(next.type)) {
                    // 先执行流体填充
                    tank.fill(next.type, FluidType.BUCKET_VOLUME);

                    // 根据是否无限资源处理队列元素
                    if (currentResult != null && !currentResult.isInfinite()) {
                        pendingPositions.poll(); // 移除队列元素
                        removeFluidBlock(next.pos); // 移除实际方块
                    } else {
                        // 无限资源时轮转队列元素：移除并重新添加至队尾
                        FluidPosition recycled = pendingPositions.poll();
                        pendingPositions.offer(recycled);
                    }
                }
            }
        }
        //
        if (this.pumpMode == PumpMode.FILLING && scanComplete && !pendingPositions.isEmpty()) {
            handleFillingMode();
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
        if (fluidFillScanner != null) {
            fluidFillScanner.reset();
            fluidFillScanner = null;
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
        if (areaDisplay.isActive()) {
            Pair<BlockPos, BlockPos> oldArea = Helper.calculateScanRange(
                    this.getBlockPos(),
                    this.currentArea,
                    this.currentOffset
            );
            VisiableHelper.removeHollowCube(level, BlockRegister.VISIABLE_BLOCK.get(),
                    oldArea.first(), oldArea.second());
        }

        updateParameters(xRadius, yExtend, zRadius, xOffset, yOffset, zOffset);

        // 如果显示需要保持激活状态，显示新范围
        if (areaDisplay.isActive()) {
            Pair<BlockPos, BlockPos> newArea = Helper.calculateScanRange(
                    this.getBlockPos(),
                    this.currentArea,
                    this.currentOffset
            );
            VisiableHelper.placeHollowCube(level, BlockRegister.VISIABLE_BLOCK.get(),
                    newArea.first(), newArea.second());
        }
    }
    public void handleScan(PumpMode mode) {
        if (level == null || level.isClientSide) return;

        switch (mode) {
            case EXTRACTING_AUTO -> startAutoScan();
            case EXTRACTING_RANGE -> startRangeScan();
            case FILLING -> startFillScan();
        }

    }

    private void startFillScan() {
        resetScanners();
        Pair<BlockPos, BlockPos> scanRange = Helper.calculateScanRange(
                getBlockPos(),
                currentArea,
                currentOffset
        );
        pendingPositions.clear();
        fluidFillScanner = new FluidFillScanner(level, scanRange.first(), scanRange.second());
        fluidFillScanner.setProgressListener(result -> {
            if (result!=null) {
                // 将扫描结果加入填充队列
                result.getAllPositions().forEach(pos -> {
                    if (!isPositionQueued(pos)) {
                        pendingPositions.add(new FluidPosition(tank.fluidType, pos));
                    }
                });
                scanComplete = true;
                this.setChanged();
            }
        });

        this.scanning = true;
        this.scanComplete = false;
        fluidFillScanner.startScan();
        this.setChanged();
    }


    public void updateParameters(int xr, int ye, int zr, int xo, int yo, int zo) {
        this.currentArea = new BlockPos(xr, ye, zr);
        this.currentOffset = new BlockPos(xo, yo, zo);
        this.markParametersDirty();
        this.setChanged();
    }
    private void processScanResult(FluidResult result) {
        if (level instanceof ServerLevel) {
            result.getAllPositions().forEach(pos -> {
                FluidState state = level.getFluidState(pos);
                if (state.isSource()) {
                    synchronized (pendingPositions) {
                        if (!isPositionQueued(pos)) {
                            pendingPositions.add(new FluidPosition(state.getType(), pos));
                        }
                    }
                }
            });
        }
        handleScanResult();
    }

    private void startAutoScan() {
        resetScanners();
        BlockPos startPos = getBlockPos();
        if (pumpMode == PumpMode.EXTRACTING_AUTO) {
            fluidClusterScanner = new ClusterFluidScanner(level, startPos);
            fluidClusterScanner.setProgressListener(result -> {
                currentResult = result;
                processScanResult(result);
                handleScanResult();
                scanning = false;
                scanComplete = true;
                this.setChanged();
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
        Pair<BlockPos, BlockPos> scanRange = Helper.calculateScanRange(
                getBlockPos(),
                currentArea,
                currentOffset
        );
        this.cuboidFluidScanner = new CuboidFluidScanner(level, scanRange.first(), scanRange.second(),this.currentArea,this.currentOffset);
        cuboidFluidScanner.setProgressListener(result -> {
            currentResult = result;
            processScanResult(result);
            handleScanResult();
            scanning = false;
            scanComplete = true;
            markParametersDirty();
            this.setChanged();
        });

        this.scanning = true;
        this.scanComplete = false;
        cuboidFluidScanner.startScan();
        this.setChanged();
    }

    // 修改获取流体类型的方法
    public Fluid getFluidType() {
        return tank.fluidType;
    }

    private void handleAutoModeScan() {
        if (fluidClusterScanner != null) {
            if (fluidClusterScanner.isScanning()) {
                fluidClusterScanner.tick();
            }
        }
    }

    public void switchMode() {
        if (areaDisplay.isActive() && level != null) {
            Pair<BlockPos, BlockPos> currentArea = Helper.calculateScanRange(
                    getBlockPos(),
                    this.currentArea,
                    this.currentOffset
            );
            VisiableHelper.removeHollowCube(
                    level,
                    BlockRegister.VISIABLE_BLOCK.get(),
                    currentArea.first(),
                    currentArea.second()
            );
            areaDisplay.setActive(false); // 直接设置状态为关闭
            setChanged(); // 确保状态保存
        }

        resetScanners();
        this.pumpMode = pumpMode.next();
        this.scanComplete = false;
        this.scanning = false;
        this.setChanged();
        this.tank.clear();
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
        if (fluidFillScanner != null && fluidFillScanner.isScanning()) {
            fluidFillScanner.tick();
        }
    }


    private void handleFillingMode() {
        if (pumpMode != PumpMode.FILLING) return;
        if (tank.isEmpty() || tank.amount < FluidType.BUCKET_VOLUME) return; // 需要至少1000mB（1桶）

        synchronized (pendingPositions) {
            Iterator<FluidPosition> iterator = pendingPositions.iterator();
            while (iterator.hasNext()) {
                FluidPosition position = iterator.next();

                // 检查位置是否有效
                if (level.getBlockState(position.pos).canBeReplaced()) {
                    // 尝试放置流体源
                    if (tryPlaceFluidSource(tank.fluidType, position.pos)) {
                        // 成功放置后扣除流体量
                        tank.drain(FluidType.BUCKET_VOLUME);
                        iterator.remove(); // 从队列移除已处理位置
                        return; // 每tick只处理一个位置
                    }
                } else {
                    iterator.remove(); // 移除无效位置
                }
            }
        }
    }

    public boolean tryPlaceFluidSource(Fluid fluid, BlockPos pos) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        // 通过FluidState获取方块状态
        FluidState fluidState = fluid.defaultFluidState();
        BlockState targetState = fluidState.createLegacyBlock(); // 使用FluidState的公共方法

        // 检查目标方块是否有效
        if (targetState.isAir()) return false;

        // 可替换性检查
        BlockState currentState = serverLevel.getBlockState(pos);
        if (!currentState.isAir() && !currentState.canBeReplaced()) return false;

        boolean success = serverLevel.setBlock(pos,targetState,Block.UPDATE_ALL | Block.UPDATE_NEIGHBORS);
        return success;
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
        super.loadAdditional(tag, registries);
        // 加载泵模式
        if (tag.contains("PumpMode")) {
            try {
                pumpMode = PumpMode.valueOf(tag.getString("PumpMode"));
            } catch (IllegalArgumentException e) {
                pumpMode = PumpMode.EXTRACTING_AUTO; // 默认值
            }
        }

        if (tag.contains("AreaDisplay"))
        {
            areaDisplay.fromNBT(tag.getCompound("AreaDisplay"));
        }

        if (tag.contains("Tank")) {
            tank.readFromNBT(tag.getCompound("Tank"));
        }

        ListTag queueTag = tag.getList("Queue", Tag.TAG_COMPOUND);
        queueTag.forEach(entry -> {
            CompoundTag posTag = (CompoundTag) entry;
            Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(posTag.getString("Fluid")));
            BlockPos pos = NbtUtils.readBlockPos(posTag, "Pos").orElse(BlockPos.ZERO);
            pendingPositions.add(new FluidPosition(fluid, pos));
        });

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
        tag.putString("PumpMode", pumpMode.name());

        tag.put("AreaDisplay", areaDisplay.toNBT());
        tag.put("Tank", tank.writeToNBT());

        ListTag queueTag = new ListTag();
        pendingPositions.forEach(pos -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Fluid", BuiltInRegistries.FLUID.getKey(pos.type).toString());
            entry.put("Pos", NbtUtils.writeBlockPos(pos.pos));
            queueTag.add(entry);
        });
        tag.put("Queue", queueTag);

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
