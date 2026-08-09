package teamport.creatures.extra.mixin;

import net.minecraft.core.enums.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import teamport.creatures.MMConfig;

@Mixin(value = MobCategory.class, remap = false)
public abstract class MobCategoryMixin {

	@Inject(method = "getMaxCreaturesPerChunk", at = @At("HEAD"), cancellable = true)
	private void creatures_configurableSpawnCap(CallbackInfoReturnable<Integer> cir) {
		MobCategory self = (MobCategory) (Object) this;

		if (self == MobCategory.MONSTER) {
			cir.setReturnValue(MMConfig.maxHostiles);
		} else if (self == MobCategory.CREATURE) {
			cir.setReturnValue(MMConfig.maxAnimals);
		} else if (self == MobCategory.WATER_CREATURE) {
			cir.setReturnValue(MMConfig.maxWaterMobs);
		}
	}
}
