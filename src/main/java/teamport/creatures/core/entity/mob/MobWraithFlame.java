package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;

/**
 * The flame wraith. Tougher than the plain {@link MobWraith}, wreathed in fire, and — since the fix
 * noted in the original's changelog, "FlameWraiths and FireOgres are now fire resistant" — immune to
 * it. Because it cannot burn, daylight has to hurt it directly instead of setting it alight.
 * <p>
 * The original restricted it to Hard, alongside the fire ogre, and also let it spawn in the Nether.
 * Only the difficulty gate is enforced here; which dimensions it appears in is a spawn-list decision
 * and belongs with registration.
 */
public class MobWraithFlame extends MobWraith {
	/** Ticks of fire it leaves on whatever it touches. */
	public static final int BURN_TICKS_ON_HIT = 30;
	/** Health lost per daylight tick that gets through. */
	public static final int DAYLIGHT_DAMAGE = 2;
	/** Chance per tick of a cosmetic flicker of flame. */
	public static final int FLICKER_CHANCE = 40;

	public MobWraithFlame(World world) {
		super(world);
		setTextureIdentifier(MoreMobs.MOD_ID, "wraith_flame");

		fireImmune = true;
		attackStrength = 2;
		scoreValue = 300;

		mobDrops.clear();
		mobDrops.add(new WeightedRandomLootObject(Items.COAL.getDefaultStack(), 1, 2));
	}

	@Override
	protected String textureFolder() {
		return "wraith_flame";
	}

	@Override
	public int getMaxHealth() {
		return 15;
	}

	@Override
	public void onLivingUpdate() {
		if (!world.isClientSide && random.nextInt(FLICKER_CHANCE) == 0) {
			// Purely for the look: it is fire immune, so this only ever draws flames.
			remainingFireTicks = Math.max(remainingFireTicks, 2);
		}
		super.onLivingUpdate();
	}

	/** Its touch is weaker than a plain wraith's; the burn it leaves behind does the real work. */
	@Override
	protected void updateAttackStrength() {
		attackStrength = 2;
	}

	/** Fire cannot touch it, so the sun burns it out from the inside instead. */
	@Override
	protected void handleDaylight() {
		if (!world.isDaytime()) {
			return;
		}

		float brightness = calcBrightness(1.0F);
		if (brightness <= 0.5F) {
			return;
		}
		if (!world.canBlockSeeTheSky(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z))) {
			return;
		}
		if (random.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F) {
			hurt(null, DAYLIGHT_DAMAGE, DamageType.FIRE);
		}
	}

	@Override
	protected void onHitTarget(@NotNull Entity entity) {
		entity.remainingFireTicks = Math.max(entity.remainingFireTicks, BURN_TICKS_ON_HIT);
	}

	/**
	 * {@code HostileMobs.flameWraithSpawnDifficulty} — the original's
	 * {@code flameWraithSpawnDifficulty}, which defaults to Hard and so leaves this where it was.
	 */
	@Override
	public boolean canSpawnHere() {
		return MMConfig.spawnsAt(world.getDifficulty(), MMConfig.flameWraithSpawnDifficulty)
			&& super.canSpawnHere();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}
}
