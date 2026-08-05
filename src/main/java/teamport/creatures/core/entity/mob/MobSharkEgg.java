package teamport.creatures.core.entity.mob;

import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;
import teamport.creatures.MoreMobs;

/**
 * A shark's egg case. Dropped by a well-grown shark when it dies and hatches after long enough
 * underwater.
 * <p>
 * The pup comes out tame, which is the only route to a tame shark: there is no way to gentle a wild
 * one. It hatches at {@link MobShark#babyGrowth()} and grows from there, so there is a long stretch
 * where it is far too small to bite anything.
 */
public class MobSharkEgg extends MobAquaticEggBase {
	public MobSharkEgg(World world) {
		super(world);
		textureIdentifier = new NamespaceID(MoreMobs.MOD_ID, "shark_egg");
	}

	@Override
	protected Mob hatch() {
		MobShark pup = new MobShark(world);
		pup.setGrowth(pup.babyGrowth());
		pup.setSharkTamed(true);
		return pup;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/shark_egg/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/shark_egg/0.png";
	}
}
