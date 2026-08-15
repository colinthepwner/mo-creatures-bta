package teamport.creatures.core;

import net.minecraft.core.entity.Entity;

public final class MMRiderInput {

	private static final boolean CLIENT_CLASSES_PRESENT = clientClassesPresent();

	public final float strafe;
	public final float forward;
	public final boolean jump;

	MMRiderInput(float strafe, float forward, boolean jump) {
		this.strafe = strafe;
		this.forward = forward;
		this.jump = jump;
	}

	public static MMRiderInput of(Entity rider) {
		if (rider == null || !CLIENT_CLASSES_PRESENT) {
			return null;
		}
		return MMLocalRiderInput.read(rider);
	}

	private static boolean clientClassesPresent() {
		try {

			Class.forName("net.minecraft.client.entity.player.PlayerLocal", false,
				MMRiderInput.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}
}
