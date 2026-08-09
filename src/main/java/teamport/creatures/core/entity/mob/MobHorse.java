package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.player.inventory.container.ContainerSimple;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.UUIDHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MMConfig;
import teamport.creatures.core.MMRiderControl;
import teamport.creatures.core.MMUtils;
import teamport.creatures.MoreMobs;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MobHorse extends MobAnimal {
	public static final int MASK_TAMED = 0b0000_0001;
	public static final int MASK_SADDLED = 0b0000_0010;
	public static final int MASK_CHESTED = 0b0000_0100;

	public static final int CHEST_SIZE = 27;
	public static final int DATA_GENERIC_FLAGS = 16;
	public static final int DATA_OWNER_UUID = 17;

	public static final int DATA_GROWTH = 18;

	private static final int GESTATION = 50;

	private static final float GROWTH_PER_STEP = 0.01F;
	private static final int GROWTH_ODDS = 200;

	private static final float GROWTH_PER_FEED = 0.05F;

	private static final double COURTING_RANGE = 4.0;

	protected int annoyance = 0;
	protected int chanceForTame = 0;
	protected int tameCounter = 0;

	protected boolean fedForBreeding = false;

	protected boolean sterile = false;
	protected int gestation = 0;

	protected final Container chest = new ContainerSimple("container.chest", CHEST_SIZE);

	protected final MMRiderControl riderControl = new MMRiderControl();

	public MobHorse(World world) {
		super(world);
		textureIdentifier = new NamespaceID(MoreMobs.MOD_ID, "horse");
		setSkinVariant(random.nextInt(3));

		setHealthRaw(getMaxHealth());

		setSize(1.4F, 1.6F);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
		entityData.define(DATA_OWNER_UUID, null, UUID.class);
		entityData.define(DATA_GROWTH, (short) 1000, Short.class);
	}

	public int geneticValue() {
		return (getSkinVariant() % 3) + 1;
	}

	public float getGrowth() {
		return entityData.getShort(DATA_GROWTH) / 1000.0F;
	}

	public void setGrowth(float growth) {
		entityData.set(DATA_GROWTH, (short) (Math.max(0.0F, Math.min(1.0F, growth)) * 1000.0F));
	}

	public boolean isAdult() {
		return getGrowth() >= 1.0F;
	}

	@Override
	public int getMaxHealth() {
		switch (geneticValue()) {
			case 1: return 25;
			case 2: return 30;
			case 3: return 35;
			case 7:
			case 8: return 50;
			default: return 40;
		}
	}

	public String textureIndex() {
		return String.valueOf(getSkinVariant() % 3);
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/horse/"
			+ (isHorseSaddled() ? "saddled_" : "") + textureIndex() + ".png";
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

	protected int annoyanceRate() {
		return 20;
	}

	protected int annoyanceLimit() {
		return 300;
	}

	protected int tameRate() {
		return 20;
	}

	protected int tameThreshold() {
		return 1000;
	}

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
			if (handleChest(player, item)) {
				return false;
			}

			if (isBreedingFood(item)) {
				eatBreedingFood(player, item);
				return false;
			}

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
		} else if (isAdult()) {

			player.startRiding(this);
		}
		return false;
	}

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

	protected boolean isBreedingFood(ItemStack stack) {
		return stack.itemID == Blocks.PUMPKIN.id()
			|| stack.itemID == Items.EGG_CHICKEN.id
			|| stack.itemID == Items.FOOD_CAKE.id
			|| stack.itemID == Items.FOOD_STEW_MUSHROOM.id;
	}

	protected void eatBreedingFood(Player player, ItemStack stack) {
		boolean stew = stack.itemID == Items.FOOD_STEW_MUSHROOM.id;
		stack.consumeItem(player);
		if (stew) {
			player.inventory.insertItem(new ItemStack(Items.BOWL), true);
		}

		if (isAdult()) {
			if (!sterile) fedForBreeding = true;
		} else {
			setGrowth(getGrowth() + GROWTH_PER_FEED);
		}

		heal(getMaxHealth());
		playEatingSound();
	}

	protected boolean readyForParenting() {
		return isHorseTamed() && isAdult() && fedForBreeding && !sterile
			&& passenger == null && vehicle == null;
	}

	protected static int geneticsOf(MobHorse a, MobHorse b) {
		int mine = a.geneticValue();
		int theirs = b.geneticValue();
		if (mine == theirs) return mine;

		boolean lucky = MMConfig.easyHorseBreeding || a.random.nextInt(3) == 0;
		if (!lucky) return 0;

		switch (mine + theirs) {
			case 7: return 6;
			case 9: return 7;
			case 10: return 5;
			case 12: return 8;
			default: return 0;
		}
	}

	protected MobHorse foalFor(int genetic) {
		switch (genetic) {
			case 4: return new MobHorseUnicorn(world);
			case 5: return new MobHorsePegasus(world);
			case 6: return new MobHorsePack(world);
			case 7: return new MobHorseNightmare(world);
			case 8: return new MobHorsePegasusBlack(world);
			case 1:
			case 2:
			case 3: {
				MobHorse foal = new MobHorse(world);
				foal.setSkinVariant(genetic - 1);

				foal.setHealthRaw(foal.getMaxHealth());
				return foal;
			}
			default:
				return foalFor(rollNaturalType());
		}
	}

	private int rollNaturalType() {
		int roll = random.nextInt(100) + 1;
		if (roll <= 51) return 1;
		if (roll <= 86) return 2;
		if (roll <= 95) return 3;
		if (roll <= 99) return 4;
		return 5;
	}

	private void tickCourting() {
		if (!readyForParenting()) return;

		List<Entity> nearby = world.getEntitiesWithinAABBExcludingEntity(
			this, MMUtils.grow(bb, COURTING_RANGE, COURTING_RANGE, COURTING_RANGE));

		for (Entity entity : nearby) {
			if (!(entity instanceof MobHorse)) continue;
			MobHorse mate = (MobHorse) entity;
			if (mate == this || !mate.readyForParenting()) continue;

			if (random.nextInt(100) == 0) gestation++;
			if (gestation <= GESTATION) return;

			breedWith(mate);
			return;
		}
	}

	private void breedWith(MobHorse mate) {
		MobHorse foal = foalFor(geneticsOf(this, mate));
		foal.moveTo(x, y, z, yRot, xRot);
		foal.setGrowth(0.0F);
		world.entityJoinedWorld(foal);

		world.playSoundAtEntity(null, this, "mob.chickenplop", 1.0F,
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);

		if (!MMConfig.easyHorseBreeding) sterile = true;

		fedForBreeding = false;
		mate.fedForBreeding = false;
		gestation = 0;
		mate.gestation = 0;
	}

	public boolean acceptsChest() {
		return false;
	}

	public boolean isChested() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_CHESTED) != 0;
	}

	public void setChested(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		if (flag) {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data | MASK_CHESTED));
		} else {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data & ~MASK_CHESTED));
		}
	}

	public Container getChest() {
		return chest;
	}

	private static boolean isChestItem(ItemStack stack) {
		return stack.itemID == Blocks.CHEST_PLANKS_OAK.id()
			|| stack.itemID == Blocks.CHEST_PLANKS_OAK_PAINTED.id()
			|| stack.itemID == Blocks.CHEST_LEGACY.id()
			|| stack.itemID == Blocks.CHEST_LEGACY_PAINTED.id();
	}

	private static boolean opensChest(ItemStack stack) {
		return stack.itemID == Blocks.TORCH_COAL.id()
			|| stack.itemID == Items.TOOL_SHOVEL_WOOD.id
			|| stack.itemID == Items.TOOL_SHOVEL_STONE.id
			|| stack.itemID == Items.TOOL_SHOVEL_IRON.id
			|| stack.itemID == Items.TOOL_SHOVEL_STEEL.id
			|| stack.itemID == Items.TOOL_SHOVEL_GOLD.id
			|| stack.itemID == Items.TOOL_SHOVEL_DIAMOND.id;
	}

	protected boolean handleChest(Player player, ItemStack item) {
		if (!acceptsChest() || !isHorseTamed()) return false;

		if (isChestItem(item)) {
			if (isChested()) return true;
			item.consumeItem(player);
			setChested(true);
			world.playSoundAtEntity(null, this, "mob.chickenplop", 1.0F,
				(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
			return true;
		}

		if (isChested() && opensChest(item)) {
			if (!world.isClientSide) player.displayContainerScreen(chest);
			return true;
		}

		return false;
	}

	private void tickGrowth() {
		if (isAdult()) return;
		if (random.nextInt(GROWTH_ODDS) == 0) {
			setGrowth(getGrowth() + GROWTH_PER_STEP);
		}
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		if (passenger instanceof Player && !isHorseTamed()) {
			tickUntamedRider((Player) passenger);
		}

		if (!world.isClientSide) {
			tickGrowth();
			tickCourting();

			if (riderControl.apply(this)) {
				return;
			}
		}

		followPlayerHoldingItem();
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		if (passenger == null) {
			super.moveEntityWithHeading(moveStrafing, moveForward);
			return;
		}

		if (isInWater() || isInLava()) ejectRider();

		if (isHorseSaddled() && passenger instanceof PlayerLocal) {
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
			return;
		}

		super.moveEntityWithHeading(0.0F, 0.0F);
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

		if (isChested()) {
			dropItem(Blocks.CHEST_PLANKS_OAK.id(), 1);
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				ItemStack stack = chest.getItem(slot);
				if (stack != null) {
					world.dropItem(x, y, z, stack, 0.0);
					chest.setItem(slot, null);
				}
			}
		}
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
		tag.putFloat("Growth", getGrowth());
		tag.putBoolean("FedForBreeding", fedForBreeding);
		tag.putBoolean("Sterile", sterile);
		tag.putInt("Gestation", gestation);
		tag.putBoolean("Chested", isChested());
		if (isChested()) {
			ListTag items = new ListTag();
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				ItemStack stack = chest.getItem(slot);
				if (stack == null) continue;
				CompoundTag entry = new CompoundTag();
				entry.putByte("Slot", (byte) slot);
				stack.writeToNBT(entry);
				items.addTag(entry);
			}
			tag.put("Items", items);
		}

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

		setGrowth(tag.containsKey("Growth") ? tag.getFloat("Growth") : 1.0F);
		fedForBreeding = tag.getBoolean("FedForBreeding");
		sterile = tag.getBoolean("Sterile");
		gestation = tag.getInteger("Gestation");
		setChested(tag.getBoolean("Chested"));
		if (isChested()) {
			ListTag items = tag.getList("Items");
			for (int i = 0; i < items.tagCount(); i++) {
				CompoundTag entry = (CompoundTag) items.tagAt(i);
				int slot = entry.getByte("Slot") & 255;
				if (slot < chest.getContainerSize()) {
					chest.setItem(slot, ItemStack.readItemStackFromNbt(entry));
				}
			}
		}

		UUID ownerUUID = UUIDHelper.readFromTag(tag, "OwnerUUID");

		if (ownerUUID == null) {

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
