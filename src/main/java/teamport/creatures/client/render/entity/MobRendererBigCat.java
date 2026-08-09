package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobBigCat;

@Environment(EnvType.CLIENT)
public class MobRendererBigCat extends MobRendererQuadrupedBase<MobBigCat> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	private static final String MANE_KEY = "mane";
	private static final int LAYER_MANE = 1;

	private static final float SIT_BODY_PITCH = (float) Math.toRadians(-25.0);
	private static final float SIT_BACK_LEG_PITCH = (float) Math.toRadians(-90.0);

	public MobRendererBigCat() {
		super("geometry.bigcat", 0.0D, 0.7F);
		setModel(MANE_KEY, "geometry.bigcat_maned", 0.0D);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected int maxRenderLayer(MobBigCat entity) {
		return entity.hasMane() ? LAYER_MANE : 0;
	}

	@Override
	protected void preRenderTransform(MobBigCat entity, double x, double y, double z, float rot, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rot, partialTick);
		GLRenderer.modelM4f().scale(entity.getRenderScaleX(), entity.getRenderScaleY(), entity.getRenderScaleZ());
	}

	@Override
	public float getShadowSize(MobBigCat entity) {
		return super.getShadowSize(entity) * entity.getRenderScaleX();
	}

	@Override
	protected float limbSwingAmplitude(MobBigCat entity, float partialTick) {

		return entity.getTarget() != null ? 1.6F : 0.9F;
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobBigCat entity, float partialTick, float unused, int layer) {
		if (layer == LAYER_MANE) {
			if (!entity.hasMane()) return null;
			bindTexture(entity.getManeTexture());

			StaticEntityModel mane = getModel(MANE_KEY);
			if (mane == null) return null;
			mane.resetBones();
			poseHead(mane, entity, getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick),
				getHeadPitch(entity, partialTick), partialTick);
			return mane;
		}

		return super.getAndSetupModelForLayer(entity, partialTick, unused, layer);
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobBigCat entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {

			float sway = entity.getTarget() != null ? 0.5F : 0.25F;
			tail.rotY = MathHelper.cos(limbSwing * 0.3331F) * sway;
		}

		if (!entity.isSitting()) return;

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
