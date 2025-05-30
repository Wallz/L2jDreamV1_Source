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
package com.src.gameserver.model.entity.clanhallsiege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class FortressOfDead
{
	private final static Log _log = LogFactory.getLog(FortressOfDead.class);

	private static FortressOfDead _instance;
	private FastMap<Integer, DamageInfo> _clansDamageInfo;

	private static int START_DAY = 1;
	private static int HOUR = Config.DEVASTATED_HOUR;
	private static int MINUTES = Config.DEVASTATED_MINUTES;

	private static final int BOSS_ID = 35629;
	private static final int BOSS1_ID = 35630;
	private static final int BOSS2_ID = 35631;
	private static final int MESSENGER_ID = 35420;

	private ScheduledFuture<?> _lidia;
	private ScheduledFuture<?> _alfred;
	private ScheduledFuture<?> _gisele;
	private ScheduledFuture<?> _monsterdespawn;

	private L2Npc _minion1 = null;
	private L2Npc _minion2 = null;

	private ArrayList<MonsterLocation> _monsters = new ArrayList<MonsterLocation>();
	private ArrayList<L2Spawn> _spawns = new ArrayList<L2Spawn>();

	private Calendar _siegetime = Calendar.getInstance();

	public boolean _progress = false;

	public static FortressOfDead getInstance()
	{
		if(_instance == null)
		{
			_instance = new FortressOfDead();
		}

		return _instance;
	}

	private class DamageInfo
	{
		public L2Clan _clan;
		public long _damage;
	}

	private FortressOfDead()
	{
		if(Config.DEVASTATED_DAY == 1)
		{
			START_DAY = Calendar.MONDAY;
		}
		else if(Config.DEVASTATED_DAY == 2)
		{
			START_DAY = Calendar.TUESDAY;
		}
		else if(Config.DEVASTATED_DAY == 3)
		{
			START_DAY = Calendar.WEDNESDAY;
		}
		else if(Config.DEVASTATED_DAY == 4)
		{
			START_DAY = Calendar.THURSDAY;
		}
		else if(Config.DEVASTATED_DAY == 5)
		{
			START_DAY = Calendar.FRIDAY;
		}
		else if(Config.DEVASTATED_DAY == 6)
		{
			START_DAY = Calendar.SATURDAY;
		}
		else if(Config.DEVASTATED_DAY == 7)
		{
			START_DAY = Calendar.SUNDAY;
		}
		else
		{
			START_DAY = Calendar.MONDAY;
		}

		if(HOUR < 0 || HOUR > 23)
		{
			HOUR = 18;
		}
		if(MINUTES < 0 || MINUTES > 59)
		{
			MINUTES = 0;
		}

		_clansDamageInfo = new FastMap<Integer, DamageInfo>();

		synchronized (this)
		{
			setCalendarForNextSiege();
			long milliToSiege = getMilliToSiege();

			RunMessengerSpawn rms = new RunMessengerSpawn();
			ThreadPoolManager.getInstance().scheduleGeneral(rms, milliToSiege);
			_log.info("DevastatedCastle: " + milliToSiege / 60000 + " min. until siege begin");
			rms = null;
		}
	}

	private void setCalendarForNextSiege()
	{
		int daysToChange = getDaysToSiege();

		if(daysToChange == 7)
		{
			if(_siegetime.get(Calendar.HOUR_OF_DAY) < HOUR)
			{
				daysToChange = 0;
			}
			else if(_siegetime.get(Calendar.HOUR_OF_DAY) == HOUR && _siegetime.get(Calendar.MINUTE) < MINUTES)
			{
				daysToChange = 0;
			}
		}

		if(daysToChange > 0)
		{
			_siegetime.add(Calendar.DATE, daysToChange);
		}

		_siegetime.set(Calendar.HOUR_OF_DAY, HOUR);
		_siegetime.set(Calendar.MINUTE, MINUTES);
	}

	private int getDaysToSiege()
	{
		int numDays = _siegetime.get(Calendar.DAY_OF_WEEK) - START_DAY;

		if(numDays < 0)
		{
			return 0 - numDays;
		}

		return 7 - numDays;
	}

	private long getMilliToSiege()
	{
		long currTimeMillis = System.currentTimeMillis();
		long siegeTimeMillis = _siegetime.getTimeInMillis();

		return siegeTimeMillis - currTimeMillis;
	}

	protected class RunMessengerSpawn implements Runnable
	{
		@Override
		public void run()
		{
			MessengerSpawn();
		}
	}

	public void MessengerSpawn()
	{
		if(!ClanHallManager.getInstance().isFree(34))
		{
			ClanHallManager.getInstance().setFree(34);
		}

		Announce("Registration for the Fortress of Dead siege is open!");
		Announce("Only for 2 hours!");

		L2Npc result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(MESSENGER_ID);

			L2Spawn spawn = new L2Spawn(template);
			spawn.setLocx(179040);
			spawn.setLocy(-13717);
			spawn.setLocz(-2263);
			spawn.stopRespawn();
			result = spawn.spawnOne();
			template = null;
		}
		catch(Exception e)
		{
			_log.error("", e);
		}

		RunSiege rs = new RunSiege();
		ThreadPoolManager.getInstance().scheduleGeneral(rs, 14400000);

		ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 7200000);
		ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceInfo("Registration for the Fortress of Dead siege is over!"), 7200000);
		ThreadPoolManager.getInstance().scheduleGeneral(new AnnounceInfo("Siege will begin in 2 hours."), 7200000);

		result = null;
		rs = null;
	}

	protected class AnnounceInfo implements Runnable
	{
		String _message;

		public AnnounceInfo(String message)
		{
			_message = message;
		}

		@Override
		public void run()
		{
			Announce(_message);
		}
	}

	public void Announce(String message)
	{
		Announcements.getInstance().announceToAll(message);
	}

	protected class RunSiege implements Runnable
	{
		@Override
		public void run()
		{
			Siege();
		}
	}

	public void Siege()
	{
		L2Npc result = null;
		L2NpcTemplate template = null;
		L2Spawn spawn = null;

		ClanHall CH = ClanHallManager.getInstance().getClanHallById(34);
		CH.banishForeigners();
		CH.spawnDoor();

		setIsInProgress(true);

		try
		{
			fillMonsters();

			template = NpcTable.getInstance().getTemplate(BOSS_ID);
			spawn = new L2Spawn(template);
			spawn.setLocx(178298);
			spawn.setLocy(-17624);
			spawn.setLocz(-2194);
			spawn.stopRespawn();
			result = spawn.spawnOne();
			_lidia = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 3600000);

			template = NpcTable.getInstance().getTemplate(BOSS1_ID);
			spawn = new L2Spawn(template);
			spawn.setLocx(178298);
			spawn.setLocy(-17624);
			spawn.setLocz(-2194);
			spawn.stopRespawn();
			result = spawn.spawnOne();
			_alfred = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 3600000);

			template = NpcTable.getInstance().getTemplate(BOSS2_ID);
			spawn = new L2Spawn(template);
			spawn.setLocx(178298);
			spawn.setLocy(-17624);
			spawn.setLocz(-2194);
			spawn.stopRespawn();
			result = spawn.spawnOne();
			_gisele = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnTimer(result), 3600000);

			spawnMonsters();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}

		result = null;
		template = null;
		spawn = null;

		Announce("Fortress Of Dead castle has begun!");
	}

	private static class MonsterLocation
	{
		private int _id;
		private int _x;
		private int _y;
		private int _z;
		private int _heading;

		private MonsterLocation(int id, int x, int y, int z, int heading)
		{
			_id = id;
			_x = x;
			_y = y;
			_z = z;
			_heading = heading;
		}

		private int getId()
		{
			return _id;
		}

		private int getX()
		{
			return _x;
		}

		private int getY()
		{
			return _y;
		}

		private int getZ()
		{
			return _z;
		}

		private int getHeading()
		{
			return _heading;
		}

	}

	private void addMonster(int id, int x, int y, int z, int heading)
	{
		_monsters.add(new MonsterLocation(id, x, y, z, heading));
	}

	private void fillMonsters()
	{
		addMonster(35413, 178288, -14924, -2200, 6320);
		addMonster(35412, 178255, -14884, -2200, 6320);
		addMonster(35413, 178222, -14924, -2200, 6320);
		addMonster(35412, 178222, -14884, -2200, 6320);
		addMonster(35412, 178420, -14904, -2200, 6320);
		addMonster(35412, 178387, -14904, -2200, 6320);
		addMonster(35412, 178288, -14884, -2200, 6320);
		addMonster(35412, 178387, -14884, -2200, 6320);
		addMonster(35413, 178354, -14944, -2200, 6320);
		addMonster(35412, 178321, -14884, -2200, 6320);
		addMonster(35413, 178222, -14944, -2200, 6320);
		addMonster(35412, 178354, -14904, -2200, 6320);
		addMonster(35413, 178255, -14924, -2200, 6320);
		addMonster(35413, 178387, -14924, -2200, 6320);
		addMonster(35413, 178354, -14924, -2200, 6320);
		addMonster(35413, 178420, -14924, -2200, 6320);
		addMonster(35412, 178354, -14884, -2200, 6320);
		addMonster(35412, 178420, -14884, -2200, 6320);
		addMonster(35413, 178454, -14944, -2200, 6320);
		addMonster(35413, 178454, -14924, -2200, 6320);
		addMonster(35413, 178420, -14944, -2200, 6320);
		addMonster(35412, 178222, -14904, -2200, 6320);
		addMonster(35413, 178321, -14944, -2200, 6320);
		addMonster(35413, 178321, -14924, -2200, 6320);
		addMonster(35412, 178288, -14904, -2200, 6320);
		addMonster(35412, 178321, -14904, -2200, 6320);
		addMonster(35413, 178255, -14944, -2200, 6320);
		addMonster(35412, 178255, -14904, -2200, 6320);
		addMonster(35413, 178288, -14944, -2200, 6320);
		addMonster(35412, 178454, -14884, -2200, 6320);
		addMonster(35413, 178387, -14944, -2200, 6320);
		addMonster(35412, 178454, -14904, -2200, 6320);
		addMonster(35413, 179052, -15226, -2221, 6320);
		addMonster(35413, 179260, -15341, -2221, 6320);
		addMonster(35413, 179101, -15253, -2221, 6320);
		addMonster(35413, 179073, -15203, -2221, 6320);
		addMonster(35413, 179144, -15271, -2221, 6320);
		addMonster(35413, 179246, -15285, -2221, 6320);
		addMonster(35413, 179164, -15247, -2221, 6320);
		addMonster(35413, 179226, -15309, -2221, 6320);
		addMonster(35413, 179322, -15349, -2221, 6320);
		addMonster(35413, 179302, -15372, -2221, 6320);
		addMonster(35413, 179189, -15286, -2221, 6320);
		addMonster(35413, 179391, -15439, -2221, 6320);
		addMonster(35413, 179341, -15406, -2221, 6320);
		addMonster(35415, 179503, -15925, -2256, 6320);
		addMonster(35415, 179562, -15984, -2256, 6320);
		addMonster(35415, 179491, -15981, -2256, 6320);
		addMonster(35415, 179370, -16196, -2256, 6320);
		addMonster(35411, 179426, -16009, -2253, 6320);
		addMonster(35415, 179544, -15882, -2256, 6320);
		addMonster(35415, 179599, -15943, -2256, 6320);
		addMonster(35415, 179570, -15901, -2256, 6320);
		addMonster(35415, 179408, -16158, -2256, 6320);
		addMonster(35415, 179279, -16219, -2256, 6320);
		addMonster(35411, 179327, -16101, -2253, 6320);
		addMonster(35411, 179540, -16876, -2246, 6320);
		addMonster(35413, 179009, -15201, -2221, 6320);
		addMonster(35413, 178951, -14699, -2080, 6320);
		addMonster(35413, 178801, -14975, -2080, 6320);
		addMonster(35413, 178865, -14857, -2080, 6320);
		addMonster(35413, 178822, -14936, -2080, 6320);
		addMonster(35413, 178843, -14897, -2080, 6320);
		addMonster(35413, 178929, -14739, -2080, 6320);
		addMonster(35413, 178908, -14778, -2080, 6320);
		addMonster(35413, 178886, -14818, -2080, 6320);
		addMonster(35411, 177719, -15951, -2253, 6320);
		addMonster(35413, 177838, -15664, -2226, 6320);
		addMonster(35411, 177627, -15953, -2250, 6320);
		addMonster(35411, 177387, -15955, -2250, 6320);
		addMonster(35411, 177667, -15921, -2253, 6320);
		addMonster(35413, 177859, -15812, -2226, 6320);
		addMonster(35411, 177661, -16014, -2253, 6320);
		addMonster(35411, 177608, -15985, -2250, 6320);
		addMonster(35415, 177663, -16154, -2250, 6320);
		addMonster(35415, 177530, -16079, -2250, 6320);
		addMonster(35411, 177709, -16043, -2253, 6320);
		addMonster(35411, 177703, -15999, -2250, 6320);
		addMonster(35411, 177810, -16145, -2253, 6320);
		addMonster(35415, 177571, -16105, -2250, 6320);
		addMonster(35415, 177473, -16011, -2250, 6320);
		addMonster(35415, 177612, -16090, -2250, 6320);
		addMonster(35415, 177657, -16113, -2250, 6320);
		addMonster(35415, 177387, -15996, -2250, 6320);
		addMonster(35411, 177564, -15963, -2250, 6320);
		addMonster(35411, 177606, -16035, -2253, 6320);
		addMonster(35411, 177470, -15856, -2250, 6320);
		addMonster(35415, 177428, -15981, -2250, 6320);
		addMonster(35411, 177506, -15887, -2250, 6320);
		addMonster(35411, 177517, -15930, -2250, 6320);
		addMonster(35411, 177308, -15861, -2253, 6320);
		addMonster(35411, 177861, -16164, -2253, 6320);
		addMonster(35413, 177906, -15791, -2226, 6320);
		addMonster(35413, 177765, -15643, -2226, 6320);
		addMonster(35413, 177880, -15744, -2226, 6320);
		addMonster(35413, 177788, -15578, -2226, 6320);
		addMonster(35413, 177811, -15622, -2226, 6320);
		addMonster(35413, 177859, -15704, -2226, 6320);
		addMonster(35413, 177769, -15540, -2226, 6320);
		addMonster(35413, 177813, -15726, -2226, 6320);
		addMonster(35413, 177707, -15427, -2226, 6320);
		addMonster(35413, 177680, -15485, -2226, 6320);
		addMonster(35413, 177722, -15561, -2226, 6320);
		addMonster(35413, 177745, -15498, -2226, 6320);
		addMonster(35413, 177803, -14971, -2210, 6320);
		addMonster(35413, 177727, -15464, -2226, 6320);
		addMonster(35415, 177433, -16026, -2250, 6320);
		addMonster(35413, 177649, -14750, -2210, 6320);
		addMonster(35413, 177619, -14705, -2210, 6320);
		addMonster(35413, 177711, -14838, -2210, 6320);
		addMonster(35413, 177834, -15015, -2210, 6320);
		addMonster(35413, 177741, -14883, -2210, 6320);
		addMonster(35413, 177772, -14927, -2210, 6320);
		addMonster(35413, 177680, -14794, -2210, 6320);
		addMonster(35411, 177400, -15854, -2250, 6320);
		addMonster(35415, 179697, -17781, -2256, 6320);
		addMonster(35411, 179479, -17133, -2256, 6320);
		addMonster(35411, 179485, -17213, -2246, 6320);
		addMonster(35411, 179593, -16876, -2246, 6320);
		addMonster(35411, 179468, -17280, -2256, 6320);
		addMonster(35411, 179433, -16991, -2246, 6320);
		addMonster(35411, 179514, -17281, -2256, 6320);
		addMonster(35411, 179525, -17135, -2256, 6320);
		addMonster(35411, 179444, -16937, -2256, 6320);
		addMonster(35411, 179438, -16875, -2246, 6320);
		addMonster(35415, 179633, -17137, -2256, 6320);
		addMonster(35411, 179537, -17214, -2246, 6320);
		addMonster(35411, 179594, -17453, -2246, 6320);
		addMonster(35415, 179576, -17137, -2256, 6320);
		addMonster(35415, 179508, -17341, -2252, 6320);
		addMonster(35415, 179446, -17391, -2252, 6320);
		addMonster(35415, 179437, -17522, -2252, 6320);
		addMonster(35415, 179536, -17842, -2252, 6320);
		addMonster(35415, 179432, -17719, -2252, 6320);
		addMonster(35415, 179436, -17841, -2252, 6320);
		addMonster(35411, 179542, -17453, -2246, 6320);
		addMonster(35415, 179436, -17776, -2256, 6320);
		addMonster(35415, 179534, -17892, -2252, 6320);
		addMonster(35415, 179482, -17841, -2252, 6320);
		addMonster(35415, 179696, -17844, -2252, 6320);
		addMonster(35415, 179604, -17525, -2252, 6320);
		addMonster(35415, 179707, -17722, -2252, 6320);
		addMonster(35411, 179715, -17454, -2246, 6320);
		addMonster(35411, 179641, -17215, -2246, 6320);
		addMonster(35415, 179665, -17527, -2252, 6320);
		addMonster(35415, 179557, -17524, -2252, 6320);
		addMonster(35415, 179636, -17780, -2256, 6320);
		addMonster(35415, 179694, -17897, -2252, 6320);
		addMonster(35414, 178682, -18200, -2200, 6320);
		addMonster(35413, 178577, -18422, -2250, 6320);
		addMonster(35412, 178745, -18186, -2200, 6320);
		addMonster(35413, 178528, -18499, -2250, 6320);
		addMonster(35414, 178640, -18196, -2200, 6320);
		addMonster(35413, 178766, -18228, -2200, 6320);
		addMonster(35412, 178724, -18184, -2200, 6320);
		addMonster(35412, 178703, -18182, -2200, 6320);
		addMonster(35413, 178575, -18500, -2250, 6320);
		addMonster(35413, 178703, -18222, -2200, 6320);
		addMonster(35413, 178530, -18421, -2250, 6320);
		addMonster(35413, 178523, -18696, -2250, 6320);
		addMonster(35412, 178661, -18178, -2200, 6320);
		addMonster(35414, 178661, -18198, -2200, 6320);
		addMonster(35413, 178788, -18229, -2200, 6320);
		addMonster(35414, 178724, -18204, -2200, 6320);
		addMonster(35413, 178574, -18539, -2250, 6320);
		addMonster(35413, 178578, -18383, -2250, 6320);
		addMonster(35413, 178573, -18577, -2250, 6320);
		addMonster(35413, 178530, -18382, -2250, 6320);
		addMonster(35413, 178528, -18461, -2250, 6320);
		addMonster(35413, 178526, -18538, -2250, 6320);
		addMonster(35413, 178526, -18576, -2250, 6320);
		addMonster(35413, 178570, -18736, -2250, 6320);
		addMonster(35413, 178524, -18618, -2250, 6320);
		addMonster(35413, 178571, -18657, -2250, 6320);
		addMonster(35413, 178523, -18656, -2250, 6320);
		addMonster(35413, 178523, -18735, -2250, 6320);
		addMonster(35413, 178571, -18618, -2250, 6320);
		addMonster(35413, 178571, -18697, -2250, 6320);
		addMonster(35413, 178576, -18461, -2250, 6320);
		addMonster(35413, 178682, -18220, -2200, 6320);
		addMonster(35413, 178661, -18218, -2200, 6320);
		addMonster(35414, 178745, -18206, -2200, 6320);
		addMonster(35412, 178682, -18180, -2200, 6320);
		addMonster(35414, 178703, -18202, -2200, 6320);
		addMonster(35412, 178640, -18176, -2200, 6320);
		addMonster(35414, 178788, -18209, -2200, 6320);
		addMonster(35413, 178640, -18216, -2200, 6320);
		addMonster(35412, 178788, -18189, -2200, 6320);
		addMonster(35413, 178745, -18226, -2200, 6320);
		addMonster(35414, 178766, -18208, -2200, 6320);
		addMonster(35412, 178766, -18188, -2200, 6320);
		addMonster(35413, 178724, -18224, -2200, 6320);
		addMonster(35413, 178430, -16901, -2217, 6320);
		addMonster(35415, 178285, -16832, -2217, 6320);
		addMonster(35413, 178153, -16914, -2217, 6320);
		addMonster(35411, 178398, -16781, -2218, 6320);
		addMonster(35415, 178363, -16768, -2217, 6320);
		addMonster(35413, 178437, -16587, -2217, 6320);
		addMonster(35413, 178431, -16784, -2217, 6320);
		addMonster(35411, 178120, -16714, -2218, 6320);
		addMonster(35415, 178366, -16721, -2217, 6320);
		addMonster(35413, 178433, -16742, -2217, 6320);
		addMonster(35413, 178430, -16862, -2217, 6320);
		addMonster(35411, 178395, -16865, -2218, 6320);
		addMonster(35415, 178288, -16783, -2217, 6320);
		addMonster(35411, 178397, -16824, -2218, 6320);
		addMonster(35415, 178090, -16878, -2217, 6320);
		addMonster(35413, 178154, -16797, -2217, 6320);
		addMonster(35415, 178027, -16773, -2217, 6320);
		addMonster(35413, 178435, -16665, -2217, 6320);
		addMonster(35413, 178433, -16704, -2217, 6320);
		addMonster(35411, 178060, -16868, -2218, 6320);
		addMonster(35413, 178156, -16718, -2217, 6320);
		addMonster(35411, 178324, -16762, -2218, 6320);
		addMonster(35413, 177869, -16832, -2217, 6320);
		addMonster(35411, 178117, -16885, -2218, 6320);
		addMonster(35415, 178366, -16674, -2217, 6320);
		addMonster(35413, 178435, -16627, -2217, 6320);
		addMonster(35415, 178366, -16582, -2217, 6320);
		addMonster(35411, 178403, -16566, -2218, 6320);
		addMonster(35413, 178437, -16548, -2217, 6320);
		addMonster(35415, 178290, -16550, -2217, 6320);
		addMonster(35413, 178160, -16600, -2217, 6320);
		addMonster(35411, 178122, -16759, -2218, 6320);
		addMonster(35411, 178122, -16672, -2218, 6320);
		addMonster(35415, 178095, -16736, -2217, 6320);
		addMonster(35411, 178324, -16804, -2218, 6320);
		addMonster(35415, 178029, -16587, -2217, 6320);
		addMonster(35415, 178290, -16690, -2217, 6320);
		addMonster(35415, 178027, -16820, -2217, 6320);
		addMonster(35413, 178431, -16822, -2217, 6320);
		addMonster(35411, 178326, -16720, -2218, 6320);
		addMonster(35413, 178158, -16679, -2217, 6320);
		addMonster(35413, 178161, -16562, -2217, 6320);
		addMonster(35413, 178158, -16640, -2217, 6320);
		addMonster(35413, 177872, -16714, -2217, 6320);
		addMonster(35411, 178061, -16738, -2218, 6320);
		addMonster(35413, 177871, -16752, -2217, 6320);
		addMonster(35413, 177876, -16597, -2217, 6320);
		addMonster(35413, 177873, -16675, -2217, 6320);
		addMonster(35413, 177869, -16873, -2217, 6320);
		addMonster(35413, 177874, -16637, -2217, 6320);
		addMonster(35411, 178060, -16826, -2218, 6320);
		addMonster(35411, 178063, -16784, -2218, 6320);
		addMonster(35415, 178029, -16727, -2217, 6320);
		addMonster(35413, 177876, -16558, -2217, 6320);
		addMonster(35413, 177870, -16794, -2217, 6320);
		addMonster(35413, 177868, -16911, -2217, 6320);
		addMonster(35413, 178156, -16756, -2217, 6320);
		addMonster(35415, 178092, -16782, -2217, 6320);
		addMonster(35413, 178153, -16876, -2217, 6320);
		addMonster(35413, 178154, -16836, -2217, 6320);
		addMonster(35412, 177217, -17168, -2200, 6320);
		addMonster(35413, 177187, -17128, -2200, 6320);
		addMonster(35413, 177276, -17128, -2200, 6320);
		addMonster(35413, 177335, -17128, -2200, 6320);
		addMonster(35414, 177246, -17148, -2200, 6320);
		addMonster(35412, 177246, -17168, -2200, 6320);
		addMonster(35412, 177129, -17168, -2200, 6320);
		addMonster(35412, 177158, -17168, -2200, 6320);
		addMonster(35413, 177217, -17128, -2200, 6320);
		addMonster(35413, 177158, -17128, -2200, 6320);
		addMonster(35413, 177129, -17128, -2200, 6320);
		addMonster(35414, 177129, -17148, -2200, 6320);
		addMonster(35412, 177276, -17168, -2200, 6320);
		addMonster(35414, 177158, -17148, -2200, 6320);
		addMonster(35413, 177305, -17128, -2200, 6320);
		addMonster(35412, 177187, -17168, -2200, 6320);
		addMonster(35414, 177305, -17148, -2200, 6320);
		addMonster(35413, 177396, -17452, -2207, 6320);
		addMonster(35413, 177397, -17634, -2207, 6320);
		addMonster(35416, 177664, -17599, -2219, 6320);
		addMonster(35416, 177721, -17813, -2219, 6320);
		addMonster(35416, 177501, -17424, -2219, 6320);
		addMonster(35415, 177632, -17788, -2215, 6320);
		addMonster(35413, 177466, -17445, -2207, 6320);
		addMonster(35414, 177217, -17148, -2200, 6320);
		addMonster(35413, 177396, -17545, -2207, 6320);
		addMonster(35414, 177187, -17148, -2200, 6320);
		addMonster(35414, 177335, -17148, -2200, 6320);
		addMonster(35412, 177335, -17168, -2200, 6320);
		addMonster(35412, 177305, -17168, -2200, 6320);
		addMonster(35415, 177439, -17426, -2215, 6320);
		addMonster(35415, 177438, -17472, -2215, 6320);
		addMonster(35416, 177662, -17685, -2219, 6320);
		addMonster(35415, 177527, -17578, -2215, 6320);
		addMonster(35415, 177531, -17399, -2215, 6320);
		addMonster(35415, 177561, -17563, -2215, 6320);
		addMonster(35413, 177604, -17718, -2207, 6320);
		addMonster(35413, 177466, -17537, -2207, 6320);
		addMonster(35416, 177500, -17469, -2219, 6320);
		addMonster(35415, 177527, -17537, -2215, 6320);
		addMonster(35413, 177467, -17809, -2207, 6320);
		addMonster(35413, 177466, -17716, -2207, 6320);
		addMonster(35416, 177496, -17645, -2219, 6320);
		addMonster(35413, 177398, -17817, -2207, 6320);
		addMonster(35416, 177560, -17849, -2219, 6320);
		addMonster(35415, 177524, -17763, -2215, 6320);
		addMonster(35415, 177432, -17791, -2215, 6320);
		addMonster(35413, 177397, -17723, -2207, 6320);
		addMonster(35415, 177523, -17810, -2215, 6320);
		addMonster(35416, 177501, -17384, -2219, 6320);
		addMonster(35413, 177603, -17539, -2207, 6320);
		addMonster(35416, 177725, -17503, -2219, 6320);
		addMonster(35416, 177666, -17508, -2219, 6320);
		addMonster(35415, 177564, -17425, -2215, 6320);
		addMonster(35413, 177749, -17538, -2207, 6320);
		addMonster(35413, 177603, -17446, -2207, 6320);
		addMonster(35413, 177749, -17446, -2207, 6320);
		addMonster(35416, 177728, -17377, -2219, 6320);
		addMonster(35415, 177440, -17380, -2215, 6320);
		addMonster(35416, 177567, -17376, -2219, 6320);
		addMonster(35414, 177276, -17148, -2200, 6320);
		addMonster(35416, 177724, -17594, -2219, 6320);
		addMonster(35415, 177436, -17605, -2215, 6320);
		addMonster(35415, 177636, -17516, -2215, 6320);
		addMonster(35413, 177601, -17811, -2207, 6320);
		addMonster(35413, 177693, -17718, -2207, 6320);
		addMonster(35416, 177722, -17728, -2219, 6320);
		addMonster(35415, 177559, -17708, -2215, 6320);
		addMonster(35416, 177662, -17644, -2219, 6320);
		addMonster(35415, 177635, -17603, -2215, 6320);
		addMonster(35413, 177694, -17811, -2207, 6320);
		addMonster(35415, 177558, -17755, -2215, 6320);
		addMonster(35413, 177693, -17447, -2207, 6320);
		addMonster(35413, 177693, -17629, -2207, 6320);
		addMonster(35416, 177664, -17559, -2219, 6320);
		addMonster(35413, 177466, -17397, -2207, 6320);
		addMonster(35413, 177466, -17627, -2207, 6320);
		addMonster(35416, 177724, -17553, -2219, 6320);
		addMonster(35413, 177750, -17628, -2207, 6320);
		addMonster(35413, 177751, -17810, -2207, 6320);
		addMonster(35416, 177661, -17818, -2219, 6320);
		addMonster(35416, 177496, -17686, -2219, 6320);
		addMonster(35413, 177750, -17717, -2207, 6320);
		addMonster(35415, 177633, -17740, -2215, 6320);
		addMonster(35413, 177693, -17539, -2207, 6320);
		addMonster(35413, 177604, -17628, -2207, 6320);
		addMonster(35415, 177431, -17836, -2215, 6320);
		addMonster(35415, 177631, -17835, -2215, 6320);
		addMonster(35416, 177722, -17768, -2219, 6320);
		addMonster(35413, 177246, -17128, -2200, 6320);
	}

	public void spawnMonsters()
	{
		for(MonsterLocation ml : _monsters)
		{
			try
			{
				L2NpcTemplate template = NpcTable.getInstance().getTemplate(ml.getId());
				L2Spawn sp = new L2Spawn(template);
				sp.setAmount(1);
				sp.setLocx(ml.getX());
				sp.setLocy(ml.getY());
				sp.setLocz(ml.getZ());
				sp.setHeading(ml.getHeading());
				sp.setRespawnDelay(300);
				sp.setLocation(0);
				sp.init();
				_spawns.add(sp);
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
		}

		_monsterdespawn = ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnMonsters(), 3600000);
	}

	protected class DeSpawnMonsters implements Runnable
	{
		@Override
		public void run()
		{
			DeSpawn();
		}
	}

	public void DeSpawn()
	{
		for(L2Spawn sp : _spawns)
		{
			sp.stopRespawn();
			sp.getLastSpawn().doDie(sp.getLastSpawn());
		}
		_spawns.clear();
		setIsInProgress(false);
		_spawns = null;
	}

	protected class DeSpawnTimer implements Runnable
	{
		L2Npc _npc = null;

		public DeSpawnTimer(L2Npc npc)
		{
			_npc = npc;
		}

		@Override
		public void run()
		{
			if(_npc.getNpcId() == 35410)
			{
				Announce("Fortress Of Dead castle is over.");
				Announce("Nobody won! ClanHall belong to NPC until next siege.");

				ClanHall CH = ClanHallManager.getInstance().getClanHallById(34);
				CH.banishForeigners();
				CH.spawnDoor();
			}
			_npc.onDecay();
		}
	}

	public final boolean Conditions(L2PcInstance player)
	{
		if(player != null && player.getClan() != null && player.isClanLeader() && player.getClan().getAuctionBiddedAt() <= 0 && ClanHallManager.getInstance().getClanHallByOwner(player.getClan()) == null && player.getClan().getLevel() > 3)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean getIsInProgress()
	{
		return _progress;
	}

	public void setIsInProgress(boolean is)
	{
		_progress = is;
	}

	public void SiegeFinish()
	{
		L2Clan clanIdMaxDamage = null;
		long tempMaxDamage = 0;
		for(DamageInfo damageInfo : _clansDamageInfo.values())
		{
			if(damageInfo != null)
			{
				if(damageInfo._damage > tempMaxDamage)
				{
					tempMaxDamage = damageInfo._damage;
					clanIdMaxDamage = damageInfo._clan;
				}
			}
		}
		if(clanIdMaxDamage != null)
		{
			ClanHallManager.getInstance().setOwner(34, clanIdMaxDamage);
			clanIdMaxDamage.setReputationScore(clanIdMaxDamage.getReputationScore() + 600, true);
			update();

			Announce("Fortress Of Dead siege is over.");
			Announce("Now it is belong to: '" + clanIdMaxDamage.getName() + "' until next siege.");
		}
		else
		{
			Announce("Fortress Of Dead siege is over.");
			Announce("Nobody won! Clan Hall belong to NPC until next siege.");
		}

		DeSpawn();
		_minion1.onDecay();
		_minion2.onDecay();
		_lidia.cancel(true);
		_alfred.cancel(true);
		_gisele.cancel(true);
		_monsterdespawn.cancel(true);

		ClanHall CH = ClanHallManager.getInstance().getClanHallById(34);
		CH.banishForeigners();
		CH.spawnDoor();
		CH = null;
	}

	public void addSiegeDamage(L2Clan clan, long damage)
	{
		DamageInfo clanDamage = _clansDamageInfo.get(clan.getClanId());
		if(clanDamage != null)
		{
			clanDamage._damage += damage;
		}
		else
		{
			clanDamage = new DamageInfo();
			clanDamage._clan = clan;
			clanDamage._damage += damage;
			_clansDamageInfo.put(clan.getClanId(), clanDamage);
		}
	}

	private void update()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("UPDATE clanhall SET paidUntil = ?, paid = ? WHERE id = ?");
			statement.setLong(1, System.currentTimeMillis() + 59760000);
			statement.setInt(2, 1);
			statement.setInt(3, 34);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.warn("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}
}