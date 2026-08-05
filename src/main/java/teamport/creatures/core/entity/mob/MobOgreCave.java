package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import teamport.creatures.MoreMobs;

/**
 * The cave ogre: tougher than its green cousin, and strictly a thing of the deep.
 * <p>
 * The original gave it 50 health against the others' 35, kept it underground ("cave ogres no longer
 * spawn outdoors", "Cave Ogres no longer spawn on dark areas of the surface") by requiring both no
 * line of sight to the sky and a low y, and still had it burn if it ever surfaced in daylight.
 */
public class MobOgreCave extends MobOgre {
	/** Highest y a cave ogre will spawn at — the original used a flat 50. */
	public static final int MAX_SPAWN_HEIGHT = 50;

	public MobOgreCave(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, "ogre_cave");

		burnsInDaylight = true;
		attackStrength = 4;
		scoreValue = 500;

		mobDrops.clear();
		mobDrops.add(new WeightedRandomLootObject(Items.COAL.getDefaultStack(), 1, 3));
	}

	@Override
	protected String textureFolder() {
		return "ogre_cave";
	}

	@Override
	public int getMaxHealth() {
		return 50;
	}

	@Override
	public boolean canSpawnHere() {
		int mhX = MathHelper.floor(x);
		int mhY = MathHelper.floor(bb.minY);
		int mhZ = MathHelper.floor(z);

		if (mhY >= MAX_SPAWN_HEIGHT || world.canBlockSeeTheSky(mhX, mhY, mhZ)) {
			return false;
		}
		return super.canSpawnHere();
	}
}
