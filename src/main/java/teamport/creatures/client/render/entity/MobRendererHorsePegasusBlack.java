package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;

@Environment(EnvType.CLIENT)
public class MobRendererHorsePegasusBlack extends MobRendererHorsePegasus {
	public MobRendererHorsePegasusBlack() {
		super("geometry.horse_pegasus_black", "geometry.horse_pegasus_black_head");
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobHorse entity, float limbSwing, float limbYaw, float partialTick) {
		super.poseExtra(model, entity, limbSwing, limbYaw, partialTick);
		MobRendererHorsePack.setVisible(model, "bagLeft", entity.isChested());
		MobRendererHorsePack.setVisible(model, "bagRight", entity.isChested());
	}
}
