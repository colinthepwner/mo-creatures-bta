package teamport.creatures.extra.mixin;

import net.minecraft.core.enums.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import teamport.creatures.MMConfig;

/**
 * Makes the three spawn caps configurable, which is the original's "Spawn Limits" panel.
 *
 * <p>{@code MobCategory} holds {@code maxPerChunk} as a {@code private final int} set in the enum
 * constructor, so there is nothing to assign at startup — the only way in is the getter, which
 * {@link net.minecraft.core.world.SpawnerMobs} consults on every spawn pass as
 * {@code countEntities(category) > cap * eligibleChunks / 256}.
 *
 * <p>This deliberately reaches past this mod's own mobs: BTA counts every entity assignable to the
 * category's base class, so the cap is global and raising it lifts cows and squid along with bunnies
 * and dolphins. That is what the original did and what it needed to do — this mod roughly doubles the
 * passive spawn pool, so under BTA's stock cap of 15 the new animals would displace the vanilla ones
 * at unchanged total density instead of adding to it. Setting {@code SpawnLimits} back to BTA's own
 * 70 / 15 / 5 restores stock behaviour exactly.
 *
 * <p>{@link MobCategory#AMBIENT} is left alone: the original had no setting for it, and butterflies
 * and fireflies are BTA's own.
 */
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
