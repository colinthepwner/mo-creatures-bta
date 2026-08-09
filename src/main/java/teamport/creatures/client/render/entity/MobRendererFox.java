package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobFox;

@Environment(EnvType.CLIENT)
public class MobRendererFox extends MobRendererQuadrupedBase<MobFox> {
	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

	public MobRendererFox() {
		super("geometry.fox", 0.0D, 0.5F);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobFox entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {

			tail.rotY = MathHelper.cos(limbSwing * 0.3331F) * 0.3F;
		}
	}
}
