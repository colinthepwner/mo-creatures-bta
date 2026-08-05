package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobDeerMoC;

/**
 * Renderer for the Mo' Creatures deer.
 * <p>
 * {@code neck} and {@code head} are siblings in the geometry — neither is parented to the other —
 * so only {@code head} is posed for looking around. Rotating {@code neck} as well would swing the
 * neck wedge out from underneath the head instead of carrying it, because the head does not inherit
 * that transform. The neck therefore stays at its authored -45 degree rest angle and the deer
 * swivels its head on top of it.
 * <p>
 * The antlers and ears <em>are</em> parented to {@code head} in the geometry and Dragonfly composes
 * parent transforms, so they follow the head for free.
 */
@Environment(EnvType.CLIENT)
public class MobRendererDeerMoC extends MobRendererQuadrupedBase<MobDeerMoC> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	public MobRendererDeerMoC() {
		super("/assets/creatures/models/entity/deer.json", 0.0D, 0.75F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobDeerMoC entity, float limbSwing, float limbYaw, float partialTick) {
		// Does have no antlers. The doe texture already leaves the antler UV block empty, but
		// hiding the bones outright saves rendering two fully transparent quads per deer.
		boolean buck = entity.isBuck();
		setVisible(model, "antlerLeft", buck);
		setVisible(model, "antlerRight", buck);

		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {
			// Flicks with the stride, and held higher while spooked — white-tails flag their tail
			// when they bolt.
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
