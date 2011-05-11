package us.axefan.plugin;

import org.bukkit.ChatColor;

public class Messages {
	
	// Error messages
	public static final String RegionExistsError = ChatColor.RED + "A region with that name already exists!";
	public static final String NoSelectionError = ChatColor.RED + "No region selected!";
	public static final String NoRegionError = ChatColor.RED + "No such region: {$region}!";
	public static final String DeleteRegionError = ChatColor.RED + "Unable to delete region!";
	public static final String LockRegionError = ChatColor.RED + "Unable to lock region!";
	public static final String UnlockRegionError = ChatColor.RED + "Unable to unlock region!";
	public static final String RegionLockedError = ChatColor.RED + "The region is locked.";
	public static final String RegionAlreadyLockedError = ChatColor.RED + "The region is already locked!";
	public static final String RegionNotLockedError = ChatColor.RED + "The region is not locked!";	
	public static final String InvalidFrameError = ChatColor.RED + "Invalid frame number!";
	public static final String PlayerCommandError = ChatColor.RED + "Error! The '{$command}' command must be issued by a player."; // TODO: replace("{$command}", 
	public static final String CreateSnapshotError = ChatColor.RED + "Unable to create snapshot!";
	public static final String NoSnapshotsError = ChatColor.RED + "No snapshots defined for this region!";
	public static final String DeleteSnapshotsError = ChatColor.RED + "Unable to delete snapshot!";
	public static final String SnapshotNotFoundError = ChatColor.RED + "Error! Current snapshot not found.";
	public static final String CheckServerLogs = ChatColor.RED + "Check the server log.";
	public static final String SpawnNotSetError = ChatColor.RED + "Spawn not set!";
	public static final String SpawnLocationError = ChatColor.RED + "Spawn location must be outside the region!";
	public static final String InvalidItemCategoryError = ChatColor.RED + "Invalid item category: {$category}!";	
	public static final String IntegerError = ChatColor.RED + "Invalid value: {$value}! The '{$setting}' setting must be an integer";
	public static final String BooleanError = ChatColor.RED + "Invalid value: {$value}! The '{$setting}' setting must be true or false!";
	public static final String NegativeOneOrGreaterError = ChatColor.RED + "Invalid value: {$value}! The '{$setting}' setting must be -1 or greater!";
	public static final String ZeroOrGreaterError = ChatColor.RED + "Invalid value: {$value}! The '{$setting}' setting must be zero or greater!";
	public static final String ValueRangeError = ChatColor.RED + "Invalid value: {$value}! The '{$setting}' setting must be in the range {$range}!";
	public static final String NoSuchSetting = ChatColor.RED + "No such setting: {$setting}!";
	public static final String NoRegionsDefined = ChatColor.RED + "No regions defined!";
	public static final String NoTemplateError = ChatColor.RED + "No such template: {$template}!";
	public static final String SaveTemplateError = ChatColor.RED + "Unable to save template!";
	public static final String SaveConfigurationError = ChatColor.RED + "Unable to save configuration!";
	public static final String RegionNameError = ChatColor.RED + "Region name must be only one word! Ex: 'MyRegion'.";
	public static final String TemplateNameError = ChatColor.RED + "Template name must be only one word! Ex: 'MyTemplate'.";
	
	// General Messages
	public static final String InstallingDatabasesMessage = ChatColor.DARK_GRAY + "{$name}: Installing database on first use...";
	
	// Successful Command Messages
	public static final String RegionCreatedMessage = ChatColor.DARK_GREEN + "Region '{$region}' created.";
	public static final String RegionLockedMessage = ChatColor.DARK_GREEN + "Region '{$region}' is now locked.";
	public static final String RegionUnlockedMessage = ChatColor.DARK_GREEN + "Region '{$region}' is now unlocked.";
	public static final String RegionDeletedMessage = ChatColor.DARK_GREEN + "Region '{$region}' deleted.";
	public static final String RegionUpdatedMessage = ChatColor.DARK_GREEN + "Region '{$region}' updated.";
	public static final String SnapshotCreatedMessage = ChatColor.DARK_GREEN + "Snapshot created.";
	public static final String SnapshotDeletedMessage = ChatColor.DARK_GREEN + "Snapshot deleted.";
	public static final String FrameRestoredMessage = ChatColor.DARK_GREEN + "Frame {$frame} restored";
	public static final String ValueSetMessage = ChatColor.DARK_GREEN + "{$setting} set to {$value}.";
	public static final String LoadedTemplateMessage = ChatColor.DARK_GREEN + "Loaded region '{$region}' settings from template '{$template}'.";
	public static final String SavedTemplateMessage = ChatColor.DARK_GREEN + "Saved region '{$region}' settings to template '{$template}'.";
	
	// Default region messages
	public static final String DefaultMaxMessage = "No more players allowed.";
	public static final String DefaultMinMessage = "Waiting for {$count} more players.";
	public static final String DefaultMinMessage1 = "Waiting for 1 more player.";
	public static final String DefaultNoEnterMessage = "You cannot enter this area!";
	public static final String DefaultNoLeaveMessage = "You cannot leave this area!";
	
}