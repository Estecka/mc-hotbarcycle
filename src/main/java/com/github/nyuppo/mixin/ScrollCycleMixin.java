package com.github.nyuppo.mixin;

import com.github.nyuppo.HotbarCycleClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class ScrollCycleMixin {
    @WrapOperation(method="onScroll", at=@At(value="INVOKE", target="Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I" ))
    private int hotbarcycleScrollInHotbar(double scrollAmount, int selectedSlot, int hotbarSize, Operation<Integer> original) {
        final HotbarCycleClient.Direction direction = Math.signum(scrollAmount) < 0
                ? HotbarCycleClient.Direction.UP
                : HotbarCycleClient.Direction.DOWN;
        if (HotbarCycleClient.getConfig().getHoldAndScroll() && HotbarCycleClient.getCycleKeyBinding().isDown()) {
            HotbarCycleClient.shiftRows(Minecraft.getInstance(), direction);
            return selectedSlot;
        } else if (HotbarCycleClient.getConfig().getHoldAndScroll() && HotbarCycleClient.getSingleCycleKeyBinding().isDown()) {
            HotbarCycleClient.shiftSingle(Minecraft.getInstance(), selectedSlot, direction);
            return selectedSlot;
        }
        else
            return original.call(scrollAmount, selectedSlot, hotbarSize);
    }
}
