package us.axefan.plugin;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name="ControlledRegions")
public class ControlledRegion {
	
    @Id
    private int id;
    
    @NotNull
    private String name;
    
    @NotNull
    private String worldName;
    
    @NotNull
    private int maxX;
    
    @NotNull
    private int maxY;
    
    @NotNull
    private int maxZ;
    
    @NotNull
    private int minX;
    
    @NotNull
    private int minY;
    
    @NotNull
    private int minZ;
    
    @NotNull
    private int snapshotId = 0;
    
    @NotNull
    private Boolean locked = false;
    
    @NotNull
    private int maxPlayers = -1; // -1 = no limit, alias = maxp
    
    @NotNull
    private int minPlayers = 0; // alias = minp
    
    @NotNull
    private int currentPlayers;
    
    @NotNull
    private int lockHealth = 0; // 0 = no change, range = 0 - 20, alias = lh
    
    @NotNull
    private int unlockHealth = 0; // 0 = no change, range = 0 - 20, alias = uh
    
    @NotNull
    private int setInventory = RegionControl.SetInventoryNever; // 0 = no change, 1 = on enter, 2 = on lock, alias = si
    
    @NotNull
    private int restoreInventory = RegionControl.RestoreInventoryNever; // 0 = no change, 1 = on leave, 2 = on unlock, alias = ri
    
    @NotNull
    private Boolean lockOnMaxPlayers = false;
    
    @NotNull
    private String maxMessage = "no more players allowed"; // alias = maxm
    
    @NotNull
    private String minMessage = "waiting for {$count} more players"; // alias = minm
    
    @NotNull
    private String minMessage1 = "waiting for 1 more player"; // alias = min1
    
    @NotNull
    private String enterMessage = ""; // alias = em
    
    @NotNull
    private String leaveMessage = ""; // alias = lm
    
    @NotNull
    private String noEnterMessage = "You cannot enter this area!"; // alias = nem
    
    @NotNull
    private String noLeaveMessage = "You cannot leave this area!"; // alias = nlm
    
    @NotNull
    private String joinMoveMessage = ""; // alias = jmm
    
    @NotNull
    private String setInventoryMessage = ""; // alias = sim
    
    @NotNull
    private String restoreInventoryMessage = ""; // alias = rim
    
    @NotNull
    private int selectionMode = 1; // 1 = WorldEdit
    
    @NotNull
    private Boolean spawnSet = false;
    
    @NotNull
    private int spawnX = 0;
    
    @NotNull
    private int spawnY = 0;
    
    @NotNull
    private int spawnZ = 0;
    
    // TODO: Add ControlledRegion.failLocation (for kick from controlled region - death on ControlledRegion.maxTries, on reload).
    // TODO: Add ControlledRegion.retryLocation point (for reset after death when less than ControlledRegion.maxTries.
    // TODO: Add ControlledRegion.victoryLocation
    // TODO: Add ControlledRegion.lives & LockedPlayer.lives (number of deaths before kick to ControlledRegion.failSpawn).
    // TODO: Add ControlledRegion.lockOnMaxPlayers
    // TODO: Add defaultFrame setting.  This frame will be loaded when the plugin loads. 0 = no change.
    
	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setMaxX(int maxX) {
		this.maxX = maxX;
	}

	public int getMaxX() {
		return maxX;
	}

	public void setMaxY(int maxY) {
		this.maxY = maxY;
	}

	public int getMaxY() {
		return maxY;
	}

	public void setMaxZ(int maxZ) {
		this.maxZ = maxZ;
	}

	public int getMaxZ() {
		return maxZ;
	}

	public void setMinX(int minX) {
		this.minX = minX;
	}

	public int getMinX() {
		return minX;
	}

	public void setMinY(int minY) {
		this.minY = minY;
	}

	public int getMinY() {
		return minY;
	}

	public void setMinZ(int minZ) {
		this.minZ = minZ;
	}

	public int getMinZ() {
		return minZ;
	}

	public void setSnapshotId(int snapshotId) {
		this.snapshotId = snapshotId;
	}

	public int getSnapshotId() {
		return snapshotId;
	}

	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}

	public int getMinPlayers() {
		return minPlayers;
	}

	public void setCurrentPlayers(int currentPlayers) {
		this.currentPlayers = currentPlayers;
	}

	public int getCurrentPlayers() {
		return currentPlayers;
	}

	public void setMaxMessage(String maxMessage) {
		this.maxMessage = maxMessage;
	}

	public String getMaxMessage() {
		return maxMessage;
	}

	public void setMinMessage(String minMessage) {
		this.minMessage = minMessage;
	}

	public String getMinMessage() {
		return minMessage;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	public String getWorldName() {
		return worldName;
	}

	public void setEnterMessage(String enterMessage) {
		this.enterMessage = enterMessage;
	}

	public String getEnterMessage() {
		return enterMessage;
	}

	public void setLeaveMessage(String leaveMessage) {
		this.leaveMessage = leaveMessage;
	}

	public String getLeaveMessage() {
		return leaveMessage;
	}

	public void setNoEnterMessage(String noEnterMessage) {
		this.noEnterMessage = noEnterMessage;
	}

	public String getNoEnterMessage() {
		return noEnterMessage;
	}

	public void setNoLeaveMessage(String noLeaveMessage) {
		this.noLeaveMessage = noLeaveMessage;
	}

	public String getNoLeaveMessage() {
		return noLeaveMessage;
	}

	public void setLockHealth(int lockHealth) {
		this.lockHealth = lockHealth;
	}

	public int getLockHealth() {
		return lockHealth;
	}

	public void setUnlockHealth(int unlockHealth) {
		this.unlockHealth = unlockHealth;
	}

	public int getUnlockHealth() {
		return unlockHealth;
	}

	public void setSetInventory(int setInventory) {
		this.setInventory = setInventory;
	}

	public int getSetInventory() {
		return setInventory;
	}

	public void setRestoreInventory(int restoreInventory) {
		this.restoreInventory = restoreInventory;
	}

	public int getRestoreInventory() {
		return restoreInventory;
	}

	public void setSetInventoryMessage(String setInventoryMessage) {
		this.setInventoryMessage = setInventoryMessage;
	}

	public String getSetInventoryMessage() {
		return setInventoryMessage;
	}

	public void setRestoreInventoryMessage(String restoreInventoryMessage) {
		this.restoreInventoryMessage = restoreInventoryMessage;
	}

	public String getRestoreInventoryMessage() {
		return restoreInventoryMessage;
	}

	public void setSelectionMode(int selectionMode) {
		this.selectionMode = selectionMode;
	}

	public int getSelectionMode() {
		return selectionMode;
	}

	public void setMinMessage1(String minMessage1) {
		this.minMessage1 = minMessage1;
	}

	public String getMinMessage1() {
		return minMessage1;
	}

	public void setSpawnX(int spawnX) {
		this.spawnX = spawnX;
	}

	public int getSpawnX() {
		return spawnX;
	}

	public void setSpawnY(int spawnY) {
		this.spawnY = spawnY;
	}

	public int getSpawnY() {
		return spawnY;
	}

	public void setSpawnZ(int spawnZ) {
		this.spawnZ = spawnZ;
	}

	public int getSpawnZ() {
		return spawnZ;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	public Boolean getLocked() {
		return locked;
	}

	public void setLockOnMaxPlayers(Boolean lockOnMaxPlayers) {
		this.lockOnMaxPlayers = lockOnMaxPlayers;
	}

	public Boolean getLockOnMaxPlayers() {
		return lockOnMaxPlayers;
	}

	public void setSpawnSet(Boolean spawnSet) {
		this.spawnSet = spawnSet;
	}

	public Boolean getSpawnSet() {
		return spawnSet;
	}

	public void setJoinMoveMessage(String joinMoveMessage) {
		this.joinMoveMessage = joinMoveMessage;
	}

	public String getJoinMoveMessage() {
		return joinMoveMessage;
	}
}