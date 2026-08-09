package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobSharkEgg;

@Environment(EnvType.CLIENT)
public class MobRendererSharkEgg extends MobRenderer<MobSharkEgg> {
	private static final String MODEL_KEY = "main";

	public MobRendererSharkEgg() {
		super(0.1F);
		setModel(MODEL_KEY, "geometry.shark_egg", 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobSharkEgg entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		model.resetBones();
		return model;
	}
}
