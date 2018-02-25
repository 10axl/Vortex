/*
 * Copyright 2016 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex;

import com.jagrosh.vortex.commands.automod.AntimentionCmd;
import com.jagrosh.vortex.commands.automod.AntiduplicateCmd;
import com.jagrosh.vortex.commands.automod.AutoraidmodeCmd;
import com.jagrosh.vortex.commands.settings.SettingsCmd;
import com.jagrosh.vortex.commands.automod.AntiinviteCmd;
import com.jagrosh.vortex.commands.automod.UnignoreCmd;
import com.jagrosh.vortex.commands.automod.IgnoreCmd;
import com.jagrosh.vortex.commands.general.*;
import com.jagrosh.vortex.commands.moderation.*;
import com.jagrosh.vortex.commands.owner.*;
import com.jagrosh.vortex.commands.settings.*;
import java.awt.Color;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Executors;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.*;
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.automod.StrikeHandler;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.logging.BasicLogger;
import com.jagrosh.vortex.logging.MessageCache;
import com.jagrosh.vortex.logging.ModLogger;
import com.jagrosh.vortex.logging.TextUploader;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex
{
    private final EventWaiter waiter;
    private final ScheduledExecutorService threadpool;
    private final Database database;
    private final TextUploader uploader;
    private final ShardManager shards;
    private final ModLogger modlog;
    private final BasicLogger basiclog;
    private final MessageCache messages;
    private final WebhookClient logwebhook;
    private final AutoMod automod;
    private final StrikeHandler strikehandler;
    
    public Vortex() throws Exception
    {
        /**
         * Tokens:
         * 0  - bot token
         * 1  - bots.discord.pw token
         * 2  - other token 1 (unused)
         * 3  - other token 2 (unused)
         * 4  - database location
         * 5  - database username
         * 6  - database password
         * 7  - log webhook url
         * 8  - category id
         */
        List<String> tokens = Files.readAllLines(Paths.get("config.txt"));
        waiter = new EventWaiter();
        threadpool = Executors.newScheduledThreadPool(30);
        database = new Database(tokens.get(4), tokens.get(5), tokens.get(6));
        uploader = new TextUploader(this, Long.parseLong(tokens.get(8)));
        modlog = new ModLogger(this);
        basiclog = new BasicLogger(this);
        messages = new MessageCache();
        logwebhook = new WebhookClientBuilder(tokens.get(7)).build();
        automod = new AutoMod(this);
        strikehandler = new StrikeHandler(this);
        
        CommandClient client = new CommandClientBuilder()
                        .setPrefix(Constants.PREFIX)
                        .setGame(Game.watching("Type "+Constants.PREFIX+"help | Vortex Beta (Formerly Spectra Beta)"))
                        .setOwnerId(Constants.OWNER_ID)
                        .setServerInvite(Constants.SERVER_INVITE)
                        .setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
                        .setLinkedCacheSize(0)
                        .setGuildSettingsManager(database.settings)
                        .addCommands(
                            // General
                            new AboutCommand(Color.CYAN, "and I'm here to keep your Discord server safe and make moderating easy!", 
                                                        new String[]{"Moderation commands","Configurable automoderation","Very easy setup"},Constants.PERMISSIONS),
                            new InviteCmd(),
                            new PingCommand(),
                            new ServerinfoCmd(),
                            new UserinfoCmd(),

                            // Moderation
                            new KickCmd(this),
                            new BanCmd(this),
                            new SoftbanCmd(this),
                            new CleanCmd(this),
                            new VoicemoveCmd(this),
                            new MuteCmd(this),
                            new UnmuteCmd(this),
                            new RaidCmd(this),
                            new StrikeCmd(this),
                            new PardonCmd(this),
                            new ReasonCmd(this),

                            // Settings
                            new SetupCmd(this),
                            new SetstrikesCmd(this),
                            new MessagelogCmd(this),
                            new ModlogCmd(this),
                            new ServerlogCmd(this),
                            new TimezoneCmd(this),
                            new ModroleCmd(this),
                            new PrefixCmd(this),
                            new SettingsCmd(this),

                            // Automoderation
                            new AntiinviteCmd(this),
                            new AntimentionCmd(this),
                            new AntiduplicateCmd(this),
                            new AutoraidmodeCmd(this),
                            new IgnoreCmd(this),
                            new UnignoreCmd(this),

                            // Owner
                            new EvalCmd(this),
                            new DebugCmd(this)
                        )
                        .setHelpConsumer(event -> event.replyInDm(FormatUtil.formatHelp(event, this), m -> 
                        {
                            if(event.isFromType(ChannelType.TEXT))
                                try
                                {
                                    event.getMessage().addReaction(Constants.HELP_REACTION).queue();
                                } catch(PermissionException ex) {}
                        }, t -> event.replyWarning("Help cannot be sent because you are blocking Direct Messages.")))
                        //.setDiscordBotsKey(tokens.get(1))
                        //.setCarbonitexKey(tokens.get(2))
                        //.setDiscordBotListKey(tokens.get(3))
                        .build();
        shards = new DefaultShardManagerBuilder()
                .setShardsTotal(2)
                .setToken(tokens.get(0))
                .addEventListeners(new Listener(this), client, waiter)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setGame(Game.playing("loading..."))
                .setBulkDeleteSplittingEnabled(false)
                .build();
        
        modlog.start();
    }
    
    public EventWaiter getEventWaiter()
    {
        return waiter;
    }
    
    public Database getDatabase()
    {
        return database;
    }
    
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    public TextUploader getTextUploader()
    {
        return uploader;
    }
    
    public ShardManager getShardManager()
    {
        return shards;
    }
    
    public ModLogger getModLogger()
    {
        return modlog;
    }
    
    public BasicLogger getBasicLogger()
    {
        return basiclog;
    }
    
    public MessageCache getMessageCache()
    {
        return messages;
    }
    
    public WebhookClient getLogWebhook()
    {
        return logwebhook;
    }
    
    public AutoMod getAutoMod()
    {
        return automod;
    }
    
    public StrikeHandler getStrikeHandler()
    {
        return strikehandler;
    }
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception
    {
        new Vortex();
    }
}
