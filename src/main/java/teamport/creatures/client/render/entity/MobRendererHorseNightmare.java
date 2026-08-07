package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The nightmare is a plain horse in a different coat — no horn, no wings, no panniers — so it needs
 * nothing here beyond its own geometry, exactly as the unicorn does.
 */
@Environment(EnvType.CLIENT)
public class MobRendererHorseNightmare extends MobRendererHorse {
	public MobRendererHorseNightmare() {
		super("geometry.horse_nightmare", "geometry.horse_nightmare_head");
	}
}
