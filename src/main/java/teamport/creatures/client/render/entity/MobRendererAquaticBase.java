package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobAquaticBase;

/**
 * Shared base for the mod's swimmers.
 * <p>
 * Two things separate these from the land mobs. The whole model pitches with the mob's own pitch,
 * because a fish that dives points its nose down rather than tilting its head; and the model is
 * scaled by the mob's growth, so a pup and a full-grown adult are visibly different animals.
 * <p>
 * Bone names are not ours — the geometry comes from the player's own copy of the original mod via
 * the asset bridge, and the original's model classes used names like {@code Body} and {@code PTail}
 * where this mod's own converted models use {@code body} and {@code tail}. Every lookup therefore
 * goes through {@link #bone} with a list of candidates and tolerates finding none of them.
 */
@Environment(EnvType.CLIENT)
public abstract class MobRendererAquaticBase<T extends MobAquaticBase> extends MobRenderer<T> {
	protected static final String MODEL_KEY = "main";

	protected MobRendererAquaticBase(String modelId, double inflation, float shadowSize) {
		super(shadowSize);
		setModel(MODEL_KEY, modelId, inflation);
	}

	/** Candidate names for the bones that sway with the stroke, most likely first. */
	protected abstract String[] tailBones();

	/**
	 * Empty by default, and deliberately.
	 * <p>
	 * These heads are not one bone. The original's shark carries its head as four separate boxes
	 * ({@code UHead}, {@code DHead}, {@code LHead}, {@code RHead}) that are siblings of each other,
	 * not children — so turning the first one found took a quarter of the head with it and left the
	 * other three behind. Read against the source: {@code ModelShark}'s pose method assigns to
	 * {@code UpperTailFin} and {@code LowerTailFin} and to nothing else, so the head never turned.
	 * A renderer whose model really does keep its head on one bone can override this.
	 */
	protected String[] headBones() {
		return new String[0];
	}

	/**
	 * Empty by default. No original animates a pectoral fin, and their geometry says why they could
	 * not: the shark's {@code RightFin} has its rotation point at {@code x = -10} against a box
	 * spanning {@code -10..-2} — the outer tip, not the shoulder. That is faithful, and it is
	 * harmless while the bone is only ever placed, but rotating it swings the whole fin off the body.
	 */
	protected String[] leftFinBones() {
		return new String[0];
	}

	protected String[] rightFinBones() {
		return new String[0];
	}

	/**
	 * True for a horizontal fluke that beats up and down — a dolphin — and false for the vertical
	 * fin of a fish or a shark, which sweeps side to side.
	 */
	protected boolean tailBeatsVertically() {
		return false;
	}

	protected float tailAmplitude() {
		return 0.5F;
	}

	/** Hook for anything past the shared tail/head/fin pass. */
	protected void poseExtra(StaticEntityModel model, T entity, float limbSwing, float limbYaw, float partialTick) {
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(T entity, float partialTick, float unused, int layer) {
		StaticEntityModel model = getModel(MODEL_KEY);
		model.resetBones();

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);
		// Nothing in the water ever goes completely still, so the stroke has a floor.
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

	/**
	 * Only the yaw. Pitch is applied to the whole model in {@link #preRenderTransform}, and doing
	 * both would bend the head twice as far as the mob is actually looking.
	 */
	protected void poseHead(StaticEntityModel model, T entity, float partialTick) {
		BoneTransform head = bone(model, headBones());
		if (head != null) {
			head.rotY = (getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick)) * MathHelper.DEG_TO_RAD;
		}
	}

	/** Side fins paddle in opposition, a quarter-stroke behind the tail. */
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
		// Applied where a root bone's own rotation would land, so it shares the bone sign convention.
		GLRenderer.modelM4f().rotateX(getHeadPitch(entity, partialTick) * MathHelper.DEG_TO_RAD);
	}

	@Override
	public float getShadowSize(T entity) {
		return super.getShadowSize(entity) * entity.getGrowth();
	}

	/** First of {@code names} the model actually has, or null if it has none of them. */
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
