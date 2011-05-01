package us.axefan.plugin;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

/*
 * Container for a set of blocks that represent a region in a particular state.
 */
@Entity()
@Table(name="RegionSnapshot")
public class RegionSnapshot {
	
    @Id
    private int id;
    
    @NotNull
    private int regionId;

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
    
}