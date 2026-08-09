package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

public class MobOgreCave extends MobOgre {

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
