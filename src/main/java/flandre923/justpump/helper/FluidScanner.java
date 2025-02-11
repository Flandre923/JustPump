package flandre923.justpump.helper;

import flandre923.justpump.config.ModConfigs;
import flandre923.justpump.helper.data.FluidResult;
import net.minecraft.world.level.Level;

import java.util.Optional;

public abstract class FluidScanner {
    protected final Level level;
    protected boolean isScanning = false;
    protected FluidResult result;
    protected ScanProgressListener listener;
    public static final int PER_TICK_COUNT = ModConfigs.blocksPerTick;

    public FluidScanner(Level level) {
        this.level = level;
    }

    public abstract void startScan();
    public abstract void tick();
    public abstract void reset();
    public abstract boolean isComplete();
    public boolean isScanning() {
        return isScanning;
    }

    public Optional<FluidResult> getResult() {
        return Optional.ofNullable(result);
    }

    public void setProgressListener(ScanProgressListener listener) {
        this.listener = listener;
    }

    protected void completeScan() {
        isScanning = false;
        if (listener != null) {
            listener.onScanComplete(result);
        }
    }

    public interface ScanProgressListener{
        void onScanComplete(FluidResult result);
    }


}
