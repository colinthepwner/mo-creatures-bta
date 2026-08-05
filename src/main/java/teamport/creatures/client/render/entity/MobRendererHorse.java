package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;

/**
 * Also the base for the unicorn and pegasus renderers, which differ only in geometry.
 * <p>
 * The horse geometry keeps the muzzle, ears and upper neck together on the {@code head} bone and
 * uses {@code neck} for the fixed chest wedge, so only {@code head} is posed for looking around —
 * which is what {@link MobRendererQuadrupedBase} does by default.
 */
@Environment(EnvType.CLIENT)
public class MobRendererHorse extends MobRendererQuadrupedBase<MobHorse> {
	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

	public MobRendererHorse() {
		this("/assets/creatures/models/entity/horse.json");
	}

	protected MobRendererHorse(String modelPath) {
		super(modelPath, 0.0D, 0.75F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobHorse entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {
			// Swishes side to side with the stride and lifts a little at a trot.
			tail.rotY = MathHelper.cos(limbSwing * 0.3331F) * 0.25F;
			tail.rotX = -limbYaw * 0.15F;
		}
	}
}
