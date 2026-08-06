package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.UUIDHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;

import java.util.Objects;
import java.util.UUID;

/**
 * Base for the three rideable horses. Taming works the way it always has in Mo' Creatures: feeding
 * raises the odds, riding an untamed horse builds a tame counter while the horse's temper builds in
 * parallel, and whichever fills first either tames the horse or throws the rider off.
 * <p>
 * The 7.2 source kept tamed/saddled/owner in plain fields. Those are read on the client (texture,
 * rider control) so they now live in {@code entityData} like the mod's other tameable mobs, and the
 * skin variant uses BTA 8.0's own {@code DATA_VARIANT} slot, which the base class already syncs and
 * persists.
 * <p>
 * The per-mob taming/temper constants used to be duplicated into every subclass' {@code updateAI}
 * on top of a {@code super.updateAI()} call, so the base values always fired first and the subclass
 * tuning never took effect. They are protected hooks now instead.
 */
public class MobHorse extends MobAnimal {
	public static final int MASK_TAMED = 0b0000_0001;
	public static final int MASK_SADDLED = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 16;
	public static final int DATA_OWNER_UUID = 17;

	protected int annoyance = 0;
	protected int chanceForTame = 0;
	protected int tameCounter = 0;

	public MobHorse(World world) {
		super(world);
		textureIdentifier = new NamespaceID(MoreMobs.MOD_ID, "horse");
		setSkinVariant(random.nextInt(3));

		// The original's own box: a horse is wider than it is tall in plan, not a 2-block pillar.
		setSize(1.4F, 1.6F);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
		entityData.define(DATA_OWNER_UUID, null, UUID.class);
	}

	@Override
	public int getMaxHealth() {
		return 20;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/horse/" + (getSkinVariant() % 3) + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/horse/0.png";
	}

	public boolean isHorseTamed() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_TAMED) != 0;
	}

	public void setHorseTamed(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		if (flag) {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data | MASK_TAMED));
		} else {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data & ~MASK_TAMED));
		}
	}

	public boolean isHorseSaddled() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_SADDLED) != 0;
	}

	public void setHorseSaddled(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		if (flag) {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data | MASK_SADDLED));
		} else {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data & ~MASK_SADDLED));
		}
	}

	public UUID getHorseOwner() {
		return entityData.getUUID(DATA_OWNER_UUID);
	}

	public void setHorseOwner(UUID uuid) {
		entityData.set(DATA_OWNER_UUID, uuid);
	}

	public boolean isOwnedBy(Player player) {
		return isHorseTamed() && Objects.equals(getHorseOwner(), player.uuid);
	}

	/** Temper gained per successful roll while an untamed rider is aboard. */
	protected int annoyanceRate() {
		return 20;
	}

	/** Temper at which the horse bucks its rider off. */
	protected int annoyanceLimit() {
		return 300;
	}

	/** Multiplier applied to the accumulated feeding bonus while an untamed rider is aboard. */
	protected int tameRate() {
		return 20;
	}

	/** Tame counter that has to be reached before the horse gives in. */
	protected int tameThreshold() {
		return 1000;
	}

	/** Upward kick given to a rider that gets thrown off. */
	protected double buckStrength() {
		return 0.75;
	}

	protected int tameParticleCount() {
		return 8;
	}

	protected boolean isFollowItem(ItemStack stack) {
		return stack.itemID == Items.WHEAT.id || stack.itemID == Items.DUST_SUGAR.id;
	}

	protected void playEatingSound() {
		world.playSoundAtEntity(null,
			this,
			"creatures:mob.horse.eat",
			1.0f,
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
	}

	@Override
	public boolean interact(@NotNull Player player) {
		super.interact(player);
		ItemStack item = player.inventory.getCurrentItem();
		if (item != null) {
			if (!isHorseTamed()) {
				if (item.itemID == Items.WHEAT.id) {
					chanceForTame += 1;
					item.consumeItem(player);
					playEatingSound();
				}
				if (item.itemID == Items.FOOD_APPLE.id) {
					chanceForTame += random.nextInt(4) + 1;
					item.consumeItem(player);
					playEatingSound();
				}
				if (item.itemID == Items.DUST_SUGAR.id) {
					chanceForTame += random.nextInt(8) + 1;
					item.consumeItem(player);
					playEatingSound();
				}
			}

			if (isOwnedBy(player)) {
				if (item.itemID == Items.SADDLE.id) {
					setHorseSaddled(true);
					item.consumeItem(player);
				}

				if (getHealth() < getMaxHealth()) {
					if (item.itemID == Items.WHEAT.id) {
						heal(2);
						item.consumeItem(player);
						playEatingSound();
					}
					if (item.itemID == Items.FOOD_APPLE.id) {
						heal(4);
						item.consumeItem(player);
						playEatingSound();
					}
				}
			}
		} else {
			player.startRiding(this);
		}
		return false;
	}

	/** Throws the rider clear and lets everyone within earshot know about it. */
	protected void buckOff(Player rider) {
		rider.yd += buckStrength();
		rider.xd -= yRot * 0.0015F;
		ejectRider();
		world.playSoundAtEntity(null,
			this,
			"creatures:mob.horse.mad",
			getSoundVolume(),
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
	}

	protected void tame(Player rider) {
		setHorseTamed(true);
		setHorseOwner(rider.uuid);

		for (int i = 0; i < tameParticleCount(); i++) {
			double randX = x + random.nextDouble();
			double randY = y + random.nextDouble();
			double randZ = z + random.nextDouble();

			world.spawnParticle("heart", randX, randY + 0.22, randZ, 0.0, 0.2, 0.0, 0, false);
		}
	}

	/** One tick of the tug-of-war between the horse's temper and the rider's persistence. */
	protected void tickUntamedRider(Player rider) {
		if (random.nextInt(6) == 0) {
			annoyance += annoyanceRate();
		}
		if (random.nextInt(10) == 0) {
			tameCounter += tameRate() * chanceForTame;
		}

		if (annoyance >= annoyanceLimit()) {
			annoyance = 0;
			buckOff(rider);
		}

		if (tameCounter++ >= tameThreshold()) {
			tame(rider);
		}
	}

	/** Trails after a nearby player holding something the horse likes the look of. */
	protected void followPlayerHoldingItem() {
		Player player = world.getClosestPlayerToEntity(this, 16.0);
		if (player != null && (player.distanceToSqr(x, y, z) > 4.0)) {
			ItemStack heldStack = player.getCurrentEquippedItem();
			if (heldStack != null && isFollowItem(heldStack)) {
				lookAt(player, 30.0F, 30.0F);
				moveForward = 1.0F;

				if (player.distanceToSqr(this) <= 12.0)
					moveForward = 0.0F;
			}
		}
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		if (passenger instanceof Player && !isHorseTamed()) {
			tickUntamedRider((Player) passenger);
		}

		followPlayerHoldingItem();
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		if (passenger != null) {
			if (isInWater() || isInLava()) ejectRider();

			if (isHorseSaddled()) {
				if (passenger instanceof PlayerLocal) {
					PlayerInput passengerInput = ((PlayerLocal) passenger).input;
					if (passengerInput.jump && !hasNoPhysics() && onGround) yd = 0.42;
					yRot = passenger.yRot;
					if (isInWater() || isInLava()) ejectRider();

					if (!onGround) {
						super.moveRelative(passengerInput.moveStrafe, passengerInput.moveForward, moveSpeed / 16);
					} else {
						super.moveRelative(passengerInput.moveStrafe, passengerInput.moveForward, moveSpeed / 6);
					}

					super.moveEntityWithHeading(passengerInput.moveStrafe, passengerInput.moveForward);
				}
			} else super.moveEntityWithHeading(moveStrafing, moveForward);
		} else {
			super.moveEntityWithHeading(moveStrafing, moveForward);
		}
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
	protected boolean canDespawn() {
		return super.canDespawn() && !isHorseTamed();
	}

	@Override
	public String getLivingSound() {
		return "creatures:mob.horse";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.horse.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.horse.death";
	}

	@Override
	protected void dropDeathItems() {
		super.dropDeathItems();
		if (isHorseSaddled()) dropItem(Items.SADDLE.id, 1);
	}

	@Override
	public double getRideHeight() {
		return (double) bbHeight * 0.7;
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Tamed", isHorseTamed());
		tag.putBoolean("Saddled", isHorseSaddled());
		tag.putInt("ChanceForTame", chanceForTame);
		tag.putInt("Annoyance", annoyance);
		tag.putInt("TameCounter", tameCounter);

		UUIDHelper.writeToTag(tag, getHorseOwner(), "OwnerUUID");
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setHorseTamed(tag.getBoolean("Tamed"));
		setHorseSaddled(tag.getBoolean("Saddled"));
		chanceForTame = tag.getInteger("ChanceForTame");
		annoyance = tag.getInteger("Annoyance");
		tameCounter = tag.getInteger("TameCounter");

		UUID ownerUUID = UUIDHelper.readFromTag(tag, "OwnerUUID");

		if (ownerUUID == null) {
			// 7.2 saved the owner as a bare username.
			String s = tag.getString("Owner");
			if (!s.isEmpty()) {
				UUIDHelper.runConversionAction(s, (uuid) -> {
					setHorseOwner(uuid);
					setHorseTamed(true);
				}, null);
			}
		} else {
			setHorseOwner(ownerUUID);
			setHorseTamed(true);
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!world.isClientSide) {
			if (attacker instanceof Player && !isHorseTamed()) {
				tameCounter -= 150;
			}
		}
		return super.hurt(attacker, damage, type);
	}
}
