package com.lagopusempire.homes;

import com.lagopusempire.homes.commands.user.ListHomesCommand;
import com.lagopusempire.homes.commands.user.GoHomeCommand;
import com.lagopusempire.homes.commands.user.SetHomeCommand;
import com.lagopusempire.homes.commands.user.DeleteHomeCommand;
import com.lagopusempire.homes.commands.admin.GoToOthersHomeCommand;
import com.lagopusempire.homes.commands.admin.DeleteOthersHomeCommand;
import com.lagopusempire.homes.commands.admin.ListOthersHomeCommand;
import com.lagopusempire.homes.commands.admin.SetOthersHomeCommand;
import com.lagopusempire.homes.commands.NotLoadedCommand;
import com.lagopusempire.homes.commands.ReloadCommand;
import com.lagopusempire.homes.commands.HelpCommand;
import com.lagopusempire.homes.load.LoadCallback;
import com.lagopusempire.bukkitlcs.BukkitLCS;
import com.lagopusempire.homes.config.ConfigAccessor;
import com.lagopusempire.homes.config.ConfigKeys;
import com.lagopusempire.homes.config.PluginConfig;
import com.lagopusempire.homes.help.Help;
import com.lagopusempire.homes.homeIO.HomeIO;
import com.lagopusempire.homes.homeIO.database.DBHomeIO;
import com.lagopusempire.homes.homeIO.database.DatabaseSetup;
import com.lagopusempire.homes.homeIO.database.Scripts;
import com.lagopusempire.homes.homeIO.flatfile.FlatfileHomeIO;
import com.lagopusempire.homes.messages.Messages;
import com.lagopusempire.homes.load.Loader;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Set;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author MrZoraman
 */
public class HomesPlugin extends JavaPlugin implements LoadCallback
{
    private Loader loader;
    private BukkitLCS commandSystem;
    private HomeIO io;
    private HomeManager homeManager;
    private DatabaseSetup databaseSetup;
    private Connection conn;
    private Set<String> registeredBukkitCommands;

    private volatile boolean loaded = false;

    @Override
    public void onEnable()
    {
        reload(this);
    }

    public void reload(final LoadCallback callback)
    {
        loaded = false;
        
        setupConfig();
        
        loader = new Loader(this);
        loader.addStep(this::unloadDb, false);
        loader.addStep(this::unregisterEvents, false);
        loader.addStep(this::setupMessages, false);
        loader.addStep(this::setupHelp, false);
        loader.addStep(this::setupRegisteredBukkitCommands, false);
        loader.addStep(this::setupNotLoadedCommand, false);
        if (needToSetupDatabase())
        {
            loader.addStep(this::setupScripts, false);
            loader.addStep(this::setupDbSetup, false);
            loader.addStep(this::setupDatabase, true);
            loader.addStep(this::setupPostDb, false);
        }
        loader.addStep(this::setupHomeIO, false);
        loader.addStep(this::setupHomeManager, false);
        loader.addStep(this::setupEvents, false);
        loader.addStep(this::loadOnlinePlayers, false);
        loader.addStep(this::setupCommandSystem, false);
        loader.addStep(this::setupCommands, false);
        
        loader.load(callback);
    }

    @Override
    public void reloadFinished(boolean result)
    {
        if (result)
        {
            getLogger().info(getDescription().getName() + " has been loaded successfully.");
            loaded = true;
        }
        else
        {
            disablePlugin();
        }
    }

    public void disablePlugin()
    {
        getLogger().severe("Something went wrong in " + getDescription().getName() + "! Disabling...");
        getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void onDisable()
    {
        unloadDb();
    }

    private boolean unloadDb()
    {
        if (io != null)
        {
            return io.close();
        }
        return true;
    }
    
    private void setupConfig()
    {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        PluginConfig.setConfig(getConfig());
        PluginConfig.howToSave(this::saveConfig);
    }

    private boolean unregisterEvents()
    {
        if (homeManager != null)
        {
            HandlerList.unregisterAll(homeManager);
        }
        if(io != null)
        {
            io.unregisterEvents();
        }
        return true;
    }

    private boolean setupMessages()
    {
        final ConfigAccessor messages = createYamlFile("messages.yml");
        if (messages == null)
        {
            return false;
        }

        Messages.setMessages(messages.getConfig());

        return true;
    }
    
    private boolean setupHelp()
    {
        final ConfigAccessor helpMessages = createYamlFile("help.yml");
        if (helpMessages == null)
        {
            return false;
        }
        
        Help.setHelpMessages(helpMessages.getConfig());
        
        return true;
    }
    
    private boolean setupRegisteredBukkitCommands()
    {
        registeredBukkitCommands = getDescription().getCommands().keySet();
        return true;
    }
    
    private boolean setupNotLoadedCommand()
    {
        registeredBukkitCommands.forEach((command) -> getCommand(command).setExecutor(new NotLoadedCommand()));
        return true;
    }

    private boolean setupScripts()
    {
        Scripts.setPlugin(this);
        return true;
    }

    private boolean setupDbSetup()
    {
        databaseSetup = new DatabaseSetup(this);
        return true;
    }

    private boolean setupDatabase()
    {
        return databaseSetup.setup();
    }

    private boolean setupPostDb()
    {
        final boolean success = databaseSetup.postSetup();
        if (!success)
        {
            return false;
        }

        this.conn = databaseSetup.getConnection();
        return true;
    }

    private boolean setupHomeIO()
    {
        if (needToSetupDatabase())
        {
            this.io = new DBHomeIO(this, conn);
        }
        else
        {
            final ConfigAccessor homes = createYamlFile("homes.yml");
            if (homes == null)
            {
                return false;
            }

            this.io = new FlatfileHomeIO(homes);
        }
        
        io.onLoad();
        return true;
    }

    private boolean setupHomeManager()
    {
        this.homeManager = new HomeManager(this, io);
        return true;
    }

    private boolean setupEvents()
    {
        getServer().getPluginManager().registerEvents(homeManager, this);
        io.registerEvents();
        return true;
    }

    private boolean loadOnlinePlayers()
    {
        boolean success = true;
        success &= homeManager.loadOnlinePlayerMaps();
        success &= homeManager.loadOnlinePlayerHomes();
        return success;
    }

    private boolean setupCommandSystem()
    {
        commandSystem = new BukkitLCS();
        registeredBukkitCommands.forEach((command) -> getCommand(command).setExecutor(commandSystem));

        return true;
    }

    private boolean setupCommands()
    {
        commandSystem.registerCommand("home reload", new ReloadCommand(this));
        commandSystem.registerCommand("home help|?", new HelpCommand(this));
        
        commandSystem.registerCommand("{home set}|sethome", new SetHomeCommand(this, homeManager));
        commandSystem.registerCommand("home", new GoHomeCommand(this, homeManager));
        commandSystem.registerCommand("{home delete|del|remove|rm|clear}|delhome", new DeleteHomeCommand(this, homeManager));
        
        commandSystem.registerCommand("home other", new GoToOthersHomeCommand(this, homeManager));
        commandSystem.registerCommand("{home set}|sethome other", new SetOthersHomeCommand(this, homeManager));
        commandSystem.registerCommand("{home delete|del|remove|rm|clear}|delhome other", new DeleteOthersHomeCommand(this, homeManager));
        
        if(!PluginConfig.getBoolean(ConfigKeys.SINGLE_HOME_ONLY))
        {
            commandSystem.registerCommand("home list", new ListHomesCommand(this, homeManager));
            commandSystem.registerCommand("home list other", new ListOthersHomeCommand(this, homeManager));
        }
        return true;
    }

    private ConfigAccessor createYamlFile(String fileName)
    {
        final File file = new File(getDataFolder(), fileName);
        try
        {
            file.createNewFile();
        }
        catch (IOException ex)
        {
            getLogger().severe("Failed to create " + fileName + "!");
            ex.printStackTrace();
            return null;
        }
        final ConfigAccessor config = new ConfigAccessor(this, fileName);
        config.getConfig().options().copyDefaults(true);
        config.saveConfig();

        return config;
    }
    
    private boolean needToSetupDatabase()
    {
        return PluginConfig.getBoolean(ConfigKeys.USE_DATABASE);
    }

    public boolean isLoaded()
    {
        return loaded;
    }
}
