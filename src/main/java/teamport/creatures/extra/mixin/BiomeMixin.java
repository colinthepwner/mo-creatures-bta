package teamport.creatures.extra.mixin;

import net.minecraft.core.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamport.creatures.core.MMSpawns;

/**
 * Puts this mod's mobs on every biome as it is built.
 *
 * <p>The table itself lives in {@link MMSpawns}, because this injection is not the last word on it:
 * a {@code Biome} subclass's own constructor runs after this point and may clear the spawn lists it
 * inherited, which takes these entries with it. {@link MMSpawns#sweep()} runs after the game starts
 * and puts back anything that happened to.
 */
@Mixin(value = Biome.class, remap = false)
public abstract class BiomeMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void creatures_addMobs(CallbackInfo ci) {
		MMSpawns.apply((Biome) (Object) this);
	}
}
