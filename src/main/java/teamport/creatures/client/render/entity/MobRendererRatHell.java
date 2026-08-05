package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The hell rat is the same animal drawn bigger, so it only swaps in its own geometry and a wider
 * shadow; the walk cycle, tail and climb pitch all come from {@link MobRendererRat}.
 */
@Environment(EnvType.CLIENT)
public class MobRendererRatHell extends MobRendererRat {
	public MobRendererRatHell() {
		super("/assets/creatures/models/entity/rat_hell.json", 0.4F);
	}
}
