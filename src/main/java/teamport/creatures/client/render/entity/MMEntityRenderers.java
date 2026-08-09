package teamport.creatures.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.EntityRendererDispatcher;
import teamport.creatures.core.entity.mob.MobBigCat;
import teamport.creatures.core.entity.mob.MobRat;
import teamport.creatures.core.entity.mob.MobRatHell;
import teamport.creatures.core.entity.mob.MobMouse;
import teamport.creatures.core.entity.mob.MobDolphin;
import teamport.creatures.core.entity.mob.MobShark;
import teamport.creatures.core.entity.mob.MobFishy;
import teamport.creatures.core.entity.mob.MobSharkEgg;
import teamport.creatures.core.entity.mob.MobFishyEgg;
import teamport.creatures.core.entity.mob.MobOgre;
import teamport.creatures.core.entity.mob.MobOgreFire;
import teamport.creatures.core.entity.mob.MobOgreCave;
import teamport.creatures.core.entity.mob.MobWerewolf;
import teamport.creatures.core.entity.mob.MobWerewolfWolf;
import teamport.creatures.core.entity.mob.MobWraith;
import teamport.creatures.core.entity.mob.MobWraithFlame;
import teamport.creatures.core.entity.mob.MobBear;
import teamport.creatures.core.entity.mob.MobBird;
import teamport.creatures.core.entity.mob.MobBoar;
import teamport.creatures.core.entity.mob.MobBunny;
import teamport.creatures.core.entity.mob.MobDeerMoC;
import teamport.creatures.core.entity.mob.MobDuck;
import teamport.creatures.core.entity.mob.MobFox;
import teamport.creatures.core.entity.mob.MobHorse;
import teamport.creatures.core.entity.mob.MobHorseNightmare;
import teamport.creatures.core.entity.mob.MobHorsePack;
import teamport.creatures.core.entity.mob.MobHorsePegasus;
import teamport.creatures.core.entity.mob.MobHorsePegasusBlack;
import teamport.creatures.core.entity.mob.MobHorseUnicorn;
import teamport.creatures.core.entity.mob.MobKitty;

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
		dispatcher.assignRenderer(MobHorsePack.class, new MobRendererHorsePack());
		dispatcher.assignRenderer(MobHorseNightmare.class, new MobRendererHorseNightmare());
		dispatcher.assignRenderer(MobHorsePegasusBlack.class, new MobRendererHorsePegasusBlack());
		dispatcher.assignRenderer(MobKitty.class, new MobRendererKitty());
		dispatcher.assignRenderer(MobDeerMoC.class, new MobRendererDeerMoC());
		dispatcher.assignRenderer(MobBigCat.class, new MobRendererBigCat());
		dispatcher.assignRenderer(MobRat.class, new MobRendererRat());
		dispatcher.assignRenderer(MobRatHell.class, new MobRendererRatHell());
		dispatcher.assignRenderer(MobMouse.class, new MobRendererMouse());
		dispatcher.assignRenderer(MobDolphin.class, new MobRendererDolphin());
		dispatcher.assignRenderer(MobShark.class, new MobRendererShark());
		dispatcher.assignRenderer(MobFishy.class, new MobRendererFishy());
		dispatcher.assignRenderer(MobSharkEgg.class, new MobRendererSharkEgg());
		dispatcher.assignRenderer(MobFishyEgg.class, new MobRendererFishyEgg());
		dispatcher.assignRenderer(MobOgre.class, new MobRendererOgre());
		dispatcher.assignRenderer(MobOgreFire.class, new MobRendererOgreFire());
		dispatcher.assignRenderer(MobOgreCave.class, new MobRendererOgreCave());
		dispatcher.assignRenderer(MobWerewolf.class, new MobRendererWerewolf());
		dispatcher.assignRenderer(MobWerewolfWolf.class, new MobRendererWerewolfWolf());
		dispatcher.assignRenderer(MobWraith.class, new MobRendererWraith());
		dispatcher.assignRenderer(MobWraithFlame.class, new MobRendererWraithFlame());
	}
}
