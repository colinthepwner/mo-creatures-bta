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

public class MobDeerMoC extends MobAnimal {
	public static final int MASK_BUCK = 0b0000_0001;
	public static final int MASK_SCARED = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 16;

	public final List<WeightedRandomLootObject> burningMobDrops = new ArrayList<>();

	private int scaredTick;

	public MobDeerMoC(World world) {
		super(world);

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
