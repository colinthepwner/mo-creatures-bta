package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;

@Environment(EnvType.CLIENT)
public class MobRendererHorse extends MobRendererQuadrupedBase<MobHorse> {
	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

	protected static final String HEAD_KEY = "head";
	private static final int LAYER_HEAD = 1;

	private static final float FOAL_SCALE = 0.4F;

	public MobRendererHorse() {
		this("geometry.horse", "geometry.horse_head");
	}

	protected MobRendererHorse(String bodyId, String headId) {
		super(bodyId, 0.0D, 0.5F);
		setModel(HEAD_KEY, headId, 0.0D);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected void preRenderTransform(MobHorse entity, double x, double y, double z, float rotation, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rotation, partialTick);

		if (!entity.isAdult()) {
			float scale = FOAL_SCALE + (1.0F - FOAL_SCALE) * entity.getGrowth();
			GLRenderer.modelM4f().scale(scale, scale, scale);
		}
	}

	@Override
	public float getShadowSize(MobHorse entity) {
		return entity.isAdult()
			? super.getShadowSize(entity)
			: super.getShadowSize(entity) * (FOAL_SCALE + (1.0F - FOAL_SCALE) * entity.getGrowth());
	}

	@Override
	protected int maxRenderLayer(MobHorse entity) {
		return getModel(HEAD_KEY) != null ? LAYER_HEAD : 0;
	}

	protected String headTexture(MobHorse entity) {
		return "/assets/creatures/textures/entity/horse/b_" + entity.textureIndex() + ".png";
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobHorse entity, float partialTick, float unused, int layer) {
		if (layer != LAYER_HEAD) {
			return super.getAndSetupModelForLayer(entity, partialTick, unused, layer);
		}

		StaticEntityModel head = getModel(HEAD_KEY);
		if (head == null) {

			return null;
		}
		bindTexture(headTexture(entity));
		head.resetBones();

		poseHead(head, entity, getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick),
			getHeadPitch(entity, partialTick), partialTick);

		poseExtra(head, entity, getLimbSwing(entity, partialTick), getLimbYaw(entity, partialTick), partialTick);
		return head;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobHorse entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {

			tail.rotY = MathHelper.cos(limbSwing * 0.3331F) * 0.25F;
			tail.rotX = -limbYaw * 0.15F;
		}
	}
}
