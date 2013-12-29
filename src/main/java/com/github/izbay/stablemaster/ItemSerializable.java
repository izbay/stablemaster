package com.github.izbay.stablemaster;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
 



import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

/**
 * 
 * @author Geekola @ bukkitdev
 *
 */

/* Some fields are deprecated, but left in because ItemSerializable will be nixxed
 * in favor of item.serialize.  Use this old method of loading to update users to 
 * the new format in StableMgr at runtime. */
public class ItemSerializable implements Serializable {
 
 
	private static final long serialVersionUID = 9218747208794761041L;
	private int materialId;
	private byte data;
	private short durability;
	private int amount;
	private Map<String, Object> meta;
 
	@SuppressWarnings("deprecation")
	public ItemSerializable( ItemStack i ) {
		this.materialId = i.getType().getId();
		this.data	= i.getData().getData();
		this.durability = i.getDurability();
		this.amount = i.getAmount();
 
		if ( i.getItemMeta() instanceof ConfigurationSerializable ) {
			this.meta = this.getNewMap(((ConfigurationSerializable) i.getItemMeta()).serialize());
		}
 
	}
 
	@SuppressWarnings("deprecation")
	public ItemStack getItemStack() {
		Material m = Material.getMaterial(materialId);
		ItemStack i = new ItemStack(m);
		i.setAmount( this.amount );
		i.setDurability( this.durability );
		i.setData( new MaterialData( this.data ) );
		i.setDurability( this.durability );
 
		if ( this.meta != null && !this.meta.isEmpty() ) {
			i.setItemMeta( (ItemMeta) ConfigurationSerialization.deserializeObject(this.meta, ConfigurationSerialization.getClassByAlias("ItemMeta")) );
		}
 
		return i;
 
	}
 
	@SuppressWarnings("unchecked")
	private Map<String, Object> getNewMap( Map<String, Object> map ) {
 
		Map<String, Object> newMap = new HashMap<String, Object>();
 
		if ( !map.isEmpty() ) {
 
			for ( String x : map.keySet() ) {
 
				Object value = map.get(x);
 
				if ( value instanceof Map ) {
					value = getNewMap( (Map<String, Object>) value );
				}
 
				newMap.put( new String(x), value);
			}
 
		}
 
		return newMap;
	}
 
}