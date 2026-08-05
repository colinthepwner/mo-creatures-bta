package teamport.creatures;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.sound.SoundRepository;
import teamport.creatures.client.render.entity.MMEntityRenderers;
import turniplabs.halplibe.event.defs.ClientEvents;
import turniplabs.halplibe.util.dependency.Key;

public class MoreMobsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientEvents.AFTER_CLIENT_START.listen(Key.of(MoreMobs.MOD_ID), this::afterClientStart);
		ClientEvents.ENTITY_RENDERER_RELOAD.listen(Key.of(MoreMobs.MOD_ID), MMEntityRenderers::registerRenderers);
	}

	public void afterClientStart() {
		SoundRepository.namespaceAdded(MoreMobs.MOD_ID);
	}
}
