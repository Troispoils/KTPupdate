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
        private ArrayList<ItemStack> is = new ArrayList<ItemStack>(); 
        private ArrayList<ItemMeta> im = new ArrayList<ItemMeta>(); 
        private Server server;
        
        public KTPmenu(){
            is.add(new ItemStack(Material.APPLE));
            is.add(new ItemStack(Material.GOLDEN_APPLE));
            is.add(new ItemStack(Material.BUCKET));
            is.add(new ItemStack(Material.DIAMOND));
            is.add(new ItemStack(Material.REDSTONE));
            
            for(ItemStack list : is)
            {
                im.add(list.getItemMeta());
            }
            
            
            //Probleme de name pas bon MDR
            int i = 0;
            for(ItemMeta list : im)
            { 
                if(i == 0){
                    list.setDisplayName(ChatColor.GREEN+"Start");
                }
                else if (i == 1){
                    list.setDisplayName(ChatColor.GREEN+"Start Auto");
                }
                else if (i == 2){
                    list.setDisplayName(ChatColor.GREEN+"Border");
                }
                else if (i == 3){
                    list.setDisplayName(ChatColor.GREEN+"Team");
                }
                else if (i == 4){
                    list.setDisplayName(ChatColor.GREEN+"Random Team");
                }
                i++;
            }
            
            for(ItemStack ListStack : is)
            {
                for(ItemMeta ListMeta : im)
                {
                    ListStack.setItemMeta(ListMeta);
                }
            }
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
