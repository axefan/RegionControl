package us.axefan.plugin;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
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
		// Check all region locks in current world.
		// TODO: Store chunk ids for faster lookup.
		EbeanServer db = plugin.getDatabase();
		Player player = event.getPlayer();
		List<ControlledRegion> regions = db.find(ControlledRegion.class).where().eq("worldId", player.getWorld().getId()).findList();
		if (regions.size() == 0) return;
		for (ControlledRegion region : regions) {
			// Check if player is trying to cross region bounds.
			Boolean fromRegion = this.regionContainsLocation(region, event.getFrom());
			Boolean toRegion = this.regionContainsLocation(region, event.getTo());
			if (fromRegion && toRegion) continue;
			if (!fromRegion && !toRegion) continue;
			if (fromRegion && !toRegion) {
				// Player is trying to leave the region.
				if (region.getLocked()) {
					this.preventPlayerMove(event);
					player.sendMessage("You cannot leave this region!");
					return;
				}else{
					// TODO: Add inventory control for unlocked regions.
					player.sendMessage("Restore inventory if enabled.");
					return;					
				}
			}else{
				// Player is trying to enter the region.
				if (region.getLocked()) {
					this.preventPlayerMove(event);
					player.sendMessage("You cannot enter this region!");
					return;
				}else{
					// TODO: Add inventory control for unlocked regions.
					player.sendMessage("Save and set inventory if enabled.");
					return;
				}
			}
		}
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
	}
	
	public void onPlayerKick(PlayerKickEvent event) {
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event) {
	}
	
	public void onPlayerTeleport(PlayerTeleportEvent event) {
	}
	
	/*
	 * Indicates whether a region contains a location.
	 * @param region - The region.
	 * @param location - The location.
	 */
	private boolean regionContainsLocation(ControlledRegion region, Location location) {
		if(location.getBlockX() < region.getMinX() || location.getBlockX() > region.getMaxX()) return false;
		if(location.getBlockY() < region.getMinY() || location.getBlockY() > region.getMaxY()) return false;
		if(location.getBlockZ() < region.getMinZ() || location.getBlockZ() > region.getMaxZ()) return false;
		return true;
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
