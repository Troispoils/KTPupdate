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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;


public class KTPTeam {
	private String name;
	private String displayName;
	private ChatColor color;
	private KTPupdate plugin;
	private ArrayList<Player> players = new ArrayList<Player>();
	
	public KTPTeam(String name, String displayName, ChatColor color, KTPupdate plugin) {
		this.name = name;
		this.displayName = displayName;
		this.color = color;
		this.plugin = plugin;
		
		Scoreboard sb = this.plugin.getScoreboard();
		sb.registerNewTeam(this.name);
	
		Team t = sb.getTeam(this.name);
		t.setDisplayName(this.displayName);
		t.setCanSeeFriendlyInvisibles(true);
		t.setPrefix(this.color+"");
	}
	
	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public ArrayList<Player> getPlayers() {
		return players;
	}

	public void addPlayer(Player playerExact) {
		players.add(playerExact);
		plugin.getScoreboard().getTeam(this.name).addEntry(playerExact.getName());;
	}

	public void teleportTo(Location lo) {
		for (Player p : players) {
			p.teleport(lo);
		}
	}

	public ChatColor getChatColor() {
		return color;
	}
	
}