package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobWraith;

@Environment(EnvType.CLIENT)
public abstract class MobRendererWraithBase<T extends MobWraith> extends MobRenderer<T> {
	protected static final String MODEL_KEY = "main";

	protected static final double BOB_AMOUNT = 0.12D;

	protected static final float BOB_PERIOD = 40.0F;

	protected MobRendererWraithBase(String modelId) {

		super(0.0F);
		setModel(MODEL_KEY, modelId, 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(T entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		if (model == null) {
			return null;
		}
		model.resetBones();

		float age = entity.tickCount + partialTick;
		float phase = age / BOB_PERIOD * (float) Math.PI * 2.0F;
		float sway = MathHelper.sin(phase) * 0.15F;

		float headYaw = getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick);
		float headPitch = getHeadPitch(entity, partialTick);

		BoneTransform head = model.getTransform("head");
		if (head != null) {
			head.rotX = headPitch * MathHelper.DEG_TO_RAD;
			head.rotY = headYaw * MathHelper.DEG_TO_RAD;
		}

		BoneTransform body = model.getTransform("body");
		if (body != null) {
			body.posY += MathHelper.sin(phase) * BOB_AMOUNT;
			body.rotZ = sway * 0.5F;
		}

		setRot(model, "armLeft", sway, 0.4F);
		setRot(model, "armRight", -sway, -0.4F);

		setRot(model, "legLeft", 0.35F + sway, 0.0F);
		setRot(model, "legRight", 0.35F - sway, 0.0F);

		return model;
	}

	protected static void setRot(StaticEntityModel model, String bone, float rotX, float rotZ) {
		BoneTransform transform = model.getTransform(bone);
		if (transform != null) {
			transform.rotX = rotX;
			transform.rotZ = rotZ;
		}
	}
}
