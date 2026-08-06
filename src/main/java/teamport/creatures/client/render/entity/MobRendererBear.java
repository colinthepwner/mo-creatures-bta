package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobBear;

@Environment(EnvType.CLIENT)
public class MobRendererBear extends MobRendererQuadrupedBase<MobBear> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	public MobRendererBear() {
		super("geometry.bear", 0.0D, 0.7F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected float limbSwingAmplitude(MobBear entity, float partialTick) {
		// An angry bear charges with a much longer stride than one that is just wandering.
		return entity.isBearAngry() ? (float) ((22 * Math.PI) / 45) : (float) (Math.PI / 5);
	}
}
