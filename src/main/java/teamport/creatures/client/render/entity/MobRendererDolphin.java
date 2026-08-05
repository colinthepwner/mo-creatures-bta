package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobDolphin;

/**
 * A dolphin is a mammal: its flukes are horizontal and beat up and down, unlike every fish in this
 * batch. The original's model splits the tail into a peduncle and left and right flukes, so all
 * three are driven together.
 */
@Environment(EnvType.CLIENT)
public class MobRendererDolphin extends MobRendererAquaticBase<MobDolphin> {
	private static final String[] TAIL = {"tail", "PTail", "LTail", "RTail"};
	private static final String[] DORSAL = {"finUpper", "UpperFin", "upperFin"};

	public MobRendererDolphin() {
		super("/assets/creatures/models/entity/dolphin.json", 0.0D, 0.7F);
	}

	@Override
	protected String[] tailBones() {
		return TAIL;
	}

	@Override
	protected boolean tailBeatsVertically() {
		return true;
	}

	@Override
	protected float tailAmplitude() {
		return 0.45F;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobDolphin entity, float limbSwing, float limbYaw, float partialTick) {
		// The dorsal fin rocks a little out of phase with the flukes, which sells the body flexing
		// through the stroke rather than swinging as one rigid piece.
		BoneTransform dorsal = bone(model, DORSAL);
		if (dorsal != null) {
			dorsal.rotZ = MathHelper.sin(limbSwing * 0.6662F) * 0.12F * Math.max(limbYaw, 0.25F);
		}
	}
}
