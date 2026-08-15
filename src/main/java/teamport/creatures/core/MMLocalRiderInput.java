package teamport.creatures.core;

import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.entity.Entity;

final class MMLocalRiderInput {
	private MMLocalRiderInput() {
	}

	static MMRiderInput read(Entity rider) {
		if (!(rider instanceof PlayerLocal)) {
			return null;
		}

		PlayerInput input = ((PlayerLocal) rider).input;
		if (input == null) {
			return null;
		}
		return new MMRiderInput(input.moveStrafe, input.moveForward, input.jump);
	}
}
