package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MobRendererHorseUnicorn extends MobRendererHorse {
	public MobRendererHorseUnicorn() {
		super("geometry.horse_unicorn",
			"geometry.horse_unicorn_head");
	}
}
