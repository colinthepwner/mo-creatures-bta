package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobBoar;

@Environment(EnvType.CLIENT)
public class MobRendererBoar extends MobRendererQuadrupedBase<MobBoar> {
	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

	public MobRendererBoar() {
		super("geometry.boar", 0.0D, 0.7F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected float limbSwingAmplitude(MobBoar entity, float partialTick) {
		// A provoked boar charges, so it covers ground with a much longer stride than one rooting around.
		return entity.isBoarAngry() ? 2.0F : 1.4F;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobBoar entity, float limbSwing, float limbYaw, float partialTick) {
		if (!entity.isBoarAngry()) {
			return;
		}

		// Head down, tusks forward, so the charge reads as a charge even when the boar is standing still.
		BoneTransform head = model.getTransform("head");
		if (head != null) {
			head.rotX += 0.35F;
		}
	}
}
