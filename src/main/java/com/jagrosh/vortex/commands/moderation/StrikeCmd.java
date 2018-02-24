/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.ArgsUtil.ResolvedArgs;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StrikeCmd extends ModCommand
{
    public StrikeCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "strike";
        this.arguments = "[number] <@users...> <reason>";
        this.help = "applies strikes to users";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        int numstrikes;
        String[] parts = event.getArgs().split("\\s+", 2);
        String str;
        try
        {
            numstrikes = Integer.parseInt(parts[0]);
            str = parts[1];
        }
        catch(NumberFormatException | ArrayIndexOutOfBoundsException ex)
        {
            numstrikes = 1;
            str = event.getArgs();
        }
        if(numstrikes<1 || numstrikes>100)
        {
            event.replyError("Number of strikes must be between 1 and 100!");
            return;
        }
        ResolvedArgs args = ArgsUtil.resolve(str, event.getGuild());
        if(args.reason==null || args.reason.isEmpty())
        {
            event.replyError("Please provide a reason!");
            return;
        }
        StringBuilder builder = new StringBuilder();
        
        args.members.forEach(m -> 
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to interact with ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to interact with ").append(FormatUtil.formatUser(m.getUser()));
            else
                args.ids.add(m.getUser().getIdLong());
        });
        
        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a user ID"));
        
        args.users.forEach(u -> args.ids.add(u.getIdLong()));
        
        int fnumstrikes = numstrikes;
        
        args.ids.forEach(id -> 
        {
            vortex.getStrikeHandler().applyStrikes(event.getMember(), event.getMessage().getCreationTime(), id, fnumstrikes, args.reason);
            builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully gave `").append(fnumstrikes).append("` strikes to <@").append(id).append(">");
        });
        event.reply(builder.toString());
    }
}
