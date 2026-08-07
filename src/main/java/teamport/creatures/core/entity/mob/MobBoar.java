package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.joml.primitives.AABBd;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.MMUtils;

import java.util.ArrayList;
import java.util.List;

public class MobBoar extends MobAnimal {
	public static final int MASK_ANGRY = 0b0000_0001;
	public static final int DATA_GENERIC_FLAGS = 16;

	private int angerCounter;

	public List<WeightedRandomLootObject> burningMobDrops = new ArrayList<>();

	public MobBoar(World world) {
		super(world);
		// Wires up basePath/defaultTexture/variantJsonPath together; assigning textureIdentifier on its
		// own leaves basePath pointing at the default player skin. The boar has a single skin, so the
		// missing variants.json resolves back to "0".
		setTextureIdentifier(MoreMobs.MOD_ID, "boar");

		setSize(0.9F, 0.9F);

		mobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_RAW.getDefaultStack(), 1, 2));
		burningMobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_COOKED.getDefaultStack(), 1, 2));
	}

	/**
	 * The 7.2 mob kept "angry" as a plain field, which the renderer cannot see now that posing happens
	 * client-side. It is tracked through {@code entityData} instead, the same way {@link MobBear} does,
	 * so the charge stride shows up on the client. The anger countdown itself stays server-side.
	 */
	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_GENERIC_FLAGS, (byte) 0, Byte.class);
	}

	public boolean isBoarAngry() {
		return (entityData.getByte(DATA_GENERIC_FLAGS) & MASK_ANGRY) != 0;
	}

	public void setBoarAngry(boolean flag) {
		byte data = entityData.getByte(DATA_GENERIC_FLAGS);
		if (flag) {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data | MASK_ANGRY));
		} else {
			entityData.set(DATA_GENERIC_FLAGS, (byte) (data & ~MASK_ANGRY));
		}
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/boar/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/boar/0.png";
	}

	@Override
	public void tick() {
		super.tick();
		// The counter always ticks down, exactly as in 7.2; peaceful play just suppresses the anger.
		// Server-side only now that the flag is synched; the client just reads what it is told.
		if (!world.isClientSide) {
			setBoarAngry(angerCounter-- > 0 && world.getDifficulty().canHostileMobsSpawn());
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (attacker instanceof Player) {
			angerCounter = 400;
		} else {
			setTarget(attacker);
		}

		return super.hurt(attacker, damage, type);
	}

	@Override
	protected Entity findPlayerToAttack() {
		return isBoarAngry() ? world.getClosestPlayerToEntity(this, 16.0D) : null;
	}

	@Override
	protected void attackEntity(Entity entity, float distance) {
		if (!(entity instanceof EntityItem)) {
			if (!(distance > 2.0F) || !(distance < 6.0F) || random.nextInt(10) != 0) {
				if ((double) distance < 1.5 && entity.bb.maxY > bb.minY && entity.bb.minY < bb.maxY) {
					attackTime = 20;
					// EntityBoar.attackEntity passes its force field straight through, and force is 1.
					entity.hurt(this, 1, DamageType.COMBAT);
				}
			} else if (onGround) {
				// The charge: a short leap towards the target rather than a plain melee swing.
				double d = entity.x - x;
				double d1 = entity.z - z;
				float f1 = MathHelper.sqrt(d * d + d1 * d1);
				xd = d / (double) f1 * 0.5 * 0.8F + xd * 0.2F;
				zd = d1 / (double) f1 * 0.5 * 0.8F + zd * 0.2F;
				yd = 0.4F;
			}
		}
	}

	@Override
	protected void updateAI() {
		super.updateAI();
		if (target == null && !hasPath() && world.getDifficulty().canHostileMobsSpawn() && world.rand.nextInt(200) == 0) {
			// AABB.getBoundingBoxFromPool(...).expand(...) is gone in 8.0 and JOML has no grow, so the
			// search volume is built explicitly and widened through MMUtils.
			List<Player> nearbyPlayers = world.getEntitiesWithinAABB(
				Player.class, MMUtils.grow(new AABBd(x, y, z, x + 1.0, y + 1.0, z + 1.0), 16.0, 4.0, 16.0)
			);

			for (Player player : nearbyPlayers) {
				if (player.gamemode.hasHostileMobs()) {
					setTarget(player);
				}
			}
		}
	}

	@Override
	public void playLivingSound() {
		String s = getLivingSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	@Override
	public void playHurtSound() {
		String s = getHurtSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	@Override
	public void playDeathSound() {
		String s = getDeathSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	@Override
	public String getLivingSound() {
		return "mob.pig";
	}

	@Override
	protected String getHurtSound() {
		return "mob.pig";
	}

	@Override
	protected String getDeathSound() {
		return "mob.pigdeath";
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Angry", isBoarAngry());
		tag.putInt("Anger", angerCounter);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setBoarAngry(tag.getBoolean("Angry"));
		angerCounter = tag.getInteger("Anger");
	}

	@Override
	protected List<WeightedRandomLootObject> getMobDrops() {
		return remainingFireTicks > 0 ? burningMobDrops : mobDrops;
	}
}
