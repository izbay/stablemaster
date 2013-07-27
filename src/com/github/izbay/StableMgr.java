package com.github.izbay;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.inventory.ItemStack;

/**
 * @author izbay
 * New data structure for handling player stables. Allows for multiple mounts of different types.
 */
public class StableMgr implements Serializable{
	private static final long serialVersionUID = -9136169194046773791L;
	public static Map<Integer, String> placeMap = new HashMap<Integer, String>();
	
	public static String serializeLoc(Location loc){
		return loc.getWorld()+","+loc.getX()+","+loc.getY()+","+loc.getZ()+","+
				loc.getPitch()+","+loc.getYaw();
	}
	
	public static Location deserializeLoc(String loc){
		String[] parts = loc.split(",",6);
		World w = Bukkit.getWorld(parts[0]);
		double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(w,x,y,z,yaw,pitch);
	}
	
	/**
	 * @author izbay
	 * The individual account which belongs to each player. StableMgr maps one account to each player.
	 */
	public class StableAcct implements Serializable{
		private static final long serialVersionUID = -120172658233469216L;
		
		private double hasDebt = 0;
		private long nobleCooldown = 0;
		private ArrayList<Mount> mounts = new ArrayList<Mount>();
		private int boats = 0;
		private int carts = 0;
		/**
		 * Initializes a new account.
		 */
		public StableAcct(){}
		/**
		 * Initializes a new account with a debt.
		 * @param debt How much money the player owes.
		 */
		public StableAcct(double debt){
			hasDebt = debt;
		}
		/**
		 * @return How much money the player owes.
		 */
		public double getDebt(){
			return hasDebt;
		}
		/**
		 * @return The time (in milliseconds) the player last received a free mount.
		 */
		public long getCooldown(){
			return nobleCooldown;
		}
		/**
		 * @return The number of mounts the account currently owns.
		 */
		public int getNumMounts(){
			return mounts.size();
		}
		/**
		 * @param sel The index of which mount to return.
		 * @return The mount at the designated index.
		 */
		public Mount getMount(int sel){
			return mounts.get(sel);
		}
		/**
		 * @param debt Sets the debt on the account.
		 */
		public void setDebt(double debt){
			hasDebt = debt;
		}
		/**
		 * @param cooldown Sets a new cooldown for free noble mount on the account.
		 */
		public void setCooldown(long cooldown){
			nobleCooldown = cooldown;
		}
		/**
		 * @param name The name of the mount being added to the account.
		 * @param type The EntityType of the mount.
		 * @param time The time the mount is being added. Used for later calculation of fees.
		 */
		public void addMount(String name, EntityType type, Long time){
			addMount(new Mount(name, type, time));
		}
		/**
		 * @param mount The mount object (pre-instantiated) to add to the account.
		 */
		public void addMount(Mount mount){
			mounts.add(mount);
			Collections.sort(mounts);
		}
		/**
		 * @return Returns true if the account has room for another mount (determined by the limit set in config).
		 */
		public boolean hasRoom(){
			return (mounts.size() < IOManager.Traits.stable.getMaxMounts());
		}
		/**
		 * @return Returns true if the account has room for another boat (determined by the limit set in config).
		 */
		public boolean hasBoatRoom(){
			return boats < IOManager.Traits.wharf.getMaxMounts();
		}
		/**
		 * @return Returns true if the account has room for another cart (determined by the limit set in config).
		 */
		public boolean hasCartRoom(){
			return carts < IOManager.Traits.station.getMaxMounts();
		}
		/**
		 * @param sel The index of which mount to remove from the account.
		 */
		public void removeMount(int sel){
			mounts.remove(sel);
		}
		/**
		 * @param name Searches the account for a mount by this name and removes the first one found.
		 */
		public void removeMount(String name){
			for(int i=0; i<mounts.size(); i++){
				if (mounts.get(i).name.equalsIgnoreCase(name)){
					mounts.remove(i);
					break;
				}
			}
		}
		/**
		 * @param amt Sets the amount of boats in this account to this number.
		 */
		public void setBoats(int amt){
			boats = amt;
		}
		/**
		 * @param amt Sets the amount of carts in this account to this number.
		 */
		public void setCarts(int amt){
			carts = amt;
		}
		/**
		 * @return Number of boats this account has.
		 */
		public int getBoats(){
			return boats;
		}
		/**
		 * @return Number of carts this account has.
		 */
		public int getCarts(){
			return carts;
		}
	}
	/**
	 * @author izbay
	 * Schematic for new Mount object which tracks mount name, entity, and time delivered to a stablemaster.
	 */
	public class Mount implements Serializable, Comparable<Mount> {
		private static final long serialVersionUID = -2350454181256976547L;
		
		private String name;
		private EntityType type;
		private Long time;
		
		// Used by Horses
		private Variant variant;
		private Color color;
		private Style style;
		private Boolean haschest;
		private double health;
		private double jumpstr;
		private ItemSerializable armor;
		private ItemSerializable[] inventory = new ItemSerializable[17];
		
		public Mount(){}
		
		/**
		 * @param name The name of the new mount being created.
		 * @param type The EntityType of the new mount.
		 * @param time The time the mount is being delivered to a stablemaster.
		 */		
		public Mount(String name, EntityType type, Long time){
			this.name = name;
			this.type = type;
			this.time = time;
		}
		/**
		 * @param name The name of the new mount being created.
		 * @param type The EntityType of the new mount.
		 * @param time The time the mount is being delivered to a stablemaster.
		 * @param horse The horse object that this mount object represents.
		 */
		public Mount(String name, EntityType type, Long time, Horse horse){
			this.name = name;
			this.type = type;
			this.time = time;
			this.variant = horse.getVariant();
			this.color = horse.getColor();
			this.style = horse.getStyle();
			this.haschest = horse.isCarryingChest();
			this.health = horse.getMaxHealth();
			this.jumpstr = horse.getJumpStrength();
			if(horse.getInventory().getArmor() != null)
				this.armor = new ItemSerializable(horse.getInventory().getArmor());
			
			if(this.haschest){
				int i=0;
				for (ItemStack e: horse.getInventory().getContents()){
					if(e != null){
						inventory[i] = new ItemSerializable(e);
					}
					i++;
				}
			}
		}
	
		/**
		 * Used to sort mounts in player lists. Sorts first by EntityType, then by time in stable (descending).
		 */
		public int compareTo(Mount other){
			if(type.compareTo(other.type) != 0)
				return type.compareTo(other.type);
			if(time > other.time)
				return -1;
			if(time < other.time)
				return 1;
			return 0;
		}

// GETTERS
		public String getName(){
			return name;
		}
		public EntityType getType(){
			return type;
		}
		public Long getTime(){
			return time;
		}
		public Variant getVariant(){
			return variant;
		}
		public Color getColor(){
			return color;
		}
		public Style getStyle(){
			return style;
		}
		public Boolean HasChest(){
			return haschest;
		}
		public double getHealth(){
			return health;
		}
		public double getJumpstr(){
			return jumpstr;
		}
		public ItemStack getArmor(){
			if (armor != null)
				return armor.getItemStack();
			return null;
		}
		public ItemStack[] getInventory(){
			ItemStack[] inv = new ItemStack[17];
			for(int i=0; i<inventory.length; i++){
				if(inventory[i] != null)
					inv[i] = inventory[i].getItemStack();
			}
			return inv;
		}
		
// SETTERS
		public void setName(String name){
			this.name = name;
		}
		public void setType(EntityType type){
			this.type = type;
		}
		public void setTime(long time){
			this.time = time;
		}
		public void setVariant(Variant variant){
			this.variant = variant;
		}
		public void setColor(Color color){
			this.color = color;
		}
		public void setStyle(Style style){
			this.style = style;
		}
		public void setChest(Boolean chest){
			haschest = chest;
		}
		public void setHealth(double health){
			this.health = health;
		}
		public void setJumpstr(double jumpstr){
			this.jumpstr = jumpstr;
		}
		public void setArmor(ItemStack armor){
			this.armor = new ItemSerializable(armor);
		}
	}
}