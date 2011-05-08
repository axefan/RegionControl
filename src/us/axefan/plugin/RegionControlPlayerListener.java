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
					String msg = region.getNoLeaveMessage();
					if (msg.length() > 0) player.sendMessage(msg);
					return;
				}else{
					// Send player message.
					String msg = region.getLeaveMessage();
					if (msg.length() > 0) player.sendMessage(msg);
					// Restore player inventory.
					if (region.getRestoreInventory() == RegionControl.RestoreInventoryOnLeave) {
						db.delete(plugin.restoreInventory(player, region));
					}
					// Decrement current players.
					region.setCurrentPlayers(region.getCurrentPlayers()-1);
					db.update(region);
					return;
				}
			}else{
				// Player is trying to enter the region.
				if (region.getLocked()) {
					// Region is locked
					this.preventPlayerMove(event);
					String msg = region.getNoEnterMessage();
					if (msg.length() > 0) player.sendMessage(msg);
					return;
				}else{
					// Check max players.
					if (region.getMaxPlayers() >= 0 && region.getCurrentPlayers() == region.getMaxPlayers()) {
						// No more players allowed.
						String msg = region.getMaxMessage();
						if (msg.length() > 0) player.sendMessage(msg);
						this.preventPlayerMove(event);
						return;
					}
					// Send player message.
					String msg = region.getEnterMessage();
					if (msg.length() > 0) player.sendMessage(msg);
					// Set player inventory.
					if (region.getSetInventory() == RegionControl.SetInventoryOnEnter) {
						db.save(plugin.saveInventory(player, region));
						plugin.setInventory(player, region);
					}
					// Increment current players.
					region.setCurrentPlayers(region.getCurrentPlayers()+1);
					db.update(region);
					return;
				}
			}
		}
	}
	
	public void onPlayerJoin(PlayerJoinEvent event) {
		// Check all region in current world.
		EbeanServer db = plugin.getDatabase();
		Player player = event.getPlayer();
		List<ControlledRegion> worldRegions = db.find(ControlledRegion.class).where().eq("worldName", player.getWorld().getName()).findList();
		if (worldRegions.size() == 0) return;
		List<ControlledRegion> saveRegions = new ArrayList<ControlledRegion>();
		Location location = player.getLocation();
		for (ControlledRegion region : worldRegions) {
			if (plugin.regionContainsLocation(region, location)) {
				if (region.getLocked()) {
					// move player to region's spawn location and notify player.
					player.teleport(plugin.getSpawnLocation(region));
					plugin.sendMessage(player, region.getJoinMoveMessage());
				}else{
					// add player to region
					region.setCurrentPlayers(region.getCurrentPlayers()+1);
					saveRegions.add(region);
				}
			}
		}
		if (saveRegions.size() > 0) db.save(saveRegions);
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		// TODO: Remove locks and items on locked player quit.
	}
	
	public void onPlayerKick(PlayerKickEvent event) {
		// TODO: Remove locks and items on locked player quit.
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		System.out.print(event.getPlayer().getName() + " spawned");
	}
	
	public void onPlayerTeleport(PlayerTeleportEvent event) {
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
