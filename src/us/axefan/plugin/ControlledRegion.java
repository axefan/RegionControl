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
	
}