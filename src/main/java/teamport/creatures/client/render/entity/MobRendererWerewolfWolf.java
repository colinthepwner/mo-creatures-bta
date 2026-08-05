package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobWerewolfWolf;

/**
 * The pack wolf, on the same {@code werewolf_wolf} geometry the transformed werewolf uses.
 * <p>
 * That geometry does not exist in this repository — the asset bridge builds it from the player's own
 * copy — so which of the two plausible skeletons it turns out to have is not knowable from here. The
 * original had two candidates: a four-legged wolf ({@code legFrontLeft}…) and an upright beast
 * ({@code armLeft}, {@code legLeft}…). Both are posed, and whichever set of bones is absent is simply
 * skipped, so the renderer is correct either way.
 */
@Environment(EnvType.CLIENT)
public class MobRendererWerewolfWolf extends MobRenderer<MobWerewolfWolf> {
	private static final String MODEL_KEY = "main";

	/** Four-legged layout, in front-left, front-right, back-left, back-right order. */
	private static final String[] QUADRUPED_LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};
	/** Upright layout: arms lead, legs counter. */
	private static final String[] BIPED_LIMBS = {"armLeft", "armRight", "legLeft", "legRight"};

	public MobRendererWerewolfWolf() {
		super(0.5F);
		setModel(MODEL_KEY, "/assets/creatures/models/entity/werewolf_wolf.json", 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobWerewolfWolf entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
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

		setRotX(model, QUADRUPED_LEGS[0], swing);
		setRotX(model, QUADRUPED_LEGS[1], counterSwing);
		setRotX(model, QUADRUPED_LEGS[2], counterSwing);
		setRotX(model, QUADRUPED_LEGS[3], swing);

		setRotX(model, BIPED_LIMBS[0], counterSwing);
		setRotX(model, BIPED_LIMBS[1], swing);
		setRotX(model, BIPED_LIMBS[2], swing);
		setRotX(model, BIPED_LIMBS[3], counterSwing);

		// A hunting wolf carries its tail high and stiff.
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
