package com.lagopusempire.multihomes;

import com.lagopusempire.bukkitlcs.BukkitLCS;
import com.lagopusempire.multihomes.commands.user.SetHomeCommand;
import com.lagopusempire.multihomes.config.ConfigAccessor;
import com.lagopusempire.multihomes.config.ConfigKeys;
import com.lagopusempire.multihomes.config.PluginConfig;
import com.lagopusempire.multihomes.homeIO.HomeIO;
import com.lagopusempire.multihomes.homeIO.database.DBHomeIO;
import com.lagopusempire.multihomes.messages.Messages;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author MrZoraman
 */
public class MultiHomes extends JavaPlugin
{
    private BukkitLCS commandSystem;
    
    @Override
    public void onEnable()
    {
        boolean success = reload();
        if(success == false)
        {
            getLogger().severe("Something went wrong while loading " + getDescription().getName() + "! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public boolean reload()
    {
        //config
        getConfig().options().copyDefaults(true);
        saveConfig();

        PluginConfig.setConfig(getConfig());
        
        final boolean useDatabase = PluginConfig.getBoolean(ConfigKeys.USE_DATABASE);
        
        //database
        if(useDatabase || PluginConfig.getBoolean(ConfigKeys.USE_DATABASE))
        {
            boolean result = setupDatabase();
            if(result == false)
            {
                return false;
            }
        }
        
        //messages
        final String fileName = "messages.yml";
        final File messagesFile = new File(getDataFolder(), fileName);
        try
        {
            messagesFile.createNewFile();
        }
        catch (IOException ex)
        {
            getLogger().severe("Failed to create messages file!");
            ex.printStackTrace();
            return false;
        }
        final ConfigAccessor messages = new ConfigAccessor(this, fileName);
        messages.getConfig().options().copyDefaults(true);
        messages.saveConfig();
        
        Messages.setMessages(messages.getConfig());
        
        //homeIO
        HomeIO homeIO;
        if(useDatabase)
        {
            homeIO = new DBHomeIO(this);
        }
        else
        {
            getLogger().severe("Flatfile home io not implemented yet!");
            return false;
        }
        
        //home manager
        final HomeManager homeManager = new HomeManager(homeIO);
        
        //command system
        commandSystem = new BukkitLCS();
        
        getCommand("home").setExecutor(commandSystem);
        getCommand("sethome").setExecutor(commandSystem);
        
        //commands
        commandSystem.registerCommand("{home set}|sethome", new SetHomeCommand(homeManager));
        
        return true;
    }

    private boolean setupDatabase()
    {
        try
        {
            getDatabase().find(com.lagopusempire.multihomes.homeIO.database.DBHome.class).findRowCount();
        }
        catch (PersistenceException ignored)
        {
            getLogger().info("Installing database for " + getDescription().getName() + " due to first time usage");
            try
            {
                installDDL();
                System.out.println("done");
            }
            catch (RuntimeException e)
            {
                getLogger().severe(e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public List<Class<?>> getDatabaseClasses()
    {
        List<Class<?>> list = new ArrayList<>();
        list.add(com.lagopusempire.multihomes.homeIO.database.DBHome.class);
        return list;
    }
}
