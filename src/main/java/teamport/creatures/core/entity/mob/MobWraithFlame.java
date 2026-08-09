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

public class MobWraithFlame extends MobWraith {

	public static final int BURN_TICKS_ON_HIT = 30;

	public static final int DAYLIGHT_DAMAGE = 2;

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

			remainingFireTicks = Math.max(remainingFireTicks, 2);
		}
		super.onLivingUpdate();
	}

	@Override
	protected void updateAttackStrength() {
		attackStrength = 2;
	}

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
