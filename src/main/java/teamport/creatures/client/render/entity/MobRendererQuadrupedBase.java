package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;

/**
 * Shared base for the mod's four-legged mobs.
 * <p>
 * BTA 8.0 dropped the old {@code ModelBase}/{@code Cube} system entirely; entity models are now
 * Bedrock-format geometry loaded through Dragonfly and posed by mutating {@link BoneTransform}s.
 * The bone names differ between the models inherited from the 7.2 branch (some use
 * {@code legLeftFront}, others {@code legFrontLeft}), so subclasses supply their own names
 * rather than this class assuming a single convention.
 */
@Environment(EnvType.CLIENT)
public abstract class MobRendererQuadrupedBase<T extends Mob> extends MobRenderer<T> {
	protected static final String MODEL_KEY = "main";

	protected MobRendererQuadrupedBase(String modelPath, double inflation, float shadowSize) {
		super(shadowSize);
		setModel(MODEL_KEY, modelPath, inflation);
	}

	/** Front-left, front-right, back-left, back-right, in that order. */
	protected abstract String[] legBones();

	/** Amplitude of the leg swing; angrier/faster mobs override this. */
	protected float limbSwingAmplitude(T entity, float partialTick) {
		return 1.4F;
	}

	protected void poseHead(StaticEntityModel model, T entity, float headYaw, float headPitch, float partialTick) {
		BoneTransform head = model.getTransform("head");
		if (head != null) {
			head.rotX = headPitch * MathHelper.DEG_TO_RAD;
			head.rotY = headYaw * MathHelper.DEG_TO_RAD;
		}
	}

	/** Applies the standard alternating four-leg walk cycle. */
	protected void poseLegs(StaticEntityModel model, T entity, float limbSwing, float limbYaw, float partialTick) {
		String[] legs = legBones();
		float amplitude = limbSwingAmplitude(entity, partialTick);

		setLegAngle(model, legs[0], MathHelper.cos(limbSwing * 0.6662F) * amplitude * limbYaw);
		setLegAngle(model, legs[1], MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * amplitude * limbYaw);
		setLegAngle(model, legs[2], MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * amplitude * limbYaw);
		setLegAngle(model, legs[3], MathHelper.cos(limbSwing * 0.6662F) * amplitude * limbYaw);
	}

	protected static void setLegAngle(StaticEntityModel model, String bone, float angle) {
		BoneTransform transform = model.getTransform(bone);
		if (transform != null) {
			transform.rotX = angle;
		}
	}

	/** Hook for subclasses to pose extra bones (tails, wings, ears) after the shared pass. */
	protected void poseExtra(StaticEntityModel model, T entity, float limbSwing, float limbYaw, float partialTick) {
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(T entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		model.resetBones();

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);
		float headYaw = getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick);
		float headPitch = getHeadPitch(entity, partialTick);

		poseHead(model, entity, headYaw, headPitch, partialTick);
		poseLegs(model, entity, limbSwing, limbYaw, partialTick);
		poseExtra(model, entity, limbSwing, limbYaw, partialTick);

		return model;
	}
}
