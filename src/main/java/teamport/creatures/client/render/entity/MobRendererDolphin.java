package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import teamport.creatures.core.entity.mob.MobDolphin;

/**
 * A dolphin is a mammal: its flukes are horizontal and beat up and down, unlike every fish in this
 * batch. The original's model splits the tail into a peduncle and left and right flukes, so all
 * three are driven together.
 */
@Environment(EnvType.CLIENT)
public class MobRendererDolphin extends MobRendererAquaticBase<MobDolphin> {
	/**
	 * Only the two flukes, which is what {@code ModelDolphin}'s pose method assigns to.
	 * <p>
	 * {@code PTail} — the peduncle — used to be driven as well, and here that was the opposite fault
	 * to the shark's: the flukes <em>are</em> children of the peduncle in this geometry, so turning
	 * both applied the stroke to them twice and they swung out past the tail they hang off.
	 */
	private static final String[] TAIL = {"LTail", "RTail"};

	public MobRendererDolphin() {
		super("geometry.dolphin", 0.0D, 0.6F);
	}

	@Override
	protected String[] tailBones() {
		return TAIL;
	}

	@Override
	protected boolean tailBeatsVertically() {
		return true;
	}

	@Override
	protected float tailAmplitude() {
		return 0.45F;
	}
}
