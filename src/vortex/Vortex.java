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
package vortex;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.List;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import vortex.commands.*;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        List<String> tokens;
        OffsetDateTime now = OffsetDateTime.now();
        /**
         * Tokens:
         * 0 - bot token
         * 1 - carbonitex key
         * 2 - bots.discord.pw token
         */
        try {
            tokens = Files.readAllLines(Paths.get("config.txt"));
            Command[] commands = new Command[]{
                new AutomodCmd(),
                new BanCmd(),
                new KickCmd(),
                new PingCmd(),
                new AboutCmd(),
                new InviteCmd(),
                new StatsCmd(now),
                new ShutdownCmd()
            };
            String[] config = new String[]{
                tokens.get(1),
                tokens.get(2)
            };
            new JDABuilder(AccountType.BOT).setToken(tokens.get(0)).addListener(new Bot(commands, config)).buildAsync();
            //new JDABuilder(AccountType.CLIENT).setToken(tokens.get(1)).addListener(new Bot(commands)).buildAsync();
        } catch (IOException | ArrayIndexOutOfBoundsException | LoginException | RateLimitedException ex) {
            SimpleLog.getLog("Vortex").fatal(ex);
        }
    }
    
}
