package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobOgre;

@Environment(EnvType.CLIENT)
public abstract class MobRendererOgreBase<T extends MobOgre> extends MobRendererBipedBase<T> {
	protected static final String MODEL_KEY = "main";

	protected static final String OVER_KEY = "over";
	private static final int LAYER_OVER = 1;

	private final String overTexture;

	protected MobRendererOgreBase(String modelId, String overModelId, String overTexture) {
		super(1.5F);
		setModel(MODEL_KEY, modelId, 0.0D);
		setModel(OVER_KEY, overModelId, 0.0D);
		this.overTexture = overTexture;
	}

	@Override
	protected int maxRenderLayer(T entity) {
		return LAYER_OVER;
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(T entity, float partialTick, float unused, int layer) {
		if (layer == LAYER_OVER) {
			bindTexture(overTexture);
		}
		StaticEntityModel model = getModel(layer == LAYER_OVER ? OVER_KEY : MODEL_KEY);
		if (model == null) {
			return null;
		}
		model.resetBones();

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);
		float headYaw = getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick);

		poseHead(model, headYaw, 0.0F);

		poseWalk(model, limbSwing, limbYaw, entity.isOgreAngry() ? 1.3F : 0.9F);

		if (entity.isOgreAttacking()) {

			float progress = 0.5F + MathHelper.sin((entity.tickCount + partialTick) * 0.6F) * 0.5F;
			poseOverheadSwing(model, progress);
		}

		return model;
	}
}
