package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobOgre;

@Environment(EnvType.CLIENT)
public class MobRendererOgre extends MobRendererOgreBase<MobOgre> {
	public MobRendererOgre() {
		super("geometry.ogre", "geometry.ogre_over",
			"/assets/creatures/textures/entity/ogre/b_0.png");
	}
}
