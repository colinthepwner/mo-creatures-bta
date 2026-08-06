package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobWerewolfWolf;

/**
 * The pack wolf.
 * <p>
 * The geometry does not exist in this repository — the asset bridge builds it from the player's own
 * copy of the original, whose {@code ModelWolf2} is Minecraft's quadruped with a wolf's head, body
 * and snout on it and {@code wolfa.png} painted for exactly that. Until the bridge has run there is
 * no model at all, which is what the null check below is for.
 * <p>
 * It used to share the transformed werewolf's geometry, which is why neither could be converted; they
 * are two models again, as they were in the original.
 * <p>
 * A wolf is drawn in two layers, because the original drew it as two models with a texture each:
 * {@code ModelWolf2} on {@code wolfa.png} and {@code ModelWolf1} on {@code wolfb.png}. The second is
 * not decoration — it carries the neck plate that joins the head to the body, which sit two units
 * apart in the first. {@code RenderWWolf}'s condition for that pass reads
 * {@code pass == 0 && !wolfboolean} — the flag suppresses the pass rather than enabling it, and
 * nothing but a saved NBT tag ever sets it — so it always draws, the same arrangement
 * {@link MobRendererOgreBase} uses.
 * <p>
 * Two things about the finished wolf are the original's shape rather than gaps here: it carries its
 * head high on the shoulders rather than forward on a neck, and it has no tail at all — neither
 * {@code ModelWolf1} nor {@code ModelWolf2} declares one, nor does the quadruped they extend.
 */
@Environment(EnvType.CLIENT)
public class MobRendererWerewolfWolf extends MobRenderer<MobWerewolfWolf> {
	private static final String MODEL_KEY = "main";
	/** The second half: neck plate, shoulder ruff and leg fur, on the wolf's {@code b} texture. */
	private static final String OVER_KEY = "over";
	private static final int LAYER_OVER = 1;
	private static final String OVER_TEXTURE = "/assets/creatures/textures/entity/werewolf_wolf/b_0.png";

	/** Front-left, front-right, back-left, back-right. */
	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

	public MobRendererWerewolfWolf() {
		super(0.7F);
		setModel(MODEL_KEY, "geometry.werewolf_wolf", 0.0D);
		setModel(OVER_KEY, "geometry.werewolf_wolf_over", 0.0D);
	}

	/** Only worth a second pass when there is a second geometry to draw with. */
	@Override
	protected int maxRenderLayer(MobWerewolfWolf entity) {
		return getModel(OVER_KEY) != null ? LAYER_OVER : 0;
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobWerewolfWolf entity, float partialTick, float unused, int layer) {
		if (layer == LAYER_OVER) {
			bindTexture(OVER_TEXTURE);
		}
		StaticEntityModel model = getModel(layer == LAYER_OVER ? OVER_KEY : MODEL_KEY);
		if (model == null) {
			return null;
		}
		model.resetBones();

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);
		float headYaw = getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick);
		float headPitch = getHeadPitch(entity, partialTick);

		BoneTransform head = model.getTransform("head");
		if (head != null) {
			head.rotX = headPitch * MathHelper.DEG_TO_RAD;
			head.rotY = headYaw * MathHelper.DEG_TO_RAD;
		}

		float swing = MathHelper.cos(limbSwing * 0.6662F) * 1.5F * limbYaw;
		float counterSwing = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.5F * limbYaw;

		setRotX(model, LEGS[0], swing);
		setRotX(model, LEGS[1], counterSwing);
		setRotX(model, LEGS[2], counterSwing);
		setRotX(model, LEGS[3], swing);

		// A hunting wolf carries its tail high and stiff. The original's wolf has no tail bone, so
		// this only bites on geometry that does.
		setRotX(model, "tail", 0.6F);

		return model;
	}

	private static void setRotX(StaticEntityModel model, String bone, float angle) {
		BoneTransform transform = model.getTransform(bone);
		if (transform != null) {
			transform.rotX = angle;
		}
	}
}
