package teamport.creatures.core.entity.mob;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import teamport.creatures.MoreMobs;

public class MobFishyEgg extends MobAquaticEggBase {

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
