package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobFishyEgg;

/**
 * Roe sits where it lands. Like the shark's egg case there is nothing to animate, so this only
 * resolves the geometry.
 */
@Environment(EnvType.CLIENT)
public class MobRendererFishyEgg extends MobRenderer<MobFishyEgg> {
	private static final String MODEL_KEY = "main";

	public MobRendererFishyEgg() {
		super(0.1F);
		setModel(MODEL_KEY, "geometry.fishy_egg", 0.0D);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobFishyEgg entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		model.resetBones();
		return model;
	}
}
