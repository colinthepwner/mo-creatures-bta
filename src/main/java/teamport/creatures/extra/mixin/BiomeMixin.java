package teamport.creatures.extra.mixin;

import net.minecraft.core.entity.SpawnListEntry;
import net.minecraft.core.entity.animal.MobDeer;
import net.minecraft.core.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamport.creatures.MMConfig;
import teamport.creatures.core.entity.mob.MobBigCat;
import teamport.creatures.core.entity.mob.MobDolphin;
import teamport.creatures.core.entity.mob.MobFishy;
import teamport.creatures.core.entity.mob.MobMouse;
import teamport.creatures.core.entity.mob.MobOgre;
import teamport.creatures.core.entity.mob.MobOgreCave;
import teamport.creatures.core.entity.mob.MobOgreFire;
import teamport.creatures.core.entity.mob.MobRat;
import teamport.creatures.core.entity.mob.MobRatHell;
import teamport.creatures.core.entity.mob.MobShark;
import teamport.creatures.core.entity.mob.MobWerewolf;
import teamport.creatures.core.entity.mob.MobWerewolfWolf;
import teamport.creatures.core.entity.mob.MobWraith;
import teamport.creatures.core.entity.mob.MobWraithFlame;
import teamport.creatures.core.entity.mob.MobBear;
import teamport.creatures.core.entity.mob.MobBird;
import teamport.creatures.core.entity.mob.MobBoar;
import teamport.creatures.core.entity.mob.MobBunny;
import teamport.creatures.core.entity.mob.MobDuck;
import teamport.creatures.core.entity.mob.MobDeerMoC;
import teamport.creatures.core.entity.mob.MobFox;
import teamport.creatures.core.entity.mob.MobHorse;
import teamport.creatures.core.entity.mob.MobHorsePegasus;
import teamport.creatures.core.entity.mob.MobHorseUnicorn;
import teamport.creatures.core.entity.mob.MobKitty;

import java.util.Iterator;
import java.util.List;

@Mixin(value = Biome.class, remap = false)
public abstract class BiomeMixin {
	@Shadow
	protected List<SpawnListEntry> spawnableCreatureList;

	@Shadow
	protected List<SpawnListEntry> spawnableMonsterList;

	@Shadow
	protected List<SpawnListEntry> spawnableWaterCreatureList;

	@Unique
	private int creatures_getFreq(String entity) {
		return MMConfig.cfg.getInt("SpawnFrequencies." + entity);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void creatures_addMobs(CallbackInfo ci) {
		spawnableCreatureList.add(new SpawnListEntry(MobBear.class, creatures_getFreq("bear")));
		spawnableCreatureList.add(new SpawnListEntry(MobBird.class, creatures_getFreq("bird")));
		spawnableCreatureList.add(new SpawnListEntry(MobFox.class, creatures_getFreq("fox")));
		spawnableCreatureList.add(new SpawnListEntry(MobBunny.class, creatures_getFreq("bunny")));
		spawnableCreatureList.add(new SpawnListEntry(MobBoar.class, creatures_getFreq("boar")));
		spawnableCreatureList.add(new SpawnListEntry(MobDuck.class, creatures_getFreq("duck")));
		spawnableCreatureList.add(new SpawnListEntry(MobKitty.class, creatures_getFreq("kitty")));
		spawnableCreatureList.add(new SpawnListEntry(MobHorse.class, creatures_getFreq("horse")));
		spawnableCreatureList.add(new SpawnListEntry(MobHorseUnicorn.class, creatures_getFreq("unicorn")));
		spawnableCreatureList.add(new SpawnListEntry(MobHorsePegasus.class, creatures_getFreq("pegasus")));
		spawnableCreatureList.add(new SpawnListEntry(MobBigCat.class, creatures_getFreq("bigcat")));
		spawnableCreatureList.add(new SpawnListEntry(MobMouse.class, creatures_getFreq("mouse")));

		// Aquatic mobs use the dedicated water list; the creature list only spawns on land.
		spawnableWaterCreatureList.add(new SpawnListEntry(MobDolphin.class, creatures_getFreq("dolphin")));
		spawnableWaterCreatureList.add(new SpawnListEntry(MobShark.class, creatures_getFreq("shark")));
		spawnableWaterCreatureList.add(new SpawnListEntry(MobFishy.class, creatures_getFreq("fishy")));

		// Hostiles belong on the monster list so they obey the difficulty/light spawn rules.
		spawnableMonsterList.add(new SpawnListEntry(MobRat.class, creatures_getFreq("rat")));
		spawnableMonsterList.add(new SpawnListEntry(MobRatHell.class, creatures_getFreq("rat_hell")));
		spawnableMonsterList.add(new SpawnListEntry(MobOgre.class, creatures_getFreq("ogre")));
		spawnableMonsterList.add(new SpawnListEntry(MobOgreFire.class, creatures_getFreq("ogre_fire")));
		spawnableMonsterList.add(new SpawnListEntry(MobOgreCave.class, creatures_getFreq("ogre_cave")));
		spawnableMonsterList.add(new SpawnListEntry(MobWerewolf.class, creatures_getFreq("werewolf")));
		spawnableMonsterList.add(new SpawnListEntry(MobWerewolfWolf.class, creatures_getFreq("werewolf_wolf")));
		spawnableMonsterList.add(new SpawnListEntry(MobWraith.class, creatures_getFreq("wraith")));
		spawnableMonsterList.add(new SpawnListEntry(MobWraithFlame.class, creatures_getFreq("wraith_flame")));

		// BTA 8.0.1 ships its own deer, added to every biome by Biome.initSpawnables() — which the
		// base constructor calls, so it has already run by the time this TAIL injection fires and
		// the entry is here to be pulled back out. Drop it and put ours in its place, otherwise the
		// two deer would spawn side by side.
		if (MMConfig.cfg.getBoolean("Replacements.replaceVanillaDeer")) {
			Iterator<SpawnListEntry> entries = spawnableCreatureList.iterator();
			while (entries.hasNext()) {
				if (entries.next().entityClass == MobDeer.class) {
					entries.remove();
				}
			}
			spawnableCreatureList.add(new SpawnListEntry(MobDeerMoC.class, creatures_getFreq("deer")));
		}
	}
}
