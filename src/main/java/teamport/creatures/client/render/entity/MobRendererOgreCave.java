package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobOgreCave;

@Environment(EnvType.CLIENT)
public class MobRendererOgreCave extends MobRendererOgreBase<MobOgreCave> {
	public MobRendererOgreCave() {
		super("/assets/creatures/models/entity/ogre_cave.json");
	}
}
