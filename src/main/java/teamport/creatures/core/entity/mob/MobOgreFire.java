package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

/**
 * The fire ogre. Same brute as {@link MobOgre}, but it leaves what it smashes burning, sets whoever
 * it hits alight, and is itself immune to fire — which is why daylight docks its health directly
 * rather than igniting it.
 * <p>
 * It drops what the readme calls bloodstone; netherrack is the obvious BTA equivalent.
 * <p>
 * <b>Spawns on Hard by default now, where this port had it on Normal.</b> "Fire ogres now spawn on
 * Normal difficulty" is a line from the 2.x changelog, and this port took it at face value — but it
 * describes a release well before the one being matched here. v2.12.2 makes the difficulty a setting
 * and ships it at Hard, so that is what {@code HostileMobs.fireOgreSpawnDifficulty} defaults to.
 * Setting it back to {@code normal} restores the old behaviour.
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
