package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;

/**
 * The fire ogre. Same brute as {@link MobOgre}, but it leaves what it smashes burning, sets whoever
 * it hits alight, and is itself immune to fire — which is why daylight docks its health directly
 * rather than igniting it.
 * <p>
 * The original spawned on Normal or harder ("Fire ogres now spawn on Normal difficulty") and dropped
 * what the readme calls bloodstone; netherrack is the obvious BTA equivalent.
 */
public class MobOgreFire extends MobOgre {
	/** Ticks of fire applied to whatever the ogre connects with. */
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
	public boolean canSpawnHere() {
		Difficulty difficulty = world.getDifficulty();
		return (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) && super.canSpawnHere();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 2;
	}
}
