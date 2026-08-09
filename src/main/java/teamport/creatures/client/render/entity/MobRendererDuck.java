package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobDuck;

@Environment(EnvType.CLIENT)
public class MobRendererDuck extends MobRenderer<MobDuck> {
	private static final String MODEL_KEY = "main";
	private static final String MODEL_FALLBACK = "bird";

	public MobRendererDuck() {
		super(0.3F);
		setModel(MODEL_KEY, "geometry.duck", 0.0D);
		setModel(MODEL_FALLBACK, "geometry.bird", 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobDuck entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		if (model == null) {
			model = getModel(MODEL_FALLBACK);
		}
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

		float flap = entity.oFlap + (entity.flap - entity.oFlap) * partialTick;
		float flapSpeed = entity.oFlapSpeed + (entity.flapSpeed - entity.oFlapSpeed) * partialTick;
		float beat = MathHelper.sin(flap) * flapSpeed;

		BoneTransform wingLeft = model.getTransform("wingLeft");
		BoneTransform wingRight = model.getTransform("wingRight");
		if (wingLeft != null && wingRight != null) {
			wingLeft.rotZ = -beat;
			wingRight.rotZ = beat;
		}

		BoneTransform legLeft = model.getTransform("legLeft");
		BoneTransform legRight = model.getTransform("legRight");
		if (legLeft != null && legRight != null) {
			if (entity.isInWater()) {

				float paddle = MathHelper.cos(entity.tickCount * 0.5F) * 0.6F;
				legLeft.rotX = paddle;
				legRight.rotX = -paddle;
			} else {
				legLeft.rotX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
				legRight.rotX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbYaw;
			}
		}

		BoneTransform tail = model.getTransform("tail");
		if (tail != null && entity.isInWater()) {

			tail.rotX = -0.3F;
		}

		return model;
	}
}
