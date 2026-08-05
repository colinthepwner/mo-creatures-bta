package teamport.creatures.client.render.block;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TileEntityRenderDispatcher;
import teamport.creatures.MoreMobs;
import teamport.creatures.core.block.LitterboxEntity;
import turniplabs.halplibe.event.defs.ClientEvents;
import turniplabs.halplibe.util.dependency.Key;

/**
 * Tile entity renderer registration, the block-side counterpart to {@code MMEntityRenderers}.
 * <p>
 * As with entity renderers in BTA 8.0, these are assigned on the dispatcher and have to be
 * re-assigned whenever it reloads, hence {@code ClientEvents.TILE_ENTITY_RENDERER_RELOAD}. This
 * class carries its own {@code client} entrypoint so the block hookup stays independent of
 * {@code MoreMobsClient}; {@link #registerRenderers} is public so it can be folded into that class
 * later without touching anything here.
 */
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
