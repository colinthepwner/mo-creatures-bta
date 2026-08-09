package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MobRendererRatHell extends MobRendererRat {
	public MobRendererRatHell() {

		super("geometry.rat_hell", 0.4F, 1.3F);
	}
}
