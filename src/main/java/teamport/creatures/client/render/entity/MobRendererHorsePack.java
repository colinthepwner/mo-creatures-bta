package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;

@Environment(EnvType.CLIENT)
public class MobRendererHorsePack extends MobRendererHorse {
	public MobRendererHorsePack() {
		super("geometry.horse_pack", "geometry.horse_pack_head");
	}

	protected MobRendererHorsePack(String bodyId, String headId) {
		super(bodyId, headId);
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobHorse entity, float limbSwing, float limbYaw, float partialTick) {
		super.poseExtra(model, entity, limbSwing, limbYaw, partialTick);
		setVisible(model, "bagLeft", entity.isChested());
		setVisible(model, "bagRight", entity.isChested());
	}

	static void setVisible(StaticEntityModel model, String bone, boolean visible) {
		BoneTransform transform = model.getTransform(bone);
		if (transform != null) {
			transform.visible = visible;
		}
	}
}
