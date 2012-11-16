package com.github.izbay;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;



public class MountListener implements Listener {

	public static StablemasterPlugin plugin;

	public MountListener(StablemasterPlugin instance) {
		plugin = instance;
	}

	// Was Mounted?
	static Map<String, Boolean> leftMounted = new HashMap<String, Boolean>();


	
	@EventHandler
	public void onPlayerOut(PlayerQuitEvent event) {
		Player player = (event.getPlayer());
		if (player.getVehicle() instanceof Pig) {
			if(StablemasterTrait.mobLock){
				player.getVehicle().remove();
				leftMounted.put(player.toString(), true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		Player player = (event.getPlayer());
		if (player.getVehicle() instanceof Pig) {
			if(StablemasterTrait.mobLock){
				player.getVehicle().remove();
				leftMounted.put(player.toString(), true);
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();

		if (leftMounted.get(player.toString()) != null) {
			Location loc = player.getLocation();
			final Entity piggie = player.getWorld().spawnEntity(loc, EntityType.PIG);
			StablemasterPlugin.delay(player, piggie);
			// piggie.setPassenger(player);

			leftMounted.remove(player.toString());
		}
	}
}
