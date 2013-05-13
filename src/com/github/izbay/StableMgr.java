package com.github.izbay;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.entity.EntityType;

/**
 * @author izbay
 * New data structure for handling player stables. Allows for multiple mounts of different types.
 */
public class StableMgr implements Serializable{
	private static final long serialVersionUID = -9136169194046773791L;
	
	/**
	 * @author izbay
	 * The individual account which belongs to each player. StableMgr maps one account to each player.
	 */
	public class StableAcct implements Serializable{
		private static final long serialVersionUID = -120172658233469216L;
		
		private double hasDebt = 0;
		private long nobleCooldown = 0;
		private ArrayList<Mount> mounts = new ArrayList<Mount>();
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
		 * @return The time (in milliseconds) the player last recieved a free mount.
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
		/**
		 * @return Returns the name of this mount.
		 */
		public String getName(){
			return name;
		}
		/**
		 * @return Returns the EntityType of this mount.
		 */
		public EntityType getType(){
			return type;
		}
		/**
		 * @return Returns time this mount was dropped off.
		 */
		public Long getTime(){
			return time;
		}
		/**
		 * @return Sets a new time for this mount being dropped off.
		 */
		public void setTime(long time){
			this.time = time;
		}
	}
		
}