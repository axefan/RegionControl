package us.axefan.plugin;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name="LockedPlayers")
public class LockedPlayer {

    @Id
    private int id;
    
    @NotNull
    private int regionId;
    
    @NotNull
    private String name;
    
    @NotNull
    private int health; // 0 - 20
    

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

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public int getHealth() {
		return health;
	}

}
