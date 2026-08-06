package teamport.creatures.client.render.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.renderer.GLRenderer;
import net.minecraft.client.render.tessellator.TessellatorGeneral;
import net.minecraft.client.render.tileentity.TileEntityRenderer;
import net.minecraft.core.block.BlockLogicRotatable;
import net.minecraft.core.util.helper.Direction;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.block.LitterboxEntity;

/**
 * Draws the litter box, showing that a kitty has used it.
 * <p>
 * Two ways round, because the original and this port disagree about how. The original had one image
 * and swapped a clean litter box for a used one on the model, both painted on the same sheet; this
 * port's own model has one tray and a second, dirty image. So when the asset bridge has converted the
 * original's geometry the state is a bone that is hidden — the way the doe's antlers are, and
 * {@code resetBones()} puts it back every frame — and otherwise it is still a second texture.
 * <p>
 * The 7.2 renderer drove a hand-written {@code ModelBase} through raw {@code GL11} calls. BTA 8.0
 * deleted {@code ModelBase} — geometry is Bedrock JSON loaded by Dragonfly and posed through
 * {@link StaticEntityModel} — and moved the fixed-function calls behind {@link GLRenderer}, whose
 * model matrix replaces {@code glTranslate}/{@code glScale}/{@code glRotate}. The old
 * {@code glScalef(-1, -1, 1)} plus 24-pixel Y nudge is gone with it: Dragonfly geometry is already
 * Y-up, so the same 1/16 scale with a negated Z that {@code MobRenderer} uses is all that's needed.
 */
@Environment(EnvType.CLIENT)
public class LitterboxRenderer extends TileEntityRenderer<LitterboxEntity> {
	private static final String MODEL_KEY = "main";
	private static final String TEXTURE_CLEAN = "/assets/creatures/textures/entity/litterbox/0.png";
	private static final String TEXTURE_FILTHY = "/assets/creatures/textures/entity/litterbox/1.png";

	public LitterboxRenderer() {
		setModel(MODEL_KEY, "/assets/creatures/models/entity/litterbox.json", 0.0D);
	}

	@Override
	public void doRender(TessellatorGeneral tessellator, LitterboxEntity tileEntity, double x, double y, double z, float partialTick) {
		StaticEntityModel model = getModel(MODEL_KEY);
		model.resetBones();

		GLRenderer.pushFrame();
		GLRenderer.modelM4f().translate((float) (x + 0.5), (float) y, (float) (z + 0.5));
		GLRenderer.modelM4f().rotateY((float) Math.toRadians(getFacingAngle(tileEntity.getBlockMeta())));
		GLRenderer.modelM4f().scale(0.0625F, 0.0625F, -0.0625F);

		boolean shownByBone = setTrayVisible(model, tileEntity.isFilthy);
		bindTexture(!shownByBone && tileEntity.isFilthy ? TEXTURE_FILTHY : TEXTURE_CLEAN);
		model.render();

		GLRenderer.popFrame();
	}

	/**
	 * Hides whichever of the two trays does not apply.
	 *
	 * @return true when the model has both, so the state is already visible and one texture covers it
	 */
	private static boolean setTrayVisible(StaticEntityModel model, boolean filthy) {
		BoneTransform clean = model.getTransform("litter");
		BoneTransform used = model.getTransform("litterUsed");
		if (clean == null || used == null) {
			return false;
		}
		clean.visible = !filthy;
		used.visible = filthy;
		return true;
	}

	/**
	 * Same facings the 7.2 renderer used, expressed against the direction the rotatable block
	 * stores in its metadata rather than against raw meta values.
	 */
	private static float getFacingAngle(int meta) {
		Direction facing = BlockLogicRotatable.getDirectionFromMeta(meta);

		switch (facing) {
			case SOUTH:
				return 180.0F;
			case WEST:
				return 270.0F;
			case EAST:
				return 90.0F;
			default:
				return 0.0F;
		}
	}
}
