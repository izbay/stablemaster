package com.github.izbay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		Player player = event.getPlayer();

		// Is it a valid pig?
		boolean validMount = false;
		if (entity instanceof Pig && ((Pig) entity).hasSaddle()
				&& !(entity.hasMetadata("NPC")))
			validMount = true;

		// Attempt at HashMaps.
		Map<Player, Boolean> isMounted = new HashMap<Player, Boolean>();
		Map<Player, UUID> pigAssigner = new HashMap<Player, UUID>();

		// Mounted
		if (player.getVehicle() != entity && validMount) {
			isMounted.put(player, true);
			pigAssigner.put(player, entity.getUniqueId());
		}

		// Dismounted
		if ((player.getVehicle() == entity && entity instanceof Pig)
				|| (player.getVehicle() instanceof Pig && (entity instanceof Minecart
						|| entity instanceof Boat || entity instanceof Bed))) {
			isMounted.put(player, false);
			pigAssigner.remove(player);
		}
	}
	
	/*
	 * 		THIS WILL BE IMPLEMENTED LATER TO DESPAWN/RESPAWN MOUNTS ON LOG-IN/OUT
	 */
}

