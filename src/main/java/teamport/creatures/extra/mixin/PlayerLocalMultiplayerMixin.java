package teamport.creatures.extra.mixin;

import net.minecraft.client.entity.player.PlayerLocalMultiplayer;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.net.packet.PacketVehicleControl;
import net.minecraft.core.world.IVehicle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamport.creatures.core.entity.mob.MobDolphin;
import teamport.creatures.core.entity.mob.MobHorse;

@Mixin(value = PlayerLocalMultiplayer.class, remap = false)
public abstract class PlayerLocalMultiplayerMixin {

	@Inject(method = "sendSpecialVehiclePacket", at = @At("HEAD"))
	private void creatures_relayMountPosition(CallbackInfo ci) {
		PlayerLocalMultiplayer self = (PlayerLocalMultiplayer) (Object) this;
		IVehicle riding = self.vehicle;
		if (!(riding instanceof MobHorse) && !(riding instanceof MobDolphin)) {
			return;
		}

		Entity mount = (Entity) riding;

		self.sendQueue.addToSendQueue(new PacketVehicleControl(
			mount.id, mount.x, mount.bb.minY + mount.heightOffset, mount.z,
			mount.yRot, mount.fallDistance));
	}
}
