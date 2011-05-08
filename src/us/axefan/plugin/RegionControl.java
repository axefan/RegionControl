package us.axefan.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.EbeanServer;
import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class RegionControl extends JavaPlugin {
	
	private WorldEditPlugin worldEditPlugin = null;
	private WorldEditAPI worldEditApi = null;
    private final RegionControlPlayerListener playerListener = new RegionControlPlayerListener(this);
    private final RegionControlEntityListener entityListener = new RegionControlEntityListener(this);
    private final RegionControlBlockListener blockListener = new RegionControlBlockListener(this);
	private String name;
	
	protected static final int SetInventoryNever = 0;
	protected static final int SetInventoryOnEnter = 1;
	protected static final int SetInventoryOnLock = 2;
	protected static final int RestoreInventoryNever = 0;
	protected static final int RestoreInventoryOnLeave = 1;
	protected static final int RestoreInventoryOnUnlock = 2;
	
    /*
     * Setup database.
     */
    public void onLoad() {
        PluginDescriptionFile desc = this.getDescription();
        this.name = desc.getName();
		// Setup database.
		this.installDatabase();
        System.out.println(ChatColor.RED + "Loaded " + this.name + " v" + desc.getVersion() + ".");
    }
    
    /*
     * Setup commands and events.
     */
	public void onEnable() {
        // Reset all regions.
		EbeanServer db = this.getDatabase();
		List<ControlledRegion> regions = new ArrayList<ControlledRegion>();
		List<ControlledRegion> allRegions = db.find(ControlledRegion.class).findList();
		for (ControlledRegion region : allRegions) {
			if (region.getLocked() || (region.getCurrentPlayers() != 0)) {
				region.setLocked(false);
				region.setCurrentPlayers(this.getPlayersInRegion(region).size());
				regions.add(region);
			}
		}
		// Get all locked players.
		List<LockedPlayer> lockedPlayers = db.find(LockedPlayer.class).findList();
		// Get all saved items.
		List<SavedItem> savedItems = db.find(SavedItem.class).findList();
		// Update database.
		if ((regions.size() > 0) || (lockedPlayers.size() > 0) || (savedItems.size() > 0)) {
			// Update regions.
			if (regions.size() > 0) {
				System.out.print(this.name + ": Reseting " + regions.size() + " region" + (regions.size() > 1 ? "s." : "."));
				db.save(regions);
			}
			// Delete locked players.
			if (lockedPlayers.size() > 0) {
				System.out.print(this.name + ": Removing " + lockedPlayers.size() + " locked player" + (lockedPlayers.size() > 1 ? "s." : "."));
				db.delete(lockedPlayers);
			}
			// Delete saved items.
			if (savedItems.size() > 0) {
				System.out.print(this.name + ": Removing " + savedItems.size() + " saved item" + (savedItems.size() > 1 ? "s." : "."));
				db.delete(savedItems);
			}
		}
		// TODO: set player's inventory if region.setInventory = 1. 
		// Setup commands and events.
		this.getCommand("rc").setExecutor(new CommandExecutor (this));
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
//		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
//		pm.registerEvent(Event.Type.PLAYER_KICK, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.REDSTONE_CHANGE, blockListener, Event.Priority.Normal, this);
		// Hook into WorldEdit so we can easily get a selected region.
		this.worldEditPlugin = (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
		if (this.worldEditPlugin != null){
			this.worldEditApi = new WorldEditAPI(this.worldEditPlugin);
			if (this.worldEditApi != null) System.out.println(this.name + ": WorldEdit detected! Using WorldEdit for region selection!");
		}
       System.out.println(this.name + " enabled.");
	}
	
	/*
	 * Unlock regions, spawn players, restore inventory, etc.
	 */
	public void onDisable() {
       // Reset all regions.
		EbeanServer db = this.getDatabase();
		List<ControlledRegion> regions = new ArrayList<ControlledRegion>();
		List<ControlledRegion> allRegions = db.find(ControlledRegion.class).findList();
		List<LockedPlayer> lockedPlayers = new ArrayList<LockedPlayer>();
		List<SavedItem> savedItems = new ArrayList<SavedItem>();
		for (ControlledRegion region : allRegions) {
			if (region.getLocked()) {
				// Unlock players.
				List<LockedPlayer> regionPlayers = db.find(LockedPlayer.class).where().eq("regionId", region.getId()).findList();
				lockedPlayers.addAll(regionPlayers);
				for (LockedPlayer lockedPlayer : regionPlayers) {
					Player player = this.getServer().getPlayer(lockedPlayer.getName());
					// Teleport to default spawn location.
					player.teleport(this.getSpawnLocation(region));
					// Restore inventory.
					if (region.getSetInventory() == RegionControl.SetInventoryOnLock){
						savedItems.addAll(this.restoreInventory(player, region));
					}
				}
				// Unlock the region and clear player count.
				region.setLocked(false);
				region.setCurrentPlayers(0);
				regions.add(region);
			}else if(region.getSetInventory() == RegionControl.SetInventoryOnEnter){
				// Restore inventory.
				List<Player> regionPlayers = this.getPlayersInRegion(region);
				for (Player player : regionPlayers){
					savedItems.addAll(this.restoreInventory(player, region));
				}
			}
		}
		try{
			// Update regions.
			db.save(regions);
			// Delete locked players.
			if (lockedPlayers.size() > 0) db.delete(lockedPlayers);
			// Delete saved items.
			if (savedItems.size() > 0) db.delete(savedItems);
		}catch(Exception ex){
			
		}
		System.out.println(this.name + " disabled.");
	}
	
	/*
	 * Advances a region to the next snapshot frame.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region to modify.
	 */
	protected void frame(CommandSender sender, String regionName) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null){
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Get the current snapshot.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0){
			this.sendMessage(sender, Messages.NoSnapshotsError);
			return;
		}
		// Auto-index to the next frame.  Wrap to first frame on last entry.
		int index;
		int snapshotId = region.getSnapshotId();
		if (snapshotId == 0) {
			this.sendMessage(sender, Messages.SnapshotNotFoundError);
			return;
		}else{
			RegionSnapshot currentSnapshot = db.find(RegionSnapshot.class).where().eq("id", snapshotId).findUnique();
			index = snapshots.indexOf(currentSnapshot) + 1;
			if (index == snapshots.size()) index = 0;
		}
		// Restore the frame.
		this.frame(sender, region, snapshots, index + 1);
	}
	
	/*
	 * Restores a region to the specified snapshot frame.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region.
	 * @param frameNumber - The number of the frame to restore. The first frame is always 1.
	 */
	protected void frame(CommandSender sender, String regionName, int frameNumber) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null) {
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Get the requested snapshot.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0) {
			this.sendMessage(sender, Messages.NoSnapshotsError);
			return;
		}
		// Restore the frame.
		this.frame(sender, region, snapshots, frameNumber);
	}
	
	/*
	 * Restores a region to the specified snapshot frame.
	 * @param sender - The command sender.
	 * @param region - The controlled region.
	 * @param snapshots - A list of snapshots for the controlled region.
	 * @param frameNumber - The number of the frame to restore. The first frame is always 1.
	 */
	private void frame(CommandSender sender, ControlledRegion region, List<RegionSnapshot> snapshots, int frameNumber) {
		// Check index bounds.
		if (frameNumber < 1 || frameNumber > snapshots.size()) {
			this.sendMessage(sender, Messages.InvalidFrameError);
			return;
		}
		// Get the specified snapshot frame.
		RegionSnapshot snapshot = snapshots.get(frameNumber-1);
		// Restore the blocks for this snapshot.
		EbeanServer db = this.getDatabase();
		World world = this.getServer().getWorld(region.getWorldName());
		List<SavedBlock> savedBlocks = db.find(SavedBlock.class).where().eq("snapshotId", snapshot.getId()).orderBy("id").findList();
		for (SavedBlock savedBlock : savedBlocks) {
			Block block = world.getBlockAt(savedBlock.getX(), savedBlock.getY(), savedBlock.getZ());
			if (block.getTypeId() != savedBlock.getTypeId() || block.getData() != savedBlock.getData()) {
				block.setTypeIdAndData(savedBlock.getTypeId(), savedBlock.getData(), false);
			}
		}
		// Set controlled region's current snapshot.
		region.setSnapshotId(snapshot.getId());
		db.update(region);
		// Notify player.
		if (sender != null) sender.sendMessage(ChatColor.DARK_GREEN + Messages.FrameRestoredMessage.replace("{$frame}", Integer.toString(frameNumber)));
	}
	
	/*
	 * Locks a region.
	 * When a region is locked...
	 *  - The players that were inside the region at the time that the lock was placed
	 *    may have their inventory, armor and health saved.
	 *  - The players that are inside the region at the time that the lock is placed
	 *    are not allowed to leave the region.  A player will receive the region's 
	 *    [leaveWarning] message when attempting to leave a locked region.
	 *  - No additional players will be allowed to enter the region.  A player will 
	 *    receive the regions's [enterWarning] message when attempting to enter a locked region.
	 *  - The region cannot be locked until the [minPlayers] condition has been met.
	 *  - If a lock is attempted when the [minPlayers] condition has not been met, all players in the
	 *    region will receive the [minMessage] notification.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region to lock.
	 */
	protected void lock(CommandSender sender, String regionName) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null) {
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Check current region status.
		if (region.getLocked()) {
			this.sendMessage(sender, Messages.RegionAlreadyLockedError);
			return;
		}
		// Check minPlayers
		if (region.getCurrentPlayers() < region.getMinPlayers()) {
			// Not enough players.
			List<Player> regionPlayers = this.getPlayersInRegion(region);
			int needed = region.getMinPlayers() - region.getCurrentPlayers();
			if (needed == 1) {
				this.sendMessage(regionPlayers, region.getMinMessage1());
			}else{
				this.sendMessage(regionPlayers, region.getMinMessage().replace("{$count}", Integer.toString(needed)));
			}
			return;
		}
		// Lock the region.
		region.setLocked(true);
		// Create locked player and saved item objects.
		List<LockedPlayer> lockedPlayers = new ArrayList<LockedPlayer>();
		List<SavedItem> savedItems = new ArrayList<SavedItem>();
		List<Player> regionPlayers = this.getPlayersInRegion(region);
		for (Player regionPlayer : regionPlayers) {
			LockedPlayer lockedPlayer = new LockedPlayer();
			lockedPlayer.setName(regionPlayer.getName());
			lockedPlayer.setRegionId(region.getId());
			lockedPlayer.setHealth(regionPlayer.getHealth());
			lockedPlayers.add(lockedPlayer);
			// Set the player's inventory.
			if (region.getSetInventory() == RegionControl.SetInventoryOnLock) {
				savedItems.addAll(this.saveInventory(regionPlayer, region));
				this.setInventory(regionPlayer, region);
			}
			// Set player health.
			int lockHealth = region.getLockHealth();
			if (lockHealth > 0) regionPlayer.setHealth(lockHealth);
		}
		// Write records.
		try {
			// Begin transaction.
			db.beginTransaction();
			// Set the region lock.
			db.update(region);
			// Save the locked players.
			if (lockedPlayers.size() > 0) db.save(lockedPlayers);
			// Save the player's inventory items.
			if (savedItems.size() > 0) db.save(savedItems);
			// Commit transaction.
			db.commitTransaction();
			this.sendMessage(sender, Messages.RegionLockedMessage);
		} catch(Exception ex) {
			ex.printStackTrace();
			this.sendMessage(sender, Messages.LockRegionError);
			if (sender instanceof Player) this.sendMessage(sender, Messages.CheckServerLogs);
			db.rollbackTransaction();
		}
	}
	
	/*
	 * Unlocks a region.
	 * When a region is unlocked...
	 *  - The players that were inside the region at the time that the lock was placed
	 *    may have their inventory, armor and health restored.
	 *  - The players that were inside the region at the tile that the lock was placed
	 *    will receive the contents of the region's rewards table.
	 *  - Any player may leave the region.
	 *  - Any player may enter the region (up to [maxPlayers] if defined).
	 * @param sender - The command sender.
	 * @param regionName - The name of the region to unlock.
	 */
	protected void unlock(CommandSender sender, String regionName) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null) {
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Check current region status.
		if (!region.getLocked()) {
			this.sendMessage(sender, Messages.RegionNotLockedError);
			return;
		}
		// Unlock the region.
		region.setLocked(false);
		// Get all locked players for this region.
		List<LockedPlayer> lockedPlayers = db.find(LockedPlayer.class).where().eq("regionId", region.getId()).findList();
		List<SavedItem> savedItems = new ArrayList<SavedItem>();
		for (LockedPlayer lockedPlayer : lockedPlayers) {
			// Restore player's inventory.
			Player regionPlayer = this.getServer().getPlayer(lockedPlayer.getName());
			if (region.getRestoreInventory() == RegionControl.RestoreInventoryOnUnlock) {
				savedItems.addAll(this.restoreInventory(regionPlayer, region));
			}
			// TODO: Give players the contents of the rewards table.
			// Restore player's health.
			int unlockHealth = region.getUnlockHealth();
			if (unlockHealth > 0) regionPlayer.setHealth(unlockHealth);
		}
		// Delete records.
		try {
			// Begin transaction.
			db.beginTransaction();
			// Delete the player's saved inventory records.
			if (savedItems.size() > 0) db.delete(savedItems);
			// Delete the locked player records.
			if (lockedPlayers.size() > 0) db.delete(lockedPlayers);
			// Remove the region lock.
			db.update(region);
			// Commit transaction.
			db.commitTransaction();
			this.sendMessage(sender, Messages.RegionUnlockedMessage);
		}catch(Exception ex){
			ex.printStackTrace();
			this.sendMessage(sender, Messages.UnlockRegionError);
			if (sender instanceof Player) this.sendMessage(sender, Messages.CheckServerLogs);
			db.rollbackTransaction();
		}
	}
	
	// TODO: Add ControlledRegion.victory setting.
	// TODO: Add victory handling (on unlock region perform action).  1 = teleport to [victorySpawn].
	// TODO: And fail handling (on unlock region perform action).  1 = teleport to [failSpawn].
	// TODO: Add a PlayerRewards table.  Rewards table should be able to support inventory, armor & money.
	// TODO: Award an achievement if custom achievements can be created.
	// TODO: Increment statistic for fail and victory if custom statistics can be created.
	// TODO: Give each player the contents of the PlayerRewards table on unlock if victory.

	/*
	 * Creates a new snapshot for a region.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region.
	 */
	protected void snap(CommandSender sender, String regionName) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null){
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		try{
			// Begin transaction
			db.beginTransaction();
			// Create a new snapshot record.
			RegionSnapshot snapshot = new RegionSnapshot();
			int regionId = region.getId();
			snapshot.setRegionId(regionId);
			db.save(snapshot);
			// Create new saved block records.
			World world = this.getServer().getWorld(region.getWorldName());
			int snapshotId = snapshot.getId();
			List<SavedBlock> savedBlocks = new ArrayList<SavedBlock>();
			for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
				for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
					for (int z = region.getMinZ(); z <= region.getMaxZ(); z++) {
						Block block = world.getBlockAt(x, y, z);
						SavedBlock savedBlock = new SavedBlock();
						savedBlock.setRegionId(regionId);
						savedBlock.setSnapshotId(snapshotId);
						savedBlock.setTypeId(block.getTypeId());
						savedBlock.setData(block.getData());
						savedBlock.setX(x);
						savedBlock.setY(y);
						savedBlock.setZ(z);
						savedBlocks.add(savedBlock);
					}
				}
			}
			// Save the blocks.
			if (savedBlocks.size() > 0) db.save(savedBlocks);
			// Set current snapshot in controlled region.
			region.setSnapshotId(snapshotId);
			db.update(region);
			// Commit transaction
			db.commitTransaction();			
			// Notify player.
			this.sendMessage(sender, Messages.SnapshotCreatedMessage);
		}catch(Exception ex) {
			ex.printStackTrace();
			this.sendMessage(sender, Messages.CreateSnapshotError);
			if (sender instanceof Player) this.sendMessage(sender, Messages.CheckServerLogs);
			db.rollbackTransaction();
		}
	}
	
	/*
	 * Deletes the last snapshot for a region.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region.
	 */
	protected void delsnap(CommandSender sender, String regionName) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null) {
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Get the region's snapshots in order.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0) {
			this.sendMessage(sender, Messages.NoSnapshotsError);
			return;
		}
		// Delete the snapshot.
		this.delsnap(sender, region, snapshots, snapshots.size());
	}
	
	/*
	 * Deletes the specified snapshot for a region.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region.
	 * @param frameNumber - The number of the frame to delete. The first frame is always 1.
	 */
	protected void delsnap(CommandSender sender, String regionName, int frameNumber) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null) {
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Get the region's snapshots in order.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		// Delete the snapshot.
		this.delsnap(sender, region, snapshots, frameNumber);
	}
	
	/*
	 * Deletes the specified snapshot for a region.
	 * @param sender - The command sender.
	 * @param region - The controlled region.
	 * @param snapshots - A list of snapshots for the controlled region.
	 * @param frameNumber - The number of the frame to delete. The first frame is always 1.
	 */
	private void delsnap(CommandSender sender, ControlledRegion region, List<RegionSnapshot> snapshots, int frameNumber) {
		// Check index bounds.
		if (frameNumber < 1 || frameNumber > snapshots.size()) {
			this.sendMessage(sender, Messages.InvalidFrameError);
			return;
		}
		// Get the blocks for this snapshot.
		EbeanServer db = this.getDatabase();
		List<SavedBlock> savedBlocks = db.find(SavedBlock.class).where().eq("regionId", region.getId()).findList();
		// Delete the snapshot.
		try {
			db.beginTransaction();
			db.delete(savedBlocks);
			db.delete(snapshots.get(frameNumber-1));
			db.commitTransaction();
			this.sendMessage(sender, Messages.SnapshotDeletedMessage);
		}catch(Exception ex){
			ex.printStackTrace();
			this.sendMessage(sender, Messages.DeleteSnapshotsError);
			if (sender instanceof Player) this.sendMessage(sender, Messages.CheckServerLogs);
			db.rollbackTransaction();			
		}
	}
	
	// TODO: Add movesnap command to re-order snapshots.
	
	/*
	 * Creates a new controlled region.
	 * @param player - The player creating the region.
	 * @param regionName - The name of the new region.
	 */
	protected void create(Player player, String regionName) {
		// check for existing region
		EbeanServer db = this.getDatabase();
		if (db.find(ControlledRegion.class).where().eq("name", regionName).findRowCount() != 0) {
			player.sendMessage(Messages.RegionExistsError);
			return;
		}
		// get selected region
		com.sk89q.worldedit.regions.Region selection = this.getSelectedRegion(player);
		if (selection == null) {
			player.sendMessage(Messages.NoSelectionError);
			return;
		}
		com.sk89q.worldedit.Vector max = selection.getMaximumPoint();
		com.sk89q.worldedit.Vector min = selection.getMinimumPoint();
		// create new controlled region
		ControlledRegion region = new ControlledRegion();
		region.setWorldName(player.getWorld().getName());
		region.setName(regionName);
		region.setMaxX((int)max.getX());
		region.setMaxY((int)max.getY());
		region.setMaxZ((int)max.getZ());
		region.setMinX((int)min.getX());
		region.setMinY((int)min.getY());
		region.setMinZ((int)min.getZ());
		region.setCurrentPlayers(this.getPlayersInRegion(region).size());
		// Save the new controlled region record.
		db.save(region);
		player.sendMessage(Messages.RegionCreatedMessage);
	}
	
	/*
	 * Updates a region field value.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region.
	 * @param setting - The name of the setting to update.
	 * @param value - The new setting value.
	 */
	protected void edit(CommandSender sender, String regionName, String setting, String value) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null){
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Update setting
		if (setting.equals("name")) {
			if (db.find(ControlledRegion.class).where().eq("name", value).findRowCount() != 0) {
				this.sendMessage(sender, Messages.RegionExistsError);
				return;
			}
			region.setName(value);
		}else if(setting.equalsIgnoreCase("maxPlayers") || setting.equalsIgnoreCase("maxp")) {
			int maxPlayers;
			try{
				maxPlayers = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(sender, Messages.IntegerError.replace("{$value}", setting));
				return;
			}
			if (maxPlayers < -1) {
				this.sendMessage(sender, Messages.NegativeOneOrGreaterError.replace("{$value}", setting));
				return;
			}
			region.setMaxPlayers(maxPlayers);
		}else if(setting.equalsIgnoreCase("minPlayers") || setting.equalsIgnoreCase("minp")) {
			int minPlayers;
			try{
				minPlayers = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(sender, Messages.IntegerError.replace("{$value}", setting));
				return;
			}
			if (minPlayers < 0) {
				this.sendMessage(sender, Messages.ZeroOrGreaterError.replace("{$value}", setting));
				return;
			}
			region.setMinPlayers(minPlayers);
		}else if(setting.equalsIgnoreCase("lockHealth") || setting.equalsIgnoreCase("lh")) {
			int lockHealth;
			try{
				lockHealth = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(sender, Messages.IntegerError.replace("{$value}", setting));
				return;
			}
			if (lockHealth < 0 || lockHealth > 20) {
				this.sendMessage(sender, Messages.ValueRangeError.replace("{$value}", setting).replace("{$range}", "0 to 20"));
				return;
			}
			region.setLockHealth(lockHealth);
		}else if(setting.equalsIgnoreCase("unlockHealth") || setting.equalsIgnoreCase("uh")) {
			int unlockHealth;
			try{
				unlockHealth = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(sender, Messages.IntegerError.replace("{$value}", setting));
				return;
			}
			if (unlockHealth < 0 || unlockHealth > 20) {
				this.sendMessage(sender, Messages.ValueRangeError.replace("{$value}", setting).replace("{$range}", "0 to 20"));
				return;
			}
			region.setUnlockHealth(unlockHealth);
		}else if(setting.equalsIgnoreCase("setInventory") || setting.equalsIgnoreCase("si")) {
			int setInventory;
			try{
				setInventory = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(sender, Messages.IntegerError.replace("{$value}", setting));
				return;
			}
			if (setInventory < 0 || setInventory > 2) {
				this.sendMessage(sender, Messages.ValueRangeError.replace("{$value}", setting).replace("{$range}", "0 to 2"));
				return;
			}
			region.setSetInventory(setInventory);
		}else if(setting.equalsIgnoreCase("restoreInventory") || setting.equalsIgnoreCase("ri")) {
			int restoreInventory;
			try{
				restoreInventory = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(sender, Messages.IntegerError.replace("{$value}", setting));
				return;
			}
			if (restoreInventory < 0 || restoreInventory > 2) {
				this.sendMessage(sender, Messages.ValueRangeError.replace("{$value}", setting).replace("{$range}", "0 to 2"));
				return;
			}
			region.setRestoreInventory(restoreInventory);
		}else if(setting.equalsIgnoreCase("maxMessage") || setting.equalsIgnoreCase("maxm")) {
			region.setMaxMessage(value);
		}else if(setting.equalsIgnoreCase("minMessage") || setting.equalsIgnoreCase("minm")) {
			region.setMinMessage(value);
		}else if(setting.equalsIgnoreCase("minMessage1") || setting.equalsIgnoreCase("min1")) {
			region.setMinMessage1(value);
		}else if(setting.equalsIgnoreCase("enterMessage") || setting.equalsIgnoreCase("em")) {
			region.setEnterMessage(value);
		}else if(setting.equalsIgnoreCase("leaveMessage") || setting.equalsIgnoreCase("lm")) {
			region.setLeaveMessage(value);
		}else if(setting.equalsIgnoreCase("noEnterMessage") || setting.equalsIgnoreCase("nem")) {
			region.setNoEnterMessage(value);
		}else if(setting.equalsIgnoreCase("noLeaveMessage") || setting.equalsIgnoreCase("nlm")) {
			region.setNoLeaveMessage(value);
		}else if(setting.equalsIgnoreCase("setInventoryMessage") || setting.equalsIgnoreCase("sim")) {
			region.setSetInventoryMessage(value);
		}else if(setting.equalsIgnoreCase("restoreInventoryMessage") || setting.equalsIgnoreCase("rim")) {
			region.setRestoreInventoryMessage(value);
		}else if(setting.equalsIgnoreCase("joinMoveMessage") || setting.equalsIgnoreCase("jmm")) {
			region.setRestoreInventoryMessage(value);
		}else{
			this.sendMessage(sender, Messages.NoSuchSetting.replace("{$setting}", setting));
			return;
		}
		// Update the controlled section.
		db.update(region);
		this.sendMessage(sender, Messages.RegionUpdatedMessage);
	}
	
	/*
	 * Deletes a controlled region.
	 * @param sender - The command sender.
	 * @param regionName - The name of the region to delete.
	 */
	protected void delete(CommandSender sender, String regionName) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", regionName).findUnique();
		if (region == null){
			this.sendMessage(sender, Messages.NoRegionError);
			return;
		}
		// Do not allow delete of locked region.
		if (region.getLocked()) {
			this.sendMessage(sender, Messages.DeleteRegionError);
			this.sendMessage(sender, Messages.RegionLockedError);
			return;
		}
		// Delete region and all related records.
		try{
			// Get related records.
			List<SavedBlock> savedBlocks = db.find(SavedBlock.class).where().eq("regionId", region.getId()).findList();
			List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).findList();			
			List<RegionItem> regionItems = db.find(RegionItem.class).where().eq("regionId", region.getId()).findList();
			db.beginTransaction();
			db.delete(savedBlocks);
			db.delete(snapshots);
			db.delete(regionItems);
			db.delete(region);
			db.commitTransaction();
			this.sendMessage(sender, Messages.RegionDeletedMessage);
		}catch(Exception ex){
			ex.printStackTrace();
			this.sendMessage(sender, Messages.DeleteRegionError);
			if (sender instanceof Player) this.sendMessage(sender, Messages.CheckServerLogs);
			db.rollbackTransaction();
		}
	}
	
	/*
	 * Displays information about a controlled region.
	 * @param sender - The command sender.
	 * @param name - The name of the region.
	 */
	protected void info(CommandSender sender, String name) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			sender.sendMessage(Messages.NoRegionError);
			return;
		}
		// Display region info.
		sender.sendMessage(this.getSettingText("Name", region.getName()));
		sender.sendMessage(this.getSettingText("World", region.getWorldName()));
		sender.sendMessage(this.getSettingText("Locked", (region.getLocked() ? "yes" : "no")));
		// Display region bounds.
		sender.sendMessage(
			this.getSettingText("Max", Double.toString(region.getMaxX()) + ", " + Double.toString(region.getMaxY()) + ", " + Double.toString(region.getMaxZ())) + " " + 
			this.getSettingText("Min", Double.toString(region.getMinX()) + ", " + Double.toString(region.getMinY()) + ", " + Double.toString(region.getMinZ())));
		// Display snapshot info.
		int snapshotId = region.getSnapshotId();
		if (snapshotId != 0){
			List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
			RegionSnapshot currentSnapshot = db.find(RegionSnapshot.class).where().eq("id", snapshotId).findUnique();
			sender.sendMessage(
				this.getSettingText("Snapshots", Integer.toString(snapshots.size())) + " " +
				this.getSettingText("Frame", Integer.toString(snapshots.indexOf(currentSnapshot) + 1)));
		}else{
			sender.sendMessage(this.getSettingText("Snapshots", "none"));
		}
		// Display player info.
		sender.sendMessage(
			this.getSettingText("Max Players", (region.getMaxPlayers() < 0 ? "no limit" : Integer.toString(region.getMaxPlayers()))) + " " +
			this.getSettingText("Min Players", region.getMinPlayers()) + " " +
			this.getSettingText("Current Players", region.getCurrentPlayers()));
		// Display health settings.
		sender.sendMessage(
			this.getSettingText("Lock Health", region.getLockHealth()) + " " +
			this.getSettingText("Unlock Health", region.getUnlockHealth()));
		// Display inventory settings.
		sender.sendMessage(
			this.getSettingText("Set Inventory", region.getSetInventory()) + " " +
			this.getSettingText("Restore Inventory", region.getRestoreInventory()));
		// Display spawn information.
		sender.sendMessage(this.getSettingText("Spawn Location: ", 
			region.getSpawnSet() ? Double.toString(region.getSpawnX()) + ", " + Double.toString(region.getSpawnY()) + ", " + Double.toString(region.getSpawnZ()) : "not set"));
	}
	
	/*
	 * Returns a formatted setting string
	 * @param settingName - The name of the setting.
	 * @param value - The setting value.
	 */
	private String getSettingText(String settingName, String value){
		return ChatColor.GRAY + settingName + ": " + ChatColor.BLUE + value;
	}
	
	/*
	 * Returns a formatted setting string
	 * @param settingName - The name of the setting.
	 * @param value - The setting value.
	 */
	private String getSettingText(String settingName, Integer value){
		return this.getSettingText(settingName, Integer.toString(value));
	}
	
	/*
	 * Displays a controlled region's messages.
	 * @param sender - The command sender.
	 * @param name - The name of the region.
	 */
	protected void messages(CommandSender sender, String name) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			sender.sendMessage(Messages.NoRegionError);
			return;
		}
		// Display messages
		sender.sendMessage(this.getSettingText("Max Players Message", region.getMaxMessage()));
		sender.sendMessage(this.getSettingText("Min Players Message", region.getMinMessage()));
		sender.sendMessage(this.getSettingText("Min Players Message (1)", region.getMinMessage1()));
		sender.sendMessage(this.getSettingText("Enter Region Message", region.getEnterMessage()));
		sender.sendMessage(this.getSettingText("Leave Region Message", region.getLeaveMessage()));
		sender.sendMessage(this.getSettingText("Cannot Enter Message", region.getNoEnterMessage()));
		sender.sendMessage(this.getSettingText("Cannot Leave Message", region.getNoLeaveMessage()));
		sender.sendMessage(this.getSettingText("Set Inventory Message", region.getSetInventoryMessage()));
		sender.sendMessage(this.getSettingText("Restore Inventory Message", region.getRestoreInventoryMessage()));
		sender.sendMessage(this.getSettingText("Teleport On Join Message", region.getJoinMoveMessage()));
	}
	
	/*
	 * Sets the default spawn locations.
	 * This location is used to move players out of a locked region when the plugin is 
	 * disabled or when a player joins into a locked region. Can also be used to teleport to
	 * the region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void setSpawnLocation(Player player, String name) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			player.sendMessage(Messages.NoRegionError);
			return;
		}
		// Spawn location must be outside  region.
		Location location = player.getLocation();
		if (this.regionContainsLocation(region, location)) {
			player.sendMessage(Messages.SpawnNotSetError);
			player.sendMessage(Messages.SpawnLocationError);
			return;
		}
		// Update the spawn location.
		region.setSpawnX(location.getBlockX());
		region.setSpawnY(location.getBlockY());
		region.setSpawnZ(location.getBlockZ());
		region.setSpawnSet(true);
		// Save changes.
		try{
			db.update(region);
			player.sendMessage(Messages.ValueSetMessage.replace("{$setting}", "Spawn Location").replace("{$value}", 
				region.getSpawnX() + ", " + region.getSpawnY() + ", " + region.getSpawnZ()));
		}catch(Exception ex){
			ex.printStackTrace();
			this.sendMessage(player, Messages.LockRegionError);
			this.sendMessage(player, Messages.CheckServerLogs);			
		}
	}
	
	/*
	 * Teleports the player to the region's default spawn location if defined.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void teleport(Player player, String name) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			player.sendMessage(Messages.NoRegionError);
			return;
		}
		// Teleport the player.
		if (region.getSpawnSet()) {
			player.teleport(this.getSpawnLocation(region));
		}else{
			player.sendMessage(Messages.SpawnNotSetError);
		}
	}
	
	/*
	 * Lists all controlled regions including locked status and number of players if locked.
	 * @param sender - The command sender.
	 */
	protected void list(CommandSender sender) {
		StringBuilder message = new StringBuilder();
		message.append(ChatColor.BLUE);
		message.append("Controlled Regions: ");
		int lockedCount = 0;
		EbeanServer db = this.getDatabase();
		List<ControlledRegion> regions = db.find(ControlledRegion.class).orderBy("name").findList();
		if (regions.size() == 0){
			message.append(ChatColor.BLUE);
			message.append("No regions defined.");
		}else{
			for (ControlledRegion region : regions){
				message.append(ChatColor.BLUE);
				message.append(region.getName());
				if (region.getLocked()){
					lockedCount++;
					message.append(ChatColor.DARK_AQUA);
					message.append(" (L)");
				}
				if (region != regions.get(regions.size()-1)){
					message.append(ChatColor.BLUE);
					message.append(", ");
				}
			}
		}
		message.append(". ");
		message.append(ChatColor.DARK_AQUA);
		message.append(lockedCount);
		message.append("/");
		message.append(regions.size());
		message.append(" regions are locked.");
		sender.sendMessage(message.toString());
	}
	
	/*
	 * Gets the region currently selected by the player.
	 * @param player - The player.
	 */
	private com.sk89q.worldedit.regions.Region getSelectedRegion(Player player) {
		com.sk89q.worldedit.LocalSession session = this.worldEditApi.getSession(player);
		com.sk89q.worldedit.regions.Region region = null;
		try {
			region = session.getSelection(session.getSelectionWorld());
		} catch (com.sk89q.worldedit.IncompleteRegionException e) {}
		return region;
	}
	
	/*
	 * Returns the default spawn location for a region.
	 * @param region - The region.
	 */
	protected Location getSpawnLocation(ControlledRegion region) {
		World world = this.getServer().getWorld(region.getWorldName());
		if (region.getSpawnSet()){
			return new Location(world, region.getSpawnX(), region.getSpawnY(), region.getSpawnZ());
		}
		return world.getSpawnLocation();
	}
	
    /*
     * Ensures that the database for this plugin is installed and available.
     */
    private void installDatabase() {
		try {
			File ebeans = new File("ebean.properties");
			if (!ebeans.exists()) {
				try {
					ebeans.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.getDatabase().find(ControlledRegion.class).findRowCount();
		} catch (PersistenceException ex) {
			System.out.print(Messages.InstallingDatabasesMessage.replace("{$name}", this.name));
			installDDL();
		}
	}
    
    /*
     * List the classes to store in the database.
     */
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(ControlledRegion.class);
		list.add(LockedPlayer.class);
		list.add(RegionItem.class);
		list.add(RegionSnapshot.class);
		list.add(SavedBlock.class);
		list.add(SavedItem.class);
		return list;
	}
	
	/*
	 * Sends a message to a player.
	 * @param sender - The command sender.
	 * @param message - The message.
	 */
	protected void sendMessage(CommandSender sender, String message){
		if (message.length() == 0) return;
		if (sender == null){
			this.getServer().broadcastMessage(message);
		}else{
			sender.sendMessage(message);
		}
	}
	
	/*
	 * Broadcasts a message to a list of players.
	 * @param players - The list of players.
	 * @param message - The message.
	 */
	protected void sendMessage(List<Player> players, String message) {
		if (message.length() == 0) return;
		for (Player player : players){
			this.sendMessage(player, message);
		}
	}

	/*
	 * Saves a player's inventory.
	 * @param player - The player.
	 * @param region - The region.
	 */
	protected List<SavedItem> saveInventory(Player player, ControlledRegion region) {
		List<SavedItem> savedItems = new ArrayList<SavedItem>();
		PlayerInventory inventory = player.getInventory();
		for (ItemStack item : inventory.getContents()) {
			if (item != null && item.getTypeId() != 0) {
				savedItems.add(this.createSavedItem(item, player, 0));
			}
		}
		ItemStack helmet = inventory.getHelmet();
		if (helmet != null) savedItems.add(this.createSavedItem(helmet, player, 1));
		ItemStack chestPlate = inventory.getChestplate();
		if (chestPlate != null) savedItems.add(this.createSavedItem(chestPlate, player, 2));
		ItemStack leggings = inventory.getLeggings();
		if (leggings != null) savedItems.add(this.createSavedItem(leggings, player, 3));
		ItemStack boots = inventory.getBoots();
		if (boots != null) savedItems.add(this.createSavedItem(boots, player, 4));
		return savedItems;
	}

	/*
	 * Indicates whether a player is inside a controlled region.
	 * @param region - The region.
	 * @param player - The player.
	 */
	protected boolean regionContainsPlayer(ControlledRegion region, Player player) {
		Location location = player.getLocation();
		if (location.getX() > region.getMaxX()) return false;
		if (location.getY() > region.getMaxY()) return false;
		if (location.getZ() > region.getMaxZ()) return false;
		if (location.getX() < region.getMinX()) return false;
		if (location.getY() < region.getMinY()) return false;
		if (location.getZ() < region.getMinZ()) return false;
		return true;
	}
	
	/*
	 * Indicates whether a region contains a location.
	 * @param region - The region.
	 * @param location - The location.
	 */
	protected boolean regionContainsLocation(ControlledRegion region, Location location) {
		if(location.getBlockX() < region.getMinX() || location.getBlockX() > region.getMaxX()) return false;
		if(location.getBlockY() < region.getMinY() || location.getBlockY() > region.getMaxY()) return false;
		if(location.getBlockZ() < region.getMinZ() || location.getBlockZ() > region.getMaxZ()) return false;
		return true;
	}

	/*
	 * Creates a SavedItem from an ItemStack.
	 * @param item - The item.
	 * @param player - The player.
	 * @param category - The RegionControl-specific item category.
	 */
	private SavedItem createSavedItem(ItemStack item, Player player, int category) {
		SavedItem savedItem = new SavedItem();
		savedItem.setCategory(category);
		savedItem.setPlayerName(player.getName());
		savedItem.setTypeId(item.getTypeId());
		savedItem.setDurability(item.getDurability());
		savedItem.setAmount(item.getAmount());
		MaterialData data = item.getData();
		if (data == null){
			savedItem.setData((byte) 0);
		}else{
			savedItem.setData(data.getData());
		}
		return savedItem;
	}
	
	/*
	 * Creates an ItemStack from a SavedItem.
	 * @param savedItem - The saved item.
	 */
	private ItemStack createItemStack(SavedItem savedItem) {
		int typeId = savedItem.getTypeId();
		if (typeId == 0) return null;
		ItemStack item = new ItemStack(typeId, 1);
		item.setDurability(savedItem.getDurability());
		item.setAmount(savedItem.getAmount());
		if (savedItem.getData() != 0){
			item.setData(new MaterialData(typeId, savedItem.getData()));
		}
		return item;
	}
	
	/*
	 * Creates an ItemStack from a RegionItem.
	 * @param regionItem - The region item.
	 */
	private ItemStack createItemStack(RegionItem regionItem) {
		int typeId = regionItem.getTypeId();
		if (typeId == 0) return null;
		ItemStack item = new ItemStack(typeId, 1);
		item.setDurability(regionItem.getDurability());
		item.setAmount(regionItem.getAmount());
		if (regionItem.getData() != 0){
			item.setData(new MaterialData(typeId, regionItem.getData()));
		}
		return item;
	}
	
	/*
	 * Sets a players inventory to a region's items.
	 * @param player - The player.
	 * @param region - The region.
	 */
	protected void setInventory(Player player, ControlledRegion region) {
		// Clear player's inventory.
		PlayerInventory inventory = player.getInventory();
		inventory.clear();
		inventory.setHelmet(null);
		inventory.setChestplate(null);
		inventory.setLeggings(null);
		inventory.setBoots(null);
		// Give player region's items.
		EbeanServer db = this.getDatabase();
		List<RegionItem> regionItems = db.find(RegionItem.class).where().eq("regionId", region.getId()).findList();
		for (RegionItem regionItem : regionItems) {
			this.setInventoryItem(regionItem, inventory);
		}
		// Notify player.
		String message = region.getSetInventoryMessage();
		if (message.length() > 0) player.sendMessage(message);
	}
	
	/*
	 * Gives the player a region item.
	 * @param regionItem - The region item to add.
	 * @param inventory - The player's inventory.
	 */
	private void setInventoryItem(RegionItem regionItem, PlayerInventory inventory) {
		if (regionItem == null) return;
		this.setInventoryItem(this.createItemStack(regionItem), inventory, regionItem.getCategory());
	}
	
	/*
	 * Restores a saved item to a player's inventory.
	 * @param savedItem - The saved item to add.
	 * @param inventory - The player's inventory.
	 */
	private void setInventoryItem(SavedItem savedItem, PlayerInventory inventory) {
		if (savedItem == null) return;
		this.setInventoryItem(this.createItemStack(savedItem), inventory, savedItem.getCategory());
	}
	
	/*
	 * Restores a player's inventory.
	 * TODO: Test what happens when inventory cannot hold all items.
	 * @param player - The player.
	 * @param region - The region.
	 */
	protected List<SavedItem> restoreInventory(Player player, ControlledRegion region) {
		// Clear player's inventory.
		PlayerInventory inventory = player.getInventory();
		inventory.clear();
		inventory.setHelmet(null);
		inventory.setChestplate(null);
		inventory.setLeggings(null);
		inventory.setBoots(null);
		EbeanServer db = this.getDatabase();
		List<SavedItem> savedItems = db.find(SavedItem.class).where().eq("playerName", player.getName()).findList();
		for (SavedItem savedItem : savedItems) {
			this.setInventoryItem(savedItem, inventory);
		}
		// Notify player.
		String message = region.getRestoreInventoryMessage();
		if (message.length() > 0) player.sendMessage(message);
		// Return the saved items so they can be deleted.
		return savedItems;
	}

	/*
	 * Gives an item to a player.
	 * @param item - The item.
	 * @param inventory - The player's inventory.
	 * @param category - The saved item category.
	 */
	private void setInventoryItem(ItemStack item, PlayerInventory inventory, int category) {
		if (item == null) return;
		switch(category){
		case 0: // item
			inventory.addItem(item);
			break;
		case 1: // helmet
			inventory.setHelmet(item);
			break;
		case 2: // chest plate
			inventory.setChestplate(item);
			break;
		case 3: // leggings
			inventory.setLeggings(item);
			break;
		case 4: // boots
			inventory.setBoots(item);
			break;
		default:
			System.out.print(Messages.InvalidItemCategoryError.replace("{$category}", Integer.toString(category)));
			break;
		}		
	}

	/*
	 * Gets a list of all player in the specified region.
	 * @param region - The region.
	 */
	private List<Player> getPlayersInRegion(ControlledRegion region) {
		World world = this.getServer().getWorld(region.getWorldName());
		List<Player> regionPlayers = new ArrayList<Player>();
		List<Player> worldPlayers = world.getPlayers();
		for (Player worldPlayer : worldPlayers) {
			if (this.regionContainsPlayer(region, worldPlayer)) {
				regionPlayers.add(worldPlayer);
			}
		}
		return regionPlayers;
	}

}