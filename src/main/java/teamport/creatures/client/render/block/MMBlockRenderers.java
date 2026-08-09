package teamport.creatures.client.render.block;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.block.LitterboxEntity;
import turniplabs.halplibe.event.defs.ClientEvents;
import turniplabs.halplibe.util.dependency.Key;

@Environment(EnvType.CLIENT)
public final class MMBlockRenderers implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientEvents.TILE_ENTITY_RENDERER_RELOAD.listen(Key.of(MoreMobs.MOD_ID), MMBlockRenderers::registerRenderers);
	}

	public static void registerRenderers(TileEntityRenderDispatcher dispatcher) {
		dispatcher.assignRenderer(LitterboxEntity.class, new LitterboxRenderer());
	}
}
