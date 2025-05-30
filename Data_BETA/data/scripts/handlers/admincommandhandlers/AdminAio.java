/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package handlers.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.database.L2DatabaseFactory;

public class AdminAio implements IAdminCommandHandler
{
	private final static Logger _log = Logger.getLogger(AdminAio.class.getName());
	
	private static String[] _adminCommands =
	{
		"admin_setaio",
		"admin_removeaio" 
	};
	
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_setaio"))
		{
			StringTokenizer str = new StringTokenizer(command);
			L2Object target = activeChar.getTarget();
			
			L2PcInstance player = null;
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
			
			if (target != null && target instanceof L2PcInstance)
				player = (L2PcInstance)target;
			else
				player = activeChar;
			
			try
			{
				str.nextToken();
				String time = str.nextToken();
				if (str.hasMoreTokens())
				{
					String playername = time;
					time = str.nextToken();
					player = L2World.getInstance().getPlayer(playername);
					doAio(activeChar, player, playername, time);
				}
				else
				{
					String playername = player.getName();
					doAio(activeChar, player, playername, time);
				}
				if(!time.equals("0"))
				{
					if(Config.ALLOW_AIO_ITEM)
						player.getInventory().addItem("", Config.AIO_ITEMID, 1, player, null);
					player.sendChatMessage(0, 0, "SYS", "You are now a Aio, Congratulations!");
					player.sendPacket(sm);
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //setaio <char_name> [time](in days)");
			}
			
			player.broadcastUserInfo();
			if(player.isAio())
				return true;
		}
		else if(command.startsWith("admin_removeaio"))
		{
			StringTokenizer str = new StringTokenizer(command);
			L2Object target = activeChar.getTarget();
			
			L2PcInstance player = null;
			
			if (target != null && target instanceof L2PcInstance)
				player = (L2PcInstance)target;
			else
				player = activeChar;
			
			try
			{
				str.nextToken();
				if (str.hasMoreTokens())
				{
					String playername = str.nextToken();
					player = L2World.getInstance().getPlayer(playername);
					removeAio(activeChar, player, playername);
				}
				else
				{
					String playername = player.getName();
					removeAio(activeChar, player, playername);
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //removeaio <char_name>");
			}
			player.broadcastUserInfo();
			if(!player.isAio())
				return true;
		}
		return false;
	}
	
	public void doAio(L2PcInstance activeChar, L2PcInstance _player, String _playername, String _time)
	{
		int days = Integer.parseInt(_time);
		if (_player == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "not found char" + _playername);
			return;
		}
		
		if(days > 0)
		{
			_player.setAio(true);
			_player.setEndTime("aio", days);
			_player.getStat().addExp(_player.getStat().getExpForLevel(81));
			
			Connection connection = null;
			try
			{
				connection = L2DatabaseFactory.getInstance().getConnection();
				
				PreparedStatement statement = connection.prepareStatement("UPDATE characters SET aio = 1, aio_end = ? WHERE obj_id = ?");
				statement.setLong(1, _player.getAioEndTime());
				statement.setInt(2, _player.getObjectId());
				statement.execute();
				statement.close();
				connection.close();
				
				if(Config.ALLOW_AIO_NCOLOR && activeChar.isAio())
					_player.getAppearance().setNameColor(Config.AIO_NCOLOR);
				
				if(Config.ALLOW_AIO_TCOLOR && activeChar.isAio())
					_player.getAppearance().setTitleColor(Config.AIO_TCOLOR);
				
				_player.broadcastUserInfo();
				_player.sendPacket(new EtcStatusUpdate(_player));
				_player.sendSkillList();
				_player.rewardAioSkills();
				GmListTable.broadcastMessageToGMs("Admin "+ activeChar.getName()+ " set Aio stats for player "+ _playername + " for " + _time + " day(s)");
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING,"could not set Aio stats to char:", e);
			}
			finally
			{
				try {
					connection.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			removeAio(activeChar, _player, _playername);
		}
	}
	
	public void removeAio(L2PcInstance activeChar, L2PcInstance _player, String _playername)
	{
		_player.setAio(false);
		_player.setAioEndTime(0);
		
		Connection connection = null;
		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = connection.prepareStatement("UPDATE characters SET aio = 0, aio_end = 0 WHERE obj_id = ?");
			statement.setInt(1, _player.getObjectId());
			statement.execute();
			statement.close();
			connection.close();
			
			_player.lostAioSkills();
			if(Config.ALLOW_AIO_ITEM && activeChar.isAio() == false)
				_player.getInventory().destroyItemByItemId("", Config.AIO_ITEMID, 1, _player, null);
			_player.getWarehouse().destroyItemByItemId("", Config.AIO_ITEMID, 1, _player, null);
			_player.getAppearance().setNameColor(0xFFFFFF);
			_player.getAppearance().setTitleColor(0xFFFFFF);
			_player.broadcastUserInfo();
			_player.sendPacket(new EtcStatusUpdate(_player));
			_player.sendSkillList();
			GmListTable.broadcastMessageToGMs("Admin "+activeChar.getName()+" removed Aio stats of player "+ _playername);
			_player.sendChatMessage(0, 0, "SYS", "Admin removed your Aio Stats!");
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING,"could not remove Aio stats of char:", e);
		}
		finally
		{
			try {
				connection.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}