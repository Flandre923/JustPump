package flandre923.justpump.client;

import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientAreaOffsetHelper{
    public static BlockPos area = new BlockPos(10,10,10);
    public static BlockPos offset = new BlockPos(0,0,0);

    public static void setArea(BlockPos pos){
        area = pos;
    }

    public static void setOffset(BlockPos pos){
        offset = pos;
    }

    public static BlockPos getArea(){
        return area;
    }
    public static BlockPos getOffset(){
        return offset;
    }
    public static void reset(){
        area = new BlockPos(10,10,10);
        offset = new BlockPos(0,0,0);
    }
    public static boolean isScanning =false;
    public static boolean isScanComplete = false;

    public static boolean getScanning(){
        return isScanning;
    }
    public static boolean getScanComplete(){
        return isScanComplete;
    }

    public static void setIsScanning(boolean newScanning){
        isScanning = newScanning;
    }
    public static void setIsScanComplete(boolean newScanComplete){
        isScanComplete = newScanComplete;
    }
}