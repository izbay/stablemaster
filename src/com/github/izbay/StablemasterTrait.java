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
import net.citizensnpcs.api.util.DataKey;

public class StablemasterTrait extends Trait implements Listener {

	private final StablemasterPlugin plugin;

	private final Map<String, Long> isChatting = new HashMap<String, Long>();
	static Map<String, Long> hasStabled = new HashMap<String, Long>();
	static Map<String, Long> nobleCooldown = new HashMap<String, Long>();
	static Map<String, Integer> hasDebt = new HashMap<String, Integer>();
	private final Map<String, Boolean> mountWatcher = new HashMap<String, Boolean>();
	
	
	
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
	public static Boolean mobLock = Setting.MOB_LOCK.asBoolean();

	
	
	public StablemasterTrait() {
		super("stablemaster");
		plugin = (StablemasterPlugin) Bukkit.getServer().getPluginManager()
				.getPlugin("Stablemaster");
	}
	
	
	
	@Override
	public void load(DataKey key) {
		// Override defaults if they exist
		if (key.keyExists("values.base"))
			basePrice = key.getInt("values.base");
		if (key.keyExists("values.perday"))
			pricePerDay = key.getInt("values.perday");
		if (key.keyExists("values.pig"))
			pigBasePrice = key.getInt("values.pig");
		if (key.keyExists("values.cooldown"))
			nobleCooldownLen = key.getDouble("values.cooldown");
		if (key.keyExists("values.moblock"))
			mobLock = key.getBoolean("values.moblock");
		
		if (key.keyExists("text.cost"))
			costMsg = key.getString("text.cost");
		if (key.keyExists("text.funds"))
			insufficientFundsMsg = key.getString("text.funds");
		if (key.keyExists("text.invalid"))
			invalidMountMsg = key.getString("text.invalid");
		if (key.keyExists("text.already"))
			alreadyMountMsg = key.getString("text.already");
		if (key.keyExists("text.sale"))
			saleMsg = key.getString("text.sale");
		if (key.keyExists("text.give"))
			giveMsg = key.getString("text.give");
		if (key.keyExists("text.short"))
			shortMsg = key.getString("text.short");
		if (key.keyExists("text.debt"))
			debtMsg = key.getString("text.debt");
		if (key.keyExists("text.paid"))
			paidMsg = key.getString("text.paid");
		if (key.keyExists("text.stow"))
			stowMsg = key.getString("text.stow");
		if (key.keyExists("text.take"))
			takeMsg = key.getString("text.take");
		if (key.keyExists("text.take2"))
			take2Msg = key.getString("text.take2");
		if (key.keyExists("noble.offer"))
			nobleOfferMsg = key.getString("noble.offer");
		if (key.keyExists("noble.stow"))
			nobleStowMsg = key.getString("noble.stow");
		if (key.keyExists("noble.free"))
			nobleFreeMsg = key.getString("noble.free");
		if (key.keyExists("noble.take"))
			nobleTakeMsg = key.getString("noble.take");
		if (key.keyExists("noble.deny"))
			nobleDenyMsg = key.getString("noble.deny");
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

	//	Move on to second action from NPC iff player remains mounted/dismounted.
	private boolean chatCheck(Player player) {
		if (isChatting.get(player.toString()) == null
				|| System.currentTimeMillis() >= isChatting.get(player
						.toString()) + 10 * 1000) {
			isChatting.put(player.toString(), System.currentTimeMillis());
			mountWatcher.put(player.toString(), player.isInsideVehicle());
			return false;
		} else {
			if ((mountWatcher.get(player.toString()) && player.isInsideVehicle()) || (!mountWatcher.get(player.toString()) && !player.isInsideVehicle())){
				isChatting.remove(player.toString());
				mountWatcher.remove(player.toString());
				return true;
			} else
				mountWatcher.put(player.toString(), player.isInsideVehicle());
				return false;
		}
	}

	//	How many days (86,400 seconds)?
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
								hasStabled.remove(player.toString());
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
