package us.axefan.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
	private boolean initialized = false;
	private String name;
	
    /*
     * Setup database.
     */
    public void onLoad() {
        PluginDescriptionFile desc = this.getDescription();
        this.name = desc.getName();
		// Setup database.
		this.setupDatabase();
		// Initialize database
		this.initDatabase();
        System.out.println("Loaded " + this.name + " v" + desc.getVersion() + ".");
    }
    
    /*
     * Setup commands and events.
     */
	public void onEnable() {
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
	 * Perform basic database maintenance.
	 */
	private void initDatabase() {
		if (this.initialized) return;
        // Reset all regions.
		EbeanServer db = this.getDatabase();
		List<ControlledRegion> regions = new ArrayList<ControlledRegion>();
		List<ControlledRegion> allRegions = db.find(ControlledRegion.class).findList();
		for (ControlledRegion region : allRegions) {
			if (region.getLocked() || (region.getCurrentPlayers() != 0)) {
				region.setLocked(false);
				region.setCurrentPlayers(0);
				regions.add(region);
			}
		}
		// Get all locked regions.
		List<LockedPlayer> lockedPlayers = db.find(LockedPlayer.class).findList();
		// Get all saved items.
		List<SavedItem> savedItems = db.find(SavedItem.class).findList();
		if ((regions.size() > 0) || (lockedPlayers.size() > 0) || (savedItems.size() > 0)) {
			// Begin transaction.
			db.beginTransaction();
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
			// Commit transaction.
			db.commitTransaction();
		}
		this.initialized = true;
	}

	public void onDisable() {
		System.out.println(this.name + " disabled.");
	}
	
	/*
	 * Restores a region to the next snapshot frame.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void frame(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			this.sendMessage(player, "no such region");
			return;
		}
		// Get the current snapshot.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0){
			this.sendMessage(player, "no snapshots defined for this region");
			return;
		}
		// Auto-index to the next frame.  Wrap to first frame on last entry.
		int index;
		int snapshotId = region.getSnapshotId();
		if (snapshotId == 0) {
			this.sendMessage(player, "Error: Invalid region! Current snapshot not found.");
			return;
		}else{
			RegionSnapshot currentSnapshot = db.find(RegionSnapshot.class).where().eq("id", snapshotId).findUnique();
			index = snapshots.indexOf(currentSnapshot) + 1;
			if (index == snapshots.size()) index = 0;
		}
		// Restore the frame.
		this.frame(player, region, snapshots, index + 1);
	}
	
	/*
	 * Restores a region to the specified snapshot frame.
	 * @param player - The player.
	 * @param name - The name of the region.
	 * @param index - The index of the frame to restore.
	 */
	protected void frame(Player player, String name, int index) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			this.sendMessage(player, "no such region");
			return;
		}
		// Get the requested snapshot.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0) {
			this.sendMessage(player, "no snapshots defined for this region");
			return;
		}
		// Restore the frame.
		this.frame(player, region, snapshots, index);
	}
	
	/*
	 * Restores a region to the specified snapshot frame.
	 * @param world - The world.
	 * @param player - The player.
	 * @param region - The controlled region.
	 * @param snapshots - A list of snapshots for the controlled region.
	 * @param frameNumber - The index of the frame to restore.
	 */
	private void frame(Player player, ControlledRegion region, List<RegionSnapshot> snapshots, int frameNumber) {
		// Check index bounds.
		if (frameNumber < 1 || frameNumber > snapshots.size()) {
			this.sendMessage(player, "invalid frame");
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
		// TODO: Add ControlledRegion.frameMessage.  Support {$frame} tag.
		if (player != null) player.sendMessage("frame " + Integer.toString(frameNumber) + " restored");
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
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void lock(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			this.sendMessage(player, "no such region");
			return;
		}
		// Check current region status.
		if (region.getLocked()) {
			this.sendMessage(player, "region is already locked");
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
			if (region.getSetInventory() == 2) {
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
			this.sendMessage(player, "region locked");
		} catch(Exception ex) {
			ex.printStackTrace();
			this.sendMessage(player, "unable to lock region! check server logs.");
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
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void unlock(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			this.sendMessage(player, "no such region");
			return;
		}
		// Check current region status.
		if (!region.getLocked()) {
			this.sendMessage(player, "region is not locked");
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
			if (region.getRestoreInventory() == 2) {
				savedItems.addAll(this.restoreInventory(regionPlayer, region));
			}
			// TODO: Give each player the contents of the rewards table.
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
			this.sendMessage(player, "region unlocked");
		}catch(Exception ex){
			ex.printStackTrace();
			this.sendMessage(player, "unable to unlock region! check server logs.");
			db.rollbackTransaction();
		}
	}
	
	// TODO: Add ControlledRegion.victory setting.
	// TODO: Add victory handling (on unlock region perform action).  1 = teleport to [victorySpawn].
	// TODO: And fail handling (on unlock region perform action).  1 = teleport to [failSpawn].
	// TODO: Add a PlayerRewards table.  Rewards table should be able to support inventory, armor & money.
	// TODO: Give each player the contents of the PlayerRewards table on unlock if victory.

	/*
	 * Creates a new snapshot for a region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void snap(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			this.sendMessage(player, "no such region");
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
			this.sendMessage(player, "snapshot created");
		}catch(Exception ex) {
			ex.printStackTrace();
			this.sendMessage(player, "unable to create snapshot! check server logs.");
			db.rollbackTransaction();
		}
	}
	
	/*
	 * Deletes the last snapshot for a region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void delsnap(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			this.sendMessage(player, "no such region");
			return;
		}
		// Get the region's snapshots in order.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0) {
			this.sendMessage(player, "no snapshots defined for this region");
			return;
		}
		// Delete the snapshot.
		this.delsnap(player, region, snapshots, snapshots.size());
	}
	
	/*
	 * Deletes the specified snapshot for a region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 * @param frameNumber - The index of the frame to restore.
	 */
	protected void delsnap(Player player, String name, int frameNumber) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			this.sendMessage(player, "no such region");
			return;
		}
		// Get the region's snapshots in order.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		// Delete the snapshot.
		this.delsnap(player, region, snapshots, frameNumber);
	}
	
	/*
	 * Deletes the specified snapshot for a region.
	 * @param player - The player.
	 * @param region - The controlled region.
	 * @param snapshots - A list of snapshots for the controlled region.
	 * @param frameNumber - The index of the frame to restore.
	 */
	private void delsnap(Player player, ControlledRegion region, List<RegionSnapshot> snapshots, int frameNumber) {
		// Check index bounds.
		if (frameNumber < 1 || frameNumber > snapshots.size()) {
			this.sendMessage(player, "invalid frame");
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
			this.sendMessage(player, "snapshot deleted");
		}catch(Exception ex){
			ex.printStackTrace();
			this.sendMessage(player, "unable to delete snapshot! check server logs.");
			db.rollbackTransaction();			
		}
	}
	
	// TODO: Add movesnap command to re-order snapshots.
	
	/*
	 * Creates a new controlled region.
	 * @param player - The player.
	 * @param name - The name of the new region.
	 */
	protected void create(Player player, String name) {
		// check for existing region
		EbeanServer db = this.getDatabase();
		if (db.find(ControlledRegion.class).where().eq("name", name).findRowCount() != 0){
			player.sendMessage("region already exists");
			return;
		}
		// get selected region
		com.sk89q.worldedit.regions.Region selection = this.getSelectedRegion(player);
		if (selection == null) {
			player.sendMessage("no region selected");
			return;
		}
		com.sk89q.worldedit.Vector max = selection.getMaximumPoint();
		com.sk89q.worldedit.Vector min = selection.getMinimumPoint();
		// create new controlled region
		ControlledRegion region = new ControlledRegion();
		region.setWorldName(player.getWorld().getName());
		region.setName(name);
		region.setMaxX((int)max.getX());
		region.setMaxY((int)max.getY());
		region.setMaxZ((int)max.getZ());
		region.setMinX((int)min.getX());
		region.setMinY((int)min.getY());
		region.setMinZ((int)min.getZ());
		region.setCurrentPlayers(this.getPlayersInRegion(region).size());
		// Save the new controlled region record.
		db.save(region);
		player.sendMessage("region created");
	}
	
	/*
	 * Updates a region field value.
	 * @param player - The player.
	 * @param name - The name of the region.
	 * @param setting - The name of the setting to update.
	 * @param value - The new setting value.
	 */
	protected void edit(Player player, String name, String setting, String value) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			this.sendMessage(player, "no such region");
			return;
		}
		// Update setting
		if (setting.equals("name")) {
			if (db.find(ControlledRegion.class).where().eq("name", value).findRowCount() != 0) {
				this.sendMessage(player, "region already exists");
				return;
			}
			region.setName(value);
		}else if(setting.equalsIgnoreCase("maxPlayers") || setting.equalsIgnoreCase("maxp")) {
			int maxPlayers;
			try{
				maxPlayers = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(player, setting + " must be an integer!");
				return;
			}
			if (maxPlayers < -1) {
				this.sendMessage(player, setting + " must be -1 or greater!");
				return;
			}
			region.setMaxPlayers(maxPlayers);
		}else if(setting.equalsIgnoreCase("minPlayers") || setting.equalsIgnoreCase("minp")) {
			int minPlayers;
			try{
				minPlayers = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(player, setting + " must be an integer!");
				return;
			}
			if (minPlayers < 0) {
				this.sendMessage(player, setting + " must be zero or greater!");
				return;
			}
			region.setMinPlayers(minPlayers);
		}else if(setting.equalsIgnoreCase("lockHealth") || setting.equalsIgnoreCase("lh")) {
			int lockHealth;
			try{
				lockHealth = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(player, setting + " must be an integer!");
				return;
			}
			if (lockHealth < 0 || lockHealth > 20) {
				this.sendMessage(player, setting + " must be in the range 0 to 20!");
				return;
			}
			region.setLockHealth(lockHealth);
		}else if(setting.equalsIgnoreCase("unlockHealth") || setting.equalsIgnoreCase("uh")) {
			int unlockHealth;
			try{
				unlockHealth = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(player, setting + " must be an integer!");
				return;
			}
			if (unlockHealth < 0 || unlockHealth > 20) {
				this.sendMessage(player, setting + " must be in the range 0 to 20!");
				return;
			}
			region.setUnlockHealth(unlockHealth);
		}else if(setting.equalsIgnoreCase("setInventory") || setting.equalsIgnoreCase("si")) {
			int setInventory;
			try{
				setInventory = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(player, setting + " must be an integer!");
				return;
			}
			if (setInventory < 0 || setInventory > 2) {
				this.sendMessage(player, setting + " must be in the range 0 to 2!");
				return;
			}
			region.setSetInventory(setInventory);
		}else if(setting.equalsIgnoreCase("restoreInventory") || setting.equalsIgnoreCase("ri")) {
			int restoreInventory;
			try{
				restoreInventory = Integer.parseInt(value);
			}catch(Exception ex){
				this.sendMessage(player, setting + " must be an integer!");
				return;
			}
			if (restoreInventory < 0 || restoreInventory > 2) {
				this.sendMessage(player, setting + " must be in the range 0 to 2!");
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
		}else{
			this.sendMessage(player, "no such setting: " + setting);
			return;
		}
		// Update the controlled section.
		db.update(region);
		this.sendMessage(player, "region updated");
	}
	
	/*
	 * Deletes a controlled region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void delete(Player player, String name) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			this.sendMessage(player, "no such region");
			return;
		}
		// Do not allow delete of locked region.
		if (region.getLocked()) {
			this.sendMessage(player, "the region is locked");
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
			this.sendMessage(player, "region deleted");			
		}catch(Exception ex){
			ex.printStackTrace();
			this.sendMessage(player, "unable to delete region! check server logs.");
			db.rollbackTransaction();
		}
	}
	
	/*
	 * Displays information about a controlled region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void info(Player player, String name) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			player.sendMessage("no such region");
			return;
		}
		// Display name and world.
		player.sendMessage("Region: " + region.getName() + " [" + region.getWorldName() + "]");
		// Display region bounds.
		String msg = "max(" + Double.toString(region.getMaxX()) + ", " + Double.toString(region.getMaxY()) + ", " + Double.toString(region.getMaxZ()) + ")";
		msg += ", min(" + Double.toString(region.getMinX()) + ", " + Double.toString(region.getMinY()) + ", " + Double.toString(region.getMinZ()) + ")";
		player.sendMessage(msg);
		// Display snapshot info.
		int snapshotId = region.getSnapshotId();
		if (snapshotId != 0){
			List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
			RegionSnapshot currentSnapshot = db.find(RegionSnapshot.class).where().eq("id", snapshotId).findUnique();
			msg = "snapshots: " + Integer.toString(snapshots.size());
			msg += ", current: " + Integer.toString(snapshots.indexOf(currentSnapshot) + 1);
		}else{
			msg = "no snapshots";
		}
		// Display region locked status.
		if (region.getLocked()){
			msg += ", region is locked";
		}else{
			msg += ", region is not locked";
		}
		player.sendMessage(msg);
		// Display max, min and current players info.
		if (region.getMaxPlayers() < 0){
			msg = "maxPlayers: no limit";
		}else{
			msg = "maxPlayers: " + region.getMaxPlayers();
		}
		msg += ", minPlayers: " + region.getMinPlayers();
		msg += ", currentPlayers: " + region.getCurrentPlayers();
		player.sendMessage(msg);
		// Display health settings.
		msg = "lockHealth: " + region.getLockHealth();
		msg += ", unlockHealth: " + region.getUnlockHealth();
		player.sendMessage(msg);
		// Display inventory settings.
		msg = "setInventory: " + region.getSetInventory();
		msg += ", restoreInventory: " + region.getRestoreInventory();
		player.sendMessage(msg);
	}
	
	/*
	 * Displays a controlled region's messages.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void messages(Player player, String name) {
		// Get the controlled region.
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null) {
			player.sendMessage("no such region");
			return;
		}
		// Display messages.
		player.sendMessage("maxMessage: " + region.getMaxMessage());
		player.sendMessage("minMessage: " + region.getMinMessage());
		player.sendMessage("minMessage1: " + region.getMinMessage1());
		player.sendMessage("enterMessage: " + region.getEnterMessage());
		player.sendMessage("leaveMessage: " + region.getLeaveMessage());
		player.sendMessage("noEnterMessage: " + region.getNoEnterMessage());
		player.sendMessage("noLeaveMessage: " + region.getNoLeaveMessage());
		player.sendMessage("setInventoryMessage: " + region.getSetInventoryMessage());
		player.sendMessage("restoreInventoryMessage: " + region.getRestoreInventoryMessage());		
	}
	
	/*
	 * Lists all controlled regions including locked status and number of players if locked.
	 * @param player - The player.
	 */
	protected void list(Player player) {
		StringBuilder message = new StringBuilder();
		message.append("Controlled Regions: ");
		EbeanServer db = this.getDatabase();
		List<ControlledRegion> regions = db.find(ControlledRegion.class).orderBy("name").findList();
		if (regions.size() == 0) message.append("no regions defined");
		for (ControlledRegion region : regions){
			message.append(region.getName());
			if (region != regions.get(regions.size()-1)) message.append(", ");
		}
		player.sendMessage(message.toString());
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
     * Ensures that the database for this plugin is installed and available.
     */
    private void setupDatabase() {
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
			System.out.println(this.name + " installing database");
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
	 * @param player - The player.
	 * @param message - The message.
	 */
	private void sendMessage(Player player, String message){
		if (message.length() == 0) return;
		if (player == null){
			this.getServer().broadcastMessage(message);
		}else{
			player.sendMessage(message);
		}
	}
	
	/*
	 * Broadcasts a message to a list of players.
	 * @param players - The list of players.
	 * @param message - The message.
	 */
	private void sendMessage(List<Player> players, String message) {
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
	private boolean regionContainsPlayer(ControlledRegion region, Player player) {
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
			System.out.print("Error - Invalid item category: " + category);
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