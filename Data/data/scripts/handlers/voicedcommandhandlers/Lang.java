package handlers.voicedcommandhandlers;

import com.src.Config;
import com.src.gameserver.handler.IVoicedCommandHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;


public class Lang implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands = {"lang"};

	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
		if (target == null || !Config.MULTILANG_ALLOWED.contains(target))
		{
			String answer = "Wrong language! Supported languages:";
			for (String lang : Config.MULTILANG_ALLOWED)
				answer += " " + lang;
			activeChar.sendMessage(answer);
		}
		else
        {
		    activeChar.setLang(target);
            if (target.equalsIgnoreCase("en"))
                activeChar.sendMessage("You choice english language");
            else if (target.equalsIgnoreCase("ru"))
                activeChar.sendMessage("Вы выбрали р�?�?�?кий язык");
        }
		return true;
	}

	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}