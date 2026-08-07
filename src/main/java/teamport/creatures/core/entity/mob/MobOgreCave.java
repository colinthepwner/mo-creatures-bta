package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

/**
 * The cave ogre: tougher than its green cousin, and strictly a thing of the deep.
 * <p>
 * The original gave it 50 health against the others' 35, kept it underground ("cave ogres no longer
 * spawn outdoors", "Cave Ogres no longer spawn on dark areas of the surface") by requiring both no
 * line of sight to the sky and a low y, and still had it burn if it ever surfaced in daylight.
 */
public class MobOgreCave extends MobOgre {
	/**
	 * Highest y a cave ogre will spawn at.
	 *
	 * <p>The original's {@code EntityCaveOgre.getCanSpawnHere} used a flat {@code posY < 50}, and this
	 * port had been carrying that number over unchanged. It does not survive the move: beta 1.7.3 is
	 * 128 blocks tall with its sea at 64, so 50 meant a mere fourteen blocks under water level — the
	 * top of the cave layer. BTA doubled both ({@code World.HEIGHT_BLOCKS} is 256 and the sea sits at
	 * 128, measured), so a literal 50 asks for seventy-eight blocks below sea level and pins cave
	 * ogres to the bottom fifth of the world. In the original's own terms that is {@code posY < 25}.
	 *
	 * <p>Doubled to match the world it now lives in, which puts it back at fourteen blocks below the
	 * waterline.
	 */
	public static final int MAX_SPAWN_HEIGHT = 100;

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
	protected float blastCeiling() {
		return MMConfig.blastCeiling(MMConfig.caveOgreStrength);
	}

	@Override
	protected Difficulty spawnDifficulty() {
		return MMConfig.caveOgreSpawnDifficulty;
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
