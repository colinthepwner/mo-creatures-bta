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
		// Same boxes as the common rat, drawn half again as large: that is the whole difference in
		// build between the two, and the original made it here rather than in the model.
		super("geometry.rat_hell", 0.4F, 1.3F);
	}
}
