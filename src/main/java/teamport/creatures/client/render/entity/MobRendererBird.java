package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobBird;

@Environment(EnvType.CLIENT)
public class MobRendererBird extends MobRenderer<MobBird> {
	private static final String MODEL_KEY = "main";

	public MobRendererBird() {
		super(0.25F);
		setModel(MODEL_KEY, "/assets/creatures/models/entity/bird.json", 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobBird entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
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

		// Interpolated flap so wing motion stays smooth independent of tick rate.
		float flap = entity.oFlap + (entity.flap - entity.oFlap) * partialTick;
		boolean flying = entity.isBirdFlying();

		BoneTransform wingLeft = model.getTransform("wingLeft");
		BoneTransform wingRight = model.getTransform("wingRight");
		if (wingLeft != null && wingRight != null) {
			if (flying) {
				float beat = MathHelper.sin(flap * 2.0F) * 0.9F;
				wingLeft.rotZ = -beat;
				wingRight.rotZ = beat;
			} else {
				// Folded against the body while perched.
				wingLeft.rotZ = -0.15F;
				wingRight.rotZ = 0.15F;
			}
		}

		BoneTransform legLeft = model.getTransform("legLeft");
		BoneTransform legRight = model.getTransform("legRight");
		if (legLeft != null && legRight != null) {
			if (flying) {
				// Tucked back in flight.
				legLeft.rotX = 0.8F;
				legRight.rotX = 0.8F;
			} else {
				legLeft.rotX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
				legRight.rotX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbYaw;
			}
		}

		BoneTransform tail = model.getTransform("tail");
		if (tail != null && flying) {
			tail.rotX = -0.2F;
		}

		return model;
	}
}
