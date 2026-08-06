package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;
import teamport.creatures.core.entity.mob.MobHorsePegasus;

/**
 * Adds the wing beat on top of the shared horse pose.
 * <p>
 * BTA 8.0 dropped Dragonfly's {@code AnimationState}, so the beat that
 * {@code animation/horse_pegasus.animation.json} used to describe is reproduced here from the phase
 * and spread {@link MobHorsePegasus} ticks: a 60 degree sweep, one beat per 20-tick cycle, folded
 * away while the pegasus is on the ground.
 * <p>
 * The wings sit on whichever half of the horse carries them — the built-in combined model on layer 0,
 * or the converted head half on layer 1 — and {@code poseExtra} runs over both, so this needs no idea
 * of which is in play.
 */
@Environment(EnvType.CLIENT)
public class MobRendererHorsePegasus extends MobRendererHorse {
	private static final float WING_SWEEP = 60.0F * MathHelper.DEG_TO_RAD;

	public MobRendererHorsePegasus() {
		super("geometry.horse_pegasus",
			"geometry.horse_pegasus_head");
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobHorse entity, float limbSwing, float limbYaw, float partialTick) {
		super.poseExtra(model, entity, limbSwing, limbYaw, partialTick);

		if (!(entity instanceof MobHorsePegasus)) return;
		MobHorsePegasus pegasus = (MobHorsePegasus) entity;

		BoneTransform wingLeft = model.getTransform("wingLeft");
		BoneTransform wingRight = model.getTransform("wingRight");
		if (wingLeft == null || wingRight == null) return;

		// Interpolated so the beat stays smooth independent of tick rate.
		float flap = pegasus.oWingFlap + (pegasus.wingFlap - pegasus.oWingFlap) * partialTick;
		float spread = pegasus.oWingSpread + (pegasus.wingSpread - pegasus.oWingSpread) * partialTick;

		float beat = MathHelper.sin(flap * (float) Math.PI) * spread * WING_SWEEP;
		wingLeft.rotZ = -beat;
		wingRight.rotZ = beat;
	}
}
