package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobOgreCave;

@Environment(EnvType.CLIENT)
public class MobRendererOgreCave extends MobRendererOgreBase<MobOgreCave> {
	public MobRendererOgreCave() {
		super("geometry.ogre_cave", "geometry.ogre_cave_over",
			"/assets/creatures/textures/entity/ogre_cave/b_0.png");
	}
}
