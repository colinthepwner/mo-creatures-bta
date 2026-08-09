package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

public class MobOgreFire extends MobOgre {

	public static final int BURN_TICKS_ON_HIT = 30;

	public MobOgreFire(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, "ogre_fire");

		fireImmune = true;
		smashSetsFire = true;
		burnsInDaylight = true;
		scoreValue = 400;

		mobDrops.clear();
		mobDrops.add(new WeightedRandomLootObject(Blocks.NETHERRACK.getDefaultStack(), 1, 2));
	}

	@Override
	protected String textureFolder() {
		return "ogre_fire";
	}

	@Override
	protected void onHitTarget(@NotNull Entity entity) {
		entity.remainingFireTicks = Math.max(entity.remainingFireTicks, BURN_TICKS_ON_HIT);
	}

	@Override
	protected float blastCeiling() {
		return MMConfig.blastCeiling(MMConfig.fireOgreStrength);
	}

	@Override
	protected Difficulty spawnDifficulty() {
		return MMConfig.fireOgreSpawnDifficulty;
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 2;
	}
}
