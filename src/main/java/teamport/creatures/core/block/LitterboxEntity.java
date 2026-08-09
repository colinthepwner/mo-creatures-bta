package teamport.creatures.core.block;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.world.IVehicle;
import net.minecraft.core.world.pos.TilePos;

public class LitterboxEntity extends TileEntity implements IVehicle {
	private Entity passenger = null;
	public boolean isFilthy = false;

	@Override
	public boolean isRemoved() {
		return false;
	}

	private boolean isSafe(int x, int y, int z) {
		return !worldObj.isBlockNormalCube(new TilePos(x, y, z)) && !worldObj.isBlockNormalCube(new TilePos(x, y + 1, z));
	}

	@Override
	public Entity ejectRider() {
		Entity entity = passenger;
		if (entity == null) {
			return null;
		} else {
			int x = tilePos.x;
			int y = tilePos.y;
			int z = tilePos.z;

			passenger = null;
			entity.vehicle = null;
			if (isSafe(x, y + 1, z)) {
				entity.moveTo((double) x + 0.5, y + 1, (double) z + 0.5, entity.yRot, entity.xRot);
			} else if (isSafe(x - 1, y, z)) {
				entity.moveTo((double) x - 0.5, y, (double) z + 0.5, entity.yRot, entity.xRot);
			} else if (isSafe(x + 1, y, z)) {
				entity.moveTo((double) x + 1.5, y, (double) z + 0.5, entity.yRot, entity.xRot);
			} else if (isSafe(x, y, z - 1)) {
				entity.moveTo((double) x + 0.5, y, (double) z - 0.5, entity.yRot, entity.xRot);
			} else if (isSafe(x, y, z + 1)) {
				entity.moveTo((double) x + 0.5, y, (double) z + 1.5, entity.yRot, entity.xRot);
			} else {
				entity.moveTo((double) x + 0.5, y + 1, (double) z + 0.5, entity.yRot, entity.xRot);
			}

			return entity;
		}
	}

	@Override
	public void positionRider() {
		passenger.setPos(tilePos.x + 0.5, (double) tilePos.y + 0.5 + passenger.getRidingHeight(), tilePos.z + 0.5);
	}

	@Override
	public void setPassenger(Entity entity) {
		passenger = entity;
	}

	@Override
	public Entity getPassenger() {
		return passenger;
	}

	@Override
	public void moveExitingEntity(Entity entity) {
		entity.moveTo(tilePos.x, tilePos.y + 2, tilePos.z, entity.yRot, entity.xRot);
	}

	@Override
	public float getYRotDelta() {
		return 0.0F;
	}

	@Override
	public float getXRotDelta() {
		return 0.0F;
	}

	@Override
	public void tick() {
		if (passenger != null) {
			worldObj.updateEntity(passenger);
		}
	}

	@Override
	public void readAdditionalData(CompoundTag tag) {
		isFilthy = tag.getBoolean("IsFilthy");
	}

	@Override
	public void writeAdditionalData(CompoundTag tag) {
		tag.putBoolean("IsFilthy", isFilthy);
	}
}
