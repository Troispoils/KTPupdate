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
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class KTPmenu {
        //private Inventory menu;
        private Integer slot = 0;
        private Server server;
        private ItemMeta start, team, randteam;
        private ItemStack applegold, diamond, bucket;

        public KTPmenu(){
            applegold = new ItemStack(Material.GOLDEN_APPLE);
            diamond = new ItemStack(Material.DIAMOND);
            bucket = new ItemStack(Material.BUCKET);
            
            start = applegold.getItemMeta();
            team = diamond.getItemMeta();
            randteam = bucket.getItemMeta();
            
            start.setDisplayName(ChatColor.GREEN+"Start");
            team.setDisplayName(ChatColor.GREEN+"Team");
            randteam.setDisplayName(ChatColor.GREEN+"Random Team");
            
            applegold.setItemMeta(start);
            diamond.setItemMeta(team);
            bucket.setItemMeta(randteam);
        }
        
        public void openKTPmenu(Server srv, Player pl, ArrayList<KTPTeam> teams)
        {
            Inventory menu = srv.createInventory(pl, 54, "- Menu -");
            Integer slot = 0;
            ItemStack it = null;
            for (KTPTeam t : teams) {
                    it = new ItemStack(Material.APPLE/*, t.getPlayers().size()*/);
                    ItemMeta itm = it.getItemMeta();
                    itm.setDisplayName(t.getChatColor()+t.getDisplayName());
                    ArrayList<String> lore = new ArrayList<String>();
                    for (Player p : t.getPlayers()) {
                            lore.add("- "+p.getDisplayName());
                    }
                    itm.setLore(lore);
                    it.setItemMeta(itm);
                    menu.setItem(slot, it);
                    slot++;
                    pl.sendMessage(ChatColor.GRAY+"Ajout de l'equipe dans l'inventaire.");
            }
            
            slot = 45;
            for(ItemStack List : is)
            {
                menu.setItem(slot, List);
                slot++;
            }
            
            pl.openInventory(menu);
        }
}
