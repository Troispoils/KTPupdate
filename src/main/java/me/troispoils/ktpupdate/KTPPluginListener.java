/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.troispoils.ktpupdate;

/**
 *
 * @author Troispoils
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.Sound;
import org.bukkit.conversations.Conversation;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class KTPPluginListener implements Listener {

	KTPupdate p = null;
	
	public KTPPluginListener(KTPupdate p) {
		this.p = p;
	}
	
	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent ev) {
		Location l = ev.getEntity().getLocation();
		for (Player ps : Bukkit.getOnlinePlayers()) {
			ps.playSound(ps.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1F, 1F);
		}
		this.p.addDead(ev.getEntity().getName());
		BukkitRunnable playerdeath = new BukkitRunnable() {
			
			public void run() {
				p.setLife((Player)ev.getEntity(), 0);
			}
		};
		playerdeath.runTaskLater(this.p, 1L);
		BukkitRunnable jayjay = new BukkitRunnable() {
			
			public void run() {
				ev.getEntity().kickPlayer("jayjay");
			}
		};
		if (this.p.getConfig().getBoolean("kick-on-death.kick", true)) {
			jayjay.runTaskLater(this.p, 20L*this.p.getConfig().getInt("kick-on-death.time", 30)
					);
		}
		
		try { 
			ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
			SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
			skullMeta.setOwner(((Player)ev.getEntity()).getName());
			skullMeta.setDisplayName(ChatColor.RESET + ((Player)ev.getEntity()).getName());
			skull.setItemMeta(skullMeta);
			l.getWorld().dropItem(l, skull);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent ev) {
		if (ev.getItem().getItemStack().getType() == Material.GHAST_TEAR && ev.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) ev.setCancelled(true);
		p.updatePlayerListName(ev.getPlayer());
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent ev) {
		//ev.getPlayer().setHealthScale(40);
		//ev.getPlayer().setMaxHealth(40);
		if (this.p.isPlayerDead(ev.getPlayer().getName()) && !this.p.getConfig().getBoolean("allow-reconnect", true)) {
			ev.setResult(Result.KICK_OTHER);
			ev.setKickMessage("Vous étes mort !");
		}
	}
		
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent ev) {
		if (!this.p.isGameRunning()) {
			ev.getPlayer().setGameMode(GameMode.SPECTATOR);
			Location l = ev.getPlayer().getWorld().getSpawnLocation();
			ev.getPlayer().teleport(l.add(0,100,0));
		}
		p.addToScoreboard(ev.getPlayer());
		BukkitRunnable playerjoin = new BukkitRunnable() {
			
			public void run() {
				p.updatePlayerListName(ev.getPlayer());
			}
		};
		playerjoin.runTaskLater(this.p,  1L);
	}
	
	@EventHandler
	public void onBlockBreakEvent(final BlockBreakEvent ev) {
		if (!this.p.isGameRunning()) ev.setCancelled(true);
	}
	
	@EventHandler
	public void onBlockPlaceEvent(final BlockPlaceEvent ev) {
		if (!this.p.isGameRunning()) ev.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent ev) {
		Location l = ev.getTo();
		Integer mapSize = p.getConfig().getInt("map.size");
		Integer halfMapSize = (int) Math.floor(mapSize/2);
		Integer x = l.getBlockX();
		Integer z = l.getBlockZ();
		
		Location spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitXInf = spawn.add(-halfMapSize, 0, 0).getBlockX();
		
		spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitXSup = spawn.add(halfMapSize, 0, 0).getBlockX();
		
		spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitZInf = spawn.add(0, 0, -halfMapSize).getBlockZ();
		
		spawn = ev.getPlayer().getWorld().getSpawnLocation();
		Integer limitZSup = spawn.add(0, 0, halfMapSize).getBlockZ();
		
		if (x < limitXInf || x > limitXSup || z < limitZInf || z > limitZSup) {
			ev.setCancelled(true);
		}
	}	

	@EventHandler
	public void onInventoryClick(InventoryClickEvent ev) {
		if (ev.getInventory().getName().equals("- Teams -")) {
			Player pl = (Player) ev.getWhoClicked();
			ev.setCancelled(true);
			if (ev.getCurrentItem().getType() == Material.DIAMOND) {
				pl.closeInventory();
				p.getConversationFactory("teamPrompt").buildConversation(pl).begin();
			} else if (ev.getCurrentItem().getType() == Material.APPLE) {
				pl.closeInventory();
				Conversation c = p.getConversationFactory("playerPrompt").buildConversation(pl);
				c.getContext().setSessionData("nomTeam", ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName()));
				c.getContext().setSessionData("color", p.getTeam(ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName())).getChatColor());
				c.begin();
			}
		}
                // Param Menu
                if (ev.getInventory().getName().equals("- Menu -")) {
			Player pl = (Player) ev.getWhoClicked();
			ev.setCancelled(true);
			if (ev.getCurrentItem().getType() == Material.DIAMOND) {
				pl.closeInventory();
				p.getConversationFactory("teamPrompt").buildConversation(pl).begin();
			} else if (ev.getCurrentItem().getType() == Material.APPLE) {
				pl.closeInventory();
				Conversation c = p.getConversationFactory("playerPrompt").buildConversation(pl);
				c.getContext().setSessionData("nomTeam", ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName()));
				c.getContext().setSessionData("color", p.getTeam(ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName())).getChatColor());
				c.begin();
			} else if (ev.getCurrentItem().getType() == Material.GOLDEN_APPLE) {
                            pl.closeInventory();
                            this.p.start(pl);
                        }
		}
	}
	
	@EventHandler
	public void onCraftItem(CraftItemEvent ev) {
		try {
			if (ev.getRecipe() instanceof ShapedRecipe) {
				ShapedRecipe r = (ShapedRecipe)ev.getRecipe();
				String item = "la boussole";
				Boolean isCompassValid = false;
				for (Map.Entry<Character, ItemStack> e : r.getIngredientMap().entrySet()) {
					if (r.getResult().getType() == Material.GOLDEN_APPLE && e != null && e.getValue() != null && e.getValue().getType() == Material.GOLD_NUGGET) { //gotta cancel
						item = "la pomme d'or";
						ev.setCancelled(true);
					} else if (r.getResult().getType() == Material.COMPASS && e != null && e.getValue() != null && e.getValue().getType() == Material.BONE) {
						isCompassValid = true;
					}
				}
				if (!p.getConfig().getBoolean("compass")) isCompassValid = true;
				if (!isCompassValid && r.getResult().getType() == Material.COMPASS) ev.setCancelled(true);
				if (ev.isCancelled()) ((Player) ev.getWhoClicked()).sendMessage(ChatColor.RED+"Vous ne pouvez pas crafter "+item+" comme ceci");
			} else if (ev.getRecipe() instanceof ShapelessRecipe) {
				ShapelessRecipe r = (ShapelessRecipe) ev.getRecipe();
				String item = "";
				for (ItemStack i : r.getIngredientList()) {
					if (i.getType() == Material.GOLD_NUGGET && r.getResult().getType() == Material.SPECKLED_MELON) { //gotta cancel
						item = "le melon scintillant";
						ev.setCancelled(true);
					}
				}
				if (ev.isCancelled()) ((Player) ev.getWhoClicked()).sendMessage(ChatColor.RED+"Vous ne pouvez pas crafter "+item+" comme ceci");
			}
		} catch (Exception e) {
			Bukkit.getLogger().warning(ChatColor.RED+"Erreur dans le craft");
			e.printStackTrace();
		}
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent ev) {
		if (ev.getEntity() instanceof Ghast) {
			Bukkit.getLogger().info("Modifying drops for Ghast");
			List<ItemStack> drops = new ArrayList<ItemStack>(ev.getDrops());
			ev.getDrops().clear();
			for (ItemStack i : drops) {
				if (i.getType() == Material.GHAST_TEAR) {
					Bukkit.getLogger().info("Added "+i.getAmount()+" ghast tear(s)");
					ev.getDrops().add(new ItemStack(Material.GOLD_INGOT,i.getAmount()));
				} else {
					Bukkit.getLogger().info("Added "+i.getAmount()+" "+i.getType().toString());
					ev.getDrops().add(i);
				}
			}
		}
	}


	@EventHandler
	public void onEntityDamage(final EntityDamageEvent ev) {
		if (ev.getEntity() instanceof Player) {
			if (!p.isTakingDamage()) ev.setCancelled(true);
			BukkitRunnable listname = new BukkitRunnable() {
				
				public void run() {
					p.updatePlayerListName((Player)ev.getEntity());
				}
			};
			listname.runTaskLater(this.p, 1L);
		}
	}
	
	@EventHandler
	public void onEntityRegainHealth(final EntityRegainHealthEvent ev) {
		if (ev.getRegainReason() == RegainReason.SATIATED) ev.setCancelled(true);
		if (ev.getEntity() instanceof Player) {
			BukkitRunnable listname = new BukkitRunnable() {
				
				public void run() {
					p.updatePlayerListName((Player)ev.getEntity());
				}
			};
			listname.runTaskLater(this.p, 1L);
		}
	}
		
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent ev)
    {
        if ((ev.getAction() == Action.RIGHT_CLICK_AIR || ev.getAction() == Action.RIGHT_CLICK_BLOCK) && ev.getMaterial() == Material.COMPASS && p.getConfig().getBoolean("compass") && !p.isPlayerDead(ev.getPlayer().getName()))
        {
            Player player1 = ev.getPlayer();

            Boolean foundRottenFlesh = false;
            for (ItemStack item : player1.getInventory().getContents())
            {
                if (item != null && item.getType() == Material.ROTTEN_FLESH)
                {
                    if (item.getAmount() != 1)
                    {
                        item.setAmount(item.getAmount() - 1);
                    }
                    else
                    {
                        player1.getInventory().removeItem(item);
                    }

                    player1.updateInventory();
                    foundRottenFlesh = true;
                    break;
                }
            }

            if (!foundRottenFlesh)
            {
                /// Error message if a player tries to use his pointing compass without rotten flesh.
            	//Bukkit.getServer().broadcastMessage(ChatColor.AQUA+"-------- Fin episode "+episode+" --------");
                player1.sendMessage(ChatColor.GRAY+""+ChatColor.ITALIC+"Vous n'avez pas de chair de zombie.");
                player1.playSound(player1.getLocation(), Sound.BLOCK_WOOD_STEP, 1F, 1F);
                return;
            }

            Player nearest = null;
            Double distance = 99999D;
            for (Player player2 : p.getServer().getOnlinePlayers())
            {
                try
                {
                    Double calc = player1.getLocation().distance(player2.getLocation());

                    if (calc > 1 && calc < distance)
                    {
                        distance = calc;
                        if (!player2.getUniqueId().equals(player1.getUniqueId()) && !p.inSameTeam(player1, player2))
                        {
                            nearest = player2.getPlayer();
                        }
                    }
                }
                catch (Exception ignored)
                {

                }
            }

            if (nearest == null)
            {
                /// Error message if a player tries to use his pointing compass without a player nearby.
                player1.sendMessage(ChatColor.GRAY+""+ChatColor.ITALIC+"Seul le silence comble votre requéte.");

                player1.playSound(player1.getLocation(), Sound.BLOCK_WOOD_STEP, 1F, 1F);
                return;
            }

            /// Success message when a player uses his pointing compass.
            player1.sendMessage(ChatColor.GRAY+"La boussole pointe sur le joueur le plus proche.");
            player1.setCompassTarget(nearest.getLocation());

            player1.playSound(player1.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 1F, 1F);
        }
    }
	
	@EventHandler
	public void onWeatherChange(WeatherChangeEvent ev) {
		if (!p.getConfig().getBoolean("weather")) {
			ev.setCancelled(true);
		}
	}
}
