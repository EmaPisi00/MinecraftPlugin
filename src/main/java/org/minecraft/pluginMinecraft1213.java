package org.minecraft;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class pluginMinecraft1213 extends JavaPlugin implements TabCompleter {

    private final HashMap<String, Location> warps = new HashMap<>();
    private File warpFile;
    private FileConfiguration warpConfig;

    @Override
    public void onEnable() {
        getLogger().info("WarpPlugin abilitato!");

        // Inizializza i file e carica i warp
        loadWarps();

        if (warpConfig == null) {
            getLogger().severe("Errore: warpConfig non inizializzato. Controlla la configurazione!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registra il completamento automatico per il comando /warp
        Objects.requireNonNull(getCommand("warp")).setTabCompleter(this);


        // Registra il completamento automatico per il comando /delwarp
        Objects.requireNonNull(getCommand("delwarp")).setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Salvataggio dei warp...");
        if (warpConfig != null) {
            saveWarps(); // Salva i warp solo se warpConfig è inizializzato
        } else {
            getLogger().warning("Impossibile salvare i warp: warpConfig è null.");
        }
        getLogger().info("WarpPlugin disabilitato!");
    }

    private void loadWarps() {
        try {
            warpFile = new File(getDataFolder(), "warps.yml");
            if (!warpFile.exists()) {
                warpFile.getParentFile().mkdirs();
                warpFile.createNewFile(); // Crea il file se non esiste
            }
            warpConfig = YamlConfiguration.loadConfiguration(warpFile);

            // Carica i warp dalla configurazione
            for (String key : warpConfig.getKeys(false)) {
                double x = warpConfig.getDouble(key + ".x");
                double y = warpConfig.getDouble(key + ".y");
                double z = warpConfig.getDouble(key + ".z");
                String worldName = warpConfig.getString(key + ".world");

                if (worldName != null) {
                    World world = getServer().getWorld(worldName);
                    if (world != null) {
                        Location loc = new Location(world, x, y, z);
                        warps.put(key, loc);
                    } else {
                        getLogger().warning("Mondo non trovato per il warp '" + key + "'.");
                    }
                }
            }
            getLogger().info("Warp caricati con successo!");
        } catch (Exception e) {
            getLogger().severe("Errore durante il caricamento dei warp: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveWarps() {
        try {
            for (String key : warps.keySet()) {
                Location loc = warps.get(key);
                warpConfig.set(key + ".x", loc.getX());
                warpConfig.set(key + ".y", loc.getY());
                warpConfig.set(key + ".z", loc.getZ());
                warpConfig.set(key + ".world", loc.getWorld().getName());
            }
            warpConfig.save(warpFile);
            getLogger().info("Warp salvati con successo!");
        } catch (Exception e) {
            getLogger().severe("Errore durante il salvataggio dei warp: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cancellaWarps(String warpName) {
        try {
            if (warpConfig.contains(warpName)) {
                warpConfig.set(warpName, null);  // Rimuove la voce dal file
                warpConfig.save(warpFile);        // Salva il file dopo aver rimosso il warp
                getLogger().info("Warp '" + warpName + "' eliminato dal file con successo!");
            } else {
                getLogger().warning("Il warp '" + warpName + "' non esiste nel file!");
            }
        } catch (Exception e) {
            getLogger().severe("Errore durante l'eliminazione del warp '" + warpName + "' dal file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Comando per creare un nuovo warp
        if (command.getName().equalsIgnoreCase("setNewWarp")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Solo i giocatori possono usare questo comando!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Utilizzo corretto: /setNewWarp <nomeWarp>");
                return true;
            }

            String warpName = args[0].toLowerCase();

            if (warps.containsKey(warpName)) {
                player.sendMessage(ChatColor.RED + "Warp già esistente con questo nome: " + warpName);
                return true;
            }

            warps.put(warpName, player.getLocation());
            saveWarps();
            player.sendMessage(ChatColor.GREEN + "Warp '" + warpName + "' impostato con successo!");
            return true;
        }

        // Comando per tipparsi al warp voluto
        if (command.getName().equalsIgnoreCase("warp")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Solo i giocatori possono usare questo comando!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Utilizzo corretto: /warp <nomeWarp>");
                return true;
            }

            String warpName = args[0].toLowerCase();
            if (!warps.containsKey(warpName)) {
                player.sendMessage(ChatColor.RED + "Il warp '" + warpName + "' non esiste!");
                return true;
            }

            Location loc = warps.get(warpName);
            player.teleport(loc);
            player.sendMessage(ChatColor.GREEN + "Teletrasportato al warp '" + warpName + "'!");
            return true;
        }

        // Comando per cancellare il warp scelto
        if (command.getName().equalsIgnoreCase("delWarp")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Utilizzo corretto: /delWarp <nomeWarp>");
                return true;
            }

            String warpName = args[0].toLowerCase();
            if (warps.remove(warpName) != null) {
                cancellaWarps(warpName);
                sender.sendMessage(ChatColor.GREEN + "Warp '" + warpName + "' eliminato con successo!");
            } else {
                sender.sendMessage(ChatColor.RED + "Il warp '" + warpName + "' non esiste!");
            }
            return true;
        }

        // Comando per vedere la lista di warp disponibili
        if (command.getName().equalsIgnoreCase("warpList")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Solo i giocatori possono usare questo comando!");
                return true;
            }

            if (warps.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Nessun warp disponibile!");
            } else {
                player.sendMessage(ChatColor.GREEN + "Warp disponibili: " + String.join(", ", warps.keySet()));
            }
            return true;
        }

        // Givva un armorstand
        if (command.getName().equalsIgnoreCase("giveArmorStand")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Solo i giocatori possono usare questo comando!");
                return true;
            }

            // Ottieni la posizione del giocatore
            Location location = player.getLocation();

            // Crea un ArmorStand
            ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

            // Configura l'ArmorStand
            armorStand.setArms(true); // Aggiungi le braccia per consentire di equipaggiare una spada
            armorStand.setBasePlate(false); // Rimuovi la base per estetica
            armorStand.setGravity(false); // Evita che cada
            armorStand.setCustomNameVisible(false);

            // Rimuovi ogni equipaggiamento
            armorStand.getEquipment().clear();

            player.sendMessage(ChatColor.GREEN + "ArmorStand vuoto creato con successo!");
            return true;
        }

        if (label.equalsIgnoreCase("gethead")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Controlla i parametri del comando
                if (args.length == 0) {
                    // Caso senza parametri: dai la testa del giocatore stesso
                    givePlayerHead(player, player.getName());
                } else if (args.length == 1) {
                    // Caso con un parametro: dai la testa del giocatore specificato
                    String targetName = args[0];
                    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

                    if (targetPlayer.hasPlayedBefore() || targetPlayer.isOnline()) {
                        givePlayerHead(player, targetName);
                    } else {
                        player.sendMessage(ChatColor.RED + "Il giocatore " + targetName + " non esiste o non è mai entrato nel server!");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Utilizzo corretto: /gethead [nomeGiocatore]");
                }

            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("warp")) {
            if (args.length == 1) {
                String partialWarpName = args[0].toLowerCase();
                return warps.keySet().stream()
                        .filter(warpName -> warpName.toLowerCase().startsWith(partialWarpName))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }
        return null;
    }

    private void givePlayerHead(Player receiver, String playerName) {
        // Crea la testa del giocatore specificato
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

        // Imposta il proprietario della testa
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        skullMeta.setOwningPlayer(targetPlayer);

        // Aggiungi un nome personalizzato alla testa
        skullMeta.setDisplayName(ChatColor.GOLD + "Testa di " + ChatColor.AQUA + playerName);

        // Aggiorna il meta dell'oggetto
        playerHead.setItemMeta(skullMeta);

        // Dai la testa al ricevitore
        receiver.getInventory().addItem(playerHead);
        receiver.sendMessage(ChatColor.GREEN + "Hai ricevuto la testa di " + ChatColor.AQUA + playerName + ChatColor.GREEN + "!");
    }
}
