package com.ForgeEssentials.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import com.ForgeEssentials.api.permissions.Group;
import com.ForgeEssentials.api.permissions.IPermRegisterEvent;
import com.ForgeEssentials.api.permissions.PermissionsAPI;
import com.ForgeEssentials.api.permissions.RegGroup;
import com.ForgeEssentials.api.permissions.Zone;
import com.ForgeEssentials.api.permissions.ZoneManager;
import com.ForgeEssentials.api.permissions.query.PermQueryPlayer;
import com.ForgeEssentials.api.permissions.query.PropQueryPlayerSpot;
import com.ForgeEssentials.commands.util.CommandDataManager;
import com.ForgeEssentials.commands.util.FEcmdModuleCommands;
import com.ForgeEssentials.commands.util.PWarp;
import com.ForgeEssentials.core.PlayerInfo;
import com.ForgeEssentials.util.FunctionHelper;
import com.ForgeEssentials.util.Localization;
import com.ForgeEssentials.util.OutputHandler;
import com.ForgeEssentials.util.TeleportCenter;
import com.ForgeEssentials.util.AreaSelector.WarpPoint;
import com.ForgeEssentials.util.AreaSelector.WorldPoint;

import cpw.mods.fml.common.FMLCommonHandler;

public class CommandPersonalWarp extends FEcmdModuleCommands
{
    public final String PERMSETLIMIT = getCommandPerm() + ".setLimit";
    public final String PERMPROP = getCommandPerm() + ".max";
	@Override
	public String getCommandName()
	{
		return "personalwarp";
	}

	@Override
	public String[] getDefaultAliases()
	{
		return new String[]
		{ "pw" };
	}

	@Override
	public void processCommandPlayer(EntityPlayer sender, String[] args)
	{
		HashMap<String, PWarp> map = CommandDataManager.pwMap.get(sender.username);

		if (map == null)
		{
		    map = new HashMap<String, PWarp>();
		    CommandDataManager.pwMap.put(sender.username, map);
		}
		
		if (args.length == 0)
		{
			sender.sendChatToPlayer(Localization.get("command.personalwarp.list"));
			sender.sendChatToPlayer(FunctionHelper.niceJoin(map.keySet().toArray()));
		}
		else
		{
			if (args[0].equalsIgnoreCase("goto"))
			{
				if (map.containsKey(args[1]))
				{
					PWarp warp = map.get(args[1]);
					PlayerInfo playerInfo = PlayerInfo.getPlayerInfo(sender.username);
					playerInfo.back = new WarpPoint(sender);
					TeleportCenter.addToTpQue(warp.getPoint(), sender);
				}
				else
				{
					OutputHandler.chatError(sender, Localization.get("command.personalwarp.notfound"));
				}
			}
			else if (args[0].equalsIgnoreCase("add"))
			{
				if (!map.containsKey(args[1]))
				{
				    PropQueryPlayerSpot prop = new PropQueryPlayerSpot(sender, PERMPROP);
			        PermissionsAPI.getPermissionProp(prop);
			        if (!prop.hasValue() ||  prop.getNumberValue() == -1)
			        {
			            map.put(args[1], new PWarp(sender.username, args[1], new WarpPoint(sender)));
			            OutputHandler.chatConfirmation(sender, Localization.get("command.personalwarp.made"));
			        }
			        else if (map.size() < prop.getNumberValue())
			        {
			            map.put(args[1], new PWarp(sender.username, args[1], new WarpPoint(sender)));
                        OutputHandler.chatConfirmation(sender, Localization.get("command.personalwarp.made"));
			        }
			        else
			        {
			            OutputHandler.chatError(sender, Localization.get("command.personalwarp.limitmsg"));
			        }
				}
				else
				{
					OutputHandler.chatError(sender, Localization.get("command.personalwarp.alreadyexists"));
				}
			}
			else if (args[0].equalsIgnoreCase("remove"))
			{
				if (map.containsKey(args[1]))
				{
					CommandDataManager.removePWarp(map.get(args[1]));
					map.remove(args[1]);
					OutputHandler.chatConfirmation(sender, Localization.get("command.personalwarp.remove"));
				}
				else
				{
					OutputHandler.chatError(sender, Localization.get("command.personalwarp.notfound"));
				}
			}
			else if (args[0].equalsIgnoreCase("limit") && PermissionsAPI.checkPermAllowed(new PermQueryPlayer(sender, PERMSETLIMIT)))
			{
			    if (args.length == 1)
			    {
			        OutputHandler.chatError(sender, Localization.get("command.personalwarp.limit.syntax"));
			    }
			    else
			    {
			        String target;
                    if (PermissionsAPI.getGroupForName(args[1]) != null) target = "g:" + PermissionsAPI.getGroupForName(args[1]).name;
                    else if (args[1].equalsIgnoreCase("me")) target = "p:" + sender.username;
                    else target = "p:" + FunctionHelper.getPlayerForName(sender, args[1]).username;
                    
                    if (args.length == 2)
                    {
                        OutputHandler.chatConfirmation(sender, Localization.format("command.personalwarp.limit.current", getLimit(target)));
                    }
                    else
                    {
                        setLimit(target, parseIntWithMin(sender, args[2], -1));
                        OutputHandler.chatConfirmation(sender, Localization.format("command.personalwarp.limit.new", getLimit(target)));
                    }
                    
			    }
			}
			else if (args[0].equalsIgnoreCase("limit"))
			{
			    OutputHandler.chatConfirmation(sender, Localization.format("command.personalwarp.limit.current", getLimit(sender)));
			}
		}
		CommandDataManager.pwMap.put(sender.username, map);
		CommandDataManager.savePWarps(sender.username);
	}

	private String getLimit(EntityPlayer sender)
    {
	    PropQueryPlayerSpot prop = new PropQueryPlayerSpot(sender, PERMPROP);
        PermissionsAPI.getPermissionProp(prop);
        return prop.getNumberValue() + "";
    }

    private String getLimit(String target)
    {
	    if (target.startsWith("p:")) return PermissionsAPI.getPermissionPropForPlayer(target.replaceFirst("p:", ""), ZoneManager.getGLOBAL().getZoneName(), PERMPROP);
	    else if (target.startsWith("g:")) return PermissionsAPI.getPermissionPropForGroup(target.replaceFirst("g:", ""), ZoneManager.getGLOBAL().getZoneName(), PERMPROP);
	    else return "";
    }
	
	private void setLimit(String target, int limit)
    {
        if (target.startsWith("p:")) PermissionsAPI.setPlayerPermissionProp(target.replaceFirst("p:", ""), PERMPROP, "" + limit, ZoneManager.getGLOBAL().getZoneName());
        else if (target.startsWith("g:")) PermissionsAPI.setGroupPermissionProp(target.replaceFirst("g:", ""), PERMPROP, "" + limit, ZoneManager.getGLOBAL().getZoneName());
        else return;
    }

    @Override
	public void processCommandConsole(ICommandSender sender, String[] args)
	{
	}

	@Override
	public boolean canConsoleUseCommand()
	{
		return false;
	}

	@Override
	public String getCommandPerm()
	{
		return "ForgeEssentials.BasicCommands." + getCommandName();
	}

	@Override
	public List<?> addTabCompletionOptions(ICommandSender sender, String[] args)
	{
		if (args.length == 1)
			return getListOfStringsMatchingLastWord(args, "goto", "add", "remove", "limit");
		if (args.length == 2 && args[0].equalsIgnoreCase("limit"))
		{
		    Zone zone = sender instanceof EntityPlayer ? ZoneManager.getWhichZoneIn(new WorldPoint((EntityPlayer) sender)) : ZoneManager.getGLOBAL();
            ArrayList<String> list = new ArrayList<String>();
            for (String s : FMLCommonHandler.instance().getMinecraftServerInstance().getAllUsernames())
                list.add(s);

            while (zone != null)
            {
                for (Group g : PermissionsAPI.getGroupsInZone(zone.getZoneName()))
                    list.add(g.name);
                zone = ZoneManager.getZone(zone.parent);
            }

            return getListOfStringsFromIterableMatchingLastWord(args, list);
		}
		if (args.length == 2)
		{
			if (CommandDataManager.pwMap.get(sender.getCommandSenderName()) == null)
			{
				CommandDataManager.pwMap.put(sender.getCommandSenderName(), new HashMap<String, PWarp>());
			}
			return getListOfStringsFromIterableMatchingLastWord(args, CommandDataManager.pwMap.get(sender.getCommandSenderName()).keySet());
		}
		return null;
	}

	@Override
	public RegGroup getReggroup()
	{
		return RegGroup.GUESTS;
	}
	
	@Override
	public void registerExtraPermissions(IPermRegisterEvent event)
    {
	    event.registerPermissionLevel(PERMSETLIMIT, RegGroup.OWNERS);
	    
	    event.registerGroupPermissionprop(PERMPROP, 0, RegGroup.GUESTS);
	    event.registerGroupPermissionprop(PERMPROP, 10, RegGroup.MEMBERS);
	    event.registerGroupPermissionprop(PERMPROP, -1, RegGroup.OWNERS);
    }
}
