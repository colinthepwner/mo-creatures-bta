package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.sound.SoundCategory;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.core.MMUtils;
import teamport.creatures.core.block.LitterboxEntity;

import java.util.List;
import java.util.Objects;

public class MobKitty extends MobAnimal {
	public boolean isTamed = false;
	public boolean isSitting = false;
	public String ownerName;
	private int skin;
	private int potty;
	private int usingPottyTime = 0;
	private int boredom;
	private final float soundPitch = (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F;

	public MobKitty(World world) {
		super(world);
		// A cat is longer than it is tall; the original had these the way round they are here.
		setSize(0.7F, 0.5F);

		heartsHalvesLife = 10;
		skin = random.nextInt(4);
		boredom = random.nextInt(400) + 800;
		potty = random.nextInt(6000) + 6000;
	}

	/**
	 * The original's figure. Taming does not change it; only the tail droop reads off the ratio.
	 * Matches {@code EntityKitty} in v2.12.2 — without it this inherited the base mob's default of 10.
	 */
	@Override
	public int getMaxHealth() {
		return 15;
	}

	@Override
	public String getEntityTexture() {
		return isTamed ? "/assets/creatures/textures/entity/kitty/tamed_" + skin + ".png" : "/assets/creatures/textures/entity/kitty/" + skin + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/kitty/0.png";
	}

	void showHeartsOrSmokeFX(boolean hearts) {
		String particle = "heart";
		if (!hearts) {
			particle = "smoke";
		}

		for (int i = 0; i < 7; i++) {
			double motionX = random.nextGaussian() * 0.02;
			double motionY = random.nextGaussian() * 0.02;
			double motionZ = random.nextGaussian() * 0.02;
			world
				.spawnParticle(
					particle,
					x + (double) (random.nextFloat() * bbWidth * 2.0F) - (double) bbWidth,
					y + 0.5 + (double) (random.nextFloat() * bbHeight),
					z + (double) (random.nextFloat() * bbWidth * 2.0F) - (double) bbWidth,
					motionX,
					motionY,
					motionZ,
					0,
					false
				);
		}
	}

	@Override
	public boolean interact(@NotNull Player player) {
		if (!world.isClientSide) {
			ItemStack heldItem = player.getHeldItem();

			if (heldItem != null && heldItem.itemID == Items.FOOD_FISH_RAW.id) {
				if (!isTamed) {
					world.playSoundEffect(player,
						SoundCategory.ENTITY_SOUNDS,
						x,
						y,
						z,
						"creatures:mob.kitty.eating",
						1.0F,
						soundPitch);

					lookAt(player, 30.0F, 30.0F);
					heldItem.consumeItem(player);

					if (random.nextInt(3) == 0) {
						isTamed = true;
						showHeartsOrSmokeFX(true);
						ownerName = player.username;
					} else {
						showHeartsOrSmokeFX(false);
					}
				} else {
					heal(2);
					heldItem.consumeItem(player);
				}
			}

			if (isTamed && Objects.equals(player.username, ownerName) && player.getHeldItem() == null) {
				isSitting = !isSitting;
			}
		}

		return true;
	}

	public float getTailRotation() {
		return isTamed ? (0.55F - (float) (getMaxHealth() - getHealth()) * 0.2F) * (float) Math.PI : (float) (Math.PI / 5);
	}

	@Override
	protected boolean canDespawn() {
		return !isTamed && super.canDespawn();
	}

	@Override
	protected String getDeathSound() {
		return "creatures:mob.kitty.death";
	}

	@Override
	protected String getHurtSound() {
		return "creatures:mob.kitty.hurt";
	}

	@Override
	public String getLivingSound() {
		return isTamed && random.nextInt(3) == 0 ? "creatures:mob.kitty.purr" : "creatures:mob.kitty";
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		if (!(entity instanceof EntityItem)) {
			if (!(distance > 2.0F) || !(distance < 6.0F) || this.random.nextInt(10) != 0) {
				if ((double) distance < 1.5 && entity.bb.maxY > this.bb.minY && entity.bb.minY < this.bb.maxY) {
					attackTime = 20;
					entity.hurt(this, 2, DamageType.COMBAT);
				}
			} else if (this.onGround) {
				double d = entity.x - this.x;
				double d1 = entity.z - this.z;
				float f1 = MathHelper.sqrt(d * d + d1 * d1);
				this.xd = d / (double) f1 * 0.5 * 0.8F + this.xd * 0.2F;
				this.zd = d1 / (double) f1 * 0.5 * 0.8F + this.zd * 0.2F;
				this.yd = 0.4F;
			}
		}
	}

	@Override
	protected void updateAI() {
		super.updateAI();

		// String search code for when the kitty is bored.
		// When the boredom is below 0 and above -400 it will find for nearby item entities.
		// If it isn't null and the item is string, start 'playing' with it.
		// Once the boredom reaches -400 it will reset.
		if (target == null && boredom-- <= 0) {
			List<EntityItem> nearbyEntities = world.getEntitiesWithinAABB(EntityItem.class, MMUtils.grow(bb, 16.0, 4.0, 16.0));

			if (!nearbyEntities.isEmpty()) {
				for (EntityItem entityItem : nearbyEntities) {
					if (entityItem.item != null && entityItem.item.itemID == Items.STRING.id) {
						setTarget(entityItem);
					}
				}
			}
		}

		if (boredom-- <= -400) {
			boredom = random.nextInt(400) + 800;
		}

		if (getTarget() instanceof EntityItem) {
			if (MMUtils.grow(bb, 0.5, 2.0, 0.5).intersectsAABB(getTarget().bb)) {
				getTarget().move(xd * (random.nextInt(3) + 1), yd + 0.1, zd * (random.nextInt(3) + 1));
			}
		}

		// Same AI as above, just with birds instead.
		if (target == null && boredom-- <= 0) {
			List<MobBird> nearbyBirds = world.getEntitiesWithinAABB(MobBird.class, MMUtils.grow(bb, 16.0, 4.0, 16.0));

			if (!nearbyBirds.isEmpty()) {
				setTarget(nearbyBirds.get(world.rand.nextInt(nearbyBirds.size())));
			}
		}


		if (isTamed) {
			if (isSitting) {
				moveForward = 0.0f;
				moveStrafing = 0.0f;
				isJumping = false;
			}

			// Potty system - use a litter box once? a day.
			if (potty-- <= 0) {
				List<TileEntity> tileEntities = world.getLoadedTileEntityList();

				if (!tileEntities.isEmpty()) {
					for (TileEntity tileEntity : tileEntities) {
						if (tileEntity instanceof LitterboxEntity) {
							LitterboxEntity litterbox = (LitterboxEntity) tileEntity;

							if (!litterbox.isFilthy && potty <= 0) {
								pathToEntity = world.getEntityPathToXYZ(this, litterbox.tilePos.x, litterbox.tilePos.y, litterbox.tilePos.z, 16.0F);
								if (distanceToSqr(litterbox.tilePos.x, litterbox.tilePos.y, litterbox.tilePos.z) < 4.0 && !isPassenger()) {
									setPos(litterbox.tilePos.x, litterbox.tilePos.y, litterbox.tilePos.z);
									startRiding(litterbox);
								}
							}

							if (isPassenger() && usingPottyTime-- <= -200) {
								litterbox.ejectRider();
								litterbox.isFilthy = true;
								usingPottyTime = 0;
								potty = random.nextInt(6000) + 6000;
							}
						}
					}
				}
			}
		}

		// EXPERIMENTAL //
		// Player follow code for the upcoming 7.3 release. Follow items are: Fish.
		Player player = world.getClosestPlayerToEntity(this, 16.0);
		if (player != null && player.distanceToSqr(x, y, z) > 4.0) {
			ItemStack heldStack = player.getCurrentEquippedItem();
			if (heldStack != null && !isSitting && heldStack.itemID == Items.FOOD_FISH_RAW.id) {
				lookAt(player, 30.0F, 30.0F);
				moveForward = 1.0F;

				if (player.distanceToSqr(this) <= 12.0)
					moveForward = 0.0F;
			}
		}
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("IsTamed", isTamed);
		tag.putBoolean("IsSitting", isSitting);
		tag.putInt("Skin", skin);

		if (isTamed) tag.putString("Owner", ownerName);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		isTamed = tag.getBoolean("IsTamed");
		isSitting = tag.getBoolean("IsSitting");
		skin = tag.getInteger("Skin");

		if (isTamed) ownerName = tag.getString("Owner");
	}
}
