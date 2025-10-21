PremiumAuthBypass - IP-based opt-in premium bypass for AuthMe

Flow:
- Player logs in normally with AuthMe (/login).
- After first successful login the plugin prompts the player to run /premiumbypass accept to register the current IP.
- If the player's IP matches the stored IP on future joins, the plugin automatically calls AuthMe forceLogin for them.
- If the IP changes, the player must re-authenticate and run /premiumbypass accept again.
- Bedrock-like usernames (starting with '_') are handled the same.

Installation:
- Place the compiled jar into your server plugins folder.
- If you want to compile locally, add your AuthMe jar into libs/ and uncomment the compileOnly files(...) line in build.gradle.