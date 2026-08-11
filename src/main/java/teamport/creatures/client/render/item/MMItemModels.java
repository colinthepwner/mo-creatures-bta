package teamport.creatures.client.render.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.core.item.Item;
import teamport.creatures.core.item.MMItems;

@Environment(EnvType.CLIENT)
public final class MMItemModels {
	private MMItemModels() {}

	public static void registerModels(ItemModelDispatcher dispatcher) {
		standard(dispatcher, MMItems.BIGCAT_CLAW);
		standard(dispatcher, MMItems.SHARK_TEETH);
		standard(dispatcher, MMItems.SUGAR_LUMP);
		standard(dispatcher, MMItems.PET_FOOD);
	}

	private static void standard(ItemModelDispatcher dispatcher, Item item) {
		dispatcher.addDispatch(new ItemModelStandard(item));
	}
}
