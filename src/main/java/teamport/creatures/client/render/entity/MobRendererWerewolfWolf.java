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
 */
@Environment(EnvType.CLIENT)
public class MobRendererWerewolfWolf extends MobRenderer<MobWerewolfWolf> {
	private static final String MODEL_KEY = "main";

	/** Front-left, front-right, back-left, back-right. */
	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

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
