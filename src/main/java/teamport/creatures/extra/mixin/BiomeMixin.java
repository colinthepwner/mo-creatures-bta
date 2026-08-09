package teamport.creatures.extra.mixin;

import net.minecraft.core.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamport.creatures.core.MMSpawns;

@Mixin(value = Biome.class, remap = false)
public abstract class BiomeMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void creatures_addMobs(CallbackInfo ci) {
		MMSpawns.apply((Biome) (Object) this);
	}
}
