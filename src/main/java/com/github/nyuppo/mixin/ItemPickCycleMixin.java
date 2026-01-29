package com.github.nyuppo.mixin;

import com.github.nyuppo.HotbarCycleClient;
import com.github.nyuppo.config.HotbarCycleConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public class ItemPickCycleMixin {
    @Shadow public ClientPlayerEntity player;

    @WrapOperation( method="doItemPick", at=@At(value="INVOKE", target="net/minecraft/client/network/ClientPlayerInteractionManager.pickItemFromBlock(Lnet/minecraft/util/math/BlockPos;Z)V") )
    private void doBlockPick(ClientPlayerInteractionManager manager, BlockPos blockPos, boolean includeData, Operation<Void> original){
        final World world = this.player.getWorld();
        final ItemStack pickedItem = world.getBlockState(blockPos).getPickStack(world, blockPos, includeData);

        if (!tryCyclePickedItem(pickedItem))
            original.call(manager, blockPos, includeData);
    }

    @WrapOperation( method="doItemPick", at=@At(value="INVOKE", target="net/minecraft/client/network/ClientPlayerInteractionManager.pickItemFromEntity(Lnet/minecraft/entity/Entity;Z)V") )
    private void doEntityPick(ClientPlayerInteractionManager manager, Entity entity, boolean includeData, Operation<Void> original){
        final ItemStack pickedItem = entity.getPickBlockStack();

        if (!tryCyclePickedItem(pickedItem))
            original.call(manager, entity, includeData);
    }

    @Unique
    private boolean tryCyclePickedItem(ItemStack pickedItem) {
        final PlayerInventory inventory = this.player.getInventory();
        final HotbarCycleConfig config = HotbarCycleClient.getConfig();
        int slot = inventory.getSlotWithStack(pickedItem);
        int x, y;

        if (8 < slot && config.getCycleWhenPickingBlock() && HotbarCycleClient.isColumnEnabled(x=slot%9) && HotbarCycleClient.isRowEnabled(y=slot/9))
        {
            final MinecraftClient client = (MinecraftClient)(Object)this;
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
