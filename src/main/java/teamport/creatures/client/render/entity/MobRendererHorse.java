package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobHorse;

/**
 * Also the base for the unicorn and pegasus renderers, which differ only in geometry and texture.
 * <p>
 * A horse is drawn in two layers, because the original drew it as two models with a texture each:
 * body, tail and legs on {@code horse<colour>a.png}, and head, neck, ears and the horn or wings on
 * {@code horse<colour>b.png}. The asset bridge converts both halves out of the player's own copy, so
 * layer 1 picks up the {@code _head} geometry and the {@code b_} sheet that goes with it — the same
 * arrangement {@link MobRendererBigCat} uses for the lion's mane, and vanilla's wolf for its armour.
 * <p>
 * With no copy of the original to bridge, the second geometry is simply absent: layer 1 finds no
 * model, returns null, and this mod's own single combined horse carries the whole animal on layer 0.
 * Both halves are posed the same way either way, so nothing else has to know which is in play.
 * <p>
 * The built-in geometry keeps the muzzle, ears and upper neck together on the {@code head} bone and
 * uses {@code neck} for the fixed chest wedge, so only {@code head} is posed for looking around —
 * which is what {@link MobRendererQuadrupedBase} does by default. The converted head half is built
 * the same way: its ears and horn hang off {@code head} and follow it.
 */
@Environment(EnvType.CLIENT)
public class MobRendererHorse extends MobRendererQuadrupedBase<MobHorse> {
	private static final String[] LEGS = {"legFrontLeft", "legFrontRight", "legBackLeft", "legBackRight"};

	protected static final String HEAD_KEY = "head";
	private static final int LAYER_HEAD = 1;
	/** How big a newborn foal is drawn, as a fraction of the grown horse. */
	private static final float FOAL_SCALE = 0.4F;

	public MobRendererHorse() {
		this("geometry.horse", "geometry.horse_head");
	}

	protected MobRendererHorse(String bodyId, String headId) {
		super(bodyId, 0.0D, 0.5F);
		setModel(HEAD_KEY, headId, 0.0D);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	/**
	 * Foals are drawn small and grow into themselves, the same way this port already draws a dolphin
	 * calf. Growth runs from 0, so it is mapped onto {@link #FOAL_SCALE}..1 rather than used as the
	 * scale directly — a newborn multiplied by its own growth would be scaled to nothing.
	 */
	@Override
	protected void preRenderTransform(MobHorse entity, double x, double y, double z, float rotation, float partialTick) {
		super.preRenderTransform(entity, x, y, z, rotation, partialTick);

		if (!entity.isAdult()) {
			float scale = FOAL_SCALE + (1.0F - FOAL_SCALE) * entity.getGrowth();
			GLRenderer.modelM4f().scale(scale, scale, scale);
		}
	}

	@Override
	public float getShadowSize(MobHorse entity) {
		return entity.isAdult()
			? super.getShadowSize(entity)
			: super.getShadowSize(entity) * (FOAL_SCALE + (1.0F - FOAL_SCALE) * entity.getGrowth());
	}

	/** Only worth a second pass when there is a second geometry to draw with. */
	@Override
	protected int maxRenderLayer(MobHorse entity) {
		return getModel(HEAD_KEY) != null ? LAYER_HEAD : 0;
	}

	/**
	 * The head half is painted on its own sheet, named off the same index as the body's, so a horse
	 * can never end up wearing one colour's body and another's head. Taken from the index rather than
	 * from {@code getEntityTexture}, which points at the saddled sheet once the horse is saddled —
	 * only the body half has a saddled variant.
	 */
	protected String headTexture(MobHorse entity) {
		return "/assets/creatures/textures/entity/horse/b_" + entity.textureIndex() + ".png";
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(MobHorse entity, float partialTick, float unused, int layer) {
		if (layer != LAYER_HEAD) {
			return super.getAndSetupModelForLayer(entity, partialTick, unused, layer);
		}

		StaticEntityModel head = getModel(HEAD_KEY);
		if (head == null) {
			// No bridged geometry, so layer 0 was the whole horse.
			return null;
		}
		bindTexture(headTexture(entity));
		head.resetBones();

		poseHead(head, entity, getHeadYaw(entity, partialTick) - getBodyYaw(entity, partialTick),
			getHeadPitch(entity, partialTick), partialTick);
		// The pegasus' wings live on this half; poseExtra finds whichever bones the layer actually has.
		poseExtra(head, entity, getLimbSwing(entity, partialTick), getLimbYaw(entity, partialTick), partialTick);
		return head;
	}

	@Override
	protected void poseExtra(StaticEntityModel model, MobHorse entity, float limbSwing, float limbYaw, float partialTick) {
		BoneTransform tail = model.getTransform("tail");
		if (tail != null) {
			// Swishes side to side with the stride and lifts a little at a trot.
			tail.rotY = MathHelper.cos(limbSwing * 0.3331F) * 0.25F;
			tail.rotX = -limbYaw * 0.15F;
		}
	}
}
