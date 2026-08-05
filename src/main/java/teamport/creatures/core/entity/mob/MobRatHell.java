package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.Dimension;
import net.minecraft.core.world.World;

/**
 * The hell rat: bigger, tougher, fireproof, and — because its coat is the same colour as the rock —
 * very hard to see coming. Nether only, and it drops redstone instead of coal.
 * <p>
 * Everything else, including the swarm call and the wall climbing, is inherited from {@link MobRat}.
 * It has a single coat, so the variant slot is pinned to {@link MobRat#VARIANT_BROWN} rather than
 * rolled; the textures resolve out of its own folder.
 */
public class MobRatHell extends MobRat {
	public MobRatHell(World world) {
		super(world);
		scoreValue = 100;
		attackStrength = 2;
		fireImmune = true;

		setSize(0.7F, 0.7F);
		setHealthRaw(getMaxHealth());

		mobDrops.clear();
		mobDrops.add(new WeightedRandomLootObject(Items.DUST_REDSTONE.getDefaultStack(), 0, 2));
	}

	@Override
	protected String textureFolder() {
		return "rat_hell";
	}

	/** One coat only. */
	@Override
	protected int rollVariant() {
		return VARIANT_BROWN;
	}

	@Override
	public int getMaxHealth() {
		return 20;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/rat_hell/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/rat_hell/0.png";
	}

	/**
	 * {@link MobRat}'s "only hunt in the dark" rule is a rule about surface rats coming out at night.
	 * Down here it would just leave hell rats standing about next to the lava, so they hunt regardless
	 * of light.
	 */
	@Override
	protected Entity findPlayerToAttack() {
		Player player = world.getClosestPlayerToEntity(this, 16.0);
		if (player != null && !player.gamemode.hasHostileMobs()) return null;
		return player;
	}

	@Override
	public boolean canSpawnHere() {
		return world.dimension == Dimension.NETHER && super.canSpawnHere();
	}
}
