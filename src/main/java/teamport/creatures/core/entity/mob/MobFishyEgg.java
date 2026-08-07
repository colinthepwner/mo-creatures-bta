package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;

/**
 * A clutch of fishy roe. Dropped by a lil' fish when it dies and hatches after long enough
 * underwater; this is what restocks a lake that the sharks have been working over.
 * <p>
 * The variety carries across from the parent, so a pond of one kind of fish stays that kind. Roe
 * dropped without a parent — spawned in, say — picks a variety at random, piranhas included unless
 * {@code WaterMobs.spawnPiranhas} says otherwise.
 */
public class MobFishyEgg extends MobAquaticEggBase {
	/** Which of {@link MobFishy#VARIETIES} will come out. Server-side; nothing renders it. */
	private int variety;

	public MobFishyEgg(World world) {
		super(world);
		textureIdentifier = new NamespaceID(MoreMobs.MOD_ID, "fishy_egg");
		variety = MobFishy.rollVariety(random);
	}

	public int getVariety() {
		return variety;
	}

	public void setVariety(int variety) {
		this.variety = Math.floorMod(variety, MobFishy.VARIETIES);
	}

	@Override
	protected Mob hatch() {
		MobFishy fry = new MobFishy(world);
		fry.setVariety(variety);
		fry.setGrowth(fry.babyGrowth());
		return fry;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/fishy_egg/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/fishy_egg/0.png";
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Variety", variety);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.containsKey("Variety")) {
			setVariety(tag.getInteger("Variety"));
		}
	}
}
