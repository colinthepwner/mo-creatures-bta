package teamport.creatures.core.entity.mob;

import net.minecraft.core.world.World;

public class MobHorsePegasusBlack extends MobHorsePegasus {
	public MobHorsePegasusBlack(World world) {
		super(world);

		moveSpeed = 0.49F;
		fireImmune = true;
	}

	@Override
	public int geneticValue() {
		return 8;
	}

	@Override
	public String textureIndex() {
		return "7";
	}

	@Override
	public String getDefaultEntityTexture() {

		return "/assets/creatures/textures/entity/horse/4.png";
	}

	@Override
	public boolean acceptsChest() {
		return true;
	}

	@Override
	protected int annoyanceLimit() {
		return 800;
	}

	@Override
	protected int tameThreshold() {
		return 2000;
	}
}
