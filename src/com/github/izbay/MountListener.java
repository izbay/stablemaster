package com.github.izbay;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.material.Bed;

public class MountListener implements Listener {

	public static StablemasterPlugin plugin;

	public MountListener(StablemasterPlugin instance) {
		plugin = instance;
	}

	// Attempt at HashMaps.
	Map<String, Boolean> isMounted = new HashMap<String, Boolean>();

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();

		// Is it a valid pig?
		boolean validMount = false;
		if (entity instanceof Pig && ((Pig) entity).hasSaddle()
				&& !(entity.hasMetadata("NPC")))
			validMount = true;

		// Mounted
		if (player.getVehicle() != entity && validMount) {
			isMounted.put(player.toString(), true);
		}

		// Dismounted
		if ((player.getVehicle() == entity && entity instanceof Pig)
				|| (player.getVehicle() instanceof Pig && (entity instanceof Minecart
						|| entity instanceof Boat || entity instanceof Bed))) {
			isMounted.remove(player.toString());
		}
	}
/*
	public void onPlayerLoginEvent(PlayerJoinEvent event) {
		if (isMounted.get(event.getPlayer().toString()) != null) {
			Location location = event.getPlayer().getLocation();
			Entity piggie = event.getPlayer().getWorld()
					.spawnEntity(location, EntityType.PIG);
			((Pig) piggie).setSaddle(true);
			piggie.setPassenger(event.getPlayer());
		}
	}

	 * WILL BE IMPLEMENTED FOR MOUNT DESPAWN/SPAWN ON PLAYER LOG EVENT
	 */

}
