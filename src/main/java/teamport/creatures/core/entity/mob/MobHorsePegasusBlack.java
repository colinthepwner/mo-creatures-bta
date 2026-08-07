package teamport.creatures.core.entity.mob;

import net.minecraft.core.world.World;

/**
 * Genetic value 8, the rarest horse in the mod: it needs a pairing that adds to 12, which in practice
 * means a pegasus and a nightmare. Two pack horses also add to 12 and famously do not work — the
 * original returns early when both parents are the same kind, so they simply breed true.
 * <p>
 * It flies like the white pegasus and carries a chest like the pack horse, and it shares the
 * nightmare's fire immunity without its fire trail.
 */
public class MobHorsePegasusBlack extends MobHorsePegasus {
	public MobHorsePegasusBlack(World world) {
		super(world);
		// HorseSpeed 1.3 against the white pegasus' 1.2, so the pegasus' own figure by the same ratio.
		// Scaled rather than copied from the unicorn because the pegasus divides moveSpeed down again
		// in its flight code, and the two numbers are not on the same scale.
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
		// No built-in art for the bred horses; the white pegasus' sheet is the right shape at least,
		// wings included, which the plain coats are not.
		return "/assets/creatures/textures/entity/horse/4.png";
	}

	@Override
	public boolean acceptsChest() {
		return true;
	}

	/** Temper 800, the highest in the mod. */
	@Override
	protected int annoyanceLimit() {
		return 800;
	}

	@Override
	protected int tameThreshold() {
		return 2000;
	}
}
