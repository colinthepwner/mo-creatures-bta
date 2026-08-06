package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The unicorn differs from a plain horse only in its skin: the original built the horn into the one
 * head model every horse used and left it transparent on every sheet but the gold one, which is why
 * the converted head half needs no shape of its own here.
 */
@Environment(EnvType.CLIENT)
public class MobRendererHorseUnicorn extends MobRendererHorse {
	public MobRendererHorseUnicorn() {
		super("geometry.horse_unicorn",
			"geometry.horse_unicorn_head");
	}
}
