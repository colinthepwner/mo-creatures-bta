package teamport.creatures.core.entity.mob;

import net.minecraft.core.world.World;

public class MobHorsePack extends MobHorse {
	public MobHorsePack(World world) {
		super(world);

	}

	@Override
	public int geneticValue() {
		return 6;
	}

	@Override
	public String textureIndex() {
		return "5";
	}

	@Override
	public String getDefaultEntityTexture() {

		return "/assets/creatures/textures/entity/horse/1.png";
	}

	@Override
	public boolean acceptsChest() {
		return true;
	}

	@Override
	protected int annoyanceLimit() {
		return 600;
	}

	@Override
	protected int tameThreshold() {
		return 1200;
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 1;
	}
}
