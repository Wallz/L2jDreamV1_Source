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
package com.src.gameserver.model.actor.instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.knownlist.BoatKnownList;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.OnVehicleCheckLocation;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.VehicleDeparture;
import com.src.gameserver.network.serverpackets.VehicleInfo;
import com.src.gameserver.templates.chars.L2CharTemplate;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.thread.ThreadPoolManager;

public class L2BoatInstance extends L2Character
{
	private static final Log _log = LogFactory.getLog(L2BoatInstance.class);

	public float boatSpeed;

	private class L2BoatTrajet
	{
		private Map<Integer, L2BoatPoint> _path;

		public int idWaypoint1;
		public int idWTicket1;
		public int ntx1;
		public int nty1;
		public int ntz1;
		public int max;
		public String boatName;
		public String npc1;
		public String sysmess10_1;
		public String sysmess5_1;
		public String sysmess1_1;
		public String sysmessb_1;
		public String sysmess0_1;

		protected class L2BoatPoint
		{
			public int speed1;
			public int speed2;
			public int x;
			public int y;
			public int z;
			public int time;
		}

		public L2BoatTrajet(int pIdWaypoint1, int pIdWTicket1, int pNtx1, int pNty1, int pNtz1, String pNpc1, String pSysmess10_1, String pSysmess5_1, String pSysmess1_1, String pSysmess0_1, String pSysmessb_1, String pBoatname)
		{
			idWaypoint1 = pIdWaypoint1;
			idWTicket1 = pIdWTicket1;
			ntx1 = pNtx1;
			nty1 = pNty1;
			ntz1 = pNtz1;
			npc1 = pNpc1;
			sysmess10_1 = pSysmess10_1;
			sysmess5_1 = pSysmess5_1;
			sysmess1_1 = pSysmess1_1;
			sysmessb_1 = pSysmessb_1;
			sysmess0_1 = pSysmess0_1;
			boatName = pBoatname;
			loadBoatPath();
		}

		public void parseLine(String line)
		{
			_path = new FastMap<Integer, L2BoatPoint>();
			StringTokenizer st = new StringTokenizer(line, ";");
			Integer.parseInt(st.nextToken());
			max = Integer.parseInt(st.nextToken());
			for(int i = 0; i < max; i++)
			{
				L2BoatPoint bp = new L2BoatPoint();
				bp.speed1 = Integer.parseInt(st.nextToken());
				bp.speed2 = Integer.parseInt(st.nextToken());
				bp.x = Integer.parseInt(st.nextToken());
				bp.y = Integer.parseInt(st.nextToken());
				bp.z = Integer.parseInt(st.nextToken());
				bp.time = Integer.parseInt(st.nextToken());
				_path.put(i, bp);
			}
			st = null;
			return;
		}

		@SuppressWarnings("resource")
		private void loadBoatPath()
		{
			LineNumberReader lnr = null;
			try
			{
				File doorData = new File(Config.DATAPACK_ROOT, "data/csv/boatpath.csv");
				lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));
				doorData = null;

				String line = null;
				while((line = lnr.readLine()) != null)
				{
					if(line.trim().length() == 0 || !line.startsWith(idWaypoint1 + ";"))
					{
						continue;
					}
					parseLine(line);
					return;
				}
				_log.warn("No path for boat " + boatName + " !!!");
			}
			catch(FileNotFoundException e)
			{
				_log.warn("boatpath.csv is missing in data folder");
			}
			catch(Exception e)
			{
				_log.error("error while creating boat table", e);
			}
			finally
			{
				try
				{
					lnr.close();
					lnr = null;
				}
				catch(Exception e1)
				{
				}
			}
		}

		public int state(int state, L2BoatInstance _boat)
		{
			if(state < max)
			{
				L2BoatPoint bp = _path.get(state);
				double dx = _boat.getX() - bp.x;
				double dy = _boat.getY() - bp.y;
				double distance = Math.sqrt(dx * dx + dy * dy);
				double cos = dx / distance;
				double sin = dy / distance;

				_boat.getPosition().setHeading((int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381) + 32768);

				_boat._vd = new VehicleDeparture(_boat, bp.speed1, bp.speed2, bp.x, bp.y, bp.z);
				boatSpeed = bp.speed1;
				_boat.moveToLocation(bp.x, bp.y, bp.z, (float) bp.speed1);
				Collection<L2PcInstance> knownPlayers = _boat.getKnownList().getKnownPlayers().values();
				if(knownPlayers == null || knownPlayers.isEmpty())
					return bp.time;
				for(L2PcInstance player : knownPlayers)
				{
					player.sendPacket(_boat._vd);
				}
				knownPlayers = null;

				if(bp.time == 0)
				{
					bp.time = 1;
				}

				return bp.time;
			}
			else
			{
				return 0;
			}
		}

	}

	private String _name;
	protected L2BoatTrajet _t1;
	protected L2BoatTrajet _t2;
	protected int _cycle = 0;
	protected VehicleDeparture _vd = null;
	private Map<Integer, L2PcInstance> _inboat;

	public L2BoatInstance(int objectId, L2CharTemplate template, String name)
	{
		super(objectId, template);
		super.setKnownList(new BoatKnownList(this));
		_name = name;
	}

	public void moveToLocation(int x, int y, int z, float speed)
	{
		final int curX = getX();
		final int curY = getY();

		final int dx = x - curX;
		final int dy = y - curY;
		double distance = Math.sqrt(dx * dx + dy * dy);

		double cos = dx / distance;
		double sin = dy / distance;
		MoveData m = new MoveData();

		getPosition().setHeading((int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381) + 32768);

		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z;
		m._heading = 0;
		m.onGeodataPathIndex = -1;
		m._moveStartTime = GameTimeController.getGameTicks();

		_move = m;

		GameTimeController.getInstance().registerMovingObject(this);
		m = null;
	}

	class BoatCaptain implements Runnable
	{
		private int _state;
		private L2BoatInstance _boat;

		public BoatCaptain(int i, L2BoatInstance instance)
		{
			_state = i;
			_boat = instance;
		}

		@Override
		public void run()
		{
			BoatCaptain bc;
			switch(_state)
			{
				case 1:
					_boat.say(5);
					bc = new BoatCaptain(2, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 240000);
					break;
				case 2:
					_boat.say(1);
					bc = new BoatCaptain(3, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 40000);
					break;
				case 3:
					_boat.say(0);
					bc = new BoatCaptain(4, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 20000);
					break;
				case 4:
					_boat.say(-1);
					_boat.begin();
					break;
			}
		}
	}

	class Boatrun implements Runnable
	{
		private int _state;
		private L2BoatInstance _boat;

		public Boatrun(int i, L2BoatInstance instance)
		{
			_state = i;
			_boat = instance;
		}

		@Override
		public void run()
		{
			if(!_inCycle)
			{
				return;
			}

			_boat._vd = null;
			_boat.needOnVehicleCheckLocation = false;

			if(_boat._cycle == 1)
			{
				int time = _boat._t1.state(_state, _boat);
				if(time > 0)
				{
					_state++;
					Boatrun bc = new Boatrun(_state, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, time);
					bc = null;
				}
				else if(time == 0)
				{
					_boat._cycle = 2;
					_boat.say(10);
					BoatCaptain bc = new BoatCaptain(1, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
					bc = null;
				}
				else
				{
					_boat.needOnVehicleCheckLocation = true;
					_state++;
					_boat._runstate = _state;
				}
			}
			else if(_boat._cycle == 2)
			{
				int time = _boat._t2.state(_state, _boat);
				if(time > 0)
				{
					_state++;
					Boatrun bc = new Boatrun(_state, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, time);
					bc = null;
				}
				else if(time == 0)
				{
					_boat._cycle = 1;
					_boat.say(10);
					BoatCaptain bc = new BoatCaptain(1, _boat);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
					bc = null;
				}
				else
				{
					_boat.needOnVehicleCheckLocation = true;
					_state++;
					_boat._runstate = _state;
				}
			}
		}
	}

	public int _runstate = 0;

	public void evtArrived()
	{

		if(_runstate != 0)
		{
			Boatrun bc = new Boatrun(_runstate, this);
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 10);
			_runstate = 0;
			bc = null;
		}
	}

	public void sendVehicleDeparture(L2PcInstance activeChar)
	{
		if(_vd != null)
		{
			activeChar.sendPacket(_vd);
		}
	}

	public VehicleDeparture getVehicleDeparture()
	{
		return _vd;
	}

	public void beginCycle()
	{
		say(10);
		BoatCaptain bc = new BoatCaptain(1, this);
		ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
		bc = null;
	}

	private int lastx = -1;
	private int lasty = -1;
	protected boolean needOnVehicleCheckLocation = false;
	private boolean _inCycle = true;
	private int _id;

	public void updatePeopleInTheBoat(int x, int y, int z)
	{

		if(_inboat != null)
		{
			boolean check = false;
			if(lastx == -1 || lasty == -1)
			{
				check = true;
				lastx = x;
				lasty = y;
			}
			else if((x - lastx) * (x - lastx) + (y - lasty) * (y - lasty) > 2250000)
			{
				check = true;
				lastx = x;
				lasty = y;
			}
			for(int i = 0; i < _inboat.size(); i++)
			{
				L2PcInstance player = _inboat.get(i);
				if(player != null && player.isInBoat())
				{
					if(player.getBoat() == this)
					{
						player.getPosition().setXYZ(x, y, z);
						player.revalidateZone(false);
					}
				}
				if(check == true)
				{
					if(needOnVehicleCheckLocation == true)
					{
						OnVehicleCheckLocation vcl = new OnVehicleCheckLocation(this, x, y, z);
						player.sendPacket(vcl);
					}
				}
			}
		}

	}

	public void begin()
	{
		if(!_inCycle)
		{
			return;
		}

		if(_cycle == 1)
		{
			Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
			if(knownPlayers != null && !knownPlayers.isEmpty())
			{
				_inboat = new FastMap<Integer, L2PcInstance>();
				int i = 0;
				for(L2PcInstance player : knownPlayers)
				{
					if(player.isInBoat() && player.getBoat() == this)
					{
						L2ItemInstance it;
						it = player.getInventory().getItemByItemId(_t1.idWTicket1);
						if(it != null && it.getCount() >= 1)
						{
							player.getInventory().destroyItem("Boat", it.getObjectId(), 1, player, this);
							InventoryUpdate iu = new InventoryUpdate();
							iu.addModifiedItem(it);
							player.sendPacket(iu);
							_inboat.put(i, player);
							i++;
						}
						else if(it == null && _t1.idWTicket1 == 0)
						{
							_inboat.put(i, player);
							i++;
						}
						else
						{
							player.teleToLocation(_t1.ntx1, _t1.nty1, _t1.ntz1, false);
						}
					}
				}
				knownPlayers = null;
			}
			Boatrun bc = new Boatrun(0, this);
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 0);
			bc = null;
		}
		else if(_cycle == 2)
		{
			Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
			if(knownPlayers != null && !knownPlayers.isEmpty())
			{
				_inboat = new FastMap<Integer, L2PcInstance>();
				int i = 0;
				for(L2PcInstance player : knownPlayers)
				{
					if(player.isInBoat() && player.getBoat() == this)
					{
						L2ItemInstance it;
						it = player.getInventory().getItemByItemId(_t2.idWTicket1);
						if(it != null && it.getCount() >= 1)
						{
							player.getInventory().destroyItem("Boat", it.getObjectId(), 1, player, this);
							InventoryUpdate iu = new InventoryUpdate();
							iu.addModifiedItem(it);
							player.sendPacket(iu);
							_inboat.put(i, player);
							i++;
						}
						else if(it == null && _t2.idWTicket1 == 0)
						{
							_inboat.put(i, player);
							i++;
						}
						else
						{
							player.teleToLocation(_t2.ntx1, _t2.nty1, _t2.ntz1, false);
						}
					}
				}
				knownPlayers = null;
			}
			Boatrun bc = new Boatrun(0, this);
			ThreadPoolManager.getInstance().scheduleGeneral(bc, 0);
			bc = null;
		}
	}

	public void say(int i)
	{

		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		CreatureSay sm;
		PlaySound ps;
		switch(i)
		{
			case 10:
				if(_cycle == 1)
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t1.npc1, _t1.sysmess10_1);
				}
				else
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t2.npc1, _t2.sysmess10_1);
				}
				ps = new PlaySound(0, "itemsound.ship_arrival_departure", 1, getObjectId(), getX(), getY(), getZ());
				if(knownPlayers == null || knownPlayers.isEmpty())
				{
					return;
				}
				for(L2PcInstance player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
			case 5:
				if(_cycle == 1)
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t1.npc1, _t1.sysmess5_1);
				}
				else
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t2.npc1, _t2.sysmess5_1);
				}
				ps = new PlaySound(0, "itemsound.ship_5min", 1, getObjectId(), getX(), getY(), getZ());
				if(knownPlayers == null || knownPlayers.isEmpty())
				{
					return;
				}
				for(L2PcInstance player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
			case 1:

				if(_cycle == 1)
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t1.npc1, _t1.sysmess1_1);
				}
				else
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t2.npc1, _t2.sysmess1_1);
				}
				ps = new PlaySound(0, "itemsound.ship_1min", 1, getObjectId(), getX(), getY(), getZ());
				if(knownPlayers == null || knownPlayers.isEmpty())
				{
					return;
				}
				for(L2PcInstance player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
			case 0:

				if(_cycle == 1)
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t1.npc1, _t1.sysmess0_1);
				}
				else
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t2.npc1, _t2.sysmess0_1);
				}
				if(knownPlayers == null || knownPlayers.isEmpty())
				{
					return;
				}
				for(L2PcInstance player : knownPlayers)
				{
					player.sendPacket(sm);
				}
				break;
			case -1:
				if(_cycle == 1)
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t1.npc1, _t1.sysmessb_1);
				}
				else
				{
					sm = new CreatureSay(0, Say2.SHOUT, _t2.npc1, _t2.sysmessb_1);
				}
				ps = new PlaySound(0, "itemsound.ship_arrival_departure", 1, getObjectId(), getX(), getY(), getZ());
				for(L2PcInstance player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
		}
		knownPlayers = null;
		sm = null;
		ps = null;
	}

	public void spawn()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		_cycle = 1;
		beginCycle();
		if(knownPlayers == null || knownPlayers.isEmpty())
		{
			return;
		}
		VehicleInfo vi = new VehicleInfo(this);
		for(L2PcInstance player : knownPlayers)
		{
			player.sendPacket(vi);
		}
		knownPlayers = null;
	}

	public void setTrajet1(int idWaypoint1, int idWTicket1, int ntx1, int nty1, int ntz1, String idnpc1, String sysmess10_1, String sysmess5_1, String sysmess1_1, String sysmess0_1, String sysmessb_1)
	{
		_t1 = new L2BoatTrajet(idWaypoint1, idWTicket1, ntx1, nty1, ntz1, idnpc1, sysmess10_1, sysmess5_1, sysmess1_1, sysmess0_1, sysmessb_1, _name);
	}

	public void setTrajet2(int idWaypoint1, int idWTicket1, int ntx1, int nty1, int ntz1, String idnpc1, String sysmess10_1, String sysmess5_1, String sysmess1_1, String sysmess0_1, String sysmessb_1)
	{
		_t2 = new L2BoatTrajet(idWaypoint1, idWTicket1, ntx1, nty1, ntz1, idnpc1, sysmess10_1, sysmess5_1, sysmess1_1, sysmess0_1, sysmessb_1, _name);
	}

	@Override
	public void updateAbnormalEffect()
	{
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public int getLevel()
	{
		return 0;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	public boolean isInCycle()
	{
		return _inCycle;
	}

	public void stopCycle()
	{
		_inCycle = false;
		stopMove(new L2CharPosition(getX(), getY(), getZ(), getPosition().getHeading()));
	}

	public void startCycle()
	{
		_inCycle = true;
		_cycle = 1;
		beginCycle();
	}

	public void reloadPath()
	{
		_t1.loadBoatPath();
		_t2.loadBoatPath();
		_cycle = 0;
		stopCycle();
		startCycle();
	}

	public String getBoatName()
	{
		return _name;
	}

	public int getSizeInside()
	{
		return _inboat == null ? 0 : _inboat.size();
	}

	public int getCycle()
	{
		return _cycle;
	}

	public int getId()
	{
		return _id;
	}

	public void setId(int id)
	{
		_id = id;
	}

}