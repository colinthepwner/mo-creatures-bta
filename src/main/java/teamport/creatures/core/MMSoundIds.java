package teamport.creatures.core;

import net.minecraft.core.sound.SoundTypes;

/**
 * The dedicated server resolves sounds via SoundTypes.getSoundId(name).
 * Without registration, WorldManager.playSound discards the sound ("Unable to play unknown sound").
 * SoundTypes lives in net.minecraft.core (shared), so it can be called from the main side.
 */
public final class MMSoundIds {
	private static final String[] NAMES = {
		// grep -rhoE '"creatures:mob\.[A-Za-z0-9_.]+"' src/main/java | sort -u | paste -sd, -

        "creatures:mob.bear","creatures:mob.bear.death","creatures:mob.bear.hurt","creatures:mob.bigcat","creatures:mob.bigcat.cub","creatures:mob.bigcat.cub.death","creatures:mob.bigcat.cub.hurt","creatures:mob.bigcat.death","creatures:mob.bigcat.eat","creatures:mob.bigcat.hurt","creatures:mob.bird.black","creatures:mob.bird.blue","creatures:mob.bird.green","creatures:mob.bird.red","creatures:mob.bird.white","creatures:mob.bird.yellow","creatures:mob.bunny.death","creatures:mob.bunny.hurt","creatures:mob.bunny.land","creatures:mob.bunny.lift","creatures:mob.deer","creatures:mob.deer.buck","creatures:mob.deer.death","creatures:mob.deer.hurt","creatures:mob.dolphin","creatures:mob.dolphin.death","creatures:mob.dolphin.hurt","creatures:mob.dolphin.upset","creatures:mob.duck","creatures:mob.duck.hurt","creatures:mob.fox","creatures:mob.fox.angry","creatures:mob.fox.death","creatures:mob.fox.hurt","creatures:mob.horse","creatures:mob.horse.death","creatures:mob.horse.eat","creatures:mob.horse.hurt","creatures:mob.horse.mad","creatures:mob.horse.pegasus","creatures:mob.kitty","creatures:mob.kitty.death","creatures:mob.kitty.eating","creatures:mob.kitty.hurt","creatures:mob.kitty.purr","creatures:mob.mouse","creatures:mob.mouse.death","creatures:mob.mouse.hurt","creatures:mob.ogre","creatures:mob.ogre.death","creatures:mob.ogre.destroy","creatures:mob.ogre.hurt","creatures:mob.rat","creatures:mob.rat.death","creatures:mob.rat.hurt","creatures:mob.werewolf","creatures:mob.werewolf.death","creatures:mob.werewolf.human.death","creatures:mob.werewolf.human.hurt","creatures:mob.werewolf.hurt","creatures:mob.werewolf.transform","creatures:mob.wraith","creatures:mob.wraith.death","creatures:mob.wraith.hurt","creatures:mob.x"
	};

	private MMSoundIds() {}

	public static void registerOnServer() {
		int registered = 0;
		for (String name : NAMES) {
			if (SoundTypes.register(name)) registered++;
		}
		if (registered < NAMES.length) {
			System.out.println("[MoC-SoundDebug] Only " + registered + "/" + NAMES.length
				+ " sounds were registered; at least one sound failed to load");
		}
	}
}