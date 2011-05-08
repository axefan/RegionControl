package us.axefan.plugin;

import org.bukkit.ChatColor;

public class Messages {
	
	// Error messages
	public static final String RegionExistsError = ChatColor.RED + "A region with that name already exists!";
	public static final String NoSelectionError = ChatColor.RED + "No region selected!";
	public static final String NoRegionError = ChatColor.RED + "No such region!";
	public static final String DeleteRegionError = ChatColor.RED + "Unable to delete region!";
	public static final String LockRegionError = ChatColor.RED + "Unable to lock region!";
	public static final String UnlockRegionError = ChatColor.RED + "Unable to unlock region!";
	public static final String RegionLockedError = ChatColor.RED + "The region is locked.";
	public static final String RegionAlreadyLockedError = ChatColor.RED + "The region is already locked!";
	public static final String RegionNotLockedError = ChatColor.RED + "The region is not locked!";	
	public static final String InvalidFrameError = ChatColor.RED + "Invalid frame number!";
	public static final String PlayerCommandError = ChatColor.RED + "Error! Command must be issued by a player.";
	public static final String CreateSnapshotError = ChatColor.RED + "Unable to create snapshot!";
	public static final String NoSnapshotsError = ChatColor.RED + "No snapshots defined for this region!";
	public static final String DeleteSnapshotsError = ChatColor.RED + "Unable to delete snapshot!";
	public static final String SnapshotNotFoundError = ChatColor.RED + "Error! Current snapshot not found.";
	public static final String CheckServerLogs = ChatColor.RED + "Check the server log.";
	public static final String SpawnNotSetError = ChatColor.RED + "Spawn not set!";
	public static final String SpawnLocationError = ChatColor.RED + "Spawn location must be outside the region.";
	public static final String InvalidItemCategoryError = ChatColor.RED + "Error! Invalid item category: {$category}.";
	public static final String IntegerError = ChatColor.RED + "{$value} must be an integer!";
	public static final String NegativeOneOrGreaterError = ChatColor.RED + "{$value} must be -1 or greater!";
	public static final String ZeroOrGreaterError = ChatColor.RED + "{$value} must be zero or greater!";
	public static final String ValueRangeError = ChatColor.RED + "{$value} must be in the range {$range}!";
	public static final String NoSuchSetting = ChatColor.RED + "No such setting: {$setting}";
	public static final String Default = ChatColor.RED + "";
	
	// Messages
	public static final String InstallingDatabasesMessage = ChatColor.DARK_GRAY + "{$name}: Installing databases on first use...";
	public static final String RegionCreatedMessage = ChatColor.DARK_GREEN + "Region created.";
	public static final String RegionLockedMessage = ChatColor.DARK_GREEN + "The region is now locked.";
	public static final String RegionUnlockedMessage = ChatColor.DARK_GREEN + "The region is now unlocked.";
	public static final String RegionDeletedMessage = ChatColor.DARK_GREEN + "Region deleted.";
	public static final String RegionUpdatedMessage = ChatColor.DARK_GREEN + "Region updated.";	
	public static final String SnapshotCreatedMessage = ChatColor.DARK_GREEN + "Snapshot created.";
	public static final String SnapshotDeletedMessage = ChatColor.DARK_GREEN + "Snapshot deleted.";
	public static final String FrameRestoredMessage = ChatColor.DARK_GREEN + "frame {$frame} restored";
	public static final String ValueSetMessage = ChatColor.DARK_GREEN + "{$setting} set to {$value}.";
}