package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;

/**
 * Shared base for the mod's two-legged mobs — the ogres and the werewolf.
 * <p>
 * The counterpart to {@link MobRendererQuadrupedBase}. Bone names follow the convention the mod's
 * existing geometry uses ({@code head}, {@code body}, {@code wingLeft}…), so a biped is expected to
 * carry {@code head}, {@code body}, {@code armLeft}, {@code armRight}, {@code legLeft} and
 * {@code legRight}. None of these models ship with the mod — the asset bridge supplies them from the
 * player's own copy of the original — so every lookup is guarded and a model that is missing a bone
 * simply does not get that part posed.
 */
@Environment(EnvType.CLIENT)
public abstract class MobRendererBipedBase<T extends Mob> extends MobRenderer<T> {
	protected static final String ARM_LEFT = "armLeft";
	protected static final String ARM_RIGHT = "armRight";
	protected static final String LEG_LEFT = "legLeft";
	protected static final String LEG_RIGHT = "legRight";

	protected MobRendererBipedBase(float shadowSize) {
		super(shadowSize);
	}

	protected static void poseHead(StaticEntityModel model, float headYaw, float headPitch) {
		BoneTransform head = model.getTransform("head");
		if (head != null) {
			head.rotX = headPitch * MathHelper.DEG_TO_RAD;
			head.rotY = headYaw * MathHelper.DEG_TO_RAD;
		}
	}

	/** The usual opposed arm/leg walk cycle. */
	protected static void poseWalk(StaticEntityModel model, float limbSwing, float limbYaw, float amplitude) {
		float swing = MathHelper.cos(limbSwing * 0.6662F) * amplitude * limbYaw;
		float counterSwing = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * amplitude * limbYaw;

		setRotX(model, LEG_LEFT, swing);
		setRotX(model, LEG_RIGHT, counterSwing);
		setRotX(model, ARM_LEFT, counterSwing);
		setRotX(model, ARM_RIGHT, swing);
	}

	/**
	 * Both arms up and over, for a mob that is mid-swing. {@code progress} runs 0 (arms down) to 1
	 * (fully raised).
	 */
	protected static void poseOverheadSwing(StaticEntityModel model, float progress) {
		float raise = -((float) Math.PI) * 0.85F * progress;
		setRotX(model, ARM_LEFT, raise);
		setRotX(model, ARM_RIGHT, raise);
	}

	/** Leans the whole body forward, which is what makes the werewolf read as stalking. */
	protected static void poseHunch(StaticEntityModel model, float radians) {
		BoneTransform body = model.getTransform("body");
		if (body != null) {
			body.rotX = radians;
		}
	}

	protected static void setRotX(StaticEntityModel model, String bone, float angle) {
		BoneTransform transform = model.getTransform(bone);
		if (transform != null) {
			transform.rotX = angle;
		}
	}
}
