package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobBear;

@Environment(EnvType.CLIENT)
public class MobRendererBear extends MobRendererQuadrupedBase<MobBear> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	private static final String OVER_KEY = "over";
	private static final int LAYER_OVER = 1;

	public MobRendererBear() {
		super("geometry.bear", 0.0D, 0.7F);
		setModel(OVER_KEY, "geometry.bear_over", 0.0D);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected int maxRenderLayer(MobBear entity) {
		return getModel(OVER_KEY) != null ? LAYER_OVER : 0;
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobBear entity, float partialTick, float unused, int layer) {
		if (layer != LAYER_OVER) {
			return super.getAndSetupModelForLayer(entity, partialTick, unused, layer);
		}
		StaticEntityModel model = getModel(OVER_KEY);
		if (model == null) {
			return null;
		}
		bindTexture(entity.getOverlayTexture());
		model.resetBones();

		poseHead(model, entity, getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick),
			getHeadPitch(entity, partialTick), partialTick);
		return model;
	}

	@Override
	protected float limbSwingAmplitude(MobBear entity, float partialTick) {

		return entity.isBearAngry() ? (float) ((22 * Math.PI) / 45) : (float) (Math.PI / 5);
	}
}
