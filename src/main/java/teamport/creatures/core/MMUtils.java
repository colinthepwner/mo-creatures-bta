package teamport.creatures.core;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;

public final class MMUtils {
	private MMUtils() {}

	public static void consumeHeld(Player player, ItemStack stack) {
		stack.consumeItem(player);
		if (stack.stackSize > 0) return;

		int slot = player.inventory.getCurrentSlot();
		if (player.inventory.getItem(slot) == stack) {
			player.inventory.setItem(slot, null);
		}
	}

	public static AABBd grow(AABBdc box, double x, double y, double z) {
		return new AABBd(
			box.minX() - x, box.minY() - y, box.minZ() - z,
			box.maxX() + x, box.maxY() + y, box.maxZ() + z
		);
	}
}
