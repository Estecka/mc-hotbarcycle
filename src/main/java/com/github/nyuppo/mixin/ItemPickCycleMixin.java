package com.github.nyuppo.mixin;

import com.github.nyuppo.HotbarCycleClient;
import com.github.nyuppo.config.HotbarCycleConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class ItemPickCycleMixin {
    @Shadow public LocalPlayer player;

    @WrapOperation(
        method = "pickBlockOrEntity",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/multiplayer/MultiPlayerGameMode.handlePickItemFromBlock(Lnet/minecraft/core/BlockPos;Z)V"
        )
    )
    private void doBlockPick(MultiPlayerGameMode manager, BlockPos blockPos, boolean includeData, Operation<Void> original){
        final Level world = this.player.level();
        final ItemStack pickedItem = world.getBlockState(blockPos).getCloneItemStack(world, blockPos, includeData);

        if (!tryCyclePickedItem(pickedItem))
            original.call(manager, blockPos, includeData);
    }

    @WrapOperation(
        method = "pickBlockOrEntity",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/multiplayer/MultiPlayerGameMode.handlePickItemFromEntity(Lnet/minecraft/world/entity/Entity;Z)V"
        )
    )
    private void doEntityPick(MultiPlayerGameMode manager, Entity entity, boolean includeData, Operation<Void> original){
        final ItemStack pickedItem = entity.getPickResult();

        if (!tryCyclePickedItem(pickedItem))
            original.call(manager, entity, includeData);
    }

    @Unique
    private boolean tryCyclePickedItem(ItemStack pickedItem) {
        if (pickedItem == null)
            return false;

        final Inventory inventory = this.player.getInventory();
        final HotbarCycleConfig config = HotbarCycleClient.getConfig();
        int slot = inventory.findSlotMatchingItem(pickedItem);
        int x, y;

        if (8 < slot && config.getCycleWhenPickingBlock() && HotbarCycleClient.isColumnEnabled(x=slot%9) && HotbarCycleClient.isRowEnabled(y=slot/9))
        {
            final Minecraft client = (Minecraft)(Object)this;
            int direction = -1;
            for (int i=1; i<y; ++i)
                if (HotbarCycleClient.isRowEnabled(i))
                    direction--;

            if (config.getPickCyclesWholeHotbar())
                HotbarCycleClient.shiftRows(client, direction);
            else
                HotbarCycleClient.shiftSingle(client, x, direction);

            inventory.setSelectedSlot(x);
            return true;
        }

        return false;
    }
}
