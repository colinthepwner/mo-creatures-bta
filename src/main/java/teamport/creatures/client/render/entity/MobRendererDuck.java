package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobDuck;

/**
 * The original gave the duck no model of its own: it rendered on Minecraft's {@code ModelChicken},
 * and {@code duck.png} is painted for that layout. The asset bridge rebuilds exactly that geometry
 * from what BTA already ships and pairs it with the player's own copy of the skin.
 * <p>
 * Failing that there is nothing to draw a duck with, so it falls back to the bird — the two share a
 * bone layout (head, body, wingLeft, wingRight, legLeft, legRight) and the duck was a chicken to
 * begin with. The chicken has no tail bone; the tail tilt below simply finds nothing and does nothing.
 */
@Environment(EnvType.CLIENT)
public class MobRendererDuck extends MobRenderer<MobDuck> {
	private static final String MODEL_KEY = "main";
	private static final String MODEL_FALLBACK = "bird";

	public MobRendererDuck() {
		super(0.3F);
		setModel(MODEL_KEY, "/assets/creatures/models/entity/duck.json", 0.0D);
		setModel(MODEL_FALLBACK, "/assets/creatures/models/entity/bird.json", 0.0D);
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

		// Interpolated so the flap stays smooth independent of tick rate. Inherited from the chicken:
		// flapSpeed winds up while airborne and decays on the ground, so the wings only beat on a drop.
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
				// Paddling: the feet keep working under the surface whether or not the duck is
				// making headway, so this runs off the entity age rather than the limb swing.
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
			// Tail up out of the water while the duck floats.
			tail.rotX = -0.3F;
		}

		return model;
	}
}
