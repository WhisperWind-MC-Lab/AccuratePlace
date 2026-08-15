package dev.accurateplace.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla rejects hit positions outside the clicked block before item placement runs.
 * Protocol v3 intentionally stores its payload in the positive X offset, so suppress
 * only that encoded component while retaining the normal Y/Z validation.
 */
@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1010)
public abstract class ServerGamePacketListenerMixin {
    private static final double VANILLA_HIT_LIMIT = 1.0000001D;

    @Redirect(
            method = "handleUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 accurateplace$allowEncodedProtocolX(Vec3 hitPosition, Vec3 blockCenter) {
        Vec3 offset = hitPosition.subtract(blockCenter);
        if (offset.x >= VANILLA_HIT_LIMIT) {
            return new Vec3(0.0D, offset.y, offset.z);
        }
        return offset;
    }
}
