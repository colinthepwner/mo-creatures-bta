package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MobRendererHorseNightmare extends MobRendererHorse {
	public MobRendererHorseNightmare() {
		super("geometry.horse_nightmare", "geometry.horse_nightmare_head");
	}
}
