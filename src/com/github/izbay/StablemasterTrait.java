package com.github.izbay;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import net.citizensnpcs.api.trait.Trait;

public class StablemasterTrait extends Trait {

	private final StablemasterPlugin plugin;
	private final Map<String, Long> isChatting = new HashMap<String, Long>();
	final Map<String, Long> hasStabled = new HashMap<String, Long>();
	final Map<String, Long> nobleCooldown = new HashMap<String, Long>();
	final Map<String, Integer> hasDebt = new HashMap<String, Integer>();

	public StablemasterTrait() {
		super("stablemaster");
		plugin = (StablemasterPlugin) Bukkit.getServer().getPluginManager().getPlugin("Stablemaster");
	}
	
	private void getPig(Player player) {

		// Get the midpoint.
		Location location = this.npc.getBukkitEntity().getLocation();
		location.add(player.getLocation());
		location.multiply(0.5);

		// And spawn a pig!
		Entity piggie = npc.getBukkitEntity().getWorld()
				.spawnEntity(location, EntityType.PIG);
		((Pig) piggie).setSaddle(true);
	}

	private boolean chatCheck(Player player) {
		if (isChatting.get(player.toString()) == null
				|| System.currentTimeMillis() >= isChatting.get(player
						.toString()) + 10 * 1000) {
			isChatting.put(player.toString(), System.currentTimeMillis());
			return false;
		} else {
			isChatting.remove(player.toString());
			return true;
		}
	}

	private int daysStabled(Player player) {
		return (int) ((System.currentTimeMillis() - hasStabled.get(player
				.toString())) / (86400 * 1000));
	}

	@EventHandler
	public void onRightClick(net.citizensnpcs.api.event.NPCRightClickEvent event) {
		if (this.npc != event.getNPC())
			return;

		Player player = event.getClicker();

		// Debt?
		if (hasDebt.get(player.toString()) != null) {
			if (chatCheck(player)) {
				if (plugin.canAfford(player, hasDebt.get(player.toString()))) {
					plugin.charge(npc, player, hasDebt.get(player.toString()));
					player.sendMessage(plugin.paidMsg);
					hasDebt.remove(player.toString());
				} else {
					player.sendMessage(plugin.insufficientFundsMsg);
				}
			} else {
				player.sendMessage(plugin.debtMsg.replace("<DEBT>",
						plugin.econFormat(hasDebt.get(player.toString()))));
			}
		}
		// On foot: Pickup or Buy
		else if (player.getVehicle() == null
				&& player.hasPermission("stablemaster.stable")) {

			// Nobles
			if (player.hasPermission("stablemaster.noble")) {
				if (hasStabled.get(player.toString()) != null) {
					player.sendMessage(plugin.nobleTakeMsg);
					hasStabled.remove(player.toString());
					getPig(player);
				} else {
					if (nobleCooldown.get(player.toString()) == null
							|| (System.currentTimeMillis() >= nobleCooldown
									.get(player.toString())
									+ (3600 * 1000 * plugin.nobleCooldownLen))) {
						player.sendMessage(plugin.nobleFreeMsg);
						nobleCooldown.put(player.toString(),
								System.currentTimeMillis());
						getPig(player);
					} else {
						if (chatCheck(player)) {
							if (plugin.canAfford(player, plugin.pigBasePrice)) {
								plugin.charge(npc, player, plugin.pigBasePrice);
								player.sendMessage(plugin.nobleTakeMsg.replace(
										"<MOUNT_PRICE>",
										plugin.econFormat(plugin.pigBasePrice)));
								getPig(player);
							} else
								player.sendMessage(plugin.insufficientFundsMsg);

						} else
							player.sendMessage(plugin.nobleDenyMsg);
					}
				}

				// Others
			} else {
				if (hasStabled.get(player.toString()) != null) {
					if (chatCheck(player)) {
						if (plugin.canAfford(player, plugin.pricePerDay
								* daysStabled(player))) {
							plugin.charge(npc, player, plugin.pricePerDay * daysStabled(player));
							player.sendMessage(plugin.giveMsg);
						} else {
							plugin.charge(npc, player, (int) plugin.economy.getBalance(player.getName()));
							player.sendMessage(plugin.shortMsg);
							hasDebt.put(
									player.toString(),
									(int) ((plugin.pricePerDay * daysStabled(player)) - plugin.economy
											.getBalance(player.getName())));
						}
						hasStabled.remove(player.toString());
						getPig(player);
					} else {
						player.sendMessage(plugin.takeMsg);
						if ((plugin.pricePerDay * daysStabled(player)) > 0)
							player.sendMessage(plugin.take2Msg.replace(
									"<TOTAL_PRICE>",
									plugin.econFormat(plugin.pricePerDay
											* daysStabled(player))).replace("<DAYS>", ""+daysStabled(player)));
					}

					// Looks like you're buying one!
				} else {
					if (chatCheck(player)) {
						if (plugin.canAfford(player, plugin.pigBasePrice)) {
							plugin.charge(npc, player, plugin.pigBasePrice);
							player.sendMessage(plugin.giveMsg);
							getPig(player);
						} else
							player.sendMessage(plugin.insufficientFundsMsg);

					} else
						player.sendMessage(plugin.saleMsg.replace("<MOUNT_PRICE>",
								plugin.econFormat(plugin.pigBasePrice)));
				}
			}

			// Invalid or No Permission = No Service!
		} else if (!(player.getVehicle() instanceof Pig)
				|| !player.hasPermission("stablemaster.stable")) {
			player.sendMessage(plugin.invalidMountMsg);
			return;

			// Drop off.
		} else {
			if (hasStabled.get(player.toString()) == null) {

				// Nobles
				if (player.hasPermission("stablemaster.noble")) {
					if (chatCheck(player)) {
						player.sendMessage(plugin.nobleStowMsg);
						hasStabled.put(player.toString(),
								System.currentTimeMillis());
						player.getVehicle().remove();
					} else
						player.sendMessage(plugin.nobleOfferMsg);

					// Others
				} else {
					if (chatCheck(player)) {
						if (plugin.canAfford(player, plugin.basePrice)) {
							plugin.charge(npc, player, plugin.basePrice);
							player.sendMessage(plugin.stowMsg);
							hasStabled.put(player.toString(),
									System.currentTimeMillis());
							player.getVehicle().remove();
						} else
							player.sendMessage(plugin.insufficientFundsMsg);
					} else
						player.sendMessage(plugin.costMsg.replace("<BASE_PRICE>",
								plugin.econFormat(plugin.basePrice)).replace(
								"<PRICE_PER_DAY>",
								plugin.econFormat(plugin.pricePerDay)));
				}

				// Already has your pig!
			} else {
				player.sendMessage(plugin.alreadyMountMsg);
			}
		}
	}
}
