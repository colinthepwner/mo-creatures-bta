package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobMouse;

/**
 * The mouse runs on the shared quadruped cycle. The one thing it does that nothing else in the mod
 * does is hang upside down: picked up, it dangles by the tail from the carrier's hand, so the whole
 * model is rolled over.
 * <p>
 * That flip reads {@link MobMouse#isCarried()}, which is synched — a plain field would leave every
 * client rendering carried mice the right way up.
 */
@Environment(EnvType.CLIENT)
public class MobRendererMouse extends MobRendererQuadrupedBase<MobMouse> {
	private static final String[] LEGS = {"legLeftFront", "legRightFront", "legLeftBack", "legRightBack"};

	private static final float CARRIED_ROLL = (float) Math.PI;

	public MobRendererMouse() {
		super("/assets/creatures/models/entity/mouse.json", 0.0D, 0.2F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected float limbSwingAmplitude(MobMouse entity, float partialTick) {
		return 2.2F;
	}

	/** Dangling by the tail: rolled over, and hanging a little below where it would otherwise sit. */
	@Override
	protected void preRenderTransform(MobMouse entity, double x, double y, double z, float rot, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rot, partialTick);
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

		// Held up by the tail, the legs go limp rather than carrying on with the walk cycle.
		if (entity.isCarried()) {
			for (String leg : LEGS) {
				setLegAngle(model, leg, 0.0F);
			}
		}
	}
}
