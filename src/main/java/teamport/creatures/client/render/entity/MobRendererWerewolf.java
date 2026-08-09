package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobWerewolf;

@Environment(EnvType.CLIENT)
public class MobRendererWerewolf extends MobRendererBipedBase<MobWerewolf> {
	private static final String MODEL_HUMAN = "human";
	private static final String MODEL_BEAST = "beast";

	public MobRendererWerewolf() {
		super(0.7F);
		setModel(MODEL_HUMAN, "geometry.werewolf", 0.0D);
		setModel(MODEL_BEAST, "geometry.werewolf_beast", 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobWerewolf entity, float partialTick, float unused, int layer) {
		boolean beast = entity.isTransformed();
		StaticEntityModel model = getModel(beast ? MODEL_BEAST : MODEL_HUMAN);
		if (model == null) {
			return null;
		}
		model.resetBones();

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);
		float headYaw = getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick);
		float headPitch = getHeadPitch(entity, partialTick);

		poseHead(model, headYaw, headPitch);
		poseWalk(model, limbSwing, limbYaw, beast ? 1.5F : 1.0F);

		if (entity.isTransforming()) {

			float shudder = MathHelper.sin((entity.tickCount + partialTick) * 1.7F) * 0.25F;
			poseHunch(model, shudder);
		} else if (beast && entity.isHunched()) {

			poseHunch(model, 0.5F);
		}

		if (beast) {

			setRotX(model, "tail", MathHelper.cos(limbSwing * 0.6662F) * 0.2F * limbYaw);
		}

		return model;
	}
}
