package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import teamport.creatures.core.entity.mob.MobWraithFlame;

@Environment(EnvType.CLIENT)
public class MobRendererWraithFlame extends MobRendererWraithBase<MobWraithFlame> {
	public MobRendererWraithFlame() {
		super("geometry.wraith_flame");
	}
}
