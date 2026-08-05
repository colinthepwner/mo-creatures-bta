package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class MobRendererHorseUnicorn extends MobRendererHorse {
	public MobRendererHorseUnicorn() {
		super("/assets/creatures/models/entity/horse_unicorn.json");
	}
}
