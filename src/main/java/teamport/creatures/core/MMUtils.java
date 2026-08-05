package teamport.creatures.core;

import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

/**
 * Small helpers for API shapes that changed in BTA 8.0.
 */
public final class MMUtils {
	private MMUtils() {}

	/**
	 * BTA 8.0 replaced its own AABB class with JOML's {@link AABBd}, which has no {@code grow}.
	 * This restores the old "expand outwards in every direction" behaviour without mutating the
	 * entity's own bounding box.
	 */
	public static AABBd grow(AABBdc box, double x, double y, double z) {
		return new AABBd(
			box.minX() - x, box.minY() - y, box.minZ() - z,
			box.maxX() + x, box.maxY() + y, box.maxZ() + z
		);
	}
}
