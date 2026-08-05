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
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMUtils;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * The dolphin, and the one mob in this batch with a full life cycle: it can be gentled, ridden,
 * bred, and its calves inherit a colour from both parents.
 * <p>
 * The original mod's README is the clearest statement of the intended rules and the numbers below
 * come from it:
 * <ul>
 *   <li>Six colours, from common to rare. Rarer dolphins swim noticeably faster and take
 *       proportionally more raw fish to gentle — two for the commonest, twelve for the rarest.
 *       That falls out of a per-colour {@code temper} of 50 through 300 and a fixed 25 knocked off
 *       per fish.</li>
 *   <li>Riding an ungentled one is a rodeo: it bucks, and either it gives in or the rider is
 *       thrown. Feeding first is what makes this quick, which is why {@code temper} drives both.</li>
 *   <li>Breeding needs two tame adults, both fed cooked fish, alone together. The calf's colour is
 *       decided by the sum of the parents' ranks: 3 or 4 gives the matching mid-rank colour one
 *       time in three, 5 or 6 gives one of the two rare colours one time in ten, and anything else
 *       is a re-roll.</li>
 * </ul>
 * Colour is stored in BTA's own {@code DATA_VARIANT} slot, which the base class already syncs and
 * persists; it is zero-based there and one-based ("genetic value") in the breeding maths.
 */
public class MobDolphin extends MobAquaticBase {
	public static final int MASK_TAMED = 0b0000_0001;
	public static final int MASK_BRED = 0b0000_0010;
	public static final int DATA_GENERIC_FLAGS = 17;
	public static final int DATA_OWNER_UUID = 18;

	public static final int COLOURS = 6;
	/** Raw fish knocks this much off {@link #temper}; temper starts at 50 per rank. */
	private static final int GENTLE_STEP = 25;
	private static final float ADULT_GROWTH = 1.5F;
	private static final float BABY_GROWTH = 0.35F;
	/** Gestation counter ticks up one in a hundred ticks, so this is a good few minutes of courting. */
	private static final int GESTATION = 50;

	/** How much convincing is left. Server-side bookkeeping; the client never needs it. */
	private int temper;
	/** Set by cooked fish, cleared when a calf is born. */
	private boolean fedForBreeding;
	/** Wild dolphins hunt for dropped fish constantly; tame ones only when this comes round. */
	private boolean hungry;
	private int gestation;

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

	// --- colour and genetics ------------------------------------------------------------------

	/**
	 * Spawn weights, commonest first: 36/25/25/11/2/1 percent. The last two are the pink and albino
	 * the README describes as "seen only rarely in the wild".
	 */
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
		// Rarer colours are harder to win over, which is the whole reason they are worth chasing.
		temper = (getDolphinColour() + 1) * 50;
	}

	/** One-based rank, 1 for the commonest through 6 for the rarest. The breeding maths uses this. */
	public int getGeneticValue() {
		return getDolphinColour() + 1;
	}

	/**
	 * Same colour breeds true. Otherwise the parents' ranks are added and the sum is itself the
	 * colour on offer, on a roll that gets steeper the rarer the result.
	 */
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

	// --- taming -------------------------------------------------------------------------------

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

	/** Calves stay put rather than despawning, the same way tame ones do. */
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

	// --- interaction --------------------------------------------------------------------------

	@Override
	public boolean interact(@NotNull Player player) {
		// Lets a label name the dolphin, which is BTA's answer to the original's medallion, and
		// makes it persistent into the bargain.
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
			// Calves cannot be ridden. An ungentled adult can be mounted, but it will fight back.
			player.startRiding(this);
			return true;
		}

		return false;
	}

	/**
	 * Raw fish both heals and gentles. Twelve fish will see off even an albino's temper, which is
	 * exactly the figure the README quotes.
	 */
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

	// --- AI -----------------------------------------------------------------------------------

	@Override
	public float adultGrowth() {
		return ADULT_GROWTH;
	}

	@Override
	public float babyGrowth() {
		return BABY_GROWTH;
	}

	/**
	 * Rarer dolphins are noticeably quicker. Ranks 1 to 6 land between roughly four and eight
	 * blocks a second, against a swimming player's two.
	 */
	@Override
	protected float swimThrust() {
		return 0.026F + 0.006F * getDolphinColour();
	}

	/** Grown dolphins pick fights with sharks; nothing else is worth their time. */
	@Override
	protected int targetSearchInterval() {
		return 50;
	}

	@Override
	protected Entity findSwimTarget() {
		if (!isAdult()) {
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

	/** Dropped raw fish, not live ones — a dolphin will cross a bay for a thrown fish. */
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
			// A rider steers; the dolphin only gets a say in whether it puts up with them.
			tickGrowth();
			climb = 0.0;
			if (!isDolphinTamed()) {
				tickRodeo((Player) passenger);
			}
			return;
		}

		super.updateAI();

		if (!hungry && random.nextInt(100) == 0) {
			hungry = true;
		}
		tickBreeding();
	}

	/**
	 * The tug of war between an ungentled dolphin and whoever climbed on. Feeding beforehand is
	 * what tips it: {@code temper} is the divisor on the odds of giving in.
	 */
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

	/**
	 * Two tame, grown, cooked-fish-fed dolphins alone together produce a calf. A third dolphin in
	 * the neighbourhood puts them off entirely.
	 */
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

	// --- riding -------------------------------------------------------------------------------

	@Override
	public void moveEntityWithHeading(float strafe, float forward) {
		if (isInWater() && isDolphinTamed() && passenger instanceof Player) {
			Player rider = (Player) passenger;
			yRot = rider.yRot;
			yBodyRot = yRot;
			xRot = rider.xRot * 0.5F;

			if (rider instanceof PlayerLocal) {
				PlayerInput input = ((PlayerLocal) rider).input;
				// Where the rider looks is where the dolphin goes, which is how you dive; jump
				// surfaces it, so you can always get back up for air.
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

	// --- combat, sound, housekeeping ----------------------------------------------------------

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
		// Being hit by your own rider does not count as picking a fight.
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
		// setDolphinColour resets temper to the colour's default, so the saved value goes last.
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
