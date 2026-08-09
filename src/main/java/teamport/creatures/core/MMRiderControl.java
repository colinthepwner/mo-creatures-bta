package teamport.creatures.core;

import net.minecraft.core.entity.Mob;

public final class MMRiderControl {
	private static final double MAX_HORIZONTAL_STEP = 1.0;
	private static final double MAX_VERTICAL_STEP = 2.0;

	private double targetX;
	private double targetY;
	private double targetZ;
	private boolean pending;

	public void accept(Mob mount, double x, double y, double z, float yaw) {
		mount.yRot = yaw;
		mount.yBodyRot = yaw;
		targetX = x;
		targetY = y;
		targetZ = z;
		pending = true;
	}

	public boolean apply(Mob mount) {
		if (!pending) {
			return false;
		}
		pending = false;

		mount.setPos(
			mount.x + clamp(targetX - mount.x, MAX_HORIZONTAL_STEP),
			mount.y + clamp(targetY - mount.y, MAX_VERTICAL_STEP),
			mount.z + clamp(targetZ - mount.z, MAX_HORIZONTAL_STEP));

		mount.xd = 0.0;
		mount.zd = 0.0;
		return true;
	}

	private static double clamp(double delta, double limit) {
		return Math.max(-limit, Math.min(limit, delta));
	}
}
