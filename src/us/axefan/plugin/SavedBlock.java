package us.axefan.plugin;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name="SavedBlocks")
public class SavedBlock {
	
    @Id
    private int id;
    
    @NotNull
    private int regionId;

    @NotNull
    private int snapshotId;

    @NotNull
    private int typeId;
    
    @NotNull
    private byte data;
    
    @NotNull
    private int x;
    
    @NotNull
    private int y;
    
    @NotNull
    private int z;
    
    @NotNull
    private String lines;

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public int getRegionId() {
		return regionId;
	}

	public void setTypeId(int type) {
		this.typeId = type;
	}

	public int getTypeId() {
		return typeId;
	}

	public void setSnapshotId(int snapshotId) {
		this.snapshotId = snapshotId;
	}

	public int getSnapshotId() {
		return snapshotId;
	}

	public void setData(byte data) {
		this.data = data;
	}

	public byte getData() {
		return data;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getX() {
		return x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getY() {
		return y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getZ() {
		return z;
	}

	public void setLines(String lines) {
		this.lines = lines;
	}

	public String getLines() {
		return lines;
	}
    
}