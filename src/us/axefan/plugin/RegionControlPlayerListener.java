package us.axefan.plugin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.avaje.ebean.EbeanServer;

public class RegionControlPlayerListener extends org.bukkit.event.player.PlayerListener {

	public static RegionControl plugin;
	
	public RegionControlPlayerListener(RegionControl instance) {
		plugin = instance;
	}
	
	public void onPlayerMove(PlayerMoveEvent event) {
		// Determine whether the player is crossing a controlled region's bounds.
		EbeanServer db = plugin.getDatabase();
		Player player = event.getPlayer();
		List<ControlledRegion> regions = db.find(ControlledRegion.class).where().eq("worldName", player.getWorld().getName()).findList();
		if (regions.size() == 0) return;
		Location from = event.getFrom();
		Location to = event.getTo();
		for (ControlledRegion region : regions) {
			// Check if player is trying to cross region bounds.
			Boolean fromRegion = plugin.regionContainsLocation(region, from);
			Boolean toRegion = plugin.regionContainsLocation(region, to);
			if (fromRegion && toRegion) continue;
			if (!fromRegion && !toRegion) continue;
			if (fromRegion && !toRegion) {
				// Player is trying to leave the region.
				if (region.getLocked()) {
					this.preventPlayerMove(event);
					plugin.sendMessage(player, region.getNoLeaveMessage());
					return;
				}else{
					// Send player message.
					plugin.sendMessage(player, region.getLeaveMessage());
					// Restore player inventory.
					if (region.getRestoreInventory() == RegionControl.RestoreInventoryOnLeave) {
						if (plugin.verbose) plugin.sendMessage("Restoring player inventory when leaving region - " + player.getName() + " left region " + region.getName() + ".");
						db.delete(plugin.restoreInventory(player, region, true));
					}
					// Set current players.
					region.setCurrentPlayers(plugin.getPlayersInRegion(region).size());
					db.update(region);
					return;
				}
			}else{
				// Player is trying to enter the region.
				if (region.getLocked()) {
					// Region is locked
					plugin.sendMessage(player, region.getNoEnterMessage());
					this.preventPlayerMove(event);
					return;
				}else{
					// Check max players.
					if (region.getMaxPlayers() > -1 && region.getCurrentPlayers() == region.getMaxPlayers()){
						// No more players allowed.
						this.preventPlayerMove(event);
						plugin.sendMessage(player, region.getMaxMessage());
						return;
					}
					// Send player message.
					plugin.sendMessage(player, region.getEnterMessage());
					// Set player inventory.
					if (region.getSetInventory() == RegionControl.SetInventoryOnEnter) {
						if (plugin.verbose) plugin.sendMessage("Setting player inventory on player move - " + player.getName() + " entered region " + region.getName() + ".");
						db.save(plugin.saveInventory(player, region));
						plugin.setInventory(player, region);
					}
					// Increment current players.
					region.setCurrentPlayers(region.getCurrentPlayers()+1);
					// Check max player lock.
					if (region.getLockOnMaxPlayers() && region.getCurrentPlayers() == region.getMaxPlayers()){
						// Auto-lock region.
						if (plugin.verbose) plugin.sendMessage("Locking region on player move due to max players - " + player.getName() + " entered region " + region.getName() + ".");
						plugin.lock(null, region);
					}else{
						db.update(region);
					}
					return;
				}
			}
		}
	}
	
	public void onPlayerJoin(PlayerJoinEvent event) {
		// Check all regions in current world.
		EbeanServer db = plugin.getDatabase();
		Player player = event.getPlayer();
		List<ControlledRegion> worldRegions = db.find(ControlledRegion.class).where().eq("worldName", player.getWorld().getName()).findList();
		if (worldRegions.size() == 0) return;
		List<ControlledRegion> regions = new ArrayList<ControlledRegion>();
		Location location = player.getLocation();
		for (ControlledRegion region : worldRegions) {
			if (plugin.regionContainsLocation(region, location)) {
				if (region.getLocked()) {
					// Region is locked
					if (plugin.verbose) plugin.sendMessage("Teleporting player on join due to region locked - " + player.getName() + " from region " + region.getName() + ".");
					player.teleport(plugin.getSpawnLocation(region));
					plugin.sendMessage(player, region.getJoinMoveMessage()); // TODO: Change to joinLockedMessage
				}else{
					// Check max players.
					if (region.getMaxPlayers() > -1 && region.getCurrentPlayers() == region.getMaxPlayers()) {
						// No more players allowed.
						if (plugin.verbose) plugin.sendMessage("Teleporting player on join due to max players - " + player.getName() + " from region " + region.getName() + ".");
						player.teleport(plugin.getSpawnLocation(region));
						if (plugin.verbose) plugin.sendMessage(player, region.getJoinMoveMessage()); // TODO: Change to joinMaxMessage
						return;
					}
					// Send player message.
					plugin.sendMessage(player, region.getEnterMessage());
					// Set player inventory.
					if (region.getSetInventory() == RegionControl.SetInventoryOnEnter) {
						if (plugin.verbose) plugin.sendMessage("Setting player inventory on player join - " + player.getName() + " joined in region " + region.getName() + ".");
						db.save(plugin.saveInventory(player, region));
						plugin.setInventory(player, region);
					}
					// Increment current players.
					region.setCurrentPlayers(region.getCurrentPlayers()+1);
					// Check max player lock.
					if (region.getMaxPlayers() > -1 && region.getCurrentPlayers() == region.getMaxPlayers()) {
						if (plugin.verbose) plugin.sendMessage("Locking region on join due to max players - " + player.getName() + " joined in region " + region.getName() + ".");
						plugin.lock(null, region);
					}else{
						regions.add(region);
					}					
				}
			}
		}
		if (regions.size() > 0) db.save(regions);
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		plugin.removePlayer(event.getPlayer());
	}
	
	public void onPlayerKick(PlayerKickEvent event) {
		plugin.removePlayer(event.getPlayer());
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		// TODO: determine if onPlayerRespawn needs to be handled.
		System.out.print(event.getPlayer().getName() + " spawned");
	}
	
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		// TODO: Handle player teleport from and to a region.
		// Not sure how to handle this yet.
		// Probably need to handle lockOnMaxPlayers.  Should we use teleport
		// for transferring from one locked region to another or use a new command "transfer".
		System.out.print(event.getPlayer().getName() + " teleported");
	}
	
	/*
	 * Prevents a player from moving.
	 * @param event - The PLAYER_MOVE event.
	 */
	private void preventPlayerMove(PlayerMoveEvent event){
		Location loc = event.getFrom();
		event.setTo(loc);
		event.setFrom(loc);
		event.setCancelled(true);
		event.getPlayer().teleport(loc);		
	}

}
