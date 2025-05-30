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
package com.src.gameserver.model.entity.sevensigns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.AutoChatHandler;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.spawn.AutoSpawn;
import com.src.gameserver.model.spawn.AutoSpawn.AutoSpawnInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SignsSky;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class SevenSigns
{
	private static final Log _log = LogFactory.getLog(SevenSigns.class);

	private static SevenSigns _instance;

	public static final String SEVEN_SIGNS_DATA_FILE = "config/signs.properties";
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/sevensigns/";

	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;

	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;

	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;

	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 00;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;

	public static final int PERIOD_MINOR_LENGTH = 900000;
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;

	public static final int ANCIENT_ADENA_ID = 5575;
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	public static final int CERTIFICATE_OF_APPROVAL_ID = 6388;
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	public static final int ADENA_JOIN_DAWN_COST = 50000;

	public static final int ORATOR_NPC_ID = 31094;
	public static final int PREACHER_NPC_ID = 31093;
	public static final int MAMMON_MERCHANT_ID = 31113;
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	public static final int MAMMON_MARKETEER_ID = 31092;
	public static final int SPIRIT_IN_ID = 31111;
	public static final int SPIRIT_OUT_ID = 31112;
	public static final int LILITH_NPC_ID = 25283;
	public static final int ANAKIM_NPC_ID = 25286;
	public static final int CREST_OF_DAWN_ID = 31170;
	public static final int CREST_OF_DUSK_ID = 31171;
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;

	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;

	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;

	private final Calendar _calendar = Calendar.getInstance();

	protected int _activePeriod;
	protected int _currentCycle;
	protected double _dawnStoneScore;
	protected double _duskStoneScore;
	protected int _dawnFestivalScore;
	protected int _duskFestivalScore;
	protected int _compWinner;
	protected int _previousWinner;

	private Map<Integer, StatsSet> _signsPlayerData;

	private Map<Integer, Integer> _signsSealOwners;
	private Map<Integer, Integer> _signsDuskSealTotals;
	private Map<Integer, Integer> _signsDawnSealTotals;

	private static AutoSpawnInstance _merchantSpawn;
	private static AutoSpawnInstance _blacksmithSpawn;
	private static AutoSpawnInstance _spiritInSpawn;
	private static AutoSpawnInstance _spiritOutSpawn;
	private static AutoSpawnInstance _lilithSpawn;
	private static AutoSpawnInstance _anakimSpawn;
	private static AutoSpawnInstance _crestofdawnspawn;
	private static AutoSpawnInstance _crestofduskspawn;
	private static Map<Integer, AutoSpawnInstance> _oratorSpawns;
	private static Map<Integer, AutoSpawnInstance> _preacherSpawns;
	private static Map<Integer, AutoSpawnInstance> _marketeerSpawns;

	public SevenSigns()
	{
		_signsPlayerData = new FastMap<Integer, StatsSet>();
		_signsSealOwners = new FastMap<Integer, Integer>();
		_signsDuskSealTotals = new FastMap<Integer, Integer>();
		_signsDawnSealTotals = new FastMap<Integer, Integer>();

		try
		{
			restoreSevenSignsData();
		}
		catch(Exception e)
		{
			_log.error("SevenSigns: Failed to load configuration", e);
		}

		_log.info("SevenSigns: Currently in the " + getCurrentPeriodName() + " period!");
		initializeSeals();

		if(isSealValidationPeriod())
		{
			if(getCabalHighestScore() == CABAL_NULL)
			{
				_log.info("SevenSigns: The competition ended with a tie last week.");
			}
			else
			{
				_log.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " were victorious last week.");
			}
		}
		else if(getCabalHighestScore() == CABAL_NULL)
		{
			_log.info("SevenSigns: The competition, if the current trend continues, will end in a tie this week.");
		}
		else
		{
			_log.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " are in the lead this week.");
		}

		synchronized (this)
		{
			setCalendarForNextPeriodChange();
			long milliToChange = getMilliToPeriodChange();

			SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().scheduleGeneral(sspc, milliToChange);
			sspc = null;

			double numSecs = milliToChange / 1000 % 60;
			double countDown = (milliToChange / 1000 - numSecs) / 60;
			int numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			int numHours = (int) Math.floor(countDown % 24);
			int numDays = (int) Math.floor((countDown - numHours) / 24);

			_log.info("SevenSigns: Next period begins in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
		}

		spawnSevenSignsNPC();
	}

	public void spawnSevenSignsNPC()
	{
		_merchantSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(MAMMON_MERCHANT_ID, false);
		_blacksmithSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(MAMMON_BLACKSMITH_ID, false);
		_marketeerSpawns = AutoSpawn.getInstance().getAutoSpawnInstances(MAMMON_MARKETEER_ID);
		_spiritInSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(SPIRIT_IN_ID, false);
		_spiritOutSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(SPIRIT_OUT_ID, false);
		_lilithSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(LILITH_NPC_ID, false);
		_anakimSpawn = AutoSpawn.getInstance().getAutoSpawnInstance(ANAKIM_NPC_ID, false);
		_crestofdawnspawn = AutoSpawn.getInstance().getAutoSpawnInstance(CREST_OF_DAWN_ID, false);
		_crestofduskspawn = AutoSpawn.getInstance().getAutoSpawnInstance(CREST_OF_DUSK_ID, false);
		_oratorSpawns = AutoSpawn.getInstance().getAutoSpawnInstances(ORATOR_NPC_ID);
		_preacherSpawns = AutoSpawn.getInstance().getAutoSpawnInstances(PREACHER_NPC_ID);

		if(isSealValidationPeriod() || isCompResultsPeriod())
		{
			for(AutoSpawnInstance spawnInst : _marketeerSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
			}

			if(getSealOwner(SEAL_GNOSIS) == getCabalHighestScore() && getSealOwner(SEAL_GNOSIS) != CABAL_NULL)
			{
				if(!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_blacksmithSpawn.setBroadcast(false);
				}

				if(!AutoSpawn.getInstance().getAutoSpawnInstance(_blacksmithSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_blacksmithSpawn, true);
				}

				for(AutoSpawnInstance spawnInst : _oratorSpawns.values())
				{
					if(!AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
					}
				}

				for(AutoSpawnInstance spawnInst : _preacherSpawns.values())
				{
					if(!AutoSpawn.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawn.getInstance().setSpawnActive(spawnInst, true);
					}
				}

				if(!AutoChatHandler.getInstance().getAutoChatInstance(PREACHER_NPC_ID, false).isActive() && !AutoChatHandler.getInstance().getAutoChatInstance(ORATOR_NPC_ID, false).isActive())
				{
					AutoChatHandler.getInstance().setAutoChatActive(true);
				}
			}
			else
			{
				AutoSpawn.getInstance().setSpawnActive(_blacksmithSpawn, false);

				for(AutoSpawnInstance spawnInst : _oratorSpawns.values())
				{
					AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
				}

				for(AutoSpawnInstance spawnInst : _preacherSpawns.values())
				{
					AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
				}

				AutoChatHandler.getInstance().setAutoChatActive(false);
			}

			if(getSealOwner(SEAL_AVARICE) == getCabalHighestScore() && getSealOwner(SEAL_AVARICE) != CABAL_NULL)
			{
				if(!Config.ANNOUNCE_MAMMON_SPAWN)
				{
					_merchantSpawn.setBroadcast(false);
				}

				if(!AutoSpawn.getInstance().getAutoSpawnInstance(_merchantSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_merchantSpawn, true);
				}

				if(!AutoSpawn.getInstance().getAutoSpawnInstance(_spiritInSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_spiritInSpawn, true);
				}

				if(!AutoSpawn.getInstance().getAutoSpawnInstance(_spiritOutSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawn.getInstance().setSpawnActive(_spiritOutSpawn, true);
				}

				switch(getCabalHighestScore())
				{
					case CABAL_DAWN:
						if(!AutoSpawn.getInstance().getAutoSpawnInstance(_lilithSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, true);
						}

						AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, false);
						if(!AutoSpawn.getInstance().getAutoSpawnInstance(_crestofdawnspawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, true);
						}

						AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, false);
						break;

					case CABAL_DUSK:
						if(!AutoSpawn.getInstance().getAutoSpawnInstance(_anakimSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, true);
						}

						AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
						if(!AutoSpawn.getInstance().getAutoSpawnInstance(_crestofduskspawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, true);
						}

						AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, false);
						break;
				}
			}
			else
			{
				AutoSpawn.getInstance().setSpawnActive(_merchantSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, false);
				AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, false);
				AutoSpawn.getInstance().setSpawnActive(_spiritInSpawn, false);
				AutoSpawn.getInstance().setSpawnActive(_spiritOutSpawn, false);
			}
		}
		else
		{
			AutoSpawn.getInstance().setSpawnActive(_merchantSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_blacksmithSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_lilithSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_anakimSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_crestofdawnspawn, false);
			AutoSpawn.getInstance().setSpawnActive(_crestofduskspawn, false);
			AutoSpawn.getInstance().setSpawnActive(_spiritInSpawn, false);
			AutoSpawn.getInstance().setSpawnActive(_spiritOutSpawn, false);

			for(AutoSpawnInstance spawnInst : _oratorSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
			}

			for(AutoSpawnInstance spawnInst : _preacherSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
			}

			for(AutoSpawnInstance spawnInst : _marketeerSpawns.values())
			{
				AutoSpawn.getInstance().setSpawnActive(spawnInst, false);
			}

			AutoChatHandler.getInstance().setAutoChatActive(false);
		}
	}

	public static SevenSigns getInstance()
	{
		if(_instance == null)
		{
			_instance = new SevenSigns();
		}

		return _instance;
	}

	public static int calcContributionScore(int blueCount, int greenCount, int redCount)
	{
		int contrib = blueCount * BLUE_CONTRIB_POINTS;
		contrib += greenCount * GREEN_CONTRIB_POINTS;
		contrib += redCount * RED_CONTRIB_POINTS;
		return contrib;
	}

	public static int calcAncientAdenaReward(int blueCount, int greenCount, int redCount)
	{
		int reward = blueCount * SEAL_STONE_BLUE_VALUE;
		reward += greenCount * SEAL_STONE_GREEN_VALUE;
		reward += redCount * SEAL_STONE_RED_VALUE;
		return reward;
	}

	public static final String getCabalShortName(int cabal)
	{
		switch(cabal)
		{
			case CABAL_DAWN:
				return "dawn";
			case CABAL_DUSK:
				return "dusk";
		}
		return "No Cabal";
	}

	public static final String getCabalName(int cabal)
	{
		switch(cabal)
		{
			case CABAL_DAWN:
				return "Lords of Dawn";
			case CABAL_DUSK:
				return "Revolutionaries of Dusk";
		}
		return "No Cabal";
	}

	public static final String getSealName(int seal, boolean shortName)
	{
		String sealName = !shortName ? "Seal of " : "";

		switch(seal)
		{
			case SEAL_AVARICE:
				sealName += "Avarice";
				break;
			case SEAL_GNOSIS:
				sealName += "Gnosis";
				break;
			case SEAL_STRIFE:
				sealName += "Strife";
				break;
		}
		return sealName;
	}

	public final int getCurrentCycle()
	{
		return _currentCycle;
	}

	public final int getCurrentPeriod()
	{
		return _activePeriod;
	}

	private final int getDaysToPeriodChange()
	{
		int numDays = _calendar.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;

		if(numDays < 0)
			return 0 - numDays;

		return 7 - numDays;
	}

	public final long getMilliToPeriodChange()
	{
		long currTimeMillis = System.currentTimeMillis();
		long changeTimeMillis = _calendar.getTimeInMillis();

		return changeTimeMillis - currTimeMillis;
	}

	protected void setCalendarForNextPeriodChange()
	{
		switch(getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
				int daysToChange = getDaysToPeriodChange();

				if(daysToChange == 7)
				{
					if(_calendar.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
					{
						daysToChange = 0;
					}
					else if(_calendar.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR && _calendar.get(Calendar.MINUTE) < PERIOD_START_MINS)
					{
						daysToChange = 0;
					}
				}

				if(daysToChange > 0)
				{
					_calendar.add(Calendar.DATE, daysToChange);
				}

				_calendar.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_calendar.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
				_calendar.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
		}
	}

	public final String getCurrentPeriodName()
	{
		String periodName = null;

		switch(_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				periodName = "Quest Event Initialization";
				break;
			case PERIOD_COMPETITION:
				periodName = "Competition (Quest Event)";
				break;
			case PERIOD_COMP_RESULTS:
				periodName = "Quest Event Results";
				break;
			case PERIOD_SEAL_VALIDATION:
				periodName = "Seal Validation";
				break;
		}
		return periodName;
	}

	public final boolean isSealValidationPeriod()
	{
		return _activePeriod == PERIOD_SEAL_VALIDATION;
	}

	public final boolean isCompResultsPeriod()
	{
		return _activePeriod == PERIOD_COMP_RESULTS;
	}

	 /** 
       * returns true if the given date is in Seal Validation or in Quest Event Results period  
       * @param date 
       */ 
      public boolean isDateInSealValidPeriod(Calendar date) 
      { 
      long nextPeriodChange = getMilliToPeriodChange(); 
      long nextQuestStart = 0; 
      long nextValidStart = 0; 
      long tillDate = date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(); 
      while ((2 * PERIOD_MAJOR_LENGTH + 2 * PERIOD_MINOR_LENGTH) < tillDate) 
              tillDate -= (2 * PERIOD_MAJOR_LENGTH + 2 * PERIOD_MINOR_LENGTH); 
      while (tillDate < 0) 
              tillDate += (2 * PERIOD_MAJOR_LENGTH + 2 * PERIOD_MINOR_LENGTH); 
       
              switch (getCurrentPeriod()) 
              { 
                      case PERIOD_COMP_RECRUITING: 
                              nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH; 
                              nextQuestStart = nextValidStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH; 
                              break; 
                      case PERIOD_COMPETITION: 
                              nextValidStart = nextPeriodChange; 
                              nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH; 
                              break; 
                      case PERIOD_COMP_RESULTS: 
                              nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH; 
                              nextValidStart = nextQuestStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH; 
                              break; 
                      case PERIOD_SEAL_VALIDATION: 
                             nextQuestStart = nextPeriodChange; 
                             nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;                           
                             break; 
             } 
               
              if ((nextQuestStart < tillDate && tillDate < nextValidStart) || 
                              (nextValidStart < nextQuestStart && (tillDate < nextValidStart || nextQuestStart < tillDate))) 
                      return false; 
              return true; 
      }
 	
	public final int getCurrentScore(int cabal)
	{
		double totalStoneScore = _dawnStoneScore + _duskStoneScore;

		switch(cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return Math.round((float) (_dawnStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _dawnFestivalScore;
			case CABAL_DUSK:
				return Math.round((float) (_duskStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _duskFestivalScore;
		}
		return 0;
	}

	public final double getCurrentStoneScore(int cabal)
	{
		switch(cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return _dawnStoneScore;
			case CABAL_DUSK:
				return _duskStoneScore;
		}

		return 0;
	}

	public final int getCurrentFestivalScore(int cabal)
	{
		switch(cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return _dawnFestivalScore;
			case CABAL_DUSK:
				return _duskFestivalScore;
		}
		return 0;
	}

	public final int getCabalHighestScore()
	{
		if(getCurrentScore(CABAL_DUSK) == getCurrentScore(CABAL_DAWN))
		{
			return CABAL_NULL;
		}
		else if(getCurrentScore(CABAL_DUSK) > getCurrentScore(CABAL_DAWN))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_DAWN;
		}
	}

	public final int getSealOwner(int seal)
	{
		return _signsSealOwners.get(seal);
	}

	public final int getSealProportion(int seal, int cabal)
	{
		if(cabal == CABAL_NULL)
		{
			return 0;
		}
		else if(cabal == CABAL_DUSK)
		{
			return _signsDuskSealTotals.get(seal);
		}
		else
		{
			return _signsDawnSealTotals.get(seal);
		}
	}

	public final int getTotalMembers(int cabal)
	{
		int cabalMembers = 0;
		String cabalName = getCabalShortName(cabal);

		for(StatsSet sevenDat : _signsPlayerData.values())
		{
			if(sevenDat.getString("cabal").equals(cabalName))
			{
				cabalMembers++;
			}
		}

		cabalName = null;

		return cabalMembers;
	}

	public final StatsSet getPlayerData(L2PcInstance player)
	{
		if(!hasRegisteredBefore(player))
		{
			return null;
		}

		return _signsPlayerData.get(player.getObjectId());
	}

	public int getPlayerStoneContrib(L2PcInstance player)
	{
		if(!hasRegisteredBefore(player))
		{
			return 0;
		}

		int stoneCount = 0;

		StatsSet currPlayer = getPlayerData(player);

		stoneCount += currPlayer.getInteger("red_stones");
		stoneCount += currPlayer.getInteger("green_stones");
		stoneCount += currPlayer.getInteger("blue_stones");

		currPlayer = null;

		return stoneCount;
	}

	public int getPlayerContribScore(L2PcInstance player)
	{
		if(!hasRegisteredBefore(player))
		{
			return 0;
		}

		StatsSet currPlayer = getPlayerData(player);

		return currPlayer.getInteger("contribution_score");
	}

	public int getPlayerAdenaCollect(L2PcInstance player)
	{
		if(!hasRegisteredBefore(player))
		{
			return 0;
		}

		return _signsPlayerData.get(player.getObjectId()).getInteger("ancient_adena_amount");
	}

	public int getPlayerSeal(L2PcInstance player)
	{
		if(!hasRegisteredBefore(player))
		{
			return SEAL_NULL;
		}

		return getPlayerData(player).getInteger("seal");
	}

	public int getPlayerCabal(L2PcInstance player)
	{
		if(!hasRegisteredBefore(player))
		{
			return CABAL_NULL;
		}

		String playerCabal = getPlayerData(player).getString("cabal");

		if(playerCabal.equalsIgnoreCase("dawn"))
		{
			return CABAL_DAWN;
		}
		else if(playerCabal.equalsIgnoreCase("dusk"))
		{
			playerCabal = null;
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_NULL;
		}
	}

	protected void restoreSevenSignsData()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_obj_id, cabal, seal, red_stones, green_stones, blue_stones, " + "ancient_adena_amount, contribution_score FROM seven_signs");
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				int charObjId = rset.getInt("char_obj_id");

				StatsSet sevenDat = new StatsSet();
				sevenDat.set("char_obj_id", charObjId);
				sevenDat.set("cabal", rset.getString("cabal"));
				sevenDat.set("seal", rset.getInt("seal"));
				sevenDat.set("red_stones", rset.getInt("red_stones"));
				sevenDat.set("green_stones", rset.getInt("green_stones"));
				sevenDat.set("blue_stones", rset.getInt("blue_stones"));
				sevenDat.set("ancient_adena_amount", rset.getDouble("ancient_adena_amount"));
				sevenDat.set("contribution_score", rset.getDouble("contribution_score"));

				_signsPlayerData.put(charObjId, sevenDat);
			}

			rset.close();
			statement.close();

			statement = con.prepareStatement("SELECT * FROM seven_signs_status WHERE id = 0");
			rset = statement.executeQuery();

			while(rset.next())
			{
				_currentCycle = rset.getInt("current_cycle");
				_activePeriod = rset.getInt("active_period");
				_previousWinner = rset.getInt("previous_winner");

				_dawnStoneScore = rset.getDouble("dawn_stone_score");
				_dawnFestivalScore = rset.getInt("dawn_festival_score");
				_duskStoneScore = rset.getDouble("dusk_stone_score");
				_duskFestivalScore = rset.getInt("dusk_festival_score");

				_signsSealOwners.put(SEAL_AVARICE, rset.getInt("avarice_owner"));
				_signsSealOwners.put(SEAL_GNOSIS, rset.getInt("gnosis_owner"));
				_signsSealOwners.put(SEAL_STRIFE, rset.getInt("strife_owner"));

				_signsDawnSealTotals.put(SEAL_AVARICE, rset.getInt("avarice_dawn_score"));
				_signsDawnSealTotals.put(SEAL_GNOSIS, rset.getInt("gnosis_dawn_score"));
				_signsDawnSealTotals.put(SEAL_STRIFE, rset.getInt("strife_dawn_score"));
				_signsDuskSealTotals.put(SEAL_AVARICE, rset.getInt("avarice_dusk_score"));
				_signsDuskSealTotals.put(SEAL_GNOSIS, rset.getInt("gnosis_dusk_score"));
				_signsDuskSealTotals.put(SEAL_STRIFE, rset.getInt("strife_dusk_score"));
			}

			rset.close();
			statement.close();

			statement = con.prepareStatement("UPDATE seven_signs_status SET date = ? WHERE id = 0");
			statement.setInt(1, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
			statement.execute();

			statement.close();
			try
			{
				con.close();
			}
			catch(Exception e)
			{
			}

		}
		catch(SQLException e)
		{
			_log.error("SevenSigns: Unable to load Seven Signs data from database", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void saveSevenSignsData(L2PcInstance player, boolean updateSettings)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;

			for(StatsSet sevenDat : _signsPlayerData.values())
			{
				if(player != null)
				{
					if(sevenDat.getInteger("char_obj_id") != player.getObjectId())
					{
						continue;
					}
				}

				statement = con.prepareStatement("UPDATE seven_signs SET cabal = ?, seal = ?, red_stones = ?, " + "green_stones = ?, blue_stones = ?, " + "ancient_adena_amount = ?, contribution_score = ? " + "WHERE char_obj_id = ?");
				statement.setString(1, sevenDat.getString("cabal"));
				statement.setInt(2, sevenDat.getInteger("seal"));
				statement.setInt(3, sevenDat.getInteger("red_stones"));
				statement.setInt(4, sevenDat.getInteger("green_stones"));
				statement.setInt(5, sevenDat.getInteger("blue_stones"));
				statement.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
				statement.setDouble(7, sevenDat.getDouble("contribution_score"));
				statement.setInt(8, sevenDat.getInteger("char_obj_id"));
				statement.execute();

				statement.close();
			}

			if(updateSettings)
			{
				String sqlQuery = "UPDATE seven_signs_status SET current_cycle = ?, active_period = ?, previous_winner = ?, " + "dawn_stone_score = ?, dawn_festival_score = ?, dusk_stone_score = ?, dusk_festival_score = ?, " + "avarice_owner = ?, gnosis_owner = ?, strife_owner = ?, avarice_dawn_score = ?, gnosis_dawn_score = ?, " + "strife_dawn_score = ?, avarice_dusk_score = ?, gnosis_dusk_score = ?, strife_dusk_score = ?, " + "festival_cycle = ?, ";

				for(int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
				{
					sqlQuery += "accumulated_bonus" + String.valueOf(i) + " = ?, ";
				}

				sqlQuery += "date = ? WHERE id = 0";

				statement = con.prepareStatement(sqlQuery);
				statement.setInt(1, _currentCycle);
				statement.setInt(2, _activePeriod);
				statement.setInt(3, _previousWinner);
				statement.setDouble(4, _dawnStoneScore);
				statement.setInt(5, _dawnFestivalScore);
				statement.setDouble(6, _duskStoneScore);
				statement.setInt(7, _duskFestivalScore);
				statement.setInt(8, _signsSealOwners.get(SEAL_AVARICE));
				statement.setInt(9, _signsSealOwners.get(SEAL_GNOSIS));
				statement.setInt(10, _signsSealOwners.get(SEAL_STRIFE));
				statement.setInt(11, _signsDawnSealTotals.get(SEAL_AVARICE));
				statement.setInt(12, _signsDawnSealTotals.get(SEAL_GNOSIS));
				statement.setInt(13, _signsDawnSealTotals.get(SEAL_STRIFE));
				statement.setInt(14, _signsDuskSealTotals.get(SEAL_AVARICE));
				statement.setInt(15, _signsDuskSealTotals.get(SEAL_GNOSIS));
				statement.setInt(16, _signsDuskSealTotals.get(SEAL_STRIFE));
				statement.setInt(17, SevenSignsFestival.getInstance().getCurrentFestivalCycle());

				for(int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
				{
					statement.setInt(18 + i, SevenSignsFestival.getInstance().getAccumulatedBonus(i));
				}

				statement.setInt(18 + SevenSignsFestival.FESTIVAL_COUNT, Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
				statement.execute();

				statement.close();
				try
				{
					con.close();
				}
				catch(Exception e)
				{
				}

				sqlQuery = null;

			}
		}
		catch(SQLException e)
		{
			_log.error("SevenSigns: Unable to save data to database", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void resetPlayerData()
	{
		for(StatsSet sevenDat : _signsPlayerData.values())
		{
			int charObjId = sevenDat.getInteger("char_obj_id");

			sevenDat.set("cabal", "");
			sevenDat.set("seal", SEAL_NULL);
			sevenDat.set("contribution_score", 0);

			_signsPlayerData.put(charObjId, sevenDat);
		}
	}

	private boolean hasRegisteredBefore(L2PcInstance player)
	{
		return _signsPlayerData.containsKey(player.getObjectId());
	}

	public int setPlayerInfo(L2PcInstance player, int chosenCabal, int chosenSeal)
	{
		int charObjId = player.getObjectId();
		Connection con = null;
		PreparedStatement statement = null;
		StatsSet currPlayerData = getPlayerData(player);

		if(currPlayerData != null)
		{
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);

			_signsPlayerData.put(charObjId, currPlayerData);
		}
		else
		{
			currPlayerData = new StatsSet();
			currPlayerData.set("char_obj_id", charObjId);
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			currPlayerData.set("red_stones", 0);
			currPlayerData.set("green_stones", 0);
			currPlayerData.set("blue_stones", 0);
			currPlayerData.set("ancient_adena_amount", 0);
			currPlayerData.set("contribution_score", 0);

			_signsPlayerData.put(charObjId, currPlayerData);

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO seven_signs (char_obj_id, cabal, seal) VALUES (?, ?, ?)");
				statement.setInt(1, charObjId);
				statement.setString(2, getCabalShortName(chosenCabal));
				statement.setInt(3, chosenSeal);
				statement.execute();

				statement.close();
				try
				{
					con.close();
				}
				catch(Exception e)
				{
				}
			}
			catch(SQLException e)
			{
				_log.error("SevenSigns: Failed to save data", e);
			}
			finally
			{
				try
				{
					statement.close();
					statement = null;
				}
				catch(Exception e)
				{
				}

				try
				{
					con.close();
				}
				catch(Exception e)
				{
				}
				con = null;
			}
		}

		if(currPlayerData.getString("cabal") == "dawn")
		{
			_signsDawnSealTotals.put(chosenSeal, _signsDawnSealTotals.get(chosenSeal) + 1);
		}
		else
		{
			_signsDuskSealTotals.put(chosenSeal, _signsDuskSealTotals.get(chosenSeal) + 1);
		}

		currPlayerData = null;

		saveSevenSignsData(player, true);

		return chosenCabal;
	}

	public int getAncientAdenaReward(L2PcInstance player, boolean removeReward)
	{
		StatsSet currPlayer = getPlayerData(player);
		int rewardAmount = currPlayer.getInteger("ancient_adena_amount");

		currPlayer.set("red_stones", 0);
		currPlayer.set("green_stones", 0);
		currPlayer.set("blue_stones", 0);
		currPlayer.set("ancient_adena_amount", 0);

		if(removeReward)
		{
			_signsPlayerData.put(player.getObjectId(), currPlayer);
			saveSevenSignsData(player, true);
		}

		currPlayer = null;

		return rewardAmount;
	}

	public int addPlayerStoneContrib(L2PcInstance player, int blueCount, int greenCount, int redCount)
	{
		StatsSet currPlayer = getPlayerData(player);

		int contribScore = calcContributionScore(blueCount, greenCount, redCount);
		int totalAncientAdena = currPlayer.getInteger("ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
		int totalContribScore = currPlayer.getInteger("contribution_score") + contribScore;

		if(totalContribScore > Config.MAXIMUM_PLAYER_CONTRIB)
		{
			return -1;
		}

		currPlayer.set("red_stones", currPlayer.getInteger("red_stones") + redCount);
		currPlayer.set("green_stones", currPlayer.getInteger("green_stones") + greenCount);
		currPlayer.set("blue_stones", currPlayer.getInteger("blue_stones") + blueCount);
		currPlayer.set("ancient_adena_amount", totalAncientAdena);
		currPlayer.set("contribution_score", totalContribScore);
		_signsPlayerData.put(player.getObjectId(), currPlayer);

		currPlayer = null;

		switch(getPlayerCabal(player))
		{
			case CABAL_DAWN:
				_dawnStoneScore += contribScore;
				break;
			case CABAL_DUSK:
				_duskStoneScore += contribScore;
				break;
		}

		saveSevenSignsData(player, true);

		return contribScore;
	}

	public void addFestivalScore(int cabal, int amount)
	{
		if(cabal == CABAL_DUSK)
		{
			_duskFestivalScore += amount;

			if(_dawnFestivalScore >= amount)
			{
				_dawnFestivalScore -= amount;
			}
		}
		else
		{
			_dawnFestivalScore += amount;

			if(_duskFestivalScore >= amount)
			{
				_duskFestivalScore -= amount;
			}
		}
	}

	public void sendCurrentPeriodMsg(L2PcInstance player)
	{
		switch(getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING:
				player.sendPacket(new SystemMessage(SystemMessageId.PREPARATIONS_PERIOD_BEGUN));
				break;
			case PERIOD_COMPETITION:
				player.sendPacket(new SystemMessage(SystemMessageId.COMPETITION_PERIOD_BEGUN));
				break;
			case PERIOD_COMP_RESULTS:
				player.sendPacket(new SystemMessage(SystemMessageId.RESULTS_PERIOD_BEGUN));
				break;
			case PERIOD_SEAL_VALIDATION:
				player.sendPacket(new SystemMessage(SystemMessageId.VALIDATION_PERIOD_BEGUN));
				break;
		}
	}

	public void sendMessageToAll(SystemMessageId sysMsgId)
	{
		for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			player.sendPacket(new SystemMessage(sysMsgId));
		}
	}

	protected void initializeSeals()
	{
		for(Integer currSeal : _signsSealOwners.keySet())
		{
			int sealOwner = _signsSealOwners.get(currSeal);

			if(sealOwner != CABAL_NULL)
			{
				if(isSealValidationPeriod())
				{
					_log.info("SevenSigns: The " + getCabalName(sealOwner) + " have won the " + getSealName(currSeal, false) + ".");
				}
				else
				{
					_log.info("SevenSigns: The " + getSealName(currSeal, false) + " is currently owned by " + getCabalName(sealOwner) + ".");
				}
			}
			else
			{
				_log.info("SevenSigns: The " + getSealName(currSeal, false) + " remains unclaimed.");
			}
		}
	}

	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}

	protected void calcNewSealOwners()
	{
		for(Integer currSeal : _signsDawnSealTotals.keySet())
		{
			int prevSealOwner = _signsSealOwners.get(currSeal);
			int newSealOwner = CABAL_NULL;
			int dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			int totalDawnMembers = getTotalMembers(CABAL_DAWN) == 0 ? 1 : getTotalMembers(CABAL_DAWN);
			int dawnPercent = Math.round((float) dawnProportion / (float) totalDawnMembers * 100);
			int duskProportion = getSealProportion(currSeal, CABAL_DUSK);
			int totalDuskMembers = getTotalMembers(CABAL_DUSK) == 0 ? 1 : getTotalMembers(CABAL_DUSK);
			int duskPercent = Math.round((float) duskProportion / (float) totalDuskMembers * 100);

			switch(prevSealOwner)
			{
				case CABAL_NULL:
					switch(getCabalHighestScore())
					{
						case CABAL_NULL:
							newSealOwner = CABAL_NULL;
							break;
						case CABAL_DAWN:
							if(dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if(duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DAWN:
					switch(getCabalHighestScore())
					{
						case CABAL_NULL:
							if(dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if(dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if(duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else if(dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
				case CABAL_DUSK:
					switch(getCabalHighestScore())
					{
						case CABAL_NULL:
							if(duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DAWN:
							if(dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else if(duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						case CABAL_DUSK:
							if(duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
					}
					break;
			}

			_signsSealOwners.put(currSeal, newSealOwner);

			switch(currSeal)
			{
				case SEAL_AVARICE:
					if(newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_AVARICE);
					}
					else if(newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_AVARICE);
					}
					break;
				case SEAL_GNOSIS:
					if(newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_GNOSIS);
					}
					else if(newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_GNOSIS);
					}
					break;
				case SEAL_STRIFE:
					if(newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.DAWN_OBTAINED_STRIFE);
					}
					else if(newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.DUSK_OBTAINED_STRIFE);
					}

					CastleManager.getInstance().validateTaxes(newSealOwner);
					break;
			}
		}
	}

	protected void teleLosingCabalFromDungeons(String compWinner)
	{
		for(L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers())
		{
			StatsSet currPlayer = getPlayerData(onlinePlayer);

			if(isSealValidationPeriod() || isCompResultsPeriod())
			{
				if(!onlinePlayer.isGM() && onlinePlayer.isIn7sDungeon() && !currPlayer.getString("cabal").equals(compWinner))
				{
					onlinePlayer.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					onlinePlayer.setIsIn7sDungeon(false);
					onlinePlayer.sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
				}
			}
			else
			{
				if(!onlinePlayer.isGM() && onlinePlayer.isIn7sDungeon() && !currPlayer.getString("cabal").equals(""))
				{
					onlinePlayer.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					onlinePlayer.setIsIn7sDungeon(false);
					onlinePlayer.sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
				}
			}
			currPlayer = null;
		}
	}

	protected class SevenSignsPeriodChange implements Runnable
	{
		@Override
		public void run()
		{
			final int periodEnded = getCurrentPeriod();
			_activePeriod++;

			switch(periodEnded)
			{
				case PERIOD_COMP_RECRUITING:

					SevenSignsFestival.getInstance().startFestivalManager();

					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_BEGUN);
					break;
				case PERIOD_COMPETITION:

					sendMessageToAll(SystemMessageId.QUEST_EVENT_PERIOD_ENDED);

					int compWinner = getCabalHighestScore();

					SevenSignsFestival.getInstance().getFestivalManagerSchedule().cancel(false);

					calcNewSealOwners();

					switch(compWinner)
					{
						case CABAL_DAWN:
							sendMessageToAll(SystemMessageId.DAWN_WON);
							break;
						case CABAL_DUSK:
							sendMessageToAll(SystemMessageId.DUSK_WON);
							break;
					}

					_previousWinner = compWinner;
					break;
				case PERIOD_COMP_RESULTS:

					initializeSeals();

					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_BEGUN);

					_log.info("SevenSigns: The " + getCabalName(_previousWinner) + " have won the competition with " + getCurrentScore(_previousWinner) + " points!");
					break;
				case PERIOD_SEAL_VALIDATION:

					SevenSignsFestival.getInstance().rewardHighRanked();

					_activePeriod = PERIOD_COMP_RECRUITING;

					sendMessageToAll(SystemMessageId.SEAL_VALIDATION_PERIOD_ENDED);

					resetPlayerData();
					resetSeals();

					SevenSignsFestival.getInstance().resetFestivalData(false);

					_dawnStoneScore = 0;
					_duskStoneScore = 0;

					_dawnFestivalScore = 0;
					_duskFestivalScore = 0;

					_currentCycle++;

					break;
			}

			saveSevenSignsData(null, true);

			teleLosingCabalFromDungeons(getCabalShortName(getCabalHighestScore()));

			SignsSky ss = new SignsSky();

			for(L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				player.sendPacket(ss);
			}

			ss = null;

			spawnSevenSignsNPC();

			_log.info("SevenSigns: The " + getCurrentPeriodName() + " period has begun!");

			setCalendarForNextPeriodChange();

			// make sure that all the scheduled siege dates are in the Seal Validation period 
            List<Castle> castles = CastleManager.getInstance().getCastles(); 
            for (Castle castle : castles) 
            { 
                castle.getSiege().correctSiegeDateTime(); 
            }
		 	
			SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPoolManager.getInstance().scheduleGeneral(sspc, getMilliToPeriodChange());
		}
	}
}