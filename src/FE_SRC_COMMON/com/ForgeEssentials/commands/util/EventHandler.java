package com.ForgeEssentials.commands.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

import com.ForgeEssentials.api.permissions.PermissionsAPI;
import com.ForgeEssentials.api.permissions.Zone;
import com.ForgeEssentials.api.permissions.ZoneManager;
import com.ForgeEssentials.api.permissions.query.PermQueryPlayer;
import com.ForgeEssentials.api.permissions.query.PropQueryPlayerSpot;
import com.ForgeEssentials.commands.CommandSetSpawn;
import com.ForgeEssentials.core.PlayerInfo;
import com.ForgeEssentials.util.FunctionHelper;
import com.ForgeEssentials.util.AreaSelector.WarpPoint;
import com.ForgeEssentials.util.AreaSelector.WorldPoint;

import cpw.mods.fml.common.FMLCommonHandler;

public class EventHandler
{
	@ForgeSubscribe()
	public void playerInteractEvent(PlayerInteractEvent e)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		/*
		 * Colorize!
		 */

		if (e.entityPlayer.getEntityData().getBoolean("colorize"))
		{
			e.setCanceled(true);
			TileEntity te = e.entityPlayer.worldObj.getBlockTileEntity(e.x, e.y, e.z);
			if (te != null)
			{
				if (te instanceof TileEntitySign)
				{
					String[] signText = ((TileEntitySign) te).signText;

					signText[0] = FunctionHelper.formatColors(signText[0]);
					signText[1] = FunctionHelper.formatColors(signText[1]);
					signText[2] = FunctionHelper.formatColors(signText[2]);
					signText[3] = FunctionHelper.formatColors(signText[3]);

					((TileEntitySign) te).signText = signText;
					e.entityPlayer.worldObj.setBlockTileEntity(e.x, e.y, e.z, te);
					e.entityPlayer.worldObj.markBlockForUpdate(e.x, e.y, e.z);
				}
				else
				{
					e.entityPlayer.sendChatToPlayer("That is no sign!");
				}
			}
			else
			{
				e.entityPlayer.sendChatToPlayer("That is no sign!");
			}

			e.entityPlayer.getEntityData().setBoolean("colorize", false);
		}

		/*
		 * Jump with compass
		 */

		if (e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK)
		{
			if (e.entityPlayer.getCurrentEquippedItem() != null && FMLCommonHandler.instance().getEffectiveSide().isServer())
			{
				if (e.entityPlayer.getCurrentEquippedItem().itemID == Item.compass.itemID)
				{
					if (PermissionsAPI.checkPermAllowed(new PermQueryPlayer(e.entityPlayer, "ForgeEssentials.BasicCommands.jump")))
					{
						try
						{
							MovingObjectPosition mo = FunctionHelper.getPlayerLookingSpot(e.entityPlayer, false);

							((EntityPlayerMP) e.entityPlayer).playerNetServerHandler.setPlayerLocation(mo.blockX, mo.blockY, mo.blockZ, e.entityPlayer.rotationPitch, e.entityPlayer.rotationYaw);
						}
						catch (Exception ex)
						{
						}
					}
				}
			}
		}
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void onPlayerDeath(LivingDeathEvent e)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		if (e.entity instanceof EntityPlayer)
		{
			EntityPlayerMP player = (EntityPlayerMP) e.entityLiving;
			PlayerInfo.getPlayerInfo(player.username).back = new WarpPoint(player);

			// generate for un-generated dimension
			{
				int currentDim = player.worldObj.provider.dimensionId;
				int spawnDim = player.worldObj.provider.getRespawnDimension(player);

				if (spawnDim != 0 && spawnDim == currentDim && !CommandSetSpawn.dimsWithProp.contains(currentDim))
				{
					Zone z = ZoneManager.getWorldZone(player.worldObj);
					ChunkCoordinates dimPoint = player.worldObj.getSpawnPoint();
					WorldPoint point = new WorldPoint(spawnDim, dimPoint.posX, dimPoint.posY, dimPoint.posZ);
					CommandSetSpawn.setSpawnPoint(point, z);
					CommandSetSpawn.dimsWithProp.add(currentDim);

					WarpPoint p = new WarpPoint(currentDim, dimPoint.posX + .5, dimPoint.posY + 1, dimPoint.posZ + .5, player.cameraYaw, player.cameraPitch);
					CommandSetSpawn.spawns.put(player.username, p);
					return;
				}
			}
			
			PropQueryPlayerSpot query = new PropQueryPlayerSpot(player, "ForgeEssentials.BasicCommands.spawnType");
			PermissionsAPI.getPermissionProp(query);
			
			if (query.getStringValue().equalsIgnoreCase("none"))
			{
				return;
			}
			else if (query.getStringValue().equalsIgnoreCase("bed"))
			{
				if (player.getBedLocation() != null)
				{
					ChunkCoordinates spawn = player.getBedLocation();
					EntityPlayer.verifyRespawnCoordinates(player.worldObj, spawn, true);
					
					WarpPoint point = new WarpPoint(player.worldObj.provider.dimensionId, spawn.posX + .5, spawn.posY + 1, spawn.posZ + .5, player.cameraYaw, player.cameraPitch);
					CommandSetSpawn.spawns.put(player.username, point);
					
					return;
				}
			}

			query = new PropQueryPlayerSpot(player, "ForgeEssentials.BasicCommands.spawnPoint");
			PermissionsAPI.getPermissionProp(query);

			if (!query.hasValue())
				throw new RuntimeException("NO GLOBAL SPAWN SET!!!");

			String val = query.getStringValue();
			String[] split = val.split("[;_]");

			try
			{
				int dim = Integer.parseInt(split[0]);
				int x = Integer.parseInt(split[1]);
				int y = Integer.parseInt(split[2]);
				int z = Integer.parseInt(split[3]);

				WarpPoint point = new WarpPoint(dim, x + .5, y + 1, z + .5, player.cameraYaw, player.cameraPitch);
				CommandSetSpawn.spawns.put(player.username, point);
			}
			catch (Exception exception)
			{
				CommandSetSpawn.spawns.put(player.username, null);
			}
		}
	}
}
