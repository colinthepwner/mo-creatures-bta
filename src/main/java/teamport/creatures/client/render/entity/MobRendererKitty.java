package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobKitty;

@Environment(EnvType.CLIENT)
public class MobRendererKitty extends MobRendererQuadrupedBase<MobKitty> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	private static final float SIT_BODY_PITCH = (float) Math.toRadians(-20.0);
	private static final float SIT_BACK_LEG_PITCH = (float) Math.toRadians(-90.0);

	public MobRendererKitty() {
		super("geometry.kitty", 0.0D, 0.4F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobKitty entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {
			tail.rotX = entity.getTailRotation() / 5.0F;
		}

		if (!entity.isSitting) {
			return;
		}

		BoneTransform body = model.getTransform("body");
		if (body != null) {
			body.rotX = SIT_BODY_PITCH;
		}

		tuckBackLeg(model, "legLeftBack");
		tuckBackLeg(model, "legRightBack");

		if (tail != null) {
			tail.posY = -4.0;
			tail.posZ = -1.0;
		}
	}

	private static void tuckBackLeg(StaticEntityModel model, String bone) {
		BoneTransform leg = model.getTransform(bone);
		if (leg != null) {
			leg.rotX = SIT_BACK_LEG_PITCH;
			leg.posY = -2.0;
		}
	}
}
