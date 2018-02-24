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
package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.*;
import com.jagrosh.jdautilities.command.GuildSettingsManager;
import com.jagrosh.jdautilities.command.GuildSettingsProvider;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.utils.FixedCache;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Guild.VerificationLevel;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GuildSettingsDataManager extends DataManager implements GuildSettingsManager
{
    public final static int PREFIX_MAX_LENGTH = 40;
    private static final String SETTINGS_TITLE = "\uD83D\uDCCA Server Settings";
    
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L,true);
    public final static SQLColumn<Long> MOD_ROLE_ID = new LongColumn("MOD_ROLE_ID",false,0L);
    
    public final static SQLColumn<Long> MODLOG_ID = new LongColumn("MODLOG_ID",false,0L);
    public final static SQLColumn<Long> SERVERLOG_ID = new LongColumn("SERVERLOG_ID",false,0L);
    public final static SQLColumn<Long> MESSAGELOG_ID = new LongColumn("MESSAGELOG_ID",false,0L);
    
    public final static SQLColumn<String> PREFIX = new StringColumn("PREFIX", true, null, PREFIX_MAX_LENGTH);
    public final static SQLColumn<String> TIMEZONE = new StringColumn("TIMEZONE",true,null,32);

    public final static SQLColumn<Integer> RAIDMODE = new IntegerColumn("RAIDMODE",false,-2); // -2 = Raid Mode not activated, -1+ = Raid Mode active, level to set permission when finished
    
    // Cache
    private final FixedCache<Long, GuildSettings> cache = new FixedCache<>(1000);
    private final GuildSettings blankSettings = new GuildSettings();
    
    public GuildSettingsDataManager(DatabaseConnector connector)
    {
        super(connector, "GUILD_SETTINGS");
    }
    
    // Getters
    @Override
    public GuildSettings getSettings(Guild guild)
    {
        if(cache.contains(guild.getIdLong()))
            return cache.get(guild.getIdLong());
        GuildSettings settings = read(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> rs.next() ? new GuildSettings(rs) : blankSettings);
        cache.put(guild.getIdLong(), settings);
        return settings;
    }
    
    public Field getSettingsDisplay(Guild guild)
    {
        GuildSettings settings = getSettings(guild);
        TextChannel modlog = settings.getModLogChannel(guild);
        TextChannel serverlog = settings.getServerLogChannel(guild);
        TextChannel messagelog = settings.getMessageLogChannel(guild);
        Role modrole = settings.getModeratorRole(guild);
        return new Field(SETTINGS_TITLE, "Prefix: `"+(settings.prefix==null ? Constants.PREFIX : settings.prefix)+"`"
                + "\nMod Role: "+(modrole==null ? "None" : modrole.getAsMention())
                + "\nMod Log: "+(modlog==null ? "None" : modlog.getAsMention())
                + "\nMsg Log: "+(messagelog==null ? "None" : messagelog.getAsMention())
                + "\nServer Log: "+(serverlog==null ? "None" : serverlog.getAsMention())
                + "\nTimezone: **"+settings.timezone+"**", true);
    }
    
    // Setters
    public void setModLogChannel(Guild guild, TextChannel tc)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, MODLOG_ID), rs -> 
        {
            if(rs.next())
            {
                MODLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MODLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setServerLogChannel(Guild guild, TextChannel tc)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, SERVERLOG_ID), rs -> 
        {
            if(rs.next())
            {
                SERVERLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                SERVERLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setMessageLogChannel(Guild guild, TextChannel tc)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, MESSAGELOG_ID), rs -> 
        {
            if(rs.next())
            {
                MESSAGELOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MESSAGELOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setModeratorRole(Guild guild, Role role)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, MOD_ROLE_ID), rs -> 
        {
            if(rs.next())
            {
                MOD_ROLE_ID.updateValue(rs, role==null ? 0L : role.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MOD_ROLE_ID.updateValue(rs, role==null ? 0L : role.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void enableRaidMode(Guild guild)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, RAIDMODE), rs -> 
        {
            if(rs.next())
            {
                RAIDMODE.updateValue(rs, guild.getVerificationLevel().getKey());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                RAIDMODE.updateValue(rs, guild.getVerificationLevel().getKey());
                rs.insertRow();
            }
        });
    }
    
    public VerificationLevel disableRaidMode(Guild guild)
    {
        invalidateCache(guild);
        return readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, RAIDMODE), rs -> 
        {
            VerificationLevel old = null;
            if(rs.next())
            {
                old = VerificationLevel.fromKey(RAIDMODE.getValue(rs));
                RAIDMODE.updateValue(rs, -2);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                RAIDMODE.updateValue(rs, -2);
                rs.insertRow();
            }
            return old;
        });
    }
    
    private void invalidateCache(Guild guild)
    {
        cache.pull(guild.getIdLong());
    }
    
    public class GuildSettings implements GuildSettingsProvider
    {
        private final long modRole, modlog, serverlog, messagelog;
        private final String prefix;
        private final ZoneId timezone;
        private final int raidMode;
        
        private GuildSettings()
        {
            this.modRole = 0;
            this.modlog = 0;
            this.serverlog = 0;
            this.messagelog = 0;
            this.prefix = null;
            this.timezone = ZoneId.systemDefault();
            this.raidMode = -2;
        }
        
        private GuildSettings(ResultSet rs) throws SQLException
        {
            this.modRole = MOD_ROLE_ID.getValue(rs);
            this.modlog = MODLOG_ID.getValue(rs);
            this.serverlog = SERVERLOG_ID.getValue(rs);
            this.messagelog = MESSAGELOG_ID.getValue(rs);
            this.prefix = PREFIX.getValue(rs);
            String str = TIMEZONE.getValue(rs);
            this.timezone = str==null ? ZoneId.systemDefault() : ZoneId.of(str);
            this.raidMode = RAIDMODE.getValue(rs);
        }
        
        public Role getModeratorRole(Guild guild)
        {
            return guild.getRoleById(modRole);
        }
        
        public TextChannel getModLogChannel(Guild guild)
        {
            return guild.getTextChannelById(modlog);
        }
        
        public TextChannel getServerLogChannel(Guild guild)
        {
            return guild.getTextChannelById(serverlog);
        }
        
        public TextChannel getMessageLogChannel(Guild guild)
        {
            return guild.getTextChannelById(messagelog);
        }
        
        public ZoneId getTimezone()
        {
            return timezone;
        }

        @Override
        public Collection<String> getPrefixes()
        {
            if(prefix==null || prefix.isEmpty())
                return null;
            return Collections.singleton(prefix);
        }
        
        public boolean isInRaidMode()
        {
            return raidMode != -2;
        }
    }
}
