package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;
import teamport.creatures.core.entity.mob.MobHorsePegasus;

@Environment(EnvType.CLIENT)
public class MobRendererHorsePegasus extends MobRendererHorse {
	private static final float WING_SWEEP = 60.0F * MathHelper.DEG_TO_RAD;

	public MobRendererHorsePegasus() {
		super("geometry.horse_pegasus",
			"geometry.horse_pegasus_head");
	}

	protected MobRendererHorsePegasus(String bodyId, String headId) {
		super(bodyId, headId);
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobHorse entity, float limbSwing, float limbYaw, float partialTick) {
		super.poseExtra(model, entity, limbSwing, limbYaw, partialTick);

		if (!(entity instanceof MobHorsePegasus)) return;
		MobHorsePegasus pegasus = (MobHorsePegasus) entity;

		BoneTransform wingLeft = model.getTransform("wingLeft");
		BoneTransform wingRight = model.getTransform("wingRight");
		if (wingLeft == null || wingRight == null) return;

		float flap = pegasus.oWingFlap + (pegasus.wingFlap - pegasus.oWingFlap) * partialTick;
		float spread = pegasus.oWingSpread + (pegasus.wingSpread - pegasus.oWingSpread) * partialTick;

		float beat = MathHelper.sin(flap * (float) Math.PI) * spread * WING_SWEEP;
		wingLeft.rotZ = -beat;
		wingRight.rotZ = beat;
	}
}
