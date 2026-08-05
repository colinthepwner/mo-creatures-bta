package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobRat;

/**
 * Rats run on the shared quadruped cycle. What is theirs is the tail, which whips about far more than
 * a fox's, and the way the body pitches up when the rat is climbing a wall.
 * <p>
 * {@link MobRendererRatHell} reuses all of this against its own geometry, so the model path is a
 * constructor argument rather than a constant.
 */
@Environment(EnvType.CLIENT)
public class MobRendererRat extends MobRendererQuadrupedBase<MobRat> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	private static final float CLIMB_PITCH = (float) Math.toRadians(-45.0);

	public MobRendererRat() {
		this("/assets/creatures/models/entity/rat.json", 0.3F);
	}

	protected MobRendererRat(String modelPath, float shadowSize) {
		super(modelPath, 0.0D, shadowSize);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected float limbSwingAmplitude(MobRat entity, float partialTick) {
		// Rats scurry: short, very quick steps rather than a long stride.
		return 2.0F;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobRat entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {
			tail.rotY = MathHelper.cos(limbSwing * 0.6662F) * 0.6F;
		}

		// Climbing a wall tips the whole body up so the rat is not running along in mid-air.
		if (entity.canClimb()) {
			BoneTransform body = model.getTransform("body");
			if (body != null) {
				body.rotX = CLIMB_PITCH;
			}
		}
	}
}
