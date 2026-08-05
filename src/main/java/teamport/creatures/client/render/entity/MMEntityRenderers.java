package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.EntityRendererDispatcher;
import teamport.creatures.core.entity.mob.MobBear;
import teamport.creatures.core.entity.mob.MobBird;
import teamport.creatures.core.entity.mob.MobBoar;
import teamport.creatures.core.entity.mob.MobBunny;
import teamport.creatures.core.entity.mob.MobDuck;
import teamport.creatures.core.entity.mob.MobFox;
import teamport.creatures.core.entity.mob.MobHorse;
import teamport.creatures.core.entity.mob.MobHorsePegasus;
import teamport.creatures.core.entity.mob.MobHorseUnicorn;

/**
 * Renderer registration for BTA 8.0.
 * <p>
 * Renderers are no longer passed to {@code EntityHelper.createEntity}; they are assigned on the
 * dispatcher and re-assigned whenever it reloads (resource pack swap, etc.), which is why this is
 * driven by {@code ClientEvents.ENTITY_RENDERER_RELOAD} rather than run once at startup.
 */
@Environment(EnvType.CLIENT)
public final class MMEntityRenderers {
	private MMEntityRenderers() {}

	public static void registerRenderers(EntityRendererDispatcher dispatcher) {
		dispatcher.assignRenderer(MobBear.class, new MobRendererBear());
		dispatcher.assignRenderer(MobBird.class, new MobRendererBird());
		dispatcher.assignRenderer(MobFox.class, new MobRendererFox());
		dispatcher.assignRenderer(MobBunny.class, new MobRendererBunny());
		dispatcher.assignRenderer(MobBoar.class, new MobRendererBoar());
		dispatcher.assignRenderer(MobDuck.class, new MobRendererDuck());
		dispatcher.assignRenderer(MobHorse.class, new MobRendererHorse());
		dispatcher.assignRenderer(MobHorseUnicorn.class, new MobRendererHorseUnicorn());
		dispatcher.assignRenderer(MobHorsePegasus.class, new MobRendererHorsePegasus());
	}
}
