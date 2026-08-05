package teamport.creatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.sound.SoundRepository;
import teamport.creatures.client.render.block.MMBlockRenderers;
import teamport.creatures.client.render.entity.MMEntityRenderers;
import teamport.creatures.core.MMAssetBridge;
import teamport.creatures.core.MMAudit;
import turniplabs.halplibe.event.defs.ClientEvents;
import turniplabs.halplibe.util.dependency.Key;

public class MoreMobsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientEvents.AFTER_CLIENT_START.listen(Key.of(MoreMobs.MOD_ID), this::afterClientStart);
		ClientEvents.ENTITY_RENDERER_RELOAD.listen(Key.of(MoreMobs.MOD_ID), MMEntityRenderers::registerRenderers);
		ClientEvents.TILE_ENTITY_RENDERER_RELOAD.listen(Key.of(MoreMobs.MOD_ID), MMBlockRenderers::registerRenderers);
	}

	public void afterClientStart() {
		SoundRepository.namespaceAdded(MoreMobs.MOD_ID);
		// Restores the original mob textures and models from a copy of Mo' Creatures the player
		// supplies. No-op when they have not provided one; this mod ships none of that art itself.
		MMAssetBridge.run();
		MMAudit.reportBridges();
	}
}
