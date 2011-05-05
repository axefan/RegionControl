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
    private int snapshotId;
    
    @NotNull
    private Boolean locked;
    
    @NotNull
    private int maxPlayers; // -1 = no limit
    
    @NotNull
    private int minPlayers; // -1 = no limit
    
    @NotNull
    private int currentPlayers;
    
    @NotNull
    private String maxMessage; // default = "no more players allowed"
    
    @NotNull
    private String minMessage; // default = "waiting for more players"
    
    // TODO: Add ControlledRegion.failSpawn point (for kick from controlled region - death on ControlledRegion.maxTries, on reload).
    // TODO: Add ControlledRegion.retrySpawn point (for reset after death when less than ControlledRegion.maxTries.
    // TODO: Add ControlledRegion.lives & LockedPlayer.lives (number of deaths before kick to ControlledRegion.failSpawn).
    // TODO: Add ControlledRegion.lockOnMaxPlayers
    // TODO: Add ControlledRegion.setInventoryMessage
    // TODO: Add ControlledRegion.restoreInventoryMessage
    
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

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	public Boolean getLocked() {
		return locked;
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
	
}