package vg.civcraft.mc.civchat2.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChat2Manager;
import vg.civcraft.mc.civchat2.command.CivChat2CommandHandler;
import vg.civcraft.mc.civchat2.utility.CivChat2Log;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.NameAPI;

public class Ignore extends PlayerCommand{
	private CivChat2 plugin = CivChat2.getInstance();
	private CivChat2Manager chatMan;
	private CivChat2Log logger = CivChat2.getCivChat2Log();
	private CivChat2CommandHandler handler = CivChat2.getCivChat2CommandHandler();
	
	public Ignore(String name) {
		super(name);
		setIdentifier("ignore");
		setDescription("This command is used to toggle ignoring a player");
		setUsage("/ignore <Name>");
		setArguments(1,1);
	}
	
	@Override
	public boolean execute(CommandSender sender, String[] args){
		chatMan = plugin.getCivChat2Manager();
		if(!(sender instanceof Player)){
			//console man sending chat... 
			sender.sendMessage(ChatColor.YELLOW + "You must be a player to perform that command.");
			return true;
		}
		
		Player player = (Player) sender;
		
		if(!(args.length == 1)){
			handler.helpPlayer(this, sender); 
			return true;
		}
		String ignore = null;
		try{
			ignore = NameAPI.getCurrentName(NameAPI.getUUID(args[0].trim()));
		} catch (Exception e){
			sender.sendMessage(ChatColor.RED + "No player exists with that name");
			return true;
		}
		
		if(ignore == null){
			sender.sendMessage(ChatColor.RED + "No player exists with that name");
			return true;
		}
		
		String name = NameAPI.getCurrentName(player.getUniqueId());
		if(ignore == name){
			sender.sendMessage(ChatColor.RED + "You cannot ignore yourself");
			return true;
		}
		if(chatMan.addIgnoringPlayer(name, ignore)){
			//Player added to list
			String debugMessage = "Player ignored another Player, Player: " + name + " IgnoredPlayer: " + ignore;
			logger.debug(debugMessage);
			sender.sendMessage(ChatColor.YELLOW + "You have ignored: " + ignore);
			return true;
		} else{
			//player removed from list
			String debugMessage = "Player un-ignored another Player, Player: " + name + " IgnoredPlayer: " + ignore;
			logger.debug(debugMessage);
			sender.sendMessage(ChatColor.YELLOW + "You have removed: " + ignore + " from ignoring list");
			return true;
		}		
	}

	@Override
	public List<String> tabComplete(CommandSender arg0, String[] arg1) {
		// TODO Auto-generated method stub
		return null;
	}	

}
