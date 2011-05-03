package us.axefan.plugin;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name="RegionItems")
public class RegionItem {
	
    @Id
    private int id;
    
    @NotNull
    private int regionId;
    
    @NotNull
    private int typeId;
    
    @NotNull
    private short durability;
    
    @NotNull
    private short amount;
    
    @NotNull
    private byte data;
    
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

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public int getTypeId() {
		return typeId;
	}

	public void setDurability(short durability) {
		this.durability = durability;
	}

	public short getDurability() {
		return durability;
	}

	public void setAmount(short amount) {
		this.amount = amount;
	}

	public short getAmount() {
		return amount;
	}

	public void setData(byte data) {
		this.data = data;
	}

	public byte getData() {
		return data;
	}

}
