package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobBear;

/**
 * A bear is drawn in two layers, because the original drew it as two models.
 * <p>
 * {@code RenderBear} draws {@code ModelBear2} on {@code bear.png} and {@code ModelBear1} over it on
 * {@code bearb.png}, and the second pass is not a state: its condition is
 * {@code pass == 0 && !bearboolean}, and {@code bearboolean} is false at construction and only ever
 * set from a saved NBT tag, so it always draws — the same arrangement {@link MobRendererOgreBase}
 * and the pack wolf use.
 * <p>
 * That pass carries the bear's <b>ears</b>: {@code ModelBear1}'s head slot is a single flat 10x4x1
 * plate across the top of the head. Dropping it left the bear with no ears at all whenever the
 * player supplied the original archive, since the generated pack shadows this repo's own bear model.
 * <p>
 * It was previously read as an "angry coat" and {@code bearb.png} bound as a whole alternate skin.
 * It is not one: {@code EntityBear} holds a single texture string and never swaps it, and
 * {@code bearb.png} is 11% opaque against {@code bear.png}'s 63% — two ears and a patch of fur on an
 * otherwise empty sheet. Bound as a main skin it made an angry bear nearly transparent.
 */
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

	/** Only worth a second pass when the bridge has supplied the geometry to draw it with. */
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
		// The ear plate sits on the head slot, so it has to follow the head or it hangs in the air
		// where the head used to be. The legs are dropped in the manifest; there is nothing else to
		// pose on this half.
		poseHead(model, entity, getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick),
			getHeadPitch(entity, partialTick), partialTick);
		return model;
	}

	@Override
	protected float limbSwingAmplitude(MobBear entity, float partialTick) {
		// An angry bear charges with a much longer stride than one that is just wandering.
		return entity.isBearAngry() ? (float) ((22 * Math.PI) / 45) : (float) (Math.PI / 5);
	}
}
