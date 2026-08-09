package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobAquaticBase;

@Environment(EnvType.CLIENT)
public abstract class MobRendererAquaticBase<T extends MobAquaticBase> extends MobRenderer<T> {
	protected static final String MODEL_KEY = "main";

	protected MobRendererAquaticBase(String modelId, double inflation, float shadowSize) {
		super(shadowSize);
		setModel(MODEL_KEY, modelId, inflation);
	}

	protected abstract String[] tailBones();

	protected String[] headBones() {
		return new String[0];
	}

	protected String[] leftFinBones() {
		return new String[0];
	}

	protected String[] rightFinBones() {
		return new String[0];
	}

	protected boolean tailBeatsVertically() {
		return false;
	}

	protected float tailAmplitude() {
		return 0.5F;
	}

	protected void poseExtra(StaticEntityModel model, T entity, float limbSwing, float limbYaw, float partialTick) {
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(T entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		model.resetBones();

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);

		float stroke = Math.max(limbYaw, 0.25F);
		float beat = MathHelper.cos(limbSwing * 0.6662F) * tailAmplitude() * stroke;

		poseTail(model, beat);
		poseHead(model, entity, partialTick);
		poseFins(model, beat);
		poseExtra(model, entity, limbSwing, limbYaw, partialTick);

		return model;
	}

	protected void poseTail(StaticEntityModel model, float beat) {
		for (String name : tailBones()) {
			BoneTransform tail = model.getTransform(name);
			if (tail == null) {
				continue;
			}
			if (tailBeatsVertically()) {
				tail.rotX = beat;
			} else {
				tail.rotY = beat;
			}
		}
	}

	protected void poseHead(StaticEntityModel model, T entity, float partialTick) {
		BoneTransform head = bone(model, headBones());
		if (head != null) {
			head.rotY = (getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick)) * MathHelper.DEG_TO_RAD;
		}
	}

	protected void poseFins(StaticEntityModel model, float beat) {
		BoneTransform left = bone(model, leftFinBones());
		BoneTransform right = bone(model, rightFinBones());

		if (left != null) {
			left.rotZ = -beat * 0.5F;
		}
		if (right != null) {
			right.rotZ = beat * 0.5F;
		}
	}

	@Override
	protected void preRenderTransform(T entity, double x, double y, double z, float rotation, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rotation, partialTick);

		float growth = entity.getGrowth();
		GLRenderer.modelM4f().scale(growth, growth, growth);

		GLRenderer.modelM4f().rotateX(getHeadPitch(entity, partialTick) * MathHelper.DEG_TO_RAD);
	}

	@Override
	public float getShadowSize(T entity) {
		return super.getShadowSize(entity) * entity.getGrowth();
	}

	protected static BoneTransform bone(StaticEntityModel model, String... names) {
		for (String name : names) {
			BoneTransform transform = model.getTransform(name);
			if (transform != null) {
				return transform;
			}
		}
		return null;
	}
}
