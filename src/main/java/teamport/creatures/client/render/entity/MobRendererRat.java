package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
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
		this("geometry.rat", 0.2F, 0.8F);
	}

	protected MobRendererRat(String modelId, float shadowSize, float renderScale) {
		super(modelId, 0.0D, shadowSize);
		this.renderScale = renderScale;
	}

	/**
	 * How much of its modelled size a rat is actually drawn at. The geometry is a shared sheet the
	 * original scaled per mob at draw time — a rat at 0.8 and a hell rat at 1.3 off the same boxes —
	 * so without this every rat comes out at the hell rat's size.
	 */
	private final float renderScale;

	@Override
	protected void preRenderTransform(MobRat entity, double x, double y, double z, float rot, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rot, partialTick);
		GLRenderer.modelM4f().scale(renderScale, renderScale, renderScale);
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
