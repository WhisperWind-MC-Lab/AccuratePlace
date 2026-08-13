package dev.accurateplace.mixin;

import dev.accurateplace.protocol.PlacementProtocolV3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockItem.class, priority = 1010)
public abstract class BlockItemMixin {
    @Shadow
    protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void accurateplace$applyProtocol(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> callback
    ) {
        if (!(context.getPlayer() instanceof ServerPlayer)) {
            return;
        }

        BlockState vanillaState = getBlock().getStateForPlacement(context);
        if (vanillaState == null || !canPlace(context, vanillaState)) {
            return;
        }

        BlockState decoded = PlacementProtocolV3.apply(vanillaState, context);
        if (decoded != vanillaState) {
            callback.setReturnValue(decoded);
        }
    }
}
