package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Mo' Creatures' deer.
 * <p>
 * Named {@code MobDeerMoC} rather than {@code MobDeer} because BTA 8.0.1 ships its own
 * {@link net.minecraft.core.entity.animal.MobDeer}; this one replaces it in the biome spawn lists
 * (see {@code BiomeMixin}), so both classes are on the classpath at once and an unqualified name
 * clash would be needlessly confusing.
 * <p>
 * Buck/doe and the spooked state live in {@code entityData} rather than plain fields: the renderer
 * picks the texture and hides the antlers off the buck flag, and plain fields never leave the
 * server, so the client would render every deer as a doe.
 */
public class MobDeerMoC extends MobAnimal {
	public static final int MASK_BUCK = 0b0000_0001;
	public static final int MASK_SCARED = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 16;

	/** Swapped in for {@link #mobDrops} when the deer dies on fire. */
	public final List<WeightedRandomLootObject> burningMobDrops = new ArrayList<>();

	private int scaredTick;

	public MobDeerMoC(World world) {
		super(world);
		// The original's own box. What was here before was 0.3 wide and 2.0 tall -- a deer that
		// could walk through its own antlers and stood in a column narrower than one leg.
		setSize(0.9F, 1.3F);

		if (random.nextInt(2) == 0) setBuck(true);

		mobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_RAW.getDefaultStack(), 1, 2));
		burningMobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_COOKED.getDefaultStack(), 1, 2));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
	}

	public boolean isBuck() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_BUCK) != 0;
	}

	public void setBuck(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_BUCK) : (byte) (data & ~MASK_BUCK));
	}

	public boolean isScared() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_SCARED) != 0;
	}

	public void setScared(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		entityData.set(DATA_GENERIC_FLAGS, flag ? (byte) (data | MASK_SCARED) : (byte) (data & ~MASK_SCARED));
	}

	@Override
	public String getEntityTexture() {
		return isBuck() ? "/assets/creatures/textures/entity/deer/b_0.png" : "/assets/creatures/textures/entity/deer/f_0.png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/deer/b_0.png";
	}

	@Override
	public String getLivingSound() {
		// The original mod shipped a separate buck grunt (deerbgrunt) and doe grunt (deerfgrunt);
		// the buck's is the deeper bellow, so it follows the same flag the renderer picks antlers off.
		return isBuck() ? "creatures:mob.deer.buck" : "creatures:mob.deer";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.deer.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.deer.death";
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		Player player = world.getClosestPlayerToEntity(this, 16.0);

		// Just a simple flee system;
		// Checks if the player isn't null, isn't sneaking, and isn't in creative
		// or if it's in a fear state
		if (player != null && !player.isSneaking() && player.gamemode.hasHostileMobs()) {
			lookAt(player, 0.0F, 0.0F);

			moveSpeed = 0.2f;
			moveForward = 1.0F;
		} else if (isScared()) {
			moveSpeed = 0.2f;
			roamRandomPath();
		} else {
			moveSpeed = 0.1f;
		}

		// Inherited from the 7.2 entity: the flag latches on and is never cleared again, so a deer
		// that has been hurt once stays jumpy for the rest of its life.
		if (scaredTick > 0) {
			setScared(true);
			scaredTick--;
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		scaredTick = 1200;
		return super.hurt(attacker, damage, type);
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("ScaredTick", scaredTick);
		tag.putBoolean("IsBuck", isBuck());
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		scaredTick = tag.getInteger("ScaredTick");
		setBuck(tag.getBoolean("IsBuck"));
	}

	@Override
	protected List<WeightedRandomLootObject> getMobDrops() {
		return remainingFireTicks > 0 ? burningMobDrops : mobDrops;
	}
}
