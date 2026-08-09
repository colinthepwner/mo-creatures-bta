package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobMouse;

@Environment(EnvType.CLIENT)
public class MobRendererMouse extends MobRendererQuadrupedBase<MobMouse> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	private static final float CARRIED_ROLL = (float) Math.PI;

	private static final float RENDER_SCALE = 0.6F;

	public MobRendererMouse() {
		super("geometry.mouse", 0.0D, 0.1F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected float limbSwingAmplitude(MobMouse entity, float partialTick) {
		return 2.2F;
	}

	@Override
	protected void preRenderTransform(MobMouse entity, double x, double y, double z, float rot, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rot, partialTick);
		GLRenderer.modelM4f().scale(RENDER_SCALE, RENDER_SCALE, RENDER_SCALE);
		if (entity.isCarried()) {
			GLRenderer.modelM4f().rotateZ(CARRIED_ROLL);
		}
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobMouse entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {
			tail.rotY = MathHelper.cos(limbSwing * 0.6662F) * 0.7F;
		}

		if (entity.isCarried()) {
			for (String leg : LEGS) {
				setLegAngle(model, leg, 0.0F);
			}
		}
	}
}
