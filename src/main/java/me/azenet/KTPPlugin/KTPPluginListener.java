package me.azenet.KTPPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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

    KTPPlugin plugin;

    public KTPPluginListener(KTPPlugin pl) {
        this.plugin = pl;
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent ev) {
        Location l = ev.getEntity().getLocation();
        Player[] ps = Bukkit.getServer().getOnlinePlayers();
        for (Player pp : ps) {
            pp.playSound(pp.getLocation(), Sound.WITHER_SPAWN, 1F, 1F);
        }
        plugin.addDead(ev.getEntity().getName());

        if (plugin.getConfiguration().getBoolean("kick-on-death.kick", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, new BukkitRunnable() {
                @Override
                public void run() {
                    ev.getEntity().kickPlayer("Jay Jay !");
                }
            }, 20L * plugin.getConfiguration().getInt("kick-on-death.time", 30));
        }

        try {
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            skullMeta.setOwner(((Player) ev.getEntity()).getName());
            skullMeta.setDisplayName(ChatColor.RESET + ((Player) ev.getEntity()).getName());
            skull.setItemMeta(skullMeta);
            l.getWorld().dropItem(l, skull);
        } catch (Exception e) {
        }

    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent ev) {
        if (!plugin.isGameRunning()) {
            ev.setCancelled(true);
        }

        if (ev.getItem().getItemStack().getType() == Material.GHAST_TEAR && ev.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent ev) {
        if (plugin.isPlayerDead(ev.getPlayer().getName()) && !plugin.getConfiguration().getBoolean("allow-reconnect", true)) {
            ev.setResult(Result.KICK_OTHER);
            ev.setKickMessage("Vous êtes mort !");
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent ev) {
        if (!plugin.isGameRunning()) {
            ev.getPlayer().setGameMode(GameMode.ADVENTURE);
            Location l = ev.getPlayer().getWorld().getSpawnLocation();
            ev.getPlayer().teleport(l.add(0, 1, 0));
        }
        plugin.addToScoreboard(ev.getPlayer());
    }

    @EventHandler
    public void onBlockBreakEvent(final BlockBreakEvent ev) {
        if (!plugin.isGameRunning()) {
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlaceEvent(final BlockPlaceEvent ev) {
        if (!plugin.isGameRunning()) {
            ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent ev) {
        Location l = ev.getTo();
        Integer mapSize = plugin.getConfiguration().getInt("map.size");
        Integer halfMapSize = (int) Math.floor(mapSize / 2);
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
        if (ev.getInventory().getName().equals("Liste des teams")) {
            // Récupération du joueur
            Player pl = (Player) ev.getWhoClicked();

            // Annulation de l'event
            ev.setCancelled(true);

            if (ev.getCurrentItem().getType() == Material.DIAMOND) {
                // Ouverture de l'inventaire
                pl.closeInventory();
                //p.createTeamCreateInventory(pl);
            } else if (ev.getCurrentItem().getType() == Material.BEACON) {
                pl.closeInventory();
                Conversation c = plugin.getConversationFactory("playerPrompt").buildConversation(pl);
                c.getContext().setSessionData("nomTeam", ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName()));
                c.getContext().setSessionData("color", plugin.getTeam(ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName())).getChatColor());
                c.begin();
            }
        }

        if (ev.getInventory().getName().equals("Création d'une team")) {
            Player pl = (Player) ev.getWhoClicked();
            ev.setCancelled(true);
            if (ev.getCurrentItem().getType() == Material.DIAMOND) {
                pl.closeInventory();
                plugin.getConversationFactory("teamPrompt").buildConversation(pl).begin();
            } else if (ev.getCurrentItem().getType() == Material.BEACON) {
                pl.closeInventory();
                Conversation c = plugin.getConversationFactory("playerPrompt").buildConversation(pl);
                c.getContext().setSessionData("nomTeam", ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName()));
                c.getContext().setSessionData("color", plugin.getTeam(ChatColor.stripColor(ev.getCurrentItem().getItemMeta().getDisplayName())).getChatColor());
                c.begin();
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent ev) {
        try {
            if (ev.getRecipe() instanceof ShapedRecipe) {
                ShapedRecipe r = (ShapedRecipe) ev.getRecipe();
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
                if (!plugin.getConfiguration().getBoolean("compass")) {
                    isCompassValid = true;
                }
                if (!isCompassValid && r.getResult().getType() == Material.COMPASS) {
                    ev.setCancelled(true);
                }
                if (ev.isCancelled()) {
                    ((Player) ev.getWhoClicked()).sendMessage(ChatColor.RED + "Vous ne pouvez pas crafter " + item + " comme ceci");
                }
            } else if (ev.getRecipe() instanceof ShapelessRecipe) {
                ShapelessRecipe r = (ShapelessRecipe) ev.getRecipe();
                String item = "";
                for (ItemStack i : r.getIngredientList()) {
                    if (i.getType() == Material.GOLD_NUGGET && r.getResult().getType() == Material.SPECKLED_MELON) { //gotta cancel
                        item = "le melon scintillant";
                        ev.setCancelled(true);
                    }
                }
                if (ev.isCancelled()) {
                    ((Player) ev.getWhoClicked()).sendMessage(ChatColor.RED + "Vous ne pouvez pas crafter " + item + " comme ceci");
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "{0}Erreur dans le craft", ChatColor.RED);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent ev) {
        if (!plugin.isGameRunning()) {
            ev.setDroppedExp(0);
            ev.getDrops().clear();
        }

        if (ev.getEntity() instanceof Ghast) {
            Bukkit.getLogger().info("Modifying drops for Ghast");
            List<ItemStack> drops = new ArrayList<ItemStack>(ev.getDrops());
            ev.getDrops().clear();
            for (ItemStack i : drops) {
                if (i.getType() == Material.GHAST_TEAR) {
                    Bukkit.getLogger().log(Level.INFO, "Added {0} ghast tear(s)", i.getAmount());
                    ev.getDrops().add(new ItemStack(Material.GOLD_INGOT, i.getAmount()));
                } else {
                    Bukkit.getLogger().log(Level.INFO, "Added {0} {1}", new Object[]{i.getAmount(), i.getType().toString()});
                    ev.getDrops().add(i);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent ev) {
        if (ev.getEntity() instanceof Player) {
            if (!plugin.isTakingDamage()) {
                ev.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent ev) {
        if ((ev.getAction() == Action.RIGHT_CLICK_AIR || ev.getAction() == Action.RIGHT_CLICK_BLOCK) && ev.getPlayer().getItemInHand().getType() == Material.COMPASS && plugin.getConfiguration().getBoolean("compass")) {
            Player pl = ev.getPlayer();
            Boolean foundRottenFlesh = false;
            for (ItemStack is : pl.getInventory().getContents()) {
                if (is != null && is.getType() == Material.ROTTEN_FLESH) {
                    plugin.getLogger().log(Level.INFO, "{0}", is.getAmount());
                    if (is.getAmount() != 1) {
                        is.setAmount(is.getAmount() - 1);
                    } else {
                        plugin.getLogger().info("lol");
                        pl.getInventory().removeItem(is);
                    }
                    pl.updateInventory();
                    foundRottenFlesh = true;
                    break;
                }
            }
            if (!foundRottenFlesh) {
                pl.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Vous n'avez pas de chair de zombie.");
                pl.playSound(pl.getLocation(), Sound.STEP_WOOD, 1F, 1F);
                return;
            }
            pl.playSound(pl.getLocation(), Sound.BURP, 1F, 1F);
            Player nearest = null;
            Double distance = 99999D;
            for (Player pl2 : plugin.getServer().getOnlinePlayers()) {
                try {
                    Double calc = pl.getLocation().distance(pl2.getLocation());
                    if (calc > 1 && calc < distance) {
                        distance = calc;
                        if (pl2 != pl && !plugin.inSameTeam(pl, pl2)) {
                            nearest = pl2.getPlayer();
                        }
                    }
                } catch (Exception e) {
                }
            }
            if (nearest == null) {
                pl.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Seul le silence comble votre requête.");
                return;
            }
            pl.sendMessage(ChatColor.GRAY + "La boussole pointe sur le joueur le plus proche.");
            pl.setCompassTarget(nearest.getLocation());
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent ev) {
        if (!plugin.getConfiguration().getBoolean("weather")) {
            ev.setCancelled(true);
        }
    }
}
