package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobOgre;

/**
 * Shared posing for all three ogres. They differ only in model and texture.
 * <p>
 * An ogre is drawn in two passes, because it was modelled in two halves. {@code ModelOgre2} — the
 * main model, on {@code <ogre>.png} — is the shoulder yoke, the arms and the feet, and its head slot
 * holds nothing but a 1x1x1 dot; {@code ModelOgre1} — the second pass, on {@code <ogre>b.png} —
 * carries the head, the torso and the shins. Drawing only the first is a headless ogre, which is
 * exactly what this used to do. The original's own condition for the second pass reads
 * {@code pass == 0 && !ogreboolean} — the flag suppresses the pass rather than enabling it, and
 * nothing but a saved NBT tag ever sets it — so it is always drawn here.
 * <p>
 * The swing is driven off {@link MobOgre#isOgreAttacking()}, which is a synched flag rather than the
 * server-only {@code attackTime}; the original triggered its block-smashing from inside the renderer
 * off a plain field, which cannot work once posing happens client-side.
 */
@Environment(EnvType.CLIENT)
public abstract class MobRendererOgreBase<T extends MobOgre> extends MobRendererBipedBase<T> {
	protected static final String MODEL_KEY = "main";
	/** The second half: head, torso and shins, on the mob's {@code b} texture. */
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

		// Yaw only. Both halves hang their head off a pivot down at the waist, which is where the
		// original put it, so a pitch applied there would swing the head out in front of the body
		// rather than tip it — and the original never pitched an ogre's head either.
		poseHead(model, headYaw, 0.0F);
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
