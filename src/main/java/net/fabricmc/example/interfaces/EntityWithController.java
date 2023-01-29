package net.fabricmc.example.interfaces;

import net.minecraft.util.math.BlockPos;

public interface EntityWithController {

    void addControllerPos(BlockPos pos);
    boolean isControlled();
    boolean isController();
}
