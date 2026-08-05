package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobShark;

/**
 * The shark sweeps its tail side to side, and the head follows a beat behind it — the whole animal
 * snakes rather than swinging from a fixed shoulder.
 */
@Environment(EnvType.CLIENT)
public class MobRendererShark extends MobRendererAquaticBase<MobShark> {
	private static final String[] TAIL = {"tail", "PTail", "LTail", "RTail", "UpperTailFin", "LowerTailFin"};
	private static final String[] SNOUT = {"head", "Head", "UHead", "LHead", "RHead"};

	public MobRendererShark() {
		super("/assets/creatures/models/entity/shark.json", 0.0D, 0.8F);
	}

	@Override
	protected String[] tailBones() {
		return TAIL;
	}

	@Override
	protected String[] headBones() {
		return SNOUT;
	}

	@Override
	protected float tailAmplitude() {
		return 0.6F;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobShark entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform head = bone(model, SNOUT);
		if (head != null) {
			// Counter-swing against the tail, so the body reads as one continuous S.
			head.rotY += MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.15F * Math.max(limbYaw, 0.25F);
		}
	}
}
