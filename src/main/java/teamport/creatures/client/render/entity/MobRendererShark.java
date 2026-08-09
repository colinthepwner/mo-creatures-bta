package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobShark;

@Environment(EnvType.CLIENT)
public class MobRendererShark extends MobRendererAquaticBase<MobShark> {

	private static final String[] TAIL = {"PTail"};

	public MobRendererShark() {
		super("geometry.shark", 0.0D, 0.6F);
	}

	@Override
	protected String[] tailBones() {
		return TAIL;
	}

	@Override
	protected float tailAmplitude() {
		return 0.6F;
	}
}
