package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobWraith;

@Environment(EnvType.CLIENT)
public class MobRendererWraith extends MobRendererWraithBase<MobWraith> {
	public MobRendererWraith() {
		super("/assets/creatures/models/entity/wraith.json");
	}
}
