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

package com.src.gameserver.model.entity.olympiad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

import javolution.text.TextBuilder;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.managers.OlympiadStadiaManager;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.Hero;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.object.L2FastList;
import com.src.util.protection.nProtect;
import com.src.util.protection.nProtect.RestrictionType;

public class Olympiad
{
	protected static final Log _log = LogFactory.getLog(Olympiad.class);

	private static Olympiad _instance;
	protected static Map<Integer, StatsSet> _nobles;
	protected static L2FastList<StatsSet> _heroesToBe;
	protected static L2FastList<L2PcInstance> _nonClassBasedRegisters;
	protected static Map<Integer, L2FastList<L2PcInstance>> _classBasedRegisters;

	private static final String OLYMPIAD_DATA_FILE = "config/other/olympiad.info";
	public static final String OLYMPIAD_HTML_FILE = "data/html/olympiad/";
	private static final String OLYMPIAD_LOAD_NOBLES = "SELECT * from olympiad_nobles";
	private static final String OLYMPIAD_SAVE_NOBLES = "INSERT INTO olympiad_nobles " + "values (?, ?, ?, ?, ?)";
	private static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE olympiad_nobles set " + "olympiad_points = ?, competitions_done = ? where char_id = ?";
	private static final String OLYMPIAD_GET_HEROS = "SELECT char_id, char_name from " + "olympiad_nobles where class_id = ? and competitions_done >= 9 order by " + "olympiad_points desc, competitions_done desc";
	private static final String GET_EACH_CLASS_LEADER = "SELECT char_name from " + "olympiad_nobles where class_id = ? order by olympiad_points desc, " + "competitions_done desc";
	private static final String GET_EACH_CLASS_LEADER_A = "SELECT char_name from " + "olympiad_nobles where class_id = ? order by char_name";
	private static final String OLYMPIAD_DELETE_ALL = "DELETE from olympiad_nobles";
	
	private static final int[] HERO_IDS =
	{
		88, 89, 90, 91, 92, 93, 94,95, 96,
		97, 98, 99, 100, 101, 102, 103, 104,
		105, 106, 107, 108, 109, 110, 111, 112,
		113, 114,115, 116, 117, 118, 131, 132,
		133, 134
	};

	private static final int COMP_START = Config.OLY_START_TIME; // 6PM
	private static final int COMP_MIN = Config.OLY_MIN; // 00 mins
	private static final long COMP_PERIOD = Config.OLY_CPERIOD; // 6 hours
	protected static final long BATTLE_PERIOD = Config.OLY_BATTLE; // 6 mins
	protected static final long BATTLE_WAIT = Config.OLY_BWAIT; // 10mins
	protected static final long INITIAL_WAIT = Config.OLY_IWAIT; // 5mins
	protected static final long WEEKLY_PERIOD = Config.OLY_WPERIOD; // 1 week
	protected static final long VALIDATION_PERIOD = Config.OLY_VPERIOD; // 24 hours

	private static final int DEFAULT_POINTS = 18;
	protected static final int WEEKLY_POINTS = 3;

	public static final String CHAR_ID = "char_id";
	public static final String CLASS_ID = "class_id";
	public static final String CHAR_NAME = "char_name";
	public static final String POINTS = "olympiad_points";
	public static final String COMP_DONE = "competitions_done";

	protected long _olympiadEnd;
	protected long _validationEnd;
	protected int _period;
	protected long _nextWeeklyChange;
	protected int _currentCycle;
	private long _compEnd;
	private Calendar _compStart;
	protected static boolean _inCompPeriod;
	protected static boolean _isOlympiadEnd;
	protected static boolean _compStarted = false;
	protected static boolean _battleStarted;
	protected static boolean _cycleTerminated;
	protected ScheduledFuture<?> _scheduledCompStart;
	protected ScheduledFuture<?> _scheduledCompEnd;
	protected ScheduledFuture<?> _scheduledOlympiadEnd;
	protected ScheduledFuture<?> _scheduledManagerTask;
	protected ScheduledFuture<?> _scheduledWeeklyTask;
	protected ScheduledFuture<?> _scheduledValdationTask;

	protected static final OlympiadStadia[] STADIUMS =
	{
		new OlympiadStadia(-20814, -21189, -3030),
		new OlympiadStadia(-120324, -225077, -3331),
		new OlympiadStadia(-102495, -209023, -3331),
		new OlympiadStadia(-120156, -207378, -3331),
		new OlympiadStadia(-87628, -225021, -3331),
		new OlympiadStadia(-81705, -213209, -3331),
		new OlympiadStadia(-87593, -207339, -3331),
		new OlympiadStadia(-93709, -218304, -3331),
		new OlympiadStadia(-77157, -218608, -3331),
		new OlympiadStadia(-69682, -209027, -3331),
		new OlympiadStadia(-76887, -201256, -3331),
		new OlympiadStadia(-109985, -218701, -3331),
		new OlympiadStadia(-126367, -218228, -3331),
		new OlympiadStadia(-109629, -201292, -3331),
		new OlympiadStadia(-87523, -240169, -3331),
		new OlympiadStadia(-81748, -245950, -3331),
		new OlympiadStadia(-77123, -251473, -3331),
		new OlympiadStadia(-69778, -241801, -3331),
		new OlympiadStadia(-76754, -234014, -3331),
		new OlympiadStadia(-93742, -251032, -3331),
		new OlympiadStadia(-87466, -257752, -3331),
		new OlympiadStadia(-114413, -213241, -3331)
	};

	protected static OlympiadManager _manager;

	public static Olympiad getInstance()
	{
		if(_instance == null)
		{
			_instance = new Olympiad();
		}
		return _instance;
	}

	public Olympiad()
	{
		
	}

	public static Integer getStadiumCount()
	{
		return OlympiadManager.STADIUMS.length;
	}
	
	public void load() throws IOException, SQLException
	{
		_nobles = new FastMap<Integer, StatsSet>();

		Properties OlympiadProperties = new Properties();
		InputStream is = new FileInputStream(new File("./" + OLYMPIAD_DATA_FILE));
		OlympiadProperties.load(is);
		is.close();

		_currentCycle = Integer.parseInt(OlympiadProperties.getProperty("CurrentCycle", "1"));
		_period = Integer.parseInt(OlympiadProperties.getProperty("Period", "0"));
		_olympiadEnd = Long.parseLong(OlympiadProperties.getProperty("OlympiadEnd", "0"));
		_validationEnd = Long.parseLong(OlympiadProperties.getProperty("ValdationEnd", "0"));
		_nextWeeklyChange = Long.parseLong(OlympiadProperties.getProperty("NextWeeklyChange", "0"));

		switch(_period)
		{
			case 0:
				if(_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis())
				{
					setNewOlympiadEnd();
				}
				else
				{
					_isOlympiadEnd = false;
				}
				break;
			case 1:
				if(_validationEnd > Calendar.getInstance().getTimeInMillis())
				{
					_isOlympiadEnd = true;

					_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
						@Override
						public void run()
						{
							_period = 0;
							_currentCycle++;
							deleteNobles();
							setNewOlympiadEnd();
							init();
						}
					}, getMillisToValidationEnd());
				}
				else
				{
					_currentCycle++;
					_period = 0;
					deleteNobles();
					setNewOlympiadEnd();
				}
				break;
			default:
				_log.warn("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
				return;
		}

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				StatsSet statDat = new StatsSet();
				int charId = rset.getInt(CHAR_ID);
				statDat.set(CLASS_ID, rset.getInt(CLASS_ID));
				statDat.set(CHAR_NAME, rset.getString(CHAR_NAME));
				statDat.set(POINTS, rset.getInt(POINTS));
				statDat.set(COMP_DONE, rset.getInt(COMP_DONE));
				statDat.set("to_save", false);
				_nobles.put(charId, statDat);
			}
			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		synchronized (this)
		{
			_log.info("Olympiad System: Loading Olympiad System....");
			if(_period == 0)
			{
				_log.info("Olympiad System: Currently in Olympiad Period");
			}
			else
			{
				_log.info("Olympiad System: Currently in Validation Period");
			}

			_log.info("Olympiad System: Period Ends....");

			long milliToEnd;
			if(_period == 0)
			{
				milliToEnd = getMillisToOlympiadEnd();
			}
			else
			{
				milliToEnd = getMillisToValidationEnd();
			}

			double numSecs = milliToEnd / 1000 % 60;
			double countDown = (milliToEnd / 1000 - numSecs) / 60;
			int numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			int numHours = (int) Math.floor(countDown % 24);
			int numDays = (int) Math.floor((countDown - numHours) / 24);

			_log.info("Olympiad System: In " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");

			if(_period == 0)
			{
				_log.info("Olympiad System: Next Weekly Change is in....");

				milliToEnd = getMillisToWeekChange();

				double numSecs2 = milliToEnd / 1000 % 60;
				double countDown2 = (milliToEnd / 1000 - numSecs2) / 60;
				int numMins2 = (int) Math.floor(countDown2 % 60);
				countDown2 = (countDown2 - numMins2) / 60;
				int numHours2 = (int) Math.floor(countDown2 % 24);
				int numDays2 = (int) Math.floor((countDown2 - numHours2) / 24);

				_log.info("Olympiad System: " + numDays2 + " days, " + numHours2 + " hours and " + numMins2 + " mins.");
			}
		}
		_log.info("Olympiad System: Loaded " + _nobles.size() + " Nobles");

		if(_period == 0)
		{
			init();
		}
	}

	protected void init()
	{
		if(_period == 1)
		{
			return;
		}

		_nonClassBasedRegisters = new L2FastList<L2PcInstance>();
		_classBasedRegisters = new FastMap<Integer, L2FastList<L2PcInstance>>();

		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

		_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED).addNumber(_currentCycle));
				Announcements.getInstance().announceToAll("Olympiad Validation Period has began.");

				_isOlympiadEnd = true;
				if(_scheduledManagerTask != null)
				{
					_scheduledManagerTask.cancel(true);
				}

				if(_scheduledWeeklyTask != null)
				{
					_scheduledWeeklyTask.cancel(true);
				}

				Calendar validationEnd = Calendar.getInstance();
				_validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;

				saveNobleData();

				_period = 1;

				sortHerosToBe();

				giveHeroBonus();

				Hero.getInstance().computeNewHeroes(_heroesToBe);

				try
				{
					save();
				}
				catch(Exception e)
				{
					_log.error("Olympiad System: Failed to save Olympiad configuration", e);
				}

				_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						Announcements.getInstance().announceToAll("Olympiad Validation Period has ended.");
						_period = 0;
						_currentCycle++;
						deleteNobles();
						setNewOlympiadEnd();
						init();
					}
				}, getMillisToValidationEnd());
			}
		}, getMillisToOlympiadEnd());

		updateCompStatus();
		scheduleWeeklyChange();
	}

	public boolean registerNoble(L2PcInstance noble, boolean classBased)
	{
		if(noble.getKarma() > 0)
		{
			noble.sendMessage("You can't participate to Olympiad with karma.");
			return false;
		}

		if(!_inCompPeriod)
		{
			noble.sendPacket(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS));
			return false;
		}

		if(noble.isCursedWeaponEquiped())
		{
			noble.sendMessage("You can't participate to Olympiad while holding a cursed weapon.");
			return false;
		}


        if(noble.isAio())
        {
                noble.sendMessage("You can't participate To Olympiad whit Aio Character.");
                return false;
        }

		if(!noble.isNoble())
		{
			noble.sendPacket(new SystemMessage(SystemMessageId.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD));
			return false;
		}


		if(noble.getBaseClass() != noble.getClassId().getId())
		{
			noble.sendPacket(new SystemMessage(SystemMessageId.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER));
			return false;
		}

		if(!nProtect.getInstance().checkRestriction(noble, RestrictionType.RESTRICT_OLYMPIAD))
		{
			return false;
		}

		if(!_nobles.containsKey(noble.getObjectId()))
		{
			StatsSet statDat = new StatsSet();
			statDat.set(CLASS_ID, noble.getClassId().getId());
			statDat.set(CHAR_NAME, noble.getName());
			statDat.set(POINTS, DEFAULT_POINTS);
			statDat.set(COMP_DONE, 0);
			statDat.set("to_save", true);

			_nobles.put(noble.getObjectId(), statDat);
		}

		if(_classBasedRegisters.containsKey(noble.getClassId().getId()))
		{
			L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			for(L2PcInstance partecipant : classed)
			{
				if(partecipant != null)
				{
					if(partecipant.getObjectId() == noble.getObjectId())
					{
						noble.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_TO_PARTICIPATE_IN_THE_GAME_FOR_YOUR_CLASS));
						return false;
					}
				}
				else
				{
					classed.remove(partecipant);
				}
			}
		}

		for(L2PcInstance partecipant : _nonClassBasedRegisters)
		{
			if(partecipant != null)
			{
				if(partecipant.getObjectId() == noble.getObjectId())
				{
					noble.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME));
					return false;
				}
			}
			else
			{
				_nonClassBasedRegisters.remove(partecipant);
			}
		}

		for(L2OlympiadGame g : _manager.getOlympiadGames().values())
		{
			if(g == null)
			{
				continue;
			}

			for(L2PcInstance player : g.getPlayers())
			{
				if(player.getObjectId() == noble.getObjectId())
				{
					noble.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME));
					return false;
				}
			}
		}
		if(classBased && getNoblePoints(noble.getObjectId()) < 3)
		{
			noble.sendMessage("Cant register when you have less than 3 points.");
			return false;
		}

		if(!classBased && getNoblePoints(noble.getObjectId()) < 5)
		{
			noble.sendMessage("Cant register when you have less than 5 points.");
			return false;
		}

		if(classBased)
		{
			if(_classBasedRegisters.containsKey(noble.getClassId().getId()))
			{
				L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
				classed.add(noble);

				_classBasedRegisters.remove(noble.getClassId().getId());
				_classBasedRegisters.put(noble.getClassId().getId(), classed);

				noble.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES));
			}
			else
			{
				L2FastList<L2PcInstance> classed = new L2FastList<L2PcInstance>();
				classed.add(noble);

				_classBasedRegisters.put(noble.getClassId().getId(), classed);

				noble.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES));
			}
		}
		else
		{
			_nonClassBasedRegisters.add(noble);
			noble.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES));
		}
		return true;
	}

	public boolean isRegistered(L2PcInstance noble)
	{
		if(_nonClassBasedRegisters == null)
		{
			return false;
		}

		if(_classBasedRegisters == null)
		{
			return false;
		}

		if(!_nonClassBasedRegisters.contains(noble))
		{
			if(!_classBasedRegisters.containsKey(noble.getClassId().getId()))
			{
				return false;
			}
			else
			{
				L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
				if(classed == null || !classed.contains(noble))
				{
					return false;
				}
			}
		}
		return true;
	}

	public boolean unRegisterNoble(L2PcInstance noble)
	{
		if(!_inCompPeriod)
		{
			noble.sendPacket(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS));
			return false;
		}

		if(!noble.isNoble())
		{
			noble.sendPacket(new SystemMessage(SystemMessageId.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD));
			return false;
		}

		if(!isRegistered(noble))
		{
			noble.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME));
			return false;
		}

		if(_nonClassBasedRegisters.contains(noble))
		{
			_nonClassBasedRegisters.remove(noble);
		}
		else
		{
			L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
			classed.remove(noble);

			_classBasedRegisters.remove(noble.getClassId().getId());
			_classBasedRegisters.put(noble.getClassId().getId(), classed);
		}

		for(L2OlympiadGame game : _manager.getOlympiadGames().values())
		{
			if(game == null)
			{
				continue;
			}

			if(game._playerOne.getObjectId() == noble.getObjectId() || game._playerTwo.getObjectId() == noble.getObjectId())
			{
				noble.sendMessage("Can't Unregister whilst you are already selected for a game.");
				return false;
			}
		}

		noble.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME));

		return true;
	}

	public void removeDisconnectedCompetitor(L2PcInstance player)
	{
		if(_manager == null || _manager.getOlympiadInstance(player.getOlympiadGameId()) == null)
		{
			return;
		}

		_manager.getOlympiadInstance(player.getOlympiadGameId()).handleDisconnect(player);
	}

	private void updateCompStatus()
	{
		synchronized (this)
		{
			long milliToStart = getMillisToCompBegin();

			double numSecs = milliToStart / 1000 % 60;
			double countDown = (milliToStart / 1000 - numSecs) / 60;
			int numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			int numHours = (int) Math.floor(countDown % 24);
			int numDays = (int) Math.floor((countDown - numHours) / 24);

			_log.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");

			_log.info("Olympiad System: Event starts/started : " + _compStart.getTime());
		}

		_scheduledCompStart = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if(isOlympiadEnd())
				{
					return;
				}

				_inCompPeriod = true;
				OlympiadManager om = new OlympiadManager();

				Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED));
				_log.info("Olympiad System: Olympiad Game Started");

				Thread olyCycle = new Thread(om);
				olyCycle.start();

				_scheduledCompEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						if(isOlympiadEnd())
						{
							return;
						}
						_inCompPeriod = false;
						Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED));
						_log.info("Olympiad System: Olympiad Game Ended");
						try
						{
							while(_battleStarted)
							{
								try
								{
									Thread.sleep(60000);
								}
								catch(InterruptedException e)
								{
								}
							}
							save();
						}
						catch(Exception e)
						{
							_log.error("Olympiad System: Failed to save Olympiad configuration", e);
						}

						init();
					}
				}, getMillisToCompEnd());
			}
		}, getMillisToCompBegin());
	}

	private long getMillisToOlympiadEnd()
	{
		return _olympiadEnd - Calendar.getInstance().getTimeInMillis();
	}

	public void manualSelectHeroes()
	{
		Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED).addNumber(_currentCycle));
		Announcements.getInstance().announceToAll("Olympiad Validation Period has began.");

		_isOlympiadEnd = true;
		if(_scheduledManagerTask != null)
		{
			_scheduledManagerTask.cancel(true);
		}

		if(_scheduledWeeklyTask != null)
		{
			_scheduledWeeklyTask.cancel(true);
		}

		if(_scheduledOlympiadEnd != null)
		{
			_scheduledOlympiadEnd.cancel(true);
		}

		Calendar validationEnd = Calendar.getInstance();
		_validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;

		saveNobleData();

		_period = 1;

		sortHerosToBe();

		giveHeroBonus();

		Hero.getInstance().computeNewHeroes(_heroesToBe);

		try
		{
			save();
		}
		catch(Exception e)
		{
			_log.error("Olympiad System: Failed to save Olympiad configuration", e);
		}

		_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				Announcements.getInstance().announceToAll("Olympiad Validation Period has ended.");
				_period = 0;
				_currentCycle++;
				deleteNobles();
				setNewOlympiadEnd();
				init();
			}
		}, getMillisToValidationEnd());
	}

	protected long getMillisToValidationEnd()
	{
		if(_validationEnd > Calendar.getInstance().getTimeInMillis())
		{
			return _validationEnd - Calendar.getInstance().getTimeInMillis();
		}

		return 10L;
	}

	public boolean isOlympiadEnd()
	{
		return _isOlympiadEnd;
	}

	protected void setNewOlympiadEnd()
	{
		Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_STARTED).addNumber(_currentCycle));

		Calendar currentTime = Calendar.getInstance();
		currentTime.add(Calendar.MONTH, 1);
		currentTime.set(Calendar.DAY_OF_MONTH, 1);
		currentTime.set(Calendar.AM_PM, Calendar.AM);
		currentTime.set(Calendar.HOUR, 12);
		currentTime.set(Calendar.MINUTE, 0);
		currentTime.set(Calendar.SECOND, 0);

		if(Config.OLY_NORMAL_MODE)
		{
			_olympiadEnd = currentTime.getTimeInMillis();
		}
		else
		{
			_olympiadEnd = currentTime.getTimeInMillis() + (1000 * 60 * 60 * 24 * Config.OLY_DAYS);
		}

		Calendar nextChange = Calendar.getInstance();
		_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;

		_isOlympiadEnd = false;
	}

	public boolean inCompPeriod()
	{
		return _inCompPeriod;
	}

	private long getMillisToCompBegin()
	{
		if(_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && _compEnd > Calendar.getInstance().getTimeInMillis())
		{
			return 10L;
		}

		if(_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
		{
			return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
		}

		return setNewCompBegin();
	}

	private long setNewCompBegin()
	{
		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

		_log.info("Olympiad System: New Schedule @ " + _compStart.getTime());

		return _compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}

	protected long getMillisToCompEnd()
	{
		return _compEnd - Calendar.getInstance().getTimeInMillis();
	}

	private long getMillisToWeekChange()
	{
		if(_nextWeeklyChange > Calendar.getInstance().getTimeInMillis())
		{
			return _nextWeeklyChange - Calendar.getInstance().getTimeInMillis();
		}

		return 10L;
	}

	private void scheduleWeeklyChange()
	{
		_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				addWeeklyPoints();
				_log.info("Olympiad System: Added weekly points to nobles");

				Calendar nextChange = Calendar.getInstance();
				_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
			}
		}, getMillisToWeekChange(), WEEKLY_PERIOD);
	}

	protected synchronized void addWeeklyPoints()
	{
		if(_period == 1)
		{
			return;
		}

		for(Integer nobleId : _nobles.keySet())
		{
			StatsSet nobleInfo = _nobles.get(nobleId);
			int currentPoints = nobleInfo.getInteger(POINTS);
			currentPoints += WEEKLY_POINTS;
			nobleInfo.set(POINTS, currentPoints);

			_nobles.remove(nobleId);
			_nobles.put(nobleId, nobleInfo);
		}
	}

	public FastMap<Integer, String> getMatchList()
	{
		return _manager == null ? null : _manager.getAllTitles();
	}
	
	public L2PcInstance[] getPlayers(int Id)
	{
		if(_manager == null || _manager.getOlympiadInstance(Id) == null)
		{
			return null;
		}

		L2PcInstance[] players = _manager.getOlympiadInstance(Id).getPlayers();
		return players;
	}

	public int getCurrentCycle()
	{
		return _currentCycle;
	}

	public static void addSpectator(int id, L2PcInstance spectator, boolean storeCoords)
	{
		if (getInstance().isRegisteredInComp(spectator))
		{
			spectator.sendPacket(new SystemMessage(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME));
			return;
		}
		
		spectator.enterOlympiadObserverMode(STADIUMS[id].getCoordinates()[0], STADIUMS[id].getCoordinates()[1], STADIUMS[id].getCoordinates()[2], id);
		if (_manager.getOlympiadInstance(id) != null)
		{
			_manager.getOlympiadInstance(id).sendPlayersStatus(spectator);
		}
	}

	public static int getSpectatorArena(L2PcInstance player)
	{
		for (int i = 0; i < OlympiadManager.STADIUMS.length; i++)
		{
			if (OlympiadManager.STADIUMS[i].getSpectators(i).contains(player))
				return i;
		}
		return -1;
	}
	
	public static void removeSpectator(int id, L2PcInstance spectator)
	{
		if(_manager == null || _manager.getOlympiadInstance(id) == null)
		{
			return;
		}

		_manager.getOlympiadInstance(id).removeSpectator(spectator);
	}

	public L2FastList<L2PcInstance> getSpectators(int id)
	{
		if(_manager == null || _manager.getOlympiadInstance(id) == null)
		{
			return null;
		}

		return _manager.getOlympiadInstance(id).getSpectators();
	}

	public Map<Integer, L2OlympiadGame> getOlympiadGames()
	{
		return _manager.getOlympiadGames();
	}

	public boolean playerInStadia(L2PcInstance player)
	{
		return OlympiadStadiaManager.getInstance().getStadium(player) != null;
	}

	public int[] getWaitingList()
	{
		int[] array = new int[2];

		if(!inCompPeriod())
		{
			return null;
		}

		int classCount = 0;

		if(_classBasedRegisters.size() != 0)
		{
			for(L2FastList<L2PcInstance> classed : _classBasedRegisters.values())
			{
				classCount += classed.size();
			}
		}

		array[0] = classCount;
		array[1] = _nonClassBasedRegisters.size();

		return array;
	}

	protected synchronized void saveNobleData()
	{
		Connection con = null;

		if(_nobles == null)
		{
			return;
		}

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			for(Integer nobleId : _nobles.keySet())
			{
				StatsSet nobleInfo = _nobles.get(nobleId);

				if(nobleInfo == null)
				{
					continue;
				}

				int charId = nobleId;
				int classId = nobleInfo.getInteger(CLASS_ID);
				String charName = nobleInfo.getString(CHAR_NAME);
				int points = nobleInfo.getInteger(POINTS);
				int compDone = nobleInfo.getInteger(COMP_DONE);
				boolean toSave = nobleInfo.getBool("to_save");

				if(toSave)
				{
					statement = con.prepareStatement(OLYMPIAD_SAVE_NOBLES);
					statement.setInt(1, charId);
					statement.setInt(2, classId);
					statement.setString(3, charName);
					statement.setInt(4, points);
					statement.setInt(5, compDone);
					statement.execute();

					statement.close();

					nobleInfo.set("to_save", false);

					_nobles.remove(nobleId);
					_nobles.put(nobleId, nobleInfo);
				}
				else
				{
					statement = con.prepareStatement(OLYMPIAD_UPDATE_NOBLES);
					statement.setInt(1, points);
					statement.setInt(2, compDone);
					statement.setInt(3, charId);
					statement.execute();
					statement.close();
				}
			}
		}
		catch(SQLException e)
		{
			_log.error("Olympiad System: Couldnt save nobles info in db", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void sortHerosToBe()
	{
		if(_period != 1)
		{
			return;
		}

		_heroesToBe = new L2FastList<StatsSet>();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			StatsSet hero;

			for(int element : HERO_IDS)
			{
				statement = con.prepareStatement(OLYMPIAD_GET_HEROS);
				statement.setInt(1, element);
				rset = statement.executeQuery();

				if(rset.next())
				{
					hero = new StatsSet();
					hero.set(CLASS_ID, element);
					hero.set(CHAR_ID, rset.getInt(CHAR_ID));
					hero.set(CHAR_NAME, rset.getString(CHAR_NAME));

					_heroesToBe.add(hero);
				}

				statement.close();
				rset.close();
			}
		}
		catch(SQLException e)
		{
			_log.error("Olympiad System: Couldnt read heros from db", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public L2FastList<String> getClassLeaderBoard(int classId)
	{
		L2FastList<String> names = new L2FastList<String>();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			statement = con.prepareStatement(GET_EACH_CLASS_LEADER_A);
			if(_period == 1)
			{
				statement = con.prepareStatement(GET_EACH_CLASS_LEADER);
			}
			statement.setInt(1, classId);
			rset = statement.executeQuery();

			while(rset.next())
			{
				names.add(rset.getString(CHAR_NAME));
			}

			statement.close();
			rset.close();

			return names;
		}
		catch(SQLException e)
		{
			_log.error("Olympiad System: Couldnt heros from db", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		return names;

	}

	protected void giveHeroBonus()
	{
		if(_heroesToBe.size() == 0)
		{
			return;
		}

		for(StatsSet hero : _heroesToBe)
		{
			int charId = hero.getInteger(CHAR_ID);

			StatsSet noble = _nobles.get(charId);
			int currentPoints = noble.getInteger(POINTS);
			currentPoints += Config.OLY_HERO_POINTS;
			noble.set(POINTS, currentPoints);

			_nobles.remove(charId);
			_nobles.put(charId, noble);
		}
	}

	public int getNoblessePasses(int objId)
	{
		if(_period != 1 || _nobles.size() == 0)
		{
			return 0;
		}

		StatsSet noble = _nobles.get(objId);
		if(noble == null)
		{
			return 0;
		}

		int points = noble.getInteger(POINTS);
		if(points <= Config.OLY_MIN_POINT_FOR_EXCH)
		{
			return 0;
		}

		noble.set(POINTS, 0);
		_nobles.remove(objId);
		_nobles.put(objId, noble);

		points *= Config.OLY_GP_PER_POINT;

		return points;
	}

	public boolean isRegisteredInComp(L2PcInstance player)
	{
		boolean result = false;

		if(_nonClassBasedRegisters != null && _nonClassBasedRegisters.contains(player))
		{
			result = true;
		}
		else if(_classBasedRegisters != null && _classBasedRegisters.containsKey(player.getClassId().getId()))
		{
			L2FastList<L2PcInstance> classed = _classBasedRegisters.get(player.getClassId().getId());
			if(classed.contains(player))
			{
				result = true;
			}
		}

		if(_inCompPeriod)
		{
			if(_manager != null && _manager.getOlympiadGames() != null)
			{
				for(L2OlympiadGame game : _manager.getOlympiadGames().values())
				{
					if(game._playerOne.getObjectId() == player.getObjectId() || game._playerTwo.getObjectId() == player.getObjectId())
					{
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}

	public int getNoblePoints(int objId)
	{
		if(_nobles.size() == 0)
		{
			return 0;
		}

		StatsSet noble = _nobles.get(objId);
		if(noble == null)
		{
			return 0;
		}
		int points = noble.getInteger(POINTS);

		return points;
	}

	public int getCompetitionDone(int objId)
	{
		if(_nobles.size() == 0)
		{
			return 0;
		}

		StatsSet noble = _nobles.get(objId);
		if(noble == null)
		{
			return 0;
		}
		int points = noble.getInteger(COMP_DONE);

		return points;
	}

	protected void deleteNobles()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_DELETE_ALL);
			statement.execute();
			statement.close();
		}
		catch(SQLException e)
		{
			_log.error("Olympiad System: Couldnt delete nobles from db", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		_nobles.clear();
	}

	public void save() throws IOException
	{
		saveNobleData();

		Properties OlympiadProperties = new Properties();
		FileOutputStream fos = new FileOutputStream(new File(Config.DATAPACK_ROOT, OLYMPIAD_DATA_FILE));

		OlympiadProperties.setProperty("CurrentCycle", String.valueOf(_currentCycle));
		OlympiadProperties.setProperty("Period", String.valueOf(_period));
		OlympiadProperties.setProperty("OlympiadEnd", String.valueOf(_olympiadEnd));
		OlympiadProperties.setProperty("ValdationEnd", String.valueOf(_validationEnd));
		OlympiadProperties.setProperty("NextWeeklyChange", String.valueOf(_nextWeeklyChange));

		OlympiadProperties.store(fos, "Olympiad Properties");
		fos.close();
	}

	public void logoutPlayer(L2PcInstance player)
	{
		_classBasedRegisters.remove(Integer.valueOf(player.getClassId().getId()));
		_nonClassBasedRegisters.remove(player);
	}
	
	public static void sendMatchList(L2PcInstance player)
	{
		NpcHtmlMessage message = new NpcHtmlMessage(0);
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<center><br>Grand Olympiad Game View<table width=270 border=0 bgcolor=\"000000\">");
		replyMSG.append("<tr><td fixwidth=30>NO.</td><td fixwidth=60>Status</td><td>Player1 / Player2</td></tr>");

		FastMap<Integer, String> matches = getInstance().getMatchList();
        for (int i = 0; i < Olympiad.getStadiumCount(); i++)
        {
        	int arenaID = i + 1;
        	String players = "&nbsp;";
        	String state = "Initial State";
        	if (matches.containsKey(i))
        	{
        		state = "In Progress";
            	players = matches.get(i);
            }
        	replyMSG.append("<tr><td fixwidth=30><a action=\"bypass -h OlympiadArenaChange " + i + "\">" +
        			arenaID + "</a></td><td fixwidth=60>" + state + "</td><td>" + players + "</td></tr>");
        }
        replyMSG.append("</table></center></body></html>");

        message.setHtml(replyMSG.toString());
        player.sendPacket(message);
	}
	
	public static void bypassChangeArena(String command, L2PcInstance player)
	{
		String [] commands = command.split(" ");
		int id = Integer.parseInt(commands[1]);
        int arena = getSpectatorArena(player);
        if (arena >= 0)
        {
            Olympiad.removeSpectator(arena, player);
        }
        Olympiad.addSpectator(id, player, false);
	}
}