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

@Environment(EnvType.CLIENT)
public class LitterboxRenderer extends TileEntityRenderer<LitterboxEntity> {
	private static final String MODEL_KEY = "main";
	private static final String TEXTURE_CLEAN = "/assets/creatures/textures/entity/litterbox/0.png";
	private static final String TEXTURE_FILTHY = "/assets/creatures/textures/entity/litterbox/1.png";

	public LitterboxRenderer() {
		setModel(MODEL_KEY, "geometry.litterbox", 0.0D);
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
