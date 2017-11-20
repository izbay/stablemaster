package com.github.izbay.stablemaster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Pig;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.inventory.ItemStack;

/**
	 * @author izbay Schematic for new Mount object which tracks mount name,
	 *         entity, and time delivered to a stablemaster.
	 */
public class Mount implements Serializable, Comparable<Mount> {
	private static final long serialVersionUID = -2350454181256976547L;
	private String name;
	private EntityType type;
	private Long time;
	// Used by llamas
	private org.bukkit.entity.Llama.Color llamaColor;
	// Used by Horses
	private int age = 0;
	private Color color;
	private Style style;
	private Boolean haschest;
	private double health;
	private double jumpstr;
	private double speed;
	private String uuid;
	private Map<String, Object> Armor;
	//used by llamas, donkeys, and mules
	private List<Map<String, Object>> Inventory = new ArrayList<Map<String, Object>>();
		
		// Old format. Grab this for updating.
	private ItemSerializable armor;
	private ItemSerializable[] inventory;
	
	public Mount() {
	}

		/**
		 * @param name
		 *            The name of the new mount being created.
		 * @param type
		 *            The EntityType of the new mount.
		 * @param time
		 *            The time the mount is being delivered to a stablemaster.
		 * @param horse
	     *            The horse object that this mount object represents.
	     */
	
	public Mount(String name, EntityType type, Long time) {
		this.name = name;
		this.type = type;
		this.time = time;
		this.haschest = false;
		if(this.age == 0){
			this.age = 5;
		}
	}
	
	@SuppressWarnings("deprecation")
	public Mount(String name, EntityType type, Long time, LivingEntity entity) {
		this.name = name;
		this.type = type;
		this.time = time;
		this.health = entity.getMaxHealth();
		this.haschest = false;
		AttributeInstance attributes = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		this.speed = attributes.getBaseValue();
		if(type == EntityType.PIG){
			Pig pig = (Pig) entity;
			this.age = pig.getAge();
		}
		if(type == EntityType.HORSE){
			Horse horse = (Horse) entity;
			this.age = horse.getAge();
			this.jumpstr = horse.getJumpStrength();	    
			this.color = horse.getColor();
			this.style = horse.getStyle();			
			ItemStack saddle = horse.getInventory().getSaddle();
			if (saddle != null && saddle.getType() == Material.SADDLE){
				setSaddle(saddle);
			}
			if (horse.getInventory().getArmor() != null){
				this.Armor = new ItemStack(horse.getInventory()
						.getArmor()).serialize();
			}
		}
		if(type == EntityType.MULE){
			Mule mule = (Mule) entity;
			this.age = mule.getAge();
			this.jumpstr = mule.getJumpStrength();	    
			this.haschest = mule.isCarryingChest();
			if (this.haschest) {
				for (ItemStack e : mule.getInventory().getStorageContents()) {
					Inventory.add((e==null)?null:e.serialize());
				}
			}
			ItemStack saddle = mule.getInventory().getItem(0);
			if (saddle != null && saddle.getType() == Material.SADDLE){
				setSaddle(saddle);
			}
		}
		if(type == EntityType.LLAMA){
			Llama llama = (Llama) entity;
			this.age = llama.getAge();
			this.jumpstr = llama.getJumpStrength();
			this.haschest = llama.isCarryingChest();
			this.llamaColor = llama.getColor();
			if (this.haschest) {
				for (ItemStack e : llama.getInventory().getStorageContents()) {
					Inventory.add((e==null)?null:e.serialize());
				}
			}
			ItemStack carpet = llama.getInventory().getItem(1);
			if (carpet != null && carpet.getType() == Material.CARPET){
				setCarpet(carpet);
			}
		}
		if(type == EntityType.DONKEY){
			Donkey donkey = (Donkey) entity;
			this.age = donkey.getAge();
			this.jumpstr = donkey.getJumpStrength();
			this.haschest = donkey.isCarryingChest();
			if (this.haschest) {
				for (ItemStack e : donkey.getInventory().getContents()) {
					Inventory.add((e==null)?null:e.serialize());
				}
			}
			ItemStack saddle = donkey.getInventory().getItem(0);
			if (saddle != null && saddle.getType() == Material.SADDLE){
				setSaddle(saddle);
			}
		}
		if(type == EntityType.ZOMBIE_HORSE){
			ZombieHorse zombHorse = (ZombieHorse) entity;
			this.age = zombHorse.getAge();
			this.jumpstr = zombHorse.getJumpStrength();
			ItemStack saddle = zombHorse.getInventory().getItem(0);
			if (saddle != null && saddle.getType() == Material.SADDLE){
				setSaddle(saddle);
			}
			if (zombHorse.getInventory().getItem(1) != null){
				this.Armor = new ItemStack(zombHorse.getInventory().getItem(1)).serialize();
			}
		}
		if(type == EntityType.SKELETON_HORSE){
			SkeletonHorse skeleHorse = (SkeletonHorse) entity;
			this.age = skeleHorse.getAge();
			this.jumpstr = skeleHorse.getJumpStrength();
			ItemStack saddle = skeleHorse.getInventory().getItem(0);
			if (saddle != null && saddle.getType() == Material.SADDLE){
				setSaddle(saddle);
			}
			if (skeleHorse.getInventory().getItem(1) != null){
				this.Armor = new ItemStack(skeleHorse.getInventory().getItem(1)).serialize();
			}
		}
	}

	/**
	 * Used to sort mounts in player lists. Sorts first by EntityType, then
		 * by time in stable (descending).
		 */
	public int compareTo(Mount other) {
		if (type.compareTo(other.type) != 0)
			return type.compareTo(other.type);
		if (time > other.time)
			return -1;
		if (time < other.time)
			return 1;
		return 0;
	}
	// GETTERS
	public String getName() {
		return name;
	}

	public EntityType getType() {
		return type;
	}

	public Long getTime() {
			return time;
	}

	public Color getColor() {
		return color;
	}
	
	public int getAge(){
		return age;
	}
	
	public org.bukkit.entity.Llama.Color getLlamaColor(){
		return llamaColor;
	}

	public Style getStyle() {
		return style;
	}

	public Boolean HasChest() {
		return haschest;
	}

	public double getHealth() {
		return health;
	}

	public double getJumpstr() {
		return jumpstr;
	}

	public ItemStack getArmor() {
		if (armor != null){
			ItemStack ar = armor.getItemStack();
			armor = null;
			return ar;
		}
		if (Armor != null)
			return ItemStack.deserialize(Armor);
		return null;
	}
	
	public ItemStack getCarpet() {
		if(Inventory.size() == 0){
			return null;
		} else {
			return ItemStack.deserialize(Inventory.get(0));
		}
	}
		
	public ItemStack getSaddle() {
		if(Inventory.size() == 0){
			return null;
		} else {
			return ItemStack.deserialize(Inventory.get(0));
		}
	}
		
	public UUID getUUID(){
		return (uuid == null)? null : UUID.fromString(uuid);
	}

	public ItemStack[] getInventory() {
		updateInv();
		ItemStack[] inv = new ItemStack[17];
		for (int i = 0; i < Inventory.size(); i++) {
			if (Inventory.get(i) != null)
				inv[i] = ItemStack.deserialize(Inventory.get(i));
		}
		return inv;
	}

	public double getSpeed() {
		return speed;
	}

	// SETTERS
	public void setName(String name) {
		this.name = name;
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
	public void setAge(int age){
		this.age = age;
	}
	
	public void setLlamaColor(org.bukkit.entity.Llama.Color color){
		this.llamaColor = color;
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	public void setChest(Boolean chest) {
		haschest = chest;
	}

	public void setHealth(double health) {
		this.health = health;
	}

	public void setJumpstr(double jumpstr) {
		this.jumpstr = jumpstr;
	}

	public void setArmor(ItemStack armor) {
		this.Armor = new ItemStack(armor).serialize();
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public void setCarpet(ItemStack carpet){
		if(Inventory.size() == 0){
			Inventory.add(carpet.serialize());
		}else {
			Inventory.set(0, carpet.serialize());
		}
	}
	
	public void setSaddle(ItemStack saddle) {
		if(Inventory.size() == 0){
			Inventory.add(saddle.serialize());
		}else {
			Inventory.set(0, saddle.serialize());
		}
	}
		
	public void setUUID(UUID uuid){
		this.uuid = uuid.toString();
	}
		
	private void updateInv(){
		if(inventory != null){
			Inventory = new ArrayList<Map<String, Object>>();
			for (int i = 0; i < 17; i++){
				ItemSerializable item = inventory[i];
				Inventory.add(item == null? null : item.getItemStack().serialize());
			}
			inventory = null;
		}
	}
}