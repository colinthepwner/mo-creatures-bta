package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobFishy;

/**
 * The lil' fish has nothing but a body, a tail and four fins, so the shared pass covers all of it.
 * The stroke runs fast and wide because a small fish flicks rather than cruises.
 */
@Environment(EnvType.CLIENT)
public class MobRendererFishy extends MobRendererAquaticBase<MobFishy> {
	private static final String[] TAIL = {"tail", "Tail"};

	public MobRendererFishy() {
		super("/assets/creatures/models/entity/fishy.json", 0.0D, 0.2F);
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
