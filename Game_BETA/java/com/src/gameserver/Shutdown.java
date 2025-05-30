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
package com.src.gameserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.sql.OfflineTradersTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.managers.FourSepulchersManager;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.managers.ItemsOnGroundManager;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.managers.RaidBossSpawnManager;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.gameserverpackets.ServerStatus;
import com.src.gameserver.network.serverpackets.ServerClose;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.LoginServerThread;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Broadcast;
import com.src.gameserver.util.sql.SQLQueue;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.database.SqlUtils;

public class Shutdown extends Thread
{
	public enum ShutdownModeType1
	{
		SIGTERM("Terminating"),
		SHUTDOWN("Shutting down"),
		RESTART("Restarting"),
		ABORT("Aborting"),
		TASK_SHUT("Shuting down"),
		TASK_RES("Restarting"),
		TELL_SHUT("Shuting down"),
		TELL_RES("Restarting");

		private final String _modeText;

		ShutdownModeType1(String modeText)
		{
			_modeText = modeText;
		}

		public String getText()
		{
			return _modeText;
		}
	}

	private static Log _log = LogFactory.getLog(Shutdown.class);
	private static Shutdown _instance;
	private static Shutdown _counterInstance = null;
	private int _secondsShut;
	private int _shutdownMode;

	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	public static final int TASK_SHUTDOWN = 4;
	public static final int TASK_RESTART = 5;
	public static final int TELL_SHUTDOWN = 6;
	public static final int TELL_RESTART = 7;

	private static final String[] MODE_TEXT =
	{
		"SIGTERM", "shutting down", "restarting", "aborting",
		"shutting down",
		"restarting",
		"shutting down",
		"restarting"
	};

	private void SendServerQuit(int seconds)
	{
		Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECONDS).addNumber(seconds));
	}

	public void startTelnetShutdown(String IP, int seconds, boolean restart)
	{
		Announcements _an = Announcements.getInstance();
		_log.warn("IP: " + IP + " issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");
		_an.announceToAll("Server is " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");

		if(restart)
		{
			_shutdownMode = TELL_RESTART;
		}
		else
		{
			_shutdownMode = TELL_SHUTDOWN;
		}

		if(_shutdownMode > 0)
		{
			switch(seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
		}

		if(_counterInstance != null)
		{
			_counterInstance._abort();
		}
		_counterInstance = new Shutdown(seconds, restart, false, true);
		_counterInstance.start();
	}

	public void telnetAbort(String IP)
	{
		Announcements _an = Announcements.getInstance();
		_log.warn("IP: " + IP + " issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
		_an.announceToAll("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!");
		_an = null;

		if(_counterInstance != null)
		{
			_counterInstance._abort();
		}
	}

	public Shutdown()
	{
		_secondsShut = -1;
		_shutdownMode = SIGTERM;
	}

	public void autoRestart(int time)
	{
		_secondsShut = time;
		
		countdown();
		
		_shutdownMode = GM_RESTART;
		
		_instance.setMode(GM_RESTART);
		System.exit(2);
	}

	public Shutdown(int seconds, boolean restart, boolean task, boolean telnet)
	{
		if(seconds < 0)
		{
			seconds = 0;
		}
		_secondsShut = seconds;
		if(restart)
		{
			if(!task)
			{
				_shutdownMode = GM_RESTART;
			}
			else if(telnet)
			{
				_shutdownMode = TELL_RESTART;
			}
			else
			{
				_shutdownMode = TASK_RESTART;
			}
		}
		else
		{
			if(!task)
			{
				_shutdownMode = GM_SHUTDOWN;
			}
			else if(telnet)
			{
				_shutdownMode = TELL_SHUTDOWN;
			}
			else
			{
				_shutdownMode = TASK_SHUTDOWN;
			}
		}
	}

	public static Shutdown getInstance()
	{
		if(_instance == null)
		{
			_instance = new Shutdown();
		}
		return _instance;
	}

	public static Shutdown getCounterInstance()
	{
		return _counterInstance;
	}

	@Override
	public void run()
	{
		if(this == _instance)
		{
			try
			{
				LoginServerThread.getInstance().interrupt();
			}
			catch(Throwable t)
			{
			}

			SQLQueue.getInstance().shutdown();

			saveData();

			try
			{
				GameTimeController.getInstance().stopTimer();
			}
			catch(Throwable t)
			{
			}

			try
			{
				GameServer.getSelectorThread().shutdown();
			}
			catch(Throwable t)
			{}

			try
			{
				GameServer.getSelectorThread().setDaemon(true);
				ThreadPoolManager.getInstance().shutdown();
			}
			catch(Throwable t)
			{}

			try
			{
				SqlUtils.OpzGame();
			}
			catch(Throwable t)
			{}

			try
			{
				L2DatabaseFactory.getInstance().shutdown();
			}
			catch(Throwable t)
			{}

			System.runFinalization();
			System.gc();

			if(_instance._shutdownMode == GM_RESTART)
			{
				Runtime.getRuntime().halt(2);
			}
			else if(_instance._shutdownMode == TASK_RESTART)
			{
				Runtime.getRuntime().halt(5);
			}
			else if(_instance._shutdownMode == TASK_SHUTDOWN)
			{
				Runtime.getRuntime().halt(4);
			}
			else if(_instance._shutdownMode == TELL_RESTART)
			{
				Runtime.getRuntime().halt(7);
			}
			else if(_instance._shutdownMode == TELL_SHUTDOWN)
			{
				Runtime.getRuntime().halt(6);
			}
			else
			{
				Runtime.getRuntime().halt(0);
			}
		}
		else
		{
			countdown();

			_log.warn("GM shutdown countdown is over. " + MODE_TEXT[_shutdownMode] + " NOW!");
			switch(_shutdownMode)
			{
				case GM_SHUTDOWN:
					_instance.setMode(GM_SHUTDOWN);
					System.exit(0);
					break;

				case GM_RESTART:
					_instance.setMode(GM_RESTART);
					System.exit(2);
					break;

				case TASK_SHUTDOWN:
					_instance.setMode(TASK_SHUTDOWN);
					System.exit(4);
					break;

				case TASK_RESTART:
					_instance.setMode(TASK_RESTART);
					System.exit(5);
					break;

				case TELL_SHUTDOWN:
					_instance.setMode(TELL_SHUTDOWN);
					System.exit(6);
					break;

				case TELL_RESTART:
					_instance.setMode(TELL_RESTART);
					System.exit(7);
					break;
			}
		}
	}

	public void startShutdown(L2PcInstance activeChar, int seconds, boolean restart)
	{
		Announcements _an = Announcements.getInstance();
		_log.warn("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");

		if(restart)
		{
			_shutdownMode = GM_RESTART;
		}
		else
		{
			_shutdownMode = GM_SHUTDOWN;
		}

		if(_shutdownMode > 0)
		{
			_an.announceToAll("Server is " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");
			switch(seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
		}

		if(_counterInstance != null)
		{
			_counterInstance._abort();
		}

		_counterInstance = new Shutdown(seconds, restart, false, false);
		_counterInstance.start();
	}

	public int getCountdown()
	{
		return _secondsShut;
	}

	public void abort(L2PcInstance activeChar)
	{
		Announcements _an = Announcements.getInstance();
		_log.warn("GM: " + activeChar.getName() + "(" + activeChar.getObjectId() + ") issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
		_an.announceToAll("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!");
		_an = null;

		if(_counterInstance != null)
		{
			_counterInstance._abort();
		}
	}

	private void setMode(int mode)
	{
		_shutdownMode = mode;
	}

	private void _abort()
	{
		_shutdownMode = ABORT;
	}

	private void countdown()
	{
		try
		{
			while(_secondsShut > 0)
			{

				switch(_secondsShut)
				{
					case 540:
						SendServerQuit(540);
						break;
					case 480:
						SendServerQuit(480);
						break;
					case 420:
						SendServerQuit(420);
						break;
					case 360:
						SendServerQuit(360);
						break;
					case 300:
						SendServerQuit(300);
						break;
					case 240:
						SendServerQuit(240);
						break;
					case 180:
						SendServerQuit(180);
						break;
					case 120:
						SendServerQuit(120);
						break;
					case 60:
						LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN);
						SendServerQuit(60);
						break;
					case 30:
						SendServerQuit(30);
						break;
					case 10:
						SendServerQuit(10);
						break;
					case 5:
						SendServerQuit(5);
						break;
					case 4:
						SendServerQuit(4);
						break;
					case 3:
						SendServerQuit(3);
						break;
					case 2:
						SendServerQuit(2);
						break;
					case 1:
						SendServerQuit(1);
						break;
				}

				_secondsShut--;

				int delay = 1000;
				Thread.sleep(delay);

				if(_shutdownMode == ABORT)
				{
					break;
				}
			}
		}
		catch(InterruptedException e)
		{}
	}

	private synchronized void saveData()
	{
		Announcements _an = Announcements.getInstance();
		switch(_shutdownMode)
		{
			case SIGTERM:
				System.err.println("SIGTERM received. Shutting down NOW!");
				break;

			case GM_SHUTDOWN:
				System.err.println("GM shutdown received. Shutting down NOW!");
				break;

			case GM_RESTART:
				System.err.println("GM restart received. Restarting NOW!");
				break;

			case TASK_SHUTDOWN:
				System.err.println("Auto task shutdown received. Shutting down NOW!");
				break;

			case TASK_RESTART:
				System.err.println("Auto task restart received. Restarting NOW!");
				break;

			case TELL_SHUTDOWN:
				System.err.println("Telnet shutdown received. Shutting down NOW!");
				break;

			case TELL_RESTART:
				System.err.println("Telnet restart received. Restarting NOW!");
				break;
		}
		try
		{
			_an.announceToAll("Server is " + MODE_TEXT[_shutdownMode] + " NOW!");
			_an = null;
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}
		try
		{
			if((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.OFFLINE_RESTORE)
			{
				OfflineTradersTable.storeOffliners();
			}
		}
		catch(Throwable t)
		{
			_log.error("Error saving offline shops.",t);
		}

		// disconnect players
		try
		{
			disconnectAllCharacters();
			System.err.println("All players have been disconnected.");
		}
		catch (Throwable t)
		{
		}

		try
		{
			Thread.sleep(5000);
		}
		catch(InterruptedException e1)
		{
		}

		if(!SevenSigns.getInstance().isSealValidationPeriod())
		{
			SevenSignsFestival.getInstance().saveFestivalData(false);
		}
		SevenSigns.getInstance().saveSevenSignsData(null, true);
		System.err.println("Seven Signs Festival, general data && status have been saved.");
		FourSepulchersManager.getInstance().stop();
		
		RaidBossSpawnManager.getInstance().cleanUp();
		System.err.println("RaidBossSpawnManager: All Raid Boss info saved!!");
		GrandBossManager.getInstance().cleanUp();
		System.err.println("GrandBossManager: All Grand Boss info saved!!");
		TradeController.getInstance().dataCountStore();
		System.err.println("TradeController: All count Item Saved");
		try
		{
			Olympiad.getInstance().save();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.err.println("Olympiad System: Data saved!!");

		CursedWeaponsManager.getInstance().saveData();
		System.err.println("Cursed weapons data has been saved.");
		CastleManorManager.getInstance().save();
		System.err.println("Manors data has been saved.");
		QuestManager.getInstance().save();
		System.err.println("Global quests have been saved.");
		NpcTable.getInstance().saveNpc(null);

		if(Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().saveInDb();
			ItemsOnGroundManager.getInstance().cleanUp();
			System.err.println("ItemsOnGroundManager: All items on ground saved!!");
		}
		System.err.println("Data saved. All players disconnected, shutting down.");

		try
		{
			int delay = 10000;
			Thread.sleep(delay);
		}
		catch(InterruptedException e)
		{}
	}

	private void disconnectAllCharacters()
	{
		for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if (player == null)
				continue;
			
			try
			{
				player.store();
				if(player.getClient() != null)
				{
					player.getClient().sendPacket(ServerClose.STATIC_PACKET);
					player.getClient().setActiveChar(null);
					player.setClient(null);
				}
				player.deleteMe();
			}
			catch(Throwable t)
			{}
		}

		_log.info("Players: All players save to disk");

		try
		{
			Thread.sleep(10000);
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			try
			{
				player.closeNetConnection();
			}
			catch(Throwable t)
			{}
		}
	}

}