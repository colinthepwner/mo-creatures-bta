package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;

/**
 * The pack horse differs from a plain horse in its coat and in its panniers, which the original
 * builds into the same head model every horse uses and only draws once the horse has been given a
 * chest — {@code Bag1} and {@code Bag2}, dropped on every other horse in the manifest and kept here.
 */
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
