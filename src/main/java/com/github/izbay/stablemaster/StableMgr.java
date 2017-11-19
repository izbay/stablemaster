package com.github.izbay.stablemaster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

/**
 * @author izbay New data structure for handling player stables. Allows for
 *         multiple mounts of different types.
 */
public class StableMgr implements Serializable {
	private static final long serialVersionUID = -9136169194046773791L;
	public static Map<Integer, String> placeMap = new HashMap<Integer, String>();
	public static Map<String, StableMgr.StableAcct> stableMgr = new HashMap<String, StableMgr.StableAcct>();
	
	public static String serializeLoc(Location loc) {
		return loc.getWorld() + "," + loc.getX() + "," + loc.getY() + ","
				+ loc.getZ() + "," + loc.getPitch() + "," + loc.getYaw();
	}

	public static Location deserializeLoc(String loc) {
		String[] parts = loc.split(",", 6);
		World w = Bukkit.getWorld(parts[0]);
		double x = Double.parseDouble(parts[1]);
		double y = Double.parseDouble(parts[2]);
		double z = Double.parseDouble(parts[3]);
		float yaw = Float.parseFloat(parts[4]);
		float pitch = Float.parseFloat(parts[5]);
		return new Location(w, x, y, z, yaw, pitch);
	}

	/**
	 * @author izbay The individual account which belongs to each player.
	 *         StableMgr maps one account to each player.
	 */
	public class StableAcct implements Serializable {
		private static final long serialVersionUID = -120172658233469216L;

		private double hasDebt = 0;
		private ArrayList<Mount> mounts = new ArrayList<Mount>();
		private int boats = 0;
		private int carts = 0;

		/**
		 * Initializes a new account.
		 */
		public StableAcct() {
		}

		/**
		 * Initializes a new account with a debt.
		 * 
		 * @param debt
		 *            How much money the player owes.
		 */
		public StableAcct(double debt) {
			hasDebt = debt;
		}

		/**
		 * @return How much money the player owes.
		 */
		public double getDebt() {
			return hasDebt;
		}

		/**
		 * @return The number of mounts the account currently owns.
		 */
		public int getNumMounts() {
			return mounts.size();
		}

		/**
		 * @param sel
		 *            The index of which mount to return.
		 * @return The mount at the designated index.
		 */
		public Mount getMount(int sel) {
			return mounts.get(sel);
		}

		/**
		 * @param debt
		 *            Sets the debt on the account.
		 */
		public void setDebt(double debt) {
			hasDebt = debt;
		}

		/**
		 * @param name
		 *            The name of the mount being added to the account.
		 * @param type
		 *            The EntityType of the mount.
		 * @param time
		 *            The time the mount is being added. Used for later
		 *            calculation of fees.
		 */
		public void addMount(String name, EntityType type, Long time) {
			addMount(new Mount(name, type, time));
			Collections.sort(mounts);
		}

		/**
		 * @param mount
		 *            The mount object (pre-instantiated) to add to the account.
		 */
		public void addMount(Mount mount) {
			mounts.add(mount);
			Collections.sort(mounts);
		}

		/**
		 * @return Returns true if the account has room for another mount
		 *         (determined by the limit set in config).
		 */
		public boolean hasRoom() {
			return (mounts.size() < IOManager.Traits.stable.getMaxMounts());
		}

		/**
		 * @return Returns true if the account has room for another boat
		 *         (determined by the limit set in config).
		 */
		public boolean hasBoatRoom() {
			return boats < IOManager.Traits.wharf.getMaxMounts();
		}

		/**
		 * @return Returns true if the account has room for another cart
		 *         (determined by the limit set in config).
		 */
		public boolean hasCartRoom() {
			return carts < IOManager.Traits.station.getMaxMounts();
		}

		/**
		 * @param sel
		 *            The index of which mount to remove from the account.
		 */
		public void removeMount(int sel) {
			mounts.remove(sel);
		}

		/**
		 * @param name
		 *            Searches the account for a mount by this name and removes
		 *            the first one found.
		 */
		public void removeMount(String name) {
			for (int i = 0; i < mounts.size(); i++) {
				if (mounts.get(i).getName().equalsIgnoreCase(name)) {
					mounts.remove(i);
					break;
				}
			}
		}

		/**
		 * @param amt
		 *            Sets the amount of boats in this account to this number.
		 */
		public void setBoats(int amt) {
			boats = amt;
		}

		/**
		 * @param amt
		 *            Sets the amount of carts in this account to this number.
		 */
		public void setCarts(int amt) {
			carts = amt;
		}

		/**
		 * @return Number of boats this account has.
		 */
		public int getBoats() {
			return boats;
		}

		/**
		 * @return Number of carts this account has.
		 */
		public int getCarts() {
			return carts;
		}
	}
}