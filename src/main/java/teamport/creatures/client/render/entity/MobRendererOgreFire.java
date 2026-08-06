package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobOgreFire;

@Environment(EnvType.CLIENT)
public class MobRendererOgreFire extends MobRendererOgreBase<MobOgreFire> {
	public MobRendererOgreFire() {
		super("geometry.ogre_fire", "geometry.ogre_fire_over",
			"/assets/creatures/textures/entity/ogre_fire/b_0.png");
	}
}
