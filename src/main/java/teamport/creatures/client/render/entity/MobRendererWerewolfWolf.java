package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobWerewolfWolf;

@Environment(EnvType.CLIENT)
public class MobRendererWerewolfWolf extends MobRenderer<MobWerewolfWolf> {
	private static final String MODEL_KEY = "main";

	private static final String OVER_KEY = "over";
	private static final int LAYER_OVER = 1;
	private static final String OVER_TEXTURE = "/assets/creatures/textures/entity/werewolf_wolf/b_0.png";

	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

	public MobRendererWerewolfWolf() {
		super(0.7F);
		setModel(MODEL_KEY, "geometry.werewolf_wolf", 0.0D);
		setModel(OVER_KEY, "geometry.werewolf_wolf_over", 0.0D);
	}

	@Override
	protected int maxRenderLayer(MobWerewolfWolf entity) {
		return getModel(OVER_KEY) != null ? LAYER_OVER : 0;
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobWerewolfWolf entity, float partialTick, float unused, int layer) {
		if (layer == LAYER_OVER) {
			bindTexture(OVER_TEXTURE);
		}
		StaticEntityModel model = getModel(layer == LAYER_OVER ? OVER_KEY : MODEL_KEY);
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
