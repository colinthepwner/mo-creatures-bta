package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

	public MobRendererHorse() {
		this("/assets/creatures/models/entity/horse.json", "/assets/creatures/models/entity/horse_head.json");
	}

	protected MobRendererHorse(String bodyPath, String headPath) {
		super(bodyPath, 0.0D, 0.75F);
		setModel(HEAD_KEY, headPath, 0.0D);
	}

	@Override
	protected String[] legBones() {
		return LEGS;
	}

	/** Only worth a second pass when there is a second geometry to draw with. */
	@Override
	protected int maxRenderLayer(MobHorse entity) {
		return getModel(HEAD_KEY) != null ? LAYER_HEAD : 0;
	}

	/**
	 * The head half is painted on its own sheet, named alongside the body's. Derived from whatever
	 * texture the entity picked rather than from the variant, so a horse can never end up wearing one
	 * colour's body and another's head.
	 */
	protected String headTexture(MobHorse entity) {
		String body = entity.getEntityTexture();
		int slash = body.lastIndexOf('/');
		return body.substring(0, slash + 1) + "b_" + body.substring(slash + 1);
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
