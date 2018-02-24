/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.JDA.ShardInfo;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Listener implements EventListener
{
    private final static Logger LOG = LoggerFactory.getLogger("Listener");
    private final Vortex vortex;
    
    public Listener(Vortex vortex)
    {
        this.vortex = vortex;
    }

    @Override
    public void onEvent(Event event)
    {
        if (event instanceof GuildMessageReceivedEvent)
        {
            Message m = ((GuildMessageReceivedEvent)event).getMessage();
            
            if(!m.getAuthor().isBot()) // ignore bot messages
            {
                // Store the message
                vortex.getMessageCache().putMessage(m);
                
                // Run automod on the message
                vortex.getAutoMod().performAutomod(m);
            }
        }
        else if (event instanceof GuildMessageUpdateEvent)
        {
            Message m = ((GuildMessageUpdateEvent)event).getMessage();
            
            if(!m.getAuthor().isBot()) // ignore bot edits
            {
                // Run automod on the message
                vortex.getAutoMod().performAutomod(m);
                
                // Store and log the edit
                Message old = vortex.getMessageCache().putMessage(m);
                vortex.getBasicLogger().logMessageEdit(m, old);
            }
        }
        else if (event instanceof GuildMessageDeleteEvent)
        {
            GuildMessageDeleteEvent gevent = (GuildMessageDeleteEvent) event;
            
            // Log the deletion
            Message old = vortex.getMessageCache().pullMessage(gevent.getGuild(), gevent.getMessageIdLong());
            vortex.getBasicLogger().logMessageDelete(old);
        }
        else if (event instanceof MessageBulkDeleteEvent)
        {
            MessageBulkDeleteEvent gevent = (MessageBulkDeleteEvent) event;
            
            // Get the messages we had cached
            List<Message> logged = gevent.getMessageIds().stream()
                    .map(id -> vortex.getMessageCache().pullMessage(gevent.getGuild(), Long.parseLong(id)))
                    .filter(m -> m!=null)
                    .collect(Collectors.toList());
            
            // Log the deletion
            vortex.getBasicLogger().logMessageBulkDelete(logged, gevent.getMessageIds().size(), gevent.getChannel());
        }
        else if (event instanceof GuildMemberJoinEvent)
        {
            GuildMemberJoinEvent gevent = (GuildMemberJoinEvent) event;
            
            // Log the join
            vortex.getBasicLogger().logGuildJoin(gevent);
            
            // Perform automod on the newly-joined member
            vortex.getAutoMod().memberJoin(gevent);
        }
        else if (event instanceof GuildMemberLeaveEvent)
        {
            GuildMemberLeaveEvent gmle = (GuildMemberLeaveEvent)event;
            
            // Log the member leaving
            vortex.getBasicLogger().logGuildLeave(gmle);
            
            // Signal the modlogger because someone might have been kicked
            vortex.getModLogger().setNeedUpdate(gmle.getGuild());
        }
        else if (event instanceof GuildBanEvent)
        {
            // Signal the modlogger because someone was banned
            vortex.getModLogger().setNeedUpdate(((GuildBanEvent) event).getGuild());
        }
        else if (event instanceof GuildUnbanEvent)
        {
            // Signal the modlogger because someone was unbanned
            vortex.getModLogger().setNeedUpdate(((GuildUnbanEvent) event).getGuild());
        }
        else if (event instanceof GuildMemberRoleAddEvent)
        {
            GuildMemberRoleAddEvent gmrae = (GuildMemberRoleAddEvent) event;
            
            // Signal the modlogger if someone was muted
            if(gmrae.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("muted")))
                vortex.getModLogger().setNeedUpdate(gmrae.getGuild());
        }
        else if (event instanceof GuildMemberRoleRemoveEvent)
        {
            GuildMemberRoleRemoveEvent gmrre = (GuildMemberRoleRemoveEvent) event;
            
            // Signal the modlogger if someone was unmuted
            if(gmrre.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("muted")))
                vortex.getModLogger().setNeedUpdate(gmrre.getGuild());
        }
        else if (event instanceof UserNameUpdateEvent)
        {
            // Log the name change
            vortex.getBasicLogger().logNameChange((UserNameUpdateEvent)event);
        }
        else if (event instanceof GuildVoiceJoinEvent)
        {
            GuildVoiceJoinEvent gevent = (GuildVoiceJoinEvent)event;
            
            // Log the voice join
            if(!gevent.getMember().getUser().isBot()) // ignore bots
                vortex.getBasicLogger().logVoiceJoin(gevent);
        }
        else if (event instanceof GuildVoiceMoveEvent)
        {
            GuildVoiceMoveEvent gevent = (GuildVoiceMoveEvent)event;
            
            // Log the voice move
            if(!gevent.getMember().getUser().isBot()) // ignore bots
                vortex.getBasicLogger().logVoiceMove(gevent);
        }
        else if (event instanceof GuildVoiceLeaveEvent)
        {
            GuildVoiceLeaveEvent gevent = (GuildVoiceLeaveEvent)event;
            
            // Log the voice leave
            if(!gevent.getMember().getUser().isBot()) // ignore bots
                vortex.getBasicLogger().logVoiceLeave(gevent);
        }
        else if (event instanceof ReadyEvent)
        {
            // Log the shard that has finished loading
            ShardInfo si = event.getJDA().getShardInfo();
            String shardinfo = si==null ? "1/1" : (si.getShardId()+1)+"/"+si.getShardTotal();
            LOG.info("Shard "+shardinfo+" is ready.");
            vortex.getLogWebhook().send("\uD83C\uDF00 Shard `"+shardinfo+"` has connected. Guilds: `"
                    +event.getJDA().getGuildCache().size()+"` Users: `"+event.getJDA().getUserCache().size()+"`");
            vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempbans.checkUnbans(event.getJDA()), 0, 2, TimeUnit.MINUTES);
            vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempmutes.checkUnmutes(event.getJDA()), 0, 45, TimeUnit.SECONDS);
        }
    }
}
