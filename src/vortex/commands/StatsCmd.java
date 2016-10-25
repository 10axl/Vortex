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
package vortex.commands;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import vortex.Command;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class StatsCmd extends Command {

    private final OffsetDateTime start;
    public StatsCmd(OffsetDateTime start)
    {
        this.start = start;
        this.name = "stats";
        this.help = "shows some statistics on the bot";
        this.ownerCommand = true;
    }
    
    @Override
    protected void execute(String args, MessageReceivedEvent event) {
        long totalMb = Runtime.getRuntime().totalMemory()/(1024*1024);
        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024);
        event.getChannel().sendMessage("**"+event.getJDA().getSelfInfo().getName()+"** statistics:"
                + "\nLast Startup: "+start.format(DateTimeFormatter.RFC_1123_DATE_TIME)
                + "\nGuilds: "+event.getJDA().getGuilds().size()
                + "\nMemory: "+usedMb+"Mb / "+totalMb+"Mb"
                + "\nResponse Total: "+event.getJDA().getResponseTotal()).queue();
    }
    
}
