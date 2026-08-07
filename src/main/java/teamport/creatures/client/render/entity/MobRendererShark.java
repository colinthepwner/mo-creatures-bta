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
	 * Only the two tail fins, which is exactly what {@code ModelShark}'s pose method assigns to.
	 * <p>
	 * {@code PTail} — the tail stock — used to be in this list as well, and that was the fault: the
	 * two fins are its <em>siblings</em> in the geometry, not its children, so swinging the stock
	 * moved it out from under fins that stayed where they were and the tail came apart. Driving only
	 * the fins is both what the original does and the only thing this hierarchy supports.
	 */
	private static final String[] TAIL = {"UpperTailFin", "LowerTailFin"};

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
