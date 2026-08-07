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
	 * The peduncle alone. {@code LTail} and {@code RTail} are already its children in the manifest,
	 * so they follow it and must not be driven as well.
	 * <p>
	 * Driving the peduncle <em>and</em> both flukes was the original fault: the stroke reached the
	 * flukes twice and they swung out past the tail they hang off. Driving only the flukes fixed
	 * that but left the peduncle — most of the visible tail — completely still.
	 */
	private static final String[] TAIL = {"PTail"};

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
