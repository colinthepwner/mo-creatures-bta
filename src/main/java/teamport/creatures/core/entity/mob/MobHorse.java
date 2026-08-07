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
import teamport.creatures.core.MMUtils;
import teamport.creatures.MoreMobs;

import java.util.List;
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
	public static final int MASK_CHESTED = 0b0000_0100;
	/** A whole chest's worth, which is what the original gives a pack horse. */
	public static final int CHEST_SIZE = 27;
	public static final int DATA_GENERIC_FLAGS = 16;
	public static final int DATA_OWNER_UUID = 17;
	/** Foals grow towards 1.0; the renderer scales the model by it. */
	public static final int DATA_GROWTH = 18;

	/** Gestation ticks up on a 1-in-100 roll and a foal arrives past this, as the original has it. */
	private static final int GESTATION = 50;
	/** How much of its growth a foal puts on per successful roll, and how often that roll comes up. */
	private static final float GROWTH_PER_STEP = 0.01F;
	private static final int GROWTH_ODDS = 200;
	/** A foal fed by hand grows this much at once. */
	private static final float GROWTH_PER_FEED = 0.05F;
	/** How far apart two horses can be and still court. */
	private static final double COURTING_RANGE = 4.0;

	protected int annoyance = 0;
	protected int chanceForTame = 0;
	protected int tameCounter = 0;

	/** Set by a pumpkin, egg, cake or bowl of stew, and cleared when a foal is born. */
	protected boolean fedForBreeding = false;
	/** The original's {@code hasreproduced}: one of the two parents is left unable to breed again. */
	protected boolean sterile = false;
	protected int gestation = 0;

	/**
	 * Always allocated, only reachable once the horse has been given a chest.
	 * <p>
	 * Titled with BTA's own container key rather than one of this mod's. The container screen does
	 * not resolve a key from a mod's language files -- tried under both {@code strings.lang} and
	 * {@code item.lang}, with and without the {@code .name} tail, and the screen shows the raw key
	 * every time -- so a key of our own would read as gibberish to the player. "Chest" is accurate.
	 */
	protected final Container chest = new ContainerSimple("container.chest", CHEST_SIZE);

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
		entityData.define(DATA_GROWTH, (short) 1000, Short.class);
	}

	/**
	 * This horse's "genetic value" in the original's breeding table: 1 light, 2 brown, 3 dark,
	 * 4 unicorn, 5 pegasus, 6 pack horse, 7 nightmare, 8 black pegasus. The three coat colours are
	 * skin variants of this class, so they read off the variant; the rarer horses are their own
	 * entities and override this.
	 *
	 * @see #geneticsOf(MobHorse, MobHorse)
	 */
	public int geneticValue() {
		return (getSkinVariant() % 3) + 1;
	}

	/** 0 for a newborn foal through 1 for a grown horse. Drives both the render scale and breeding. */
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
		return 20;
	}

	/**
	 * Which of the horse sheets this horse wears, as it appears in the file name — {@code "0"} to
	 * {@code "2"} for the three coat colours, and a fixed one per subclass for the rarer horses.
	 * <p>
	 * The head half and the saddled body are named off the same index, so a horse can never end up
	 * wearing one colour's body with another's head.
	 */
	public String textureIndex() {
		return String.valueOf(getSkinVariant() % 3);
	}

	/**
	 * Saddling swaps the body sheet outright rather than drawing a saddle over it, which is what the
	 * original does — {@code RenderHorse.func_176_a} binds {@code horse<colour>saddle.png} in place of
	 * {@code horse<colour>a.png} while the horse is rideable, and leaves the head sheet alone.
	 */
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
			// A foal is too small to sit on, as it is in the original.
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

	/**
	 * What starts a horse courting. The original takes a pumpkin or an egg, and later versions added
	 * cake and a bowl of mushroom stew — the stew hands the empty bowl back, which is the only reason
	 * this is not a plain id test.
	 */
	protected boolean isBreedingFood(ItemStack stack) {
		return stack.itemID == Blocks.PUMPKIN.id()
			|| stack.itemID == Items.EGG_CHICKEN.id
			|| stack.itemID == Items.FOOD_CAKE.id
			|| stack.itemID == Items.FOOD_STEW_MUSHROOM.id;
	}

	/**
	 * Eating a breeding food readies the horse and heals it outright, both of which the original does
	 * in the same breath. A foal gets a growth spurt instead of being readied — it cannot breed.
	 */
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

	/**
	 * Both halves of the original's {@code ReadyforParenting}: tame, grown, fed, not already spent,
	 * and neither carrying a rider nor being carried.
	 */
	protected boolean readyForParenting() {
		return isHorseTamed() && isAdult() && fedForBreeding && !sterile
			&& passenger == null && vehicle == null;
	}

	/**
	 * The original's {@code HorseGenetics}, unchanged. Two horses of the same kind breed true;
	 * otherwise the parents' genetic values are added and four sums have a rare foal behind them,
	 * each on a one-in-three roll unless {@code EasyHorseBreeding} makes it certain. Any other sum
	 * gives 0, which stands for "roll an ordinary horse".
	 * <p>
	 * Breeding true is why two pack horses never produce a black pegasus even though 6 + 6 is 12.
	 */
	protected static int geneticsOf(MobHorse a, MobHorse b) {
		int mine = a.geneticValue();
		int theirs = b.geneticValue();
		if (mine == theirs) return mine;

		boolean lucky = MMConfig.easyHorseBreeding || a.random.nextInt(3) == 0;
		if (!lucky) return 0;

		switch (mine + theirs) {
			case 7: return 6;   // pack horse
			case 9: return 7;   // nightmare
			case 10: return 5;  // pegasus
			case 12: return 8;  // black pegasus
			default: return 0;
		}
	}

	/**
	 * Builds the foal a genetic value calls for. 0 means no rare combination came up, which the
	 * original answers with {@code setType(0)} — a re-roll on the natural spawn table, so an
	 * unremarkable pairing can still throw the occasional unicorn.
	 */
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
				return foal;
			}
			default:
				return foalFor(rollNaturalType());
		}
	}

	/**
	 * The spread {@code chooseType} rolls a wild horse on: 51% light, 35% brown, 9% dark, 4% unicorn
	 * and 1% pegasus. The original biases this with {@code PegasusSpawningP}; this port gives the
	 * pegasus its own entity and its own spawn weight instead, so the plain figures are used here.
	 */
	private int rollNaturalType() {
		int roll = random.nextInt(100) + 1;
		if (roll <= 51) return 1;
		if (roll <= 86) return 2;
		if (roll <= 95) return 3;
		if (roll <= 99) return 4;
		return 5;
	}

	/**
	 * One tick of courting. Both horses have to be ready and within a few blocks, and then it is a
	 * slow one-in-a-hundred climb rather than anything the player can hurry along.
	 */
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

		// Only this one is spent, which is the original's own rule -- both were sterilised until
		// 2.6.0 and the changelog calls the change out. Easy breeding spares even this one.
		if (!MMConfig.easyHorseBreeding) sterile = true;

		fedForBreeding = false;
		mate.fedForBreeding = false;
		gestation = 0;
		mate.gestation = 0;
	}

	/**
	 * Whether this horse will take a chest. Only the pack horse and the black pegasus do, which is
	 * the original's rule and the reason the chest lives on the base class rather than on them.
	 */
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

	/** The 27 slots the original gives a pack horse — a whole chest's worth, not a saddlebag. */
	public Container getChest() {
		return chest;
	}

	private static boolean isChestItem(ItemStack stack) {
		return stack.itemID == Blocks.CHEST_PLANKS_OAK.id()
			|| stack.itemID == Blocks.CHEST_PLANKS_OAK_PAINTED.id()
			|| stack.itemID == Blocks.CHEST_LEGACY.id()
			|| stack.itemID == Blocks.CHEST_LEGACY_PAINTED.id();
	}

	/** What the original opens the pack horse's chest with: a torch or a shovel, held in hand. */
	private static boolean opensChest(ItemStack stack) {
		return stack.itemID == Blocks.TORCH_COAL.id()
			|| stack.itemID == Items.TOOL_SHOVEL_WOOD.id
			|| stack.itemID == Items.TOOL_SHOVEL_STONE.id
			|| stack.itemID == Items.TOOL_SHOVEL_IRON.id
			|| stack.itemID == Items.TOOL_SHOVEL_STEEL.id
			|| stack.itemID == Items.TOOL_SHOVEL_GOLD.id
			|| stack.itemID == Items.TOOL_SHOVEL_DIAMOND.id;
	}

	/**
	 * @return true when the held item was the chest business and the caller should stop there
	 */
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

	/** A foal puts on growth slowly by itself; feeding it is much the faster route. */
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

		// The chest and everything in it comes back rather than vanishing with the horse. The
		// original crashed here until 2.7.0; its changelog calls that out as "game crashing bug when
		// killing pack horses".
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
		// Horses saved before breeding existed have no Growth tag and read back 0, which would turn
		// every one of them into a foal. They were all adults, so an absent tag means grown.
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
