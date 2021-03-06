package cavern.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public interface IEntityData
{
	IPortalCache getPortalCache(Entity entity);

	IPlayerData getPlayerData(EntityPlayer player);

	IMinerStats getMinerStats(EntityPlayer player);

	IMiningData getMiningData(EntityPlayer player);
}