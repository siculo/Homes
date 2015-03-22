package com.lagopusempire.multihomes;

import com.lagopusempire.multihomes.load.LoadCallback;
import com.lagopusempire.bukkitlcs.BukkitLCS;
import com.lagopusempire.multihomes.commands.*;
import com.lagopusempire.multihomes.commands.user.*;
import com.lagopusempire.multihomes.config.ConfigAccessor;
import com.lagopusempire.multihomes.config.ConfigKeys;
import com.lagopusempire.multihomes.config.PluginConfig;
import com.lagopusempire.multihomes.homeIO.HomeIO;
import com.lagopusempire.multihomes.homeIO.database.DBHomeIO;
import com.lagopusempire.multihomes.homeIO.database.DatabaseSetup;
import com.lagopusempire.multihomes.homeIO.database.Scripts;
import com.lagopusempire.multihomes.messages.Messages;
import com.lagopusempire.multihomes.load.Loader;
import java.io.File;
import java.io.IOException;
import org.bukkit.plugin.java.JavaPlugin;

import static com.lagopusempire.multihomes.config.ConfigKeys.*;

/**
 *
 * @author MrZoraman
 */
public class MultiHomes extends JavaPlugin implements LoadCallback
{
    private final Loader loader;
    
    private BukkitLCS commandSystem;
    private HomeIO io;
    private HomeManager homeManager;
    private DatabaseSetup databaseSetup;
    
    private boolean loaded = false;
    
    public MultiHomes()
    {
        super();
        loader = new Loader(this);
    }
    
    @Override
    public void onEnable()
    {
        loader.addStep(this::setupConfig);
        loader.addStep(this::setupMessages);
        loader.addStep(this::setupScripts);
        loader.addStep(this::setupDbSetup);
        loader.addAsyncStep(this::setupDatabase);
        loader.addStep(this::setupHomeIO);
        loader.addStep(this::setupHomeManager);
        loader.addStep(this::setupCommandSystem);
        loader.addStep(this::setupCommands);
        
        reload(this);
    }
    
    private boolean setupCommands()
    {
        commandSystem.registerCommand("{home set}|sethome", new SetHomeCommand(homeManager));
        commandSystem.registerCommand("home reload", new ReloadCommand(this));
        return true;
    }
    
    @Override
    public void reloadFinished(boolean result)
    {
        if(result)
        {
            getLogger().info(getDescription().getName() + " has been loaded successfully.");
            loaded = true;
        }
        else
        {
            disablePlugin();
        }
    }
    
    public void reload(final LoadCallback callback)
    {
        loader.load(this);
    }
    
    private void disablePlugin()
    {
        getLogger().severe("Something went wrong while loading " + getDescription().getName() + "! Disabling...");
        getServer().getPluginManager().disablePlugin(this);
    }
    
    private boolean setupConfig()
    {
        getConfig().options().copyDefaults(true);
        saveConfig();

        PluginConfig.setConfig(getConfig());
        PluginConfig.howToSave(this::saveConfig);
        
        return true;
    }
    
    private boolean setupMessages()
    {
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
        
        return true;
    }
    
    private boolean setupHomeIO()
    {
        if(PluginConfig.getBoolean(ConfigKeys.USE_DATABASE))
        {
            this.io = new DBHomeIO(this);
            return true;
        }
        else
        {
            getLogger().severe("Flatfile home io not implemented yet!");
            return false;
        }
    }
    
    private boolean setupHomeManager()
    {
        this.homeManager = new HomeManager(io);
        return true;
    }
    
    private boolean setupCommandSystem()
    {
        commandSystem = new BukkitLCS();
        
        getCommand("home").setExecutor(commandSystem);
        getCommand("sethome").setExecutor(commandSystem);
        
        return true;
    }
    
    private boolean setupScripts()
    {
        Scripts.setPlugin(this);
        return true;
    }
    
    private boolean setupDbSetup()
    {
        final String host = PluginConfig.getString(MYSQL_HOST);
        final String username = PluginConfig.getString(MYSQL_USERNAME);
        final String password = PluginConfig.getString(MYSQL_PASSWORD);
        final String port = PluginConfig.getString(MYSQL_PORT);
        final String database = PluginConfig.getString(MYSQL_DATABASE);
        
        final String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        databaseSetup = new DatabaseSetup(this, url, username, password);
        
        return true;
    }

    private boolean setupDatabase()
    {
        return databaseSetup.setup();
    }

    public boolean isLoaded()
    {
        return loaded;
    }
}
