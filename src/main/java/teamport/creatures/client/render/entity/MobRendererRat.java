package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobRat;

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

		return 2.0F;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobRat entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {
			tail.rotY = MathHelper.cos(limbSwing * 0.6662F) * 0.6F;
		}

		if (entity.canClimb()) {
			BoneTransform body = model.getTransform("body");
			if (body != null) {
				body.rotX = CLIMB_PITCH;
			}
		}
	}
}
