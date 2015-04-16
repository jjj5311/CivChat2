package vg.civcraft.mc.civchat2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civchat2.utility.CivChat2Config;
import vg.civcraft.mc.civchat2.zipper.CivChat2FileLogger;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;

public class CivChat2Manager {	
	
	private CivChat2 plugin;
	private CivChat2Config config;
	private CivChat2FileLogger chatLog;
	//chatChannels in hashmap with (Player 1 name, player 2 name)
	private HashMap<String, String> chatChannels = new HashMap<String, String>();
	
	//groupChatChannels have (Message, GroupName)
	private HashMap<String, Group> groupChatChannels = new HashMap<String, Group>();
	
	//ignorePlayers have (recieversname, list of players they are ignoring
	private HashMap<String, List<String>> ignorePlayers = new HashMap<String, List<String>>();
	
	//replyList has (playerName, whotoreplyto)
	private HashMap<String, String> replyList = new HashMap<String, String>();
	
	
	//afk has player names in a list of who is afk
	private List<String> afk_player = new ArrayList<String>();
	
	private String afkMsg = ChatColor.AQUA + "That Player is currently AFK";
	private String ignoreMsg = ChatColor.YELLOW + "That Player is ignoring you";
	protected GroupManager gm = NameAPI.getGroupManager();

	private String defaultColor;
	
	
	public CivChat2Manager(CivChat2 pluginInstance){
		this.plugin = pluginInstance;
		config = CivChat2.getPluginConfig();
		chatLog = CivChat2.getCivChat2FileLogger();
		defaultColor = config.getDefaultColor();
	}
	
	
	/**
	 * Gets the channel for player to player chat
	 * @param name    Player name of the channel
	 * @return        Returns a String of channel name, null if doesn't exist
	 */
	
	public String getChannel(String name) {
		if(chatChannels.containsKey(name)){
			CivChat2.debugmessage("getChannel returning value: " + chatChannels.get(name));
			return chatChannels.get(name);
		}
		CivChat2.debugmessage("getChannel returning null");
		return null;
	}
	
	/**
	 * Removes the channel from the channel storage
	 * @param name    Player Name of the channel
	 * 
	 */
	public void removeChannel(String name) {
		if(chatChannels.containsKey(name)){
			CivChat2.debugmessage("removeChannel removing channel: " + name);
			chatChannels.remove(name);
		}	
	}
	
	/**
	 * Adds a channel for player to player chat, if player1 is 
	 * currently in a chatChannel this will overwrite it
	 * @param player1   Senders name
	 * @param player2   Recievers name
	 */
	public void addChatChannel(String player1, String player2) {
		if(getChannel(player1) != null){
			chatChannels.put(player1, player2);
			CivChat2.debugmessage("addChatChannel adding channel for P1: " + player1 + " P2: " + player2);
		}
		else{
			chatChannels.put(player1, player2);
			CivChat2.debugmessage("addChatChannel adding channel for P1: " + player1 + " P2: " + player2);
		}
		
	}
	
	/**
	 * Method to send message in a group
	 * @param chatGroupName Name of the namelayer group
	 * @param chatMessage Message to send to the groupees
	 * @param msgSender Player that sent the message
	 */
	public void groupChat(String chatGroupName, String chatMessage, String msgSender) {
		Player sender = Bukkit.getPlayer(NameAPI.getUUID(msgSender));
		Group chatGroup = gm.getGroup(chatGroupName);
		if(chatGroup == null){
			CivChat2.debugmessage("groupChat tried chatting to a group that doesn't exist: GroupName" + chatGroupName + 
					" Message: " + chatMessage + " Sender: " + msgSender);
			return;
		}
		String groupName = chatGroup.getName();
		List<UUID> groupMembers = chatGroup.getAllMembers();
		List<Player> recievers = new ArrayList<Player>();
		for(UUID u : groupMembers){
			//check if player is ignoring this group chat or is afk
			Player p = Bukkit.getPlayer(u);
			String playerName = NameAPI.getCurrentName(u);
			if(isAfk(playerName)){
				//player is afk do not include
				continue;
			}
			if(isIgnoringGroup(playerName, groupName)){
				//player is ignoring group do not include			
				continue;
			}
			if(p == sender){
				continue;
			}
			else{
				String grayMessage = ChatColor.GRAY + "[" + groupName + "] " + msgSender + ": ";
				String whiteMessage = ChatColor.WHITE + chatMessage;
				p.sendMessage(grayMessage + whiteMessage);
			}
		}		
	}
	

	/**
	 * Method to remove player from groupchat
	 * @param name Playername to remove from chat
	 */
	public void removeGroupChat(String name) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * Method to Send private message between to players
	 * @param sender Player sending the message
	 * @param receive Player Receiving the message
	 * @param chatMessage Message to send from sender to receive
	 */
	public void sendPrivateMsg(Player sender, Player receive, String chatMessage) {	
		String senderName = sender.getName();
		String receiverName = receive.getName();
		
		String senderMessage = ChatColor.LIGHT_PURPLE + "To " + receiverName + ": "
				+ chatMessage;
		
		String receiverMessage = ChatColor.LIGHT_PURPLE + "From " + senderName
				+ ": " + chatMessage;
		
		CivChat2.debugmessage("ChatManager.sendPrivateMsg Sender: " + senderName + 
				" receiver: " + receiverName + " Message: " + chatMessage);
		if(isAfk(receive.getName())){
			receive.sendMessage(receiverMessage);
			sender.sendMessage(afkMsg);
			return;
		}
		else if(isIgnoringPlayer(receiverName, senderName)){
			//player is ignoring the sender
			sender.sendMessage(ignoreMsg);
			return;
		}
		CivChat2.debugmessage("Sending private chat message");
		chatLog.writeToChatLog(sender, chatMessage, "P MSG");
		replyList.put(receiverName, senderName);		
		sender.sendMessage(senderMessage);
		receive.sendMessage(receiverMessage);
	}

	
	/**
	 * Check to see if a receiver is ignoring a sender (P2P chat only)
	 * @param receiverName
	 * @param senderName
	 * @return True if they are ignored, False if not
	 */
	private boolean isIgnoringPlayer(String receiverName, String senderName){
		if(ignorePlayers.containsKey(receiverName)){
			//they are ignoring people lets check who
			List<String> ignoredPlayers = ignorePlayers.get(receiverName);
			if(ignoredPlayers.contains(senderName)){
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Method to broadcast a message in global chat
	 * @param sender Player who sent the message
	 * @param chatMessage Message to send
	 * @param recipients Players in range to receive the message
	 */
	public void broadcastMessage(Player sender, String chatMessage, Set<Player> recipients) {
		int range = config.getChatRange();
		chatLog.writeToChatLog(sender, chatMessage, "GLOBAL");
		Location location = sender.getLocation();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		double chatdist = 0;
		double chatrange = range;
		UUID uuid = NameAPI.getUUID(sender.getName());

		// TODO reintroduce shout and whisper if desired
		
		for (Player receiver : recipients){
			//loop through players and send to those that are close enough
			ChatColor color = ChatColor.valueOf(defaultColor);
			int rx = receiver.getLocation().getBlockX();
			int ry = receiver.getLocation().getBlockY();
			int rz = receiver.getLocation().getBlockZ();
			
			chatdist = Math.sqrt(Math.pow(x- rx, 2) + Math.pow(y - ry, 2) + Math.pow(z - rz, 2));
			
			if(chatdist <= range){
				if(receiver.getWorld() != sender.getWorld()){
					//reciever is in differnt world dont send
					continue;
				} else {
					receiver.sendMessage(color + NameAPI.getCurrentName(uuid) + ":" + chatMessage);
				}
			}
		}
	
	}

	/**
	 * Check if player is afk
	 * @param playername Name of the player to check
	 * @return True if player is afk, false if they are not
	 */
	public boolean isAfk(String playername){
		if(afk_player.contains(playername)){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Method to toggle a players afk/not afk status
	 * @param playername the player to change state
	 */
	public void toggleAfk(String playername){
		Player p = Bukkit.getPlayer(NameAPI.getUUID(playername));
		if(afk_player.contains(playername)){
			//Player was afk bring them back now
			afk_player.remove(playername);
			String afkNoMoreMessage = ChatColor.BLUE + "You are no longer in AFK status";
			p.sendMessage(afkNoMoreMessage);
		}
		else{
			afk_player.add(playername);
			String afkMessage = ChatColor.BLUE + "You have enabled AFK, type /afk to remove afk status";
			p.sendMessage(afkMessage);
		}
	}
	
	/**
	 * Get a players UUID 
	 * @param name	Players Name
	 * @return Returns the players UUID
	 */
	public UUID getPlayerUUID(String name) {
		UUID uuid = NameAPI.getUUID(name);
		if(uuid == null){
			return null;
		}
		else{
			return uuid;
		}
	}
	
	/**
	 * Method to get Group player is chatting in
	 * @param name Player to get Group for
	 * @return Group that the player is chatting in
	 */
	public Group getGroupChatting(String name) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Gets the player to send reply to
	 * @param sender the person sending reply command
	 * @return the UUID of the person to reply to, null if none
	 */
	public UUID getPlayerReply(Player sender) {
		String senderName = sender.getName();
		if(replyList.containsKey(senderName)){
			//sender has someone to reply too
			String replyeeName = replyList.get(senderName);
			UUID uuid = NameAPI.getUUID(replyeeName);
			if(uuid == null){
				String errorMsg = "This should not be occuring... ERROR INFO: sender: [" 
						+ senderName + "] replyList Value: [" + replyeeName +"]";
				CivChat2.warningMessage(errorMsg);
				sender.sendMessage(ChatColor.RED + "Internal Error while performing reply");
			}
			else{
				return uuid;
			}
		}
		return null;
	}
	
	/**
	 * Method to load list of ignored players 
	 * File Format
	 * (player, ignoredplayer1, ignoredplayer2.....)
	 * @param ignoredPlayers file containing ignored players list
	 */
	public void setIgnoredPlayer(HashMap<String, List<String>> ignoredPlayers) {
		this.ignorePlayers = ignoredPlayers;
	}
	
	/**
	 * Method to get the ignoredPlayers list (For file Management)
	 * @return ignoredPlayers Hashmap
	 */
	public HashMap<String, List<String>> getIgnoredPlayer() {
		return this.ignorePlayers;
	}

	/**
	 * Method to see if a user is ignoring a group
	 * @param name Player to check
	 * @param chatChannel Groupname to check
	 * @return true if ignoring, false otherwise
	 */
	public boolean isIgnoringGroup(String name, String chatChannel) {
		String ignoreGroupName = "GROUP:" + chatChannel;
		if(!ignorePlayers.containsKey(name)){
			//player is ignoring something
			List<String> ignored = ignorePlayers.get(name);
			if(ignored.contains(ignoreGroupName)){
				//player is ignoring the groupchat
				return true;
			}			
		}
		return false;
	}


	/**
	 * Method to add a player to ignorelist
	 * @param name Player adding a new ignoree
	 * @param ignore PlayerName to ignore
	 * @return true if player added, false if removed from list
	 */
	public boolean addIgnoringPlayer(String name, String ignore) {
		if(ignorePlayers.containsKey(name)){
			//player already ignoring stuff
			List<String> ignored = ignorePlayers.get(name);
			if(ignored.contains(ignore)){
				//take player out of list
				ignored.remove(ignore);
				ignorePlayers.put(name, ignored);
				return false;
			} else {
				//add player to list
				ignored.add(ignore);
				ignorePlayers.put(name, ignored);
				return true;
			}
		} else {
			//player not yet ignoring anything
			List<String> newIgnoree = new ArrayList<String>();
			newIgnoree.add(ignore);
			ignorePlayers.put(name, newIgnoree);
			return true;
		}
		
	}


	/**
	 * Method to toggle ignoring a group
	 * @param name Player toggling ignoree
	 * @param ignore Group to Ignore
	 * @return True if added to list, false if removed
	 */
	public boolean addIgnoringGroup(String name, String ignore) {
		String groupName = "GROUP" + ignore;
		if(ignorePlayers.containsKey(name)){
			//player already ignoring stuff
			List<String> ignored = ignorePlayers.get(name);
			if(ignored.contains(groupName)){
				//take player out of list
				ignored.remove(groupName);
				ignorePlayers.put(name, ignored);
				return false;
			} else {
				//add player to list
				ignored.add(groupName);
				ignorePlayers.put(name, ignored);
				return true;
			}
		} else {
			//player not yet ignoring anything
			List<String> newIgnoree = new ArrayList<String>();
			newIgnoree.add(groupName);
			ignorePlayers.put(name, newIgnoree);
			return true;
		}
	}

}