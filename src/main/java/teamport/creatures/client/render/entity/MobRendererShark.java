package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobShark;

/**
 * The shark sweeps its tail side to side, and the head follows a beat behind it — the whole animal
 * snakes rather than swinging from a fixed shoulder.
 */
@Environment(EnvType.CLIENT)
public class MobRendererShark extends MobRendererAquaticBase<MobShark> {
	/**
	 * The tail stock alone. The two tail fins hang off it in the manifest, so they are carried by it
	 * rather than driven separately.
	 * <p>
	 * Driving all three was the original fault — the fins were siblings of the stock then, so
	 * swinging the stock moved it out from under fins that stayed put and the tail came apart.
	 * Driving only the fins fixed that but left the body itself dead, because the stock is most of
	 * the visible tail. Parenting the fins to the stock is what makes one bone enough.
	 */
	private static final String[] TAIL = {"PTail"};

	public MobRendererShark() {
		super("geometry.shark", 0.0D, 0.6F);
	}

	@Override
	protected String[] tailBones() {
		return TAIL;
	}

	@Override
	protected float tailAmplitude() {
		return 0.6F;
	}
}
