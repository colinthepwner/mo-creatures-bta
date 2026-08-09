package teamport.creatures.core.entity.mob;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.Dimension;
import net.minecraft.core.world.World;

public class MobRatHell extends MobRat {
	public MobRatHell(World world) {
		super(world);
		scoreValue = 100;
		attackStrength = 2;
		fireImmune = true;

		setSize(0.7F, 0.7F);
		setHealthRaw(getMaxHealth());

		mobDrops.clear();
		mobDrops.add(new WeightedRandomLootObject(Items.DUST_REDSTONE.getDefaultStack(), 0, 2));
	}

	@Override
	protected String textureFolder() {
		return "rat_hell";
	}

	@Override
	protected int rollVariant() {
		return VARIANT_BROWN;
	}

	@Override
	public int getMaxHealth() {
		return 20;
	}

	@Override
	public String getEntityTexture() {
		return "/assets/creatures/textures/entity/rat_hell/" + getTextureReference() + ".png";
	}

	@Override
	public String getDefaultEntityTexture() {
		return "/assets/creatures/textures/entity/rat_hell/0.png";
	}

	@Override
	protected Entity findPlayerToAttack() {
		Player player = world.getClosestPlayerToEntity(this, 16.0);
		if (player != null && !player.gamemode.hasHostileMobs()) return null;
		return player;
	}

	@Override
	public boolean canSpawnHere() {
		return world.dimension == Dimension.NETHER && super.canSpawnHere();
	}
}
