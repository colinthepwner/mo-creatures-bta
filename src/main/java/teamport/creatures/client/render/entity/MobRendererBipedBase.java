package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;

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

	protected static void poseWalk(StaticEntityModel model, float limbSwing, float limbYaw, float amplitude) {
		float swing = MathHelper.cos(limbSwing * 0.6662F) * amplitude * limbYaw;
		float counterSwing = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * amplitude * limbYaw;

		setRotX(model, LEG_LEFT, swing);
		setRotX(model, LEG_RIGHT, counterSwing);
		setRotX(model, ARM_LEFT, counterSwing);
		setRotX(model, ARM_RIGHT, swing);
	}

	protected static void poseOverheadSwing(StaticEntityModel model, float progress) {
		float raise = -((float) Math.PI) * 0.85F * progress;
		setRotX(model, ARM_LEFT, raise);
		setRotX(model, ARM_RIGHT, raise);
	}

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
