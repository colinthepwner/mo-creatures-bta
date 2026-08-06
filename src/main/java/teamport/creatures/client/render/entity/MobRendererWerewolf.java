package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobWerewolf;

/**
 * Draws whichever of the werewolf's two shapes it is currently wearing.
 * <p>
 * This is the reason the form flag has to be synched: the renderer picks between the {@code werewolf}
 * geometry (the man) and {@code werewolf_beast} purely from {@link MobWerewolf#isTransformed()}. Both
 * are loaded up front so the swap costs nothing mid-change.
 * <p>
 * The beast used to share the pack wolf's {@code werewolf_wolf} geometry, which meant neither could
 * be converted: the original drew them as two models with a skin painted for each. They are two here
 * now, so both convert.
 */
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
			// A shudder that runs through the whole body while it changes.
			float shudder = MathHelper.sin((entity.tickCount + partialTick) * 1.7F) * 0.25F;
			poseHunch(model, shudder);
		} else if (beast && entity.isHunched()) {
			// Down on its knuckles while it closes on something; the arms keep swinging under it.
			poseHunch(model, 0.5F);
		}

		if (beast) {
			// Sway only. The tail's resting angle is carried by the geometry, which is where the
			// original put it, and Dragonfly adds this on top rather than replacing it.
			setRotX(model, "tail", MathHelper.cos(limbSwing * 0.6662F) * 0.2F * limbYaw);
		}

		return model;
	}
}
