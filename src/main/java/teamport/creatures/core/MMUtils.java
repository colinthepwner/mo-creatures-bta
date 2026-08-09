package teamport.creatures.core;

import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

public final class MMUtils {
	private MMUtils() {}

	public static AABBd grow(AABBdc box, double x, double y, double z) {
		return new AABBd(
			box.minX() - x, box.minY() - y, box.minZ() - z,
			box.maxX() + x, box.maxY() + y, box.maxZ() + z
		);
	}
}
