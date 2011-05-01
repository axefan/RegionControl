package us.axefan.plugin;

import java.util.List;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avaje.ebean.EbeanServer;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.regions.Region;

public class CommandExecutor implements org.bukkit.command.CommandExecutor {
	
	private RegionControl plugin;
	
	public CommandExecutor(RegionControl instance){
		this.plugin = instance;
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!label.toLowerCase().equals("rc")) return false;
		if (args.length < 1)  return false;
		Player player = (Player) sender;
		String action = args[0].toLowerCase();
		if (action.equals("list")) {
			this.list(player);
			return true;
		}else if (action.equals("info")) {
			if (args.length < 2) {
				player.sendMessage("/rc info <name>");
				return true;
			}
			this.info(player, args[1].trim());
			return true;
		}else if (action.equals("create")) {
			if (args.length < 2) {
				player.sendMessage("/rc create <name>");
				return true;
			}
			this.create(player, args[1].trim());
			return true;
		}else if (action.equals("update")) {
			if (args.length < 4) {
				player.sendMessage("/rc update <name> <field> <value>");
				return true;
			}
			this.update(player, args[1].trim(), args[2].trim(), args[3].trim());
			return true;
		}else if (action.equals("delete")) {
			if (args.length < 2) {
				player.sendMessage("/rc delete <name>");
				return true;
			}
			this.delete(player, args[1].trim());
			return true;
		}else if (action.equals("snap")) {
			if (args.length < 2) {
				player.sendMessage("/rc snap <name>");
				return true;
			}
			this.snap(player, args[1].trim());
			return true;
		}else if (action.equals("frame")) {
			if (args.length < 2) {
				player.sendMessage("/rc frame <name> [frame]");
				return true;
			}
			if (args.length == 2) {
				this.frame(player, args[1].trim());
			}else{
				this.frame(player, args[1].trim(), Integer.parseInt(args[2].trim()));
			}
			return true;
		}else return false;
	}
	
	/*
	 * Restores a region to the next snapshot frame.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	private void frame(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.plugin.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			player.sendMessage("no such region");
			return;
		}
		// Get the current snapshot.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0){
			player.sendMessage("no snapshots defined for this region");
			return;
		}
		// Auto-index to the next frame.  Wrap to first frame on last entry.
		int index;
		int snapshotId = region.getSnapshotId();
		if (snapshotId == 0){
			player.sendMessage("Error: Invalid region! Current snapshot not found.");
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
	private void frame(Player player, String name, int index) {
		// Get the controlled region
		EbeanServer db = this.plugin.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			player.sendMessage("no such region");
			return;
		}
		// Get the requested snapshot.
		List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).where().eq("regionId", region.getId()).orderBy("id").findList();
		if (snapshots.size() == 0){
			player.sendMessage("no snapshots defined for this region");
			return;
		}
		// Restore the frame.
		this.frame(player, region, snapshots, index);
		
//		// Check index bounds.
//		index--;
//		if (index < 0 || index >= snapshots.size()) {
//			player.sendMessage("invalid frame");
//			return;
//		}
//		RegionSnapshot snapshot = snapshots.get(index);
//		// Get the saved blocks for this snapshot.
//		World world = player.getWorld();
//		List<SavedBlock> savedBlocks = db.find(SavedBlock.class)
//			.where().eq("snapshotId", snapshot.getId())
//			.orderBy("id").findList();
//		int saveBlockIndex = 0;
//		for (int x = region.getMinX(); x <= region.getMaxX(); x++){
//			for (int y = region.getMinY(); y <= region.getMaxY(); y++){
//				for (int z = region.getMinZ(); z <= region.getMaxZ(); z++){
//					Block block = world.getBlockAt(x, y, z);
//					SavedBlock savedBlock = null;
//					try{
//						savedBlock = savedBlocks.get(saveBlockIndex++);
//					}catch(Exception ex){
//						System.out.print("Bad index: " + saveBlockIndex--);
//					}
//					if (savedBlock != null){
//						if (block.getTypeId() != savedBlock.getTypeId() || block.getData() != savedBlock.getData()){
//							block.setTypeIdAndData(savedBlock.getTypeId(), savedBlock.getData(), false);
//						}						
//					}
//				}
//			}
//		}
//		// Set controlled region's current snapshot.
//		region.setSnapshotId(snapshot.getId());
//		db.update(region);
//		// Notify player.
//		player.sendMessage("frame restored");
	}
	
	/*
	 * Restores a region to the specified snapshot frame.
	 * @param player - The player.
	 * @param region - The controlled region.
	 * @param snapshots - A list of snapshots for the controlled region.
	 * @param index - The index of the frame to restore.
	 */
	private void frame(Player player, ControlledRegion region, List<RegionSnapshot> snapshots, int index) {
		// Check index bounds.
		if (index < 1 || index > snapshots.size()) {
			player.sendMessage("invalid frame");
			return;
		}
		RegionSnapshot snapshot = snapshots.get(index-1);
		// Get the saved blocks for this snapshot.
		World world = player.getWorld();
		EbeanServer db = this.plugin.getDatabase();
		List<SavedBlock> savedBlocks = db.find(SavedBlock.class)
			.where().eq("snapshotId", snapshot.getId())
			.orderBy("id").findList();
		int saveBlockIndex = 0;
		for (int x = region.getMinX(); x <= region.getMaxX(); x++){
			for (int y = region.getMinY(); y <= region.getMaxY(); y++){
				for (int z = region.getMinZ(); z <= region.getMaxZ(); z++){
					Block block = world.getBlockAt(x, y, z);
					SavedBlock savedBlock = null;
					try{
						savedBlock = savedBlocks.get(saveBlockIndex++);
					}catch(Exception ex){
						System.out.print("Bad index: " + saveBlockIndex--);
					}
					if (savedBlock != null){
						if (block.getTypeId() != savedBlock.getTypeId() || block.getData() != savedBlock.getData()){
							block.setTypeIdAndData(savedBlock.getTypeId(), savedBlock.getData(), false);
						}						
					}
				}
			}
		}
		// Set controlled region's current snapshot.
		region.setSnapshotId(snapshot.getId());
		db.update(region);
		// Notify player.
		player.sendMessage("frame " + Integer.toString(index) + "." + snapshot.getId() + " restored");		
	}
	
	/*
	 * Creates a new snapshot for a region.
	 * All blocks will be saved in their current state.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	private void snap(Player player, String name) {
		// Get the controlled region
		EbeanServer db = this.plugin.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			player.sendMessage("no such region");
			return;
		}
		// Create a new snapshot record.
		RegionSnapshot snapshot = new RegionSnapshot();
		int regionId = region.getId();
		snapshot.setRegionId(regionId);
		db.save(snapshot);
		// Create new saved block records.
		World world = player.getWorld();
		int snapshotId = snapshot.getId();
		for (int x = region.getMinX(); x <= region.getMaxX(); x++){
			for (int y = region.getMinY(); y <= region.getMaxY(); y++){
				for (int z = region.getMinZ(); z <= region.getMaxZ(); z++){
					Block block = world.getBlockAt(x, y, z);
					SavedBlock savedBlock = new SavedBlock();
					savedBlock.setRegionId(regionId);
					savedBlock.setSnapshotId(snapshotId);
					savedBlock.setTypeId(block.getTypeId());
					savedBlock.setData(block.getData());
					db.save(savedBlock);
				}
			}
		}
		// Set current snapshot in controlled region.
		region.setSnapshotId(snapshotId);
		db.update(region);
		// Notify player.
		player.sendMessage("snapshot created");
	}
	
	/*
	 * Creates a new controlled region.
	 * @param player - The player.
	 * @param name - The name of the new region.
	 */
	private void create(Player player, String name) {
		// check for existing region
		EbeanServer db = this.plugin.getDatabase();
		if (db.find(ControlledRegion.class).where().eq("name", name).findRowCount() != 0){
			player.sendMessage("region already exists");
			return;
		}
		// get selected region
		Region selection = this.getSelectedRegion(player);
		if (selection == null) {
			player.sendMessage("no region selected");
			return;
		}
		Vector max = selection.getMaximumPoint();
		Vector min = selection.getMinimumPoint();
		// create new controlled region
		ControlledRegion region = new ControlledRegion();
		region.setName(name);
		region.setMaxX((int)max.getX());
		region.setMaxY((int)max.getY());
		region.setMaxZ((int)max.getZ());
		region.setMinX((int)min.getX());
		region.setMinY((int)min.getY());
		region.setMinZ((int)min.getZ());
		region.setSnapshotId(0);
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
	private void update(Player player, String name, String setting, String value){
		// Get the controlled region.
		EbeanServer db = this.plugin.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			player.sendMessage("no such region");
			return;
		}
		// Update setting
		if (setting.equals("name")){
			if (db.find(ControlledRegion.class).where().eq("name", value).findRowCount() != 0){
				player.sendMessage("region already exists");
				return;
			}
			region.setName(value);
		}else{
			player.sendMessage("no such setting: " + setting);
			return;
		}
		// Update the controlled section.
		db.update(region);
		player.sendMessage("region updated");
	}
	
	/*
	 * Deletes a controlled region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	private void delete(Player player, String name) {
		// Get the controlled region.
		EbeanServer db = this.plugin.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			player.sendMessage("no such region");
			return;
		}
		// Delete the controlled region.
		db.delete(region);
		player.sendMessage("region deleted");
	}
	
	/*
	 * Displays information about a region.
	 * @param player - The player.
	 * @param name - The name of the region.
	 */
	private void info(Player player, String name) {
		// Get the controlled region.
		EbeanServer db = this.plugin.getDatabase();
		ControlledRegion region = db.find(ControlledRegion.class).where().eq("name", name).findUnique();
		if (region == null){
			player.sendMessage("no such region");
			return;
		}
		// Display region bounds.
		player.sendMessage("max = (" + Double.toString(region.getMaxX()) + ", " + Double.toString(region.getMaxY()) + ", " + Double.toString(region.getMaxZ()) + ")");
		player.sendMessage("min = (" + Double.toString(region.getMinX()) + ", " + Double.toString(region.getMinY()) + ", " + Double.toString(region.getMinZ()) + ")");
		// Display snapshot info.
		int snapshotId = region.getSnapshotId();
		if (snapshotId != 0){
			List<RegionSnapshot> snapshots = db.find(RegionSnapshot.class).orderBy("id").findList();
			RegionSnapshot currentSnapshot = db.find(RegionSnapshot.class).where().eq("id", snapshotId).findUnique();
			player.sendMessage("snapshots = " + Integer.toString(snapshots.size()));
			player.sendMessage("current = " + Integer.toString(snapshots.indexOf(currentSnapshot) + 1));
		}
	}
	
	/*
	 * Lists regions.
	 * @param player - The player.
	 */
	private void list(Player player) {
		StringBuilder message = new StringBuilder();
		message.append("Controlled Regions: ");
		EbeanServer db = this.plugin.getDatabase();
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
	private Region getSelectedRegion(Player player){
		WorldEditAPI api = this.plugin.getWorldEditApi();
		LocalSession session = api.getSession(player);
		Region region = null;
		try {
			region = session.getSelection(session.getSelectionWorld());
		} catch (IncompleteRegionException e) {}
		return region;
	}

}