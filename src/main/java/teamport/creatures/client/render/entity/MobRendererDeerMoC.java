package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobDeerMoC;

@Environment(EnvType.CLIENT)
public class MobRendererDeerMoC extends MobRendererQuadrupedBase<MobDeerMoC> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	private static final float BUCK_SCALE = 1.7F;
	private static final float DOE_SCALE = 1.3F;

	public MobRendererDeerMoC() {
		super("geometry.deer", 0.0D, 0.5F);
	}

	@Override
	protected void preRenderTransform(MobDeerMoC entity, double x, double y, double z, float rot, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rot, partialTick);
		float scale = entity.isBuck() ? BUCK_SCALE : DOE_SCALE;
		GLRenderer.modelM4f().scale(scale, scale, scale);
	}

	@Override
	public float getShadowSize(MobDeerMoC entity) {
		return super.getShadowSize(entity) * (entity.isBuck() ? BUCK_SCALE : DOE_SCALE);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobDeerMoC entity, float limbSwing, float limbYaw, float partialTick) {

		boolean buck = entity.isBuck();
		setVisible(model, "antlerLeft", buck);
		setVisible(model, "antlerRight", buck);

		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {

			tail.rotY = MathHelper.cos(limbSwing * 0.6662F) * 0.2F;
			if (entity.isScared()) {
				tail.rotX = -0.5F;
			}
		}
	}

	private static void setVisible(StaticEntityModel model, String bone, boolean visible) {
		BoneTransform transform = model.getTransform(bone);
		if (transform != null) {
			transform.visible = visible;
		}
	}
}
