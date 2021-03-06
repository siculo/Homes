package com.lagopusempire.homes.util;

import com.lagopusempire.homes.config.ConfigKeys;
import static com.lagopusempire.homes.config.ConfigKeys.MYSQL_DATABASE;
import static com.lagopusempire.homes.config.ConfigKeys.MYSQL_HOST;
import static com.lagopusempire.homes.config.ConfigKeys.MYSQL_PASSWORD;
import static com.lagopusempire.homes.config.ConfigKeys.MYSQL_PORT;
import static com.lagopusempire.homes.config.ConfigKeys.MYSQL_USERNAME;
import com.lagopusempire.homes.config.PluginConfig;
import com.lagopusempire.homes.messages.MessageFormatter;
import com.lagopusempire.homes.messages.MessageKeys;
import com.lagopusempire.homes.messages.Messages;
import com.lagopusempire.homes.permissions.Permissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author MrZoraman
 */
public class Util
{
    private static final int DB_LOGIN_TIMEOUT = 5;
    
    static
    {
        DriverManager.setLoginTimeout(DB_LOGIN_TIMEOUT);
    }
    
    public static Connection createConnection() throws SQLException
    {
        String url = getDatabaseURL();
        final String username = PluginConfig.getString(MYSQL_USERNAME);
        final String password = PluginConfig.getString(MYSQL_PASSWORD);
        return DriverManager.getConnection(url, username, password);
    }
    
    public static String getDatabaseURL()
    {
        final String host = PluginConfig.getString(MYSQL_HOST);
        final String port = PluginConfig.getString(MYSQL_PORT);
        final String database = PluginConfig.getString(MYSQL_DATABASE);
        
        return "jdbc:mysql://" + host + ":" + port + "/" + database;
    }
    
    public static void sendMessage(Player player, MessageFormatter formatter)
    {
        if(player != null && player.isOnline())
        {
            player.sendMessage(formatter.colorize().toString());
        }
    }
    
    public static MessageFormatter getNoPermsMsg(Permissions perm)
    {
        return Messages.getMessage(MessageKeys.NO_PERMISSION)
                .replace("node", perm.getNode());
    }
    
    public static boolean checkPerms(Player player, Permissions perm)
    {
        if(!perm.check(player))
        {
            Util.sendMessage(player, getNoPermsMsg(perm));
            return false;
        }
        
        return true;
    }
    
    public static boolean checkPerms(CommandSender sender, Permissions perm)
    {
        if(!perm.check(sender))
        {
            sender.sendMessage(getNoPermsMsg(perm).colorize().toString());
            return false;
        }
        
        return true;
    }
    
    public static UUID getPlayer(String playerName, Logger logger, Set<? extends Player> onlinePlayers)
    {
        final String uuid_regex = PluginConfig.getString(ConfigKeys.UUID_REGEX);
        if(playerName.matches(uuid_regex))//the 'name' is a uuid
        {
            return UUID.fromString(playerName);
        }
        
//        final Set<? extends Player> onlinePlayers = new HashSet<>(plugin.getServer().getOnlinePlayers());
        for(Player player : onlinePlayers)
        {
            if(player.getName().equalsIgnoreCase(playerName))
            {
                return player.getUniqueId();
            }
        }
        
        final UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(playerName));
        Map<String, UUID> response = null;
        try
        {
            response = fetcher.call(PluginConfig.getString(ConfigKeys.UUID_FETCH_URL));
        }
        catch (Exception e)
        {
            logger.warning("Failed to lookup uuid for player '" + playerName + "'!");
            e.printStackTrace();
            return null;
        }

        final UUID uuid = response.get(playerName);
        return uuid;
    }
}
