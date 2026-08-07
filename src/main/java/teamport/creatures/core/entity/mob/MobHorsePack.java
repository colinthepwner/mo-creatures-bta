package teamport.creatures.core.entity.mob;

import net.minecraft.core.world.World;

/**
 * Genetic value 6, the slow one you breed for its saddlebags rather than its legs. It comes out of a
 * pairing whose values add to 7 — a dark horse and a unicorn, or a brown and a pegasus.
 * <p>
 * Never spawns in the wild: the original's {@code chooseType} only ever rolls 1 to 5, so this and the
 * other two bred horses exist solely as breeding outcomes.
 */
public class MobHorsePack extends MobHorse {
	public MobHorsePack(World world) {
		super(world);
		// No speed of its own on purpose: the original gives the pack horse HorseSpeed 0.9, which is
		// exactly a light horse's, so it inherits the plain horse's rather than inventing a number.
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
		// No built-in art for the bred horses; without a copy of the original this falls back to the
		// brown coat its sheet is closest to rather than the missing-texture checker.
		return "/assets/creatures/textures/entity/horse/1.png";
	}

	@Override
	public int getMaxHealth() {
		return 40;
	}

	@Override
	public boolean acceptsChest() {
		return true;
	}

	/** Temper 600 in the original, so it takes noticeably more breaking-in than a wild horse. */
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
