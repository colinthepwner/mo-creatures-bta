package teamport.creatures.extra.mixin;

import net.minecraft.core.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Mob.class, remap = false)
public interface MobScoreAccessor {
	@Accessor("scoreValue")
	int creatures$getScoreValue();
}
