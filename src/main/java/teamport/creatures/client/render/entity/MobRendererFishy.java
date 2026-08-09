package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobFishy;

@Environment(EnvType.CLIENT)
public class MobRendererFishy extends MobRendererAquaticBase<MobFishy> {
	private static final String[] TAIL = {"tail", "Tail"};

	public MobRendererFishy() {
		super("geometry.fishy", 0.0D, 0.2F);
	}

	@Override
	protected String[] tailBones() {
		return TAIL;
	}

	@Override
	protected float tailAmplitude() {
		return 0.8F;
	}
}
