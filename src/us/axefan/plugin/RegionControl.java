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
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.EbeanServer;
import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class RegionControl extends JavaPlugin {
	
	private WorldEditPlugin worldEditPlugin = null;
	private WorldEditAPI worldEditApi = null;
    private final BlockListener blockListener = new BlockListener(this);
	
	public void onEnable() {
        PluginDescriptionFile desc = this.getDescription();
        System.out.println(desc.getName() + " starting...");
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.REDSTONE_CHANGE, blockListener, Event.Priority.Normal, this);
		this.getCommand("rc").setExecutor(new CommandExecutor (this));
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
		list.add(SavedBlock.class);
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
//					try{
//						savedBlock = savedBlocks.get(saveBlockIndex++);
//					}catch(Exception ex){
//						System.out.print("Bad index: " + saveBlockIndex--);
//					}
//					if (savedBlock != null){
						if (block.getTypeId() != savedBlock.getTypeId() || block.getData() != savedBlock.getData()){
							block.setTypeIdAndData(savedBlock.getTypeId(), savedBlock.getData(), false);
						}
//					}
				}
			}
		}
		// Set controlled region's current snapshot.
		region.setSnapshotId(snapshot.getId());
		db.update(region);
		// Notify player.
		if (player != null) player.sendMessage("frame " + Integer.toString(index) + " restored");
	}
	
}