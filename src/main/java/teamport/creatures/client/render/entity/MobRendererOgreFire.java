package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobOgreFire;

@Environment(EnvType.CLIENT)
public class MobRendererOgreFire extends MobRendererOgreBase<MobOgreFire> {
	public MobRendererOgreFire() {
		super("/assets/creatures/models/entity/ogre_fire.json");
	}
}
