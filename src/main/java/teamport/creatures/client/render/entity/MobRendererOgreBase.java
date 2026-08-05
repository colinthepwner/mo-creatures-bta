package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobOgre;

/**
 * Shared posing for all three ogres. They differ only in model and texture.
 * <p>
 * The swing is driven off {@link MobOgre#isOgreAttacking()}, which is a synched flag rather than the
 * server-only {@code attackTime}; the original triggered its block-smashing from inside the renderer
 * off a plain field, which cannot work once posing happens client-side.
 */
@Environment(EnvType.CLIENT)
public abstract class MobRendererOgreBase<T extends MobOgre> extends MobRendererBipedBase<T> {
	protected static final String MODEL_KEY = "main";

	protected MobRendererOgreBase(String modelPath) {
		super(1.5F);
		setModel(MODEL_KEY, modelPath, 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(T entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		if (model == null) {
			return null;
		}
		model.resetBones();

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);
		float headYaw = getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick);
		float headPitch = getHeadPitch(entity, partialTick);

		poseHead(model, headYaw, headPitch);
		// An angry ogre lumbers along with a much longer stride.
		poseWalk(model, limbSwing, limbYaw, entity.isOgreAngry() ? 1.3F : 0.9F);

		if (entity.isOgreAttacking()) {
			// Both fists up and coming down: the pose that goes with a wall being removed. The flag is
			// held for ten ticks server-side, so this is paced to one full raise-and-drop over that.
			float progress = 0.5F + MathHelper.sin((entity.tickCount + partialTick) * 0.6F) * 0.5F;
			poseOverheadSwing(model, progress);
		}

		return model;
	}
}
