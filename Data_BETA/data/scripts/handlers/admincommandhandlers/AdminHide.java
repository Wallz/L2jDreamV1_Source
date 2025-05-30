package handlers.admincommandhandlers;

import com.src.gameserver.communitybbs.Manager.RegionBBSManager;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminHide implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_hide" };
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_hide on"))
		{
			GmListTable.getInstance().hideGm(activeChar);
			activeChar.setMessageRefusal(true);
			activeChar.getAppearance().setInvisible();
			activeChar.broadcastUserInfo();
			activeChar.decayMe();
			activeChar.spawnMe();
			RegionBBSManager.getInstance().changeCommunityBoard();
			activeChar.sendChatMessage(0, 0, "SYS", "Now, you cannot be seen.");
		}
		else if (command.startsWith("admin_hide off"))
		{
			GmListTable.getInstance().showGm(activeChar);
			activeChar.setMessageRefusal(false);
			activeChar.getAppearance().setVisible();
			activeChar.broadcastUserInfo();
			RegionBBSManager.getInstance().changeCommunityBoard();
			activeChar.sendChatMessage(0, 0, "SYS", "Now, you can be seen.");
		}
		return true;
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}