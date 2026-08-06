package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobBunny;

@Environment(EnvType.CLIENT)
public class MobRendererBunny extends MobRenderer<MobBunny> {
	private static final String MODEL_KEY = "main";

	public MobRendererBunny() {
		super(0.3F);
		setModel(MODEL_KEY, "geometry.bunny", 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobBunny entity, float partialTick, float unused, int layer) {
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

		// The bunny model has no separate leg bones, so movement is sold as a hop:
		// the whole body arcs up and pitches forward over the stride.
		BoneTransform body = model.getTransform("body");
		if (body != null && limbYaw > 0.0F) {
			float hop = MathHelper.sin(limbSwing * 0.6662F);
			body.posY = Math.abs(hop) * 3.0F * limbYaw;
			body.rotX = -hop * 0.35F * limbYaw;
		}

		return model;
	}
}
