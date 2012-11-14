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
import org.bukkit.event.Listener;

import com.github.izbay.util.Settings.Setting;

import net.citizensnpcs.api.trait.Trait;

public class StablemasterTrait extends Trait implements Listener {

	private final StablemasterPlugin plugin;

	private final Map<String, Long> isChatting = new HashMap<String, Long>();
	static Map<String, Long> hasStabled = new HashMap<String, Long>();
	static Map<String, Long> nobleCooldown = new HashMap<String, Long>();
	static Map<String, Integer> hasDebt = new HashMap<String, Integer>();

	public StablemasterTrait() {
		super("stablemaster");
		plugin = (StablemasterPlugin) Bukkit.getServer().getPluginManager()
				.getPlugin("Stablemaster");
	}

	// Defaults
	public String costMsg = Setting.COST_MESSAGE.asString();
	public String insufficientFundsMsg = Setting.FUNDS_MESSAGE.asString();
	public String invalidMountMsg = Setting.INVALID_MESSAGE.asString();
	public String alreadyMountMsg = Setting.ALREADY_MESSAGE.asString();
	public String saleMsg = Setting.SALE_MESSAGE.asString();
	public String giveMsg = Setting.GIVE_MESSAGE.asString();
	public String shortMsg = Setting.SHORT_MESSAGE.asString();
	public String debtMsg = Setting.DEBT_MESSAGE.asString();
	public String paidMsg = Setting.PAID_MESSAGE.asString();
	public String stowMsg = Setting.STOW_MESSAGE.asString();
	public String takeMsg = Setting.TAKE_MESSAGE.asString();
	public String take2Msg = Setting.TAKE2_MESSAGE.asString();
	public String nobleOfferMsg = Setting.NOBLE_OFFER.asString();
	public String nobleStowMsg = Setting.NOBLE_STOW.asString();
	public String nobleFreeMsg = Setting.NOBLE_FREE.asString();
	public String nobleTakeMsg = Setting.NOBLE_TAKE.asString();
	public String nobleDenyMsg = Setting.NOBLE_DENY.asString();

	public Integer basePrice = Setting.BASE_PRICE.asInt();
	public Integer pricePerDay = Setting.PRICE_PER_DAY.asInt();
	public Integer pigBasePrice = Setting.PIG_BASE_PRICE.asInt();

	public Double nobleCooldownLen = Setting.NOBLE_COOLDOWN.asDouble();

	
	
	
	
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
					player.sendMessage(paidMsg);
					hasDebt.remove(player.toString());
				} else {
					player.sendMessage(insufficientFundsMsg);
				}
			} else {
				player.sendMessage(debtMsg.replace("<DEBT>",
						plugin.econFormat(hasDebt.get(player.toString()))));
			}
		}
		// On foot: Pickup or Buy
		else if (player.getVehicle() == null
				&& player.hasPermission("stablemaster.stable")) {

			// Nobles
			if (player.hasPermission("stablemaster.noble")) {
				if (hasStabled.get(player.toString()) != null) {
					player.sendMessage(nobleTakeMsg);
					hasStabled.remove(player.toString());
					getPig(player);
				} else {
					if (nobleCooldown.get(player.toString()) == null
							|| (System.currentTimeMillis() >= nobleCooldown
									.get(player.toString())
									+ (3600 * 1000 * nobleCooldownLen))) {
						player.sendMessage(nobleFreeMsg);
						nobleCooldown.put(player.toString(),
								System.currentTimeMillis());
						getPig(player);
					} else {
						if (chatCheck(player)) {
							if (plugin.canAfford(player, pigBasePrice)) {
								plugin.charge(npc, player, pigBasePrice);
								player.sendMessage(nobleTakeMsg.replace(
										"<MOUNT_PRICE>",
										plugin.econFormat(pigBasePrice)));
								getPig(player);
							} else
								player.sendMessage(insufficientFundsMsg);

						} else
							player.sendMessage(nobleDenyMsg);
					}
				}

				// Others
			} else {
				if (hasStabled.get(player.toString()) != null) {
					if (chatCheck(player)) {
						if (plugin.canAfford(player, pricePerDay
								* daysStabled(player))) {
							plugin.charge(npc, player, pricePerDay
									* daysStabled(player));
							player.sendMessage(giveMsg);
						} else {
							plugin.charge(npc, player, (int) plugin.economy
									.getBalance(player.getName()));
							player.sendMessage(shortMsg);
							hasDebt.put(
									player.toString(),
									(int) ((pricePerDay * daysStabled(player)) - plugin.economy
											.getBalance(player.getName())));
						}
						hasStabled.remove(player.toString());
						getPig(player);
					} else {
						player.sendMessage(takeMsg);
						if ((pricePerDay * daysStabled(player)) > 0)
							player.sendMessage(take2Msg.replace(
									"<TOTAL_PRICE>",
									plugin.econFormat(pricePerDay
											* daysStabled(player))).replace(
									"<DAYS>", "" + daysStabled(player)));
					}

					// Looks like you're buying one!
				} else {
					if (chatCheck(player)) {
						if (plugin.canAfford(player, pigBasePrice)) {
							plugin.charge(npc, player, pigBasePrice);
							player.sendMessage(giveMsg);
							getPig(player);
						} else
							player.sendMessage(insufficientFundsMsg);

					} else
						player.sendMessage(saleMsg.replace("<MOUNT_PRICE>",
								plugin.econFormat(pigBasePrice)));
				}
			}

			// Invalid or No Permission = No Service!
		} else if (!(player.getVehicle() instanceof Pig)
				|| !player.hasPermission("stablemaster.stable")) {
			player.sendMessage(invalidMountMsg);
			return;

			// Drop off.
		} else {
			if (hasStabled.get(player.toString()) == null) {

				// Nobles
				if (player.hasPermission("stablemaster.noble")) {
					if (chatCheck(player)) {
						player.sendMessage(nobleStowMsg);
						hasStabled.put(player.toString(),
								System.currentTimeMillis());
						player.getVehicle().remove();
					} else
						player.sendMessage(nobleOfferMsg);

					// Others
				} else {
					if (chatCheck(player)) {
						if (plugin.canAfford(player, basePrice)) {
							plugin.charge(npc, player, basePrice);
							player.sendMessage(stowMsg);
							hasStabled.put(player.toString(),
									System.currentTimeMillis());
							player.getVehicle().remove();
						} else
							player.sendMessage(insufficientFundsMsg);
					} else
						player.sendMessage(costMsg.replace("<BASE_PRICE>",
								plugin.econFormat(basePrice)).replace(
								"<PRICE_PER_DAY>",
								plugin.econFormat(pricePerDay)));
				}

				// Already has your pig!
			} else {
				player.sendMessage(alreadyMountMsg);
			}
		}
	}
}
