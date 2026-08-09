package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobDolphin;

@Environment(EnvType.CLIENT)
public class MobRendererDolphin extends MobRendererAquaticBase<MobDolphin> {

	private static final String[] TAIL = {"PTail"};

	public MobRendererDolphin() {
		super("geometry.dolphin", 0.0D, 0.6F);
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
}
