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
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;

public final class KTPupdate extends JavaPlugin implements ConversationAbandonedListener {

	private Logger logger = null;
	private LinkedList<Location> loc = new LinkedList<Location>();
	private Random random = null;
	private ShapelessRecipe goldenMelon = null;
	private ShapedRecipe compass = null;
	private Integer episode = 0;
	private Boolean gameRunning = false;
	private Scoreboard sb = null;
	private Integer minutesLeft = 0;
	private Integer secondsLeft = 0;
	private NumberFormat formatter = new DecimalFormat("00");
	private String sbobjname = "KTP";
	private Boolean damageIsOn = false;
	private ArrayList<KTPTeam> teams = new ArrayList<KTPTeam>();
	private HashMap<String, ConversationFactory> cfs = new HashMap<String, ConversationFactory>();
	private KTPPrompts uhp = null;
	private HashSet<String> deadPlayers = new HashSet<String>();
        private KTPmenu menu = new KTPmenu();
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
                
                File positions = new File("plugins/KTPupdate/positions.txt");
		if (positions.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(positions));
				String line;
				while ((line = br.readLine()) != null) {
					String[] l = line.split(",");
					getLogger().info("Adding position "+Integer.parseInt(l[0])+","+Integer.parseInt(l[1])+" from positions.txt");
					addLocation(Integer.parseInt(l[0]), Integer.parseInt(l[1]));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try { if (br != null) br.close(); }
				catch (Exception e) { e.printStackTrace(); } //c trÃ© l'inline
			}
			
		}
		uhp = new KTPPrompts(this);
		logger = Bukkit.getLogger();
		logger.info("KTPupdate loaded");
		random = new Random();
		
		goldenMelon = new ShapelessRecipe(new ItemStack(Material.SPECKLED_MELON));
		goldenMelon.addIngredient(1, Material.GOLD_BLOCK);
		goldenMelon.addIngredient(1, Material.MELON);
		this.getServer().addRecipe(goldenMelon);
		
		if (getConfig().getBoolean("compass")) {
			compass = new ShapedRecipe(new ItemStack(Material.COMPASS));
			compass.shape(new String[] {"CIE", "IRI", "BIF"});
			compass.setIngredient('I', Material.IRON_INGOT);
			compass.setIngredient('R', Material.REDSTONE);
			compass.setIngredient('C', Material.SULPHUR);
			compass.setIngredient('E', Material.SPIDER_EYE);
			compass.setIngredient('B', Material.BONE);
			compass.setIngredient('F', Material.ROTTEN_FLESH);
			this.getServer().addRecipe(compass);
		}
		
		getServer().getPluginManager().registerEvents(new KTPPluginListener(this), this);
		
		
		sb = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
		Objective obj = sb.registerNewObjective("Vie", "health");
		obj.setDisplayName("Vie");
		obj.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		
		setMatchInfo();
		
		getServer().getWorlds().get(0).setGameRuleValue("doDaylightCycle", "false");
		getServer().getWorlds().get(0).setTime(6000L);
		getServer().getWorlds().get(0).setStorm(false);
		getServer().getWorlds().get(0).setDifficulty(Difficulty.HARD);
		
		cfs.put("teamPrompt", new ConversationFactory(this)
		.withModality(true)
		.withFirstPrompt(uhp.getTNP())
		.withEscapeSequence("/cancel")
		.thatExcludesNonPlayersWithMessage("Il faut étre un joueur ingame.")
		.withLocalEcho(false)
		.addConversationAbandonedListener(this));
		
		cfs.put("playerPrompt", new ConversationFactory(this)
		.withModality(true)
		.withFirstPrompt(uhp.getPP())
		.withEscapeSequence("/cancel")
		.thatExcludesNonPlayersWithMessage("Il faut étre un joueur ingame.")
		.withLocalEcho(false)
		.addConversationAbandonedListener(this));
	}
	
	
	public void addLocation(int x, int z) {
		loc.add(new Location(getServer().getWorlds().get(0), x, getServer().getWorlds().get(0).getHighestBlockYAt(x,z)+120, z));
	}
	
	public void setMatchInfo() {
		Objective obj = null;
		try {
			obj = sb.getObjective(sbobjname);
			obj.setDisplaySlot(null);
			obj.unregister();
		} catch (Exception e) {
			
		}
		Random r = new Random();
		sbobjname = "KTP"+r.nextInt(10000000);
		obj = sb.registerNewObjective(sbobjname, "dummy");
		obj = sb.getObjective(sbobjname);

		obj.setDisplayName(this.getScoreboardName());
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		obj.getScore(ChatColor.GRAY+"Episode "+ChatColor.WHITE+episode).setScore(5);
		obj.getScore(ChatColor.WHITE+""+Bukkit.getServer().getOnlinePlayers().size()+ChatColor.GRAY+" joueurs").setScore(4);
		obj.getScore(ChatColor.WHITE+""+getAliveTeams().size()+ChatColor.GRAY+" teams").setScore(3);
		obj.getScore("").setScore(2);
		obj.getScore(ChatColor.WHITE+formatter.format(this.minutesLeft)+ChatColor.GRAY+":"+ChatColor.WHITE+formatter.format(this.secondsLeft)).setScore(1);
	}

	private ArrayList<KTPTeam> getAliveTeams() {
		ArrayList<KTPTeam> aliveTeams = new ArrayList<KTPTeam>();
		for (KTPTeam t : teams) {
			for (Player p : t.getPlayers()) {
				if (p.isOnline() && !aliveTeams.contains(t)) aliveTeams.add(t);
			}
		}
		return aliveTeams;
	}

	@Override
	public void onDisable() {
		logger.info("KTPupdate unloaded");
	}
	
	public boolean onCommand(final CommandSender s, Command c, String l, String[] a) {
		if (c.getName().equalsIgnoreCase("uh")) {
			if (!(s instanceof Player)) {
				s.sendMessage(ChatColor.RED+"Vous devez étre un joueur");
				return true;
			}
			Player pl = (Player)s;
			if (!pl.isOp()) {
				pl.sendMessage(ChatColor.RED+"Lolnope.");
				return true;
			}
			if (a.length == 0) {
				pl.sendMessage("Usage : /uh <start|shift|team|addspawn|generatewalls>");
				return true;
			}
			if (a[0].equalsIgnoreCase("start")) {
				if (teams.size() == 0) {
					for (Player p : getServer().getOnlinePlayers()) {
						KTPTeam uht = new KTPTeam(p.getName(), p.getName(), ChatColor.WHITE, this);
						uht.addPlayer(p);
						teams.add(uht);
					}
				}
				if (loc.size() < teams.size()) {
					s.sendMessage(ChatColor.RED+"Pas assez de positions de TP");
					return true;
				}
				LinkedList<Location> unusedTP = loc;
				for (final KTPTeam t : teams) {
					final Location lo = unusedTP.get(this.random.nextInt(unusedTP.size()));
					
					
					BukkitRunnable start = new BukkitRunnable() {

						public void run() {
							t.teleportTo(lo);
							for (Player p : t.getPlayers()) {
								p.setGameMode(GameMode.SURVIVAL);
								p.setHealth(20);
								p.setFoodLevel(20);
								p.setExhaustion(5F);
								p.getInventory().clear();
								p.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), 
										new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
								p.setExp(0L+0F);
								p.setLevel(0);
								p.closeInventory();
								p.getActivePotionEffects().clear();
								p.setCompassTarget(lo);
								setLife(p, 20);
							}
						}
					}; 					
					start.runTaskLater(this, 10L);					
					unusedTP.remove(lo);
				}
				BukkitRunnable damageOn = new BukkitRunnable() {

					public void run() {
						damageIsOn = true;
					}
				};
				damageOn.runTaskLater(this, 600L);
				
				World w = Bukkit.getWorlds().get(0);
				w.setGameRuleValue("doDaylightCycle", ((Boolean)getConfig().getBoolean("daylightCycle.do")).toString());
				w.setTime(getConfig().getLong("daylightCycle.time"));
				w.setStorm(false);
				w.setDifficulty(Difficulty.HARD);
				this.episode = 1;
				this.minutesLeft = getEpisodeLength();
				this.secondsLeft = 0;
				
				BukkitRunnable episodeTimer = new BukkitRunnable() {
					public void run() {
						setMatchInfo();
						secondsLeft--;
						if (secondsLeft == -1) {
							minutesLeft--;
							secondsLeft = 59;
						}
						if (minutesLeft == -1) {
							minutesLeft = getEpisodeLength();
							secondsLeft = 0;
							Bukkit.getServer().broadcastMessage(ChatColor.AQUA+"-------- Fin episode "+episode+" --------");
							shiftEpisode();
						}
					} 
				};
				episodeTimer.runTaskTimer(this, 20L, 20L);
				
				
				Bukkit.getServer().broadcastMessage(ChatColor.GREEN+"--- GO ---");
				this.gameRunning = true;
				return true;
			} else if (a[0].equalsIgnoreCase("shift")) {
				Bukkit.getServer().broadcastMessage(ChatColor.AQUA+"-------- Fin episode "+episode+" [forcé par "+s.getName()+"] --------");
				shiftEpisode();
				this.minutesLeft = getEpisodeLength();
				this.secondsLeft = 0;
				return true;
                        } else if (a[0].equalsIgnoreCase("menu")) {
                                menu.openKTPmenu(this.getServer(), pl, teams);
			} else if (a[0].equalsIgnoreCase("team")) {
				Inventory iv = this.getServer().createInventory(pl, 54, "- Teams -");
				Integer slot = 0;
				ItemStack is = null;
				for (KTPTeam t : teams) {
					is = new ItemStack(Material.APPLE/*, t.getPlayers().size()*/);
					ItemMeta im = is.getItemMeta();
					im.setDisplayName(t.getChatColor()+t.getDisplayName());
					ArrayList<String> lore = new ArrayList<String>();
					for (Player p : t.getPlayers()) {
						lore.add("- "+p.getDisplayName());
					}
					im.setLore(lore);
					is.setItemMeta(im);
					iv.setItem(slot, is);
					slot++;
					pl.sendMessage(ChatColor.GRAY+"Ajout de l'equipe dans l'inventaire.");
				}
				
				ItemStack is2 = new ItemStack(Material.DIAMOND);
				ItemMeta im2 = is2.getItemMeta();
				im2.setDisplayName(ChatColor.AQUA+""+ChatColor.ITALIC+"Créer une team");
				is2.setItemMeta(im2);
				iv.setItem(53, is2);
				
				pl.openInventory(iv);
				return true;
			/*} else if (a[0].equalsIgnoreCase("genteam")) {
					createTeam("Un", ChatColor.BLUE, pl);
					createTeam("Deux", ChatColor.RED, pl);
					createTeam("Trois", ChatColor.GREEN, pl);
					createTeam("Quatre", ChatColor.YELLOW, pl);
				return true;*/
			} else if (a[0].equalsIgnoreCase("addspawn")) {
				addLocation(pl.getLocation().getBlockX(), pl.getLocation().getBlockZ());
				pl.sendMessage(ChatColor.DARK_GRAY+"Position ajoutée: "+ChatColor.GRAY+pl.getLocation().getBlockX()+","+pl.getLocation().getBlockZ());
				return true;
			} else if (a[0].equalsIgnoreCase("randspawn")) {
				for(int i=1; i <= this.getServer().getOnlinePlayers().size(); i++)
				{
					int x = (int)(Math.random() * (999-(-999))) + -999;
					int y = (int)(Math.random() * (999-(-999))) + -999;
					addLocation(x, y);
					pl.sendMessage(ChatColor.DARK_GRAY+"Position ajoutée: "+ChatColor.GRAY+x+","+y);
				}
				return true;
			} else if (a[0].equalsIgnoreCase("genborder")) {
				pl.sendMessage(ChatColor.GRAY+"Génération en cours...");
				try {
					World w = pl.getWorld();
					WorldBorder border = w.getWorldBorder();
					
					border.setCenter(0, 0);
					border.setSize(2000);
				} catch (Exception e) {
					e.printStackTrace();
					pl.sendMessage(ChatColor.RED+"Echec génération. Voir console pour détails.");
					return true;
				}
				pl.sendMessage(ChatColor.GRAY+"Génération terminée.");
				return true;
			}
		}
		return false;
	}
	
	public void shiftEpisode() {
		this.episode++;
	}
        
        public boolean start(Player player) {
            if (teams.size() == 0) {
					for (Player p : getServer().getOnlinePlayers()) {
						KTPTeam uht = new KTPTeam(p.getName(), p.getName(), ChatColor.WHITE, this);
						uht.addPlayer(p);
						teams.add(uht);
					}
				}
				if (loc.size() < teams.size()) {
					player.sendMessage(ChatColor.RED+"Pas assez de positions de TP");
					return true;
				}
				LinkedList<Location> unusedTP = loc;
				for (final KTPTeam t : teams) {
					final Location lo = unusedTP.get(this.random.nextInt(unusedTP.size()));
					
					
					BukkitRunnable start = new BukkitRunnable() {

						public void run() {
							t.teleportTo(lo);
							for (Player p : t.getPlayers()) {
								p.setGameMode(GameMode.SURVIVAL);
								p.setHealth(20);
								p.setFoodLevel(20);
								p.setExhaustion(5F);
								p.getInventory().clear();
								p.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), 
										new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
								p.setExp(0L+0F);
								p.setLevel(0);
								p.closeInventory();
								p.getActivePotionEffects().clear();
								p.setCompassTarget(lo);
								setLife(p, 20);
							}
						}
					}; 					
					start.runTaskLater(this, 10L);					
					unusedTP.remove(lo);
				}
				BukkitRunnable damageOn = new BukkitRunnable() {

					public void run() {
						damageIsOn = true;
					}
				};
				damageOn.runTaskLater(this, 600L);
				
				World w = Bukkit.getWorlds().get(0);
				w.setGameRuleValue("doDaylightCycle", ((Boolean)getConfig().getBoolean("daylightCycle.do")).toString());
				w.setTime(getConfig().getLong("daylightCycle.time"));
				w.setStorm(false);
				w.setDifficulty(Difficulty.HARD);
				this.episode = 1;
				this.minutesLeft = getEpisodeLength();
				this.secondsLeft = 0;
				
				BukkitRunnable episodeTimer = new BukkitRunnable() {
					public void run() {
						setMatchInfo();
						secondsLeft--;
						if (secondsLeft == -1) {
							minutesLeft--;
							secondsLeft = 59;
						}
						if (minutesLeft == -1) {
							minutesLeft = getEpisodeLength();
							secondsLeft = 0;
							Bukkit.getServer().broadcastMessage(ChatColor.AQUA+"-------- Fin episode "+episode+" --------");
							shiftEpisode();
						}
					} 
				};
				episodeTimer.runTaskTimer(this, 20L, 20L);
				
				
				Bukkit.getServer().broadcastMessage(ChatColor.GREEN+"--- GO ---");
				this.gameRunning = true;
				return true;
        }
	
	public boolean isGameRunning() {
		return this.gameRunning;
	}

	public void updatePlayerListName(Player p) {
		p.setScoreboard(sb);
		Integer he = (int) Math.round(p.getHealth());
		sb.getObjective("Vie").getScore(p.getName()).setScore(he);
	}

	public void addToScoreboard(Player player) {
		player.setScoreboard(sb);
		sb.getObjective("Vie").getScore(player.getName()).setScore(0);
		this.updatePlayerListName(player);
	}

	public void setLife(Player entity, int i) {
		entity.setScoreboard(sb);
		sb.getObjective("Vie").getScore(entity.getName()).setScore(i);
	}

	public boolean isTakingDamage() {
		return damageIsOn;
	}
	
	public Scoreboard getScoreboard() {
		return sb;
	}
	
	public KTPTeam getTeam(String name) {
		for(KTPTeam t : teams) {
			if (t.getName().equalsIgnoreCase(name)) return t;
		}
		return null;
	}

	public KTPTeam getTeamForPlayer(Player p) {
		for(KTPTeam t : teams) {
			if (t.getPlayers().contains(p)) return t;
		}
		return null;
	}
	
	public Integer getEpisodeLength() {
		return this.getConfig().getInt("episodeLength");
	}

	public void conversationAbandoned(ConversationAbandonedEvent abandonedEvent) {
		if (!abandonedEvent.gracefulExit()) {
			abandonedEvent.getContext().getForWhom().sendRawMessage(ChatColor.RED+"Abandonné par "+abandonedEvent.getCanceller().getClass().getName());
		}		
	}
	
	public boolean createTeam(String name, ChatColor color) {
		if (teams.size() <= 50) {
			teams.add(new KTPTeam(name, name, color, this));
			//pl.sendMessage(ChatColor.GRAY+"Team creer.");
			return true;
		}
		//pl.sendMessage(ChatColor.GRAY+"Team non creer.");
		return false;
	}
	public ConversationFactory getConversationFactory(String string) {
		if (cfs.containsKey(string)) return cfs.get(string);
		return null;
	}
	
	public boolean isPlayerDead(String name) {
		return deadPlayers.contains(name);
	}
	
	public void addDead(String name) {
		deadPlayers.add(name);
	}
	
	public String getScoreboardName() {
		String s = this.getConfig().getString("scoreboard", "Kill The Patrick");
		return s.substring(0, Math.min(s.length(), 16));
	}


	public boolean inSameTeam(Player pl, Player pl2) {
		return (getTeamForPlayer(pl).equals(getTeamForPlayer(pl2)));
	}
	
}
