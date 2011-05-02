package us.axefan.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

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
    private final PlayerListener playerListener = new PlayerListener(this);
    private final EntityListener entityListener = new EntityListener(this);
    private final BlockListener blockListener = new BlockListener(this);
	
	public void onEnable() {
        PluginDescriptionFile desc = this.getDescription();
        System.out.println(desc.getName() + " starting...");
		this.getCommand("rc").setExecutor(new CommandExecutor (this));
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_KICK, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.REDSTONE_CHANGE, blockListener, Event.Priority.Normal, this);
		this.worldEditPlugin = (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
		if (this.worldEditPlugin != null){
			this.worldEditApi = new WorldEditAPI(this.worldEditPlugin);
			if (this.worldEditApi != null) System.out.println(desc.getName() + " is hooked into WorldEdit");
		}
		this.setupDatabase();
        System.out.println(desc.getName() + " " + desc.getVersion() + " enabled");
	}
	
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
			System.out.println(this.getDescription().getName() + " installing database");
			installDDL();
		}
	}
    
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(ControlledRegion.class);
		list.add(RegionSnapshot.class);
		list.add(RegionLock.class);
		list.add(RegionItem.class);
		list.add(SavedBlock.class);
		list.add(SavedItem.class);
		return list;
	}
	
	public void onDisable() {
	}
	
	protected WorldEditPlugin getWorldEditPlugin() {
		return this.worldEditPlugin;
	}
	
	protected WorldEditAPI getWorldEditApi() {
		return this.worldEditApi;
	}
	
	/*
	 * Sends a message to a player.
	 */
	private void sendMessage(Player player, String message){
		if (player == null){
			this.getServer().broadcastMessage(message);
		}else{
			player.sendMessage(message);
		}
	}
	
	/*
	 * Restores a region to the next snapshot frame.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void frame(World world, Player player, String name) {
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
		if (snapshotId == 0){
			this.sendMessage(player, "Error: Invalid region! Current snapshot not found.");
			return;
		}else{
			RegionSnapshot currentSnapshot = db.find(RegionSnapshot.class).where().eq("id", snapshotId).findUnique();
			index = snapshots.indexOf(currentSnapshot) + 1;
			if (index == snapshots.size()) index = 0;
		}
		// Restore the frame.
		this.frame(world, player, region, snapshots, index + 1);
	}
	
	/*
	 * Restores a region to the specified snapshot frame.
	 * @param player - The player.
	 * @param name - The name of the region.
	 * @param index - The index of the frame to restore.
	 */
	protected void frame(World world, Player player, String name, int index) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			this.sendMessage(player, "no such region");
			return;
		}
		// Get the requested snapshot.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0){
			this.sendMessage(player, "no snapshots defined for this region");
			return;
		}
		// Restore the frame.
		this.frame(world, player, region, snapshots, index);
	}
	
	/*
	 * Restores a region to the specified snapshot frame.
	 * @param world - The world.
	 * @param player - The player.
	 * @param region - The controlled region.
	 * @param snapshots - A list of snapshots for the controlled region.
	 * @param index - The index of the frame to restore.
	 */
	private void frame(World world, Player player, ControlledRegion region, List<RegionSnapshot> snapshots, int index) {
		// Check index bounds.
		if (index < 1 || index > snapshots.size()) {
			this.sendMessage(player, "invalid frame");
			return;
		}
		RegionSnapshot snapshot = snapshots.get(index-1);
		// Get the saved blocks for this snapshot.
		EbeanServer db = this.getDatabase();
		List<SavedBlock> savedBlocks = db.find(SavedBlock.class)
			.where().eq("snapshotId", snapshot.getId())
			.orderBy("id").findList();
		int saveBlockIndex = 0;
		for (int x = region.getMinX(); x <= region.getMaxX(); x++){
			for (int y = region.getMinY(); y <= region.getMaxY(); y++){
				for (int z = region.getMinZ(); z <= region.getMaxZ(); z++){
					Block block = world.getBlockAt(x, y, z);
					SavedBlock savedBlock = savedBlocks.get(saveBlockIndex++);
						if (block.getTypeId() != savedBlock.getTypeId() || block.getData() != savedBlock.getData()){
							block.setTypeIdAndData(savedBlock.getTypeId(), savedBlock.getData(), false);
						}
				}
			}
		}
		// Set controlled region's current snapshot.
		region.setSnapshotId(snapshot.getId());
		db.update(region);
		// Notify player.
		if (player != null) player.sendMessage("frame " + Integer.toString(index) + " restored");
	}
	
	/*
	 * Locks a region.
	 * 
	 * When a region is locked...
	 *  
	 *  The player's inventory will be saved.
	 *  
	 *  The players that are inside the region at the time that the lock is placed
	 *  are not allowed to leave the region.  A player will receive the regions's 
	 *  [leaveWarning] when attempting to leave a locked region.
	 *  
	 *  No additional players will be allowed to enter the region.  A player will 
	 *  receive the regions's [enterWarning] when attempting to enter a locked region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void lock(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			this.sendMessage(player, "no such region");
			return;
		}
		// Check current region status.
		if (db.find(RegionLock.class).where().eq("regionId", region.getId()).findRowCount() > 0){
			player.sendMessage("region is already locked");
			return;
		}
		try{
			// Begin transaction.
			db.beginTransaction();
			// Create region lock.
			RegionLock regionLock = new RegionLock();
			regionLock.setRegionId(region.getId());
			db.save(regionLock);
			
			
			
			// testing with current player.  Need to extend to all players in region.
			// Save player inventory
			PlayerInventory inventory = player.getInventory();
			for (ItemStack item : inventory.getContents()){
				SavedItem savedItem = new SavedItem();
				savedItem.setPlayerName(player.getName());
				if (item == null) {
					savedItem.setTypeId(0);
					savedItem.setDurability((short) 0);
					savedItem.setAmount(0);
					savedItem.setData((byte) 0);
				}else{
					savedItem.setTypeId(item.getTypeId());
					savedItem.setDurability(item.getDurability());
					savedItem.setAmount(item.getAmount());
					MaterialData data = item.getData();
					if (data == null){
						savedItem.setData((byte) 0);
					}else{
						savedItem.setData(data.getData());
					}
				}
				db.save(savedItem);
			}
			inventory.clear();
			player.setHealth(20);
			
			
			
			// Commit transaction.
			db.commitTransaction();
			player.sendMessage("region locked");
		}catch(Exception ex){
			ex.printStackTrace();
			player.sendMessage("unable to lock region! check server logs.");
			db.rollbackTransaction();
		}
	}
	
	/*
	 * Unlocks a region.
	 * 
	 * When a region is unlocked...
	 *  
	 *  The players that were inside the region at the time that the lock was placed
	 *  will have their inventory restored.
	 *  
	 *  Any player may leave the region.
	 *  
	 *  Any player may enter the region up to [maxPlayers].
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	protected void unlock(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			this.sendMessage(player, "no such region");
			return;
		}
		// Check current region status.
		List<RegionLock> regionLocks = db.find(RegionLock.class).where().eq("regionId", region.getId()).findList();
		if (regionLocks.size() == 0){
			player.sendMessage("region is not locked");
			return;
		}
		try{
			// Begin transaction.
			db.beginTransaction();
			
			
			// Restore player inventory
			PlayerInventory inventory = player.getInventory();
			inventory.clear();
			List<SavedItem> savedItems = db.find(SavedItem.class)
				.where().eq("playerName", player.getName()).orderBy("id").findList();
			for (SavedItem savedItem : savedItems) {
				ItemStack item = null;
				if (savedItem.getTypeId() != 0){
					item = new ItemStack(savedItem.getTypeId());
					item.setDurability(savedItem.getDurability());
					item.setAmount(savedItem.getAmount());
					if (savedItem.getData() == 0){
						item.setData(null);
					}else{
						item.setData(new MaterialData(savedItem.getTypeId(), savedItem.getData()));
					}
					inventory.addItem(item);
				}
			}
			// Delete the player's inventory records.
			db.delete(savedItems);
			
			
			
			// Remove region lock.
			db.delete(regionLocks);
			// Commit transaction.
			db.commitTransaction();
			player.sendMessage("region unlocked");			
		}catch(Exception ex){
			ex.printStackTrace();
			player.sendMessage("unable to unlock region! check server logs.");
			db.rollbackTransaction();
		}
	}	
}