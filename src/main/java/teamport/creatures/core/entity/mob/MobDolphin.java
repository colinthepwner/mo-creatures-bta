package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.UUIDHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMRiderControl;
import teamport.creatures.core.MMUtils;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class MobDolphin extends MobAquaticBase {
	public static final int MASK_TAMED = 0b0000_0001;
	public static final int MASK_BRED = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 17;
	public static final int DATA_OWNER_UUID = 18;

	public static final int COLOURS = 6;

	private static final int GENTLE_STEP = 25;
	private static final float ADULT_GROWTH = 1.5F;
	private static final float BABY_GROWTH = 0.35F;

	private static final int GESTATION = 50;

	private int temper;

	private boolean fedForBreeding;

	private boolean hungry;
	private int gestation;

	private final MMRiderControl riderControl = new MMRiderControl();

	public MobDolphin(World world) {
		super(world);
		textureIdentifier = new NamespaceID(MoreMobs.MOD_ID, "dolphin");
		scoreValue = 120;

		setSize(1.5F, 0.8F);
		setDolphinColour(rollColour(random));
		setGrowth(0.8F + random.nextFloat() * (ADULT_GROWTH - 0.8F));

		mobDrops.add(new WeightedRandomLootObject(Items.FOOD_FISH_RAW.getDefaultStack(), 1, 2));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
		entityData.define(DATA_OWNER_UUID, null, UUID.class);
	}

	public static int rollColour(Random random) {
		int roll = random.nextInt(100);
		if (roll < 36) return 0;
		if (roll < 61) return 1;
		if (roll < 86) return 2;
		if (roll < 97) return 3;
		if (roll < 99) return 4;
		return 5;
	}

	public int getDolphinColour() {
		return Math.floorMod(getSkinVariant(), COLOURS);
	}

	public void setDolphinColour(int colour) {
		setSkinVariant(Math.floorMod(colour, COLOURS));

		temper = (getDolphinColour() + 1) * 50;
	}

	public int getGeneticValue() {
		return getDolphinColour() + 1;
	}

	private int inheritedColour(MobDolphin partner) {
		if (getDolphinColour() == partner.getDolphinColour()) {
			return getDolphinColour();
		}

		int sum = getGeneticValue() + partner.getGeneticValue();
		if (sum <= 4 && random.nextInt(3) == 0) {
			return sum - 1;
		}
		if ((sum == 5 || sum == 6) && random.nextInt(10) == 0) {
			return sum - 1;
		}
		return rollColour(random);
	}

	public boolean isDolphinTamed() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_TAMED) != 0;
	}

	public void setDolphinTamed(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		if (flag) {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data | MASK_TAMED));
		} else {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data & ~MASK_TAMED));
		}
	}

	public boolean isDolphinBred() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_BRED) != 0;
	}

	public void setDolphinBred(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		if (flag) {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data | MASK_BRED));
		} else {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data & ~MASK_BRED));
		}
	}

	public UUID getDolphinOwner() {
		return entityData.getUUID(DATA_OWNER_UUID);
	}

	public void setDolphinOwner(UUID uuid) {
		entityData.set(DATA_OWNER_UUID, uuid);
	}

	public boolean isOwnedBy(Player player) {
		return isDolphinTamed() && Objects.equals(getDolphinOwner(), player.uuid);
	}

	public int getTemper() {
		return temper;
	}

	private void tame(Player owner) {
		setDolphinTamed(true);
		setDolphinOwner(owner.uuid);
		temper = 0;

		for (int i = 0; i < 8; i++) {
			world.spawnParticle("heart",
				x + random.nextDouble() - 0.5,
				y + 0.5 + random.nextDouble(),
				z + random.nextDouble() - 0.5,
				0.0, 0.2, 0.0, 0, false);
		}
	}

	private void buckOff(Player rider) {
		rider.yd += 0.9;
		rider.zd -= 0.3;
		ejectRider();
		world.playSoundAtEntity(null, this, "creatures:mob.dolphin.upset", getSoundVolume(), soundPitch());
	}

	private float soundPitch() {
		return (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F;
	}

	@Override
	public boolean interact(@NotNull Player player) {

		super.interact(player);

		ItemStack held = player.getHeldItem();

		if (!world.isClientSide && held != null) {
			if (held.itemID == Items.FOOD_FISH_RAW.id) {
				held.consumeItem(player);
				eatFish(player);
				return true;
			}
			if (held.itemID == Items.FOOD_FISH_COOKED.id && isDolphinTamed() && isAdult()) {
				held.consumeItem(player);
				heal(getMaxHealth());
				fedForBreeding = true;
				world.playSoundAtEntity(null, this, "creatures:mob.dolphin", getSoundVolume(), soundPitch());
				return true;
			}
		}

		if (held == null && isAdult()) {

			player.startRiding(this);
			return true;
		}

		return false;
	}

	private void eatFish(Player feeder) {
		heal(15);
		hungry = false;
		world.playSoundAtEntity(null, this, "creatures:mob.dolphin", getSoundVolume(), soundPitch());

		if (!isAdult()) {
			setGrowth(getGrowth() + growthStep());
		}
		if (isDolphinTamed()) {
			return;
		}

		temper -= GENTLE_STEP;
		if (temper <= 0) {
			tame(feeder);
		}
	}

	@Override
	public float adultGrowth() {
		return ADULT_GROWTH;
	}

	@Override
	public float babyGrowth() {
		return BABY_GROWTH;
	}

	@Override
	protected float swimThrust() {
		return 0.026F + 0.006F * getDolphinColour();
	}

	@Override
	protected int targetSearchInterval() {
		return 50;
	}

	@Override
	protected Entity findSwimTarget() {

		if (!isAdult() || !MMConfig.dolphinsAttackSharks) {
			return null;
		}

		MobShark nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (MobShark shark : world.getEntitiesWithinAABB(MobShark.class, MMUtils.grow(bb, 12.0, 12.0, 12.0))) {
			double distance = distanceToSqr(shark);
			if (shark.isAlive() && distance < nearestDistance) {
				nearest = shark;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	@Override
	protected boolean steerSpecial() {
		if (isDolphinTamed() && !hungry) {
			return false;
		}

		EntityItem fish = closestDroppedFish();
		if (fish == null) {
			return false;
		}

		swimToward(fish, 1.0F);
		if (distanceTo(fish) < 1.5F) {
			fish.remove();
			hungry = false;
			heal(getMaxHealth());
			world.playSoundAtEntity(null, this, "creatures:mob.dolphin", getSoundVolume(), soundPitch());
			if (!isDolphinTamed()) {
				temper -= GENTLE_STEP;
			}
		}
		return true;
	}

	private EntityItem closestDroppedFish() {
		EntityItem nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, MMUtils.grow(bb, 12.0, 8.0, 12.0))) {
			if (item.item == null || item.item.itemID != Items.FOOD_FISH_RAW.id) {
				continue;
			}
			double distance = distanceToSqr(item);
			if (distance < nearestDistance) {
				nearest = item;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	@Override
	protected void updateAI() {
		if (passenger instanceof Player) {

			tickGrowth();
			climb = 0.0;
			if (!isDolphinTamed()) {
				tickRodeo((Player) passenger);
			}
			if (!world.isClientSide) {
				riderControl.apply(this);
			}
			return;
		}

		super.updateAI();

		if (!hungry && random.nextInt(100) == 0) {
			hungry = true;
		}
		tickBreeding();
	}

	private void tickRodeo(Player rider) {
		if (random.nextInt(5) == 0) {
			yd += 0.4;
		}
		if (random.nextInt(10) == 0) {
			xd += (random.nextDouble() - random.nextDouble()) / 15.0;
			zd += (random.nextDouble() - random.nextDouble()) / 15.0;
		}
		if (random.nextInt(50) == 0) {
			buckOff(rider);
			return;
		}
		if (random.nextInt(Math.max(temper, 1) * 8) == 0) {
			tame(rider);
		}
	}

	private void tickBreeding() {
		if (!isReadyToBreed(this)) {
			return;
		}

		List<MobDolphin> neighbours = world.getEntitiesWithinAABB(MobDolphin.class, MMUtils.grow(bb, 8.0, 2.0, 8.0));
		if (neighbours.size() > 2) {
			return;
		}

		for (MobDolphin partner : world.getEntitiesWithinAABB(MobDolphin.class, MMUtils.grow(bb, 4.0, 2.0, 4.0))) {
			if (partner == this || !isReadyToBreed(partner)) {
				continue;
			}

			if (random.nextInt(100) == 0) {
				gestation++;
			}
			if (gestation <= GESTATION) {
				return;
			}

			MobDolphin calf = new MobDolphin(world);
			calf.moveTo(x, y, z, yRot, 0.0F);
			calf.setDolphinColour(inheritedColour(partner));
			calf.setGrowth(BABY_GROWTH);
			calf.setDolphinBred(true);
			world.entityJoinedWorld(calf);

			fedForBreeding = false;
			partner.fedForBreeding = false;
			gestation = 0;
			partner.gestation = 0;

			world.playSoundAtEntity(null, this, "creatures:mob.dolphin", getSoundVolume(), soundPitch());
			return;
		}
	}

	private static boolean isReadyToBreed(MobDolphin dolphin) {
		return dolphin.isDolphinTamed()
			&& dolphin.isAdult()
			&& dolphin.fedForBreeding
			&& dolphin.passenger == null
			&& !dolphin.isPassenger();
	}

	@Override
	public void moveEntityWithHeading(float strafe, float forward) {
		if (isInWater() && isDolphinTamed() && passenger instanceof Player) {
			Player rider = (Player) passenger;
			yRot = rider.yRot;
			yBodyRot = yRot;
			xRot = rider.xRot * 0.5F;

			if (rider instanceof PlayerLocal) {
				PlayerInput input = ((PlayerLocal) rider).input;

				climb = -MathHelper.sin(rider.xRot * MathHelper.DEG_TO_RAD) * input.moveForward * 0.08;
				if (input.jump) {
					climb += 0.05;
				}
				climb = MathHelper.clamp(climb, -MAX_CLIMB * 2.0, MAX_CLIMB * 2.0);

				super.moveEntityWithHeading(input.moveStrafe * 0.5F, input.moveForward);
				return;
			}
		}

		super.moveEntityWithHeading(strafe, forward);
	}

	@Override
	public void handleControlDirect(double x, double y, double z, float yaw) {
		riderControl.accept(this, x, y, z, yaw);
	}

	@Override
	public boolean relaysVehicleControl() {
		return true;
	}

	@Override
	public double getRideHeight() {
		return bbHeight * 0.7;
	}

	@Override
	public float getYRotDelta() {
		return 0;
	}

	@Override
	public float getXRotDelta() {
		return 0;
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (distance >= 3.5F || !isAdult()) {
			return;
		}
		if (entity.bb.maxY <= bb.minY || entity.bb.minY >= bb.maxY) {
			return;
		}

		attackTime = 20;
		entity.hurt(this, 5, DamageType.COMBAT);
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!super.hurt(attacker, damage, type)) {
			return false;
		}

		if (attacker instanceof Mob && attacker != this && attacker != passenger) {
			setTarget(attacker);
		}
		return true;
	}

	@Override
	public int getMaxHealth() {
		return 30;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/dolphin/" + getDolphinColour() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/dolphin/0.png";
	}

	@Override
	public String getLivingSound() {
		return "creatures:mob.dolphin";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.dolphin.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.dolphin.death";
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	protected boolean canDespawn() {
		return super.canDespawn() && !isDolphinTamed() && !isDolphinBred();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 3;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Tamed", isDolphinTamed());
		tag.putBoolean("Bred", isDolphinBred());
		tag.putInt("Colour", getDolphinColour());
		tag.putInt("Temper", temper);
		tag.putBoolean("FedForBreeding", fedForBreeding);
		tag.putBoolean("Hungry", hungry);
		tag.putInt("Gestation", gestation);

		UUIDHelper.writeToTag(tag, getDolphinOwner(), "OwnerUUID");
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.containsKey("Colour")) {
			setDolphinColour(tag.getInteger("Colour"));
		}
		setDolphinTamed(tag.getBoolean("Tamed"));
		setDolphinBred(tag.getBoolean("Bred"));

		if (tag.containsKey("Temper")) {
			temper = tag.getInteger("Temper");
		}
		fedForBreeding = tag.getBoolean("FedForBreeding");
		hungry = tag.getBoolean("Hungry");
		gestation = tag.getInteger("Gestation");

		UUID owner = UUIDHelper.readFromTag(tag, "OwnerUUID");
		if (owner != null) {
			setDolphinOwner(owner);
			setDolphinTamed(true);
		}
	}
}
