/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.managers;

import java.awt.Polygon;
import java.awt.Shape;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.src.Config;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.DimensionalRift;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public class DimensionalRiftManager
{
	private static Log _log = LogFactory.getLog(DimensionalRiftManager.class.getName());

	private static DimensionalRiftManager _instance;
	private FastMap<Byte, FastMap<Byte, DimensionalRiftRoom>> _rooms = new FastMap<Byte, FastMap<Byte, DimensionalRiftRoom>>();
	private final short DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;
	private final static int MAX_PARTY_PER_AREA = 3;

	public static DimensionalRiftManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new DimensionalRiftManager();
		}

		return _instance;
	}

	private DimensionalRiftManager()
	{
		loadRooms();
		loadSpawns();
	}

	public DimensionalRiftRoom getRoom(byte type, byte room)
	{
		return _rooms.get(type) == null ? null : _rooms.get(type).get(room);
	}

	public boolean isAreaAvailable(byte area)
	{
		FastMap<Byte, DimensionalRiftRoom> tmap = _rooms.get(area);
		if(tmap == null)
		{
			return false;
		}
		int used = 0;
		for(DimensionalRiftRoom room : tmap.values())
		{
			if(room.isUsed())
			{
				used++;
			}
		}
		return used <= MAX_PARTY_PER_AREA;
	}

	public boolean isRoomAvailable(byte area, byte room)
	{
		if(_rooms.get(area) == null || _rooms.get(area).get(room) == null)
		{
			return false;
		}
		return !_rooms.get(area).get(room).isUsed();
	}

	private void loadRooms()
	{
		DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
		factory1.setValidating(false);
		factory1.setIgnoringComments(true);
		File f = new File("./data/xml/dimensional_rift_rooms.xml");
		if(!f.exists())
		{
			_log.warn("dimensional_rift_rooms.xml could not be loaded: file not found");
			return;
		}
		try
		{
			InputSource in1 = new InputSource(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			in1.setEncoding("UTF-8");
			Document doc1 = factory1.newDocumentBuilder().parse(in1);
			for(Node n = doc1.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if(n.getNodeName().equalsIgnoreCase("list"))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if(d.getNodeName().equalsIgnoreCase("room"))
						{
							byte type = Byte.valueOf(d.getAttributes().getNamedItem("type").getNodeValue());
							byte room_id = Byte.valueOf(d.getAttributes().getNamedItem("room_id").getNodeValue());
							int xMin = Integer.valueOf(d.getAttributes().getNamedItem("xMin").getNodeValue());
							int xMax = Integer.valueOf(d.getAttributes().getNamedItem("xMax").getNodeValue());
							int yMin = Integer.valueOf(d.getAttributes().getNamedItem("yMin").getNodeValue());
							int yMax = Integer.valueOf(d.getAttributes().getNamedItem("yMax").getNodeValue());
							int z1 = Integer.valueOf(d.getAttributes().getNamedItem("zMin").getNodeValue());
							int z2 = Integer.valueOf(d.getAttributes().getNamedItem("zMax").getNodeValue());
							int xT = Integer.valueOf(d.getAttributes().getNamedItem("xT").getNodeValue());
							int yT = Integer.valueOf(d.getAttributes().getNamedItem("yT").getNodeValue());
							int zT = Integer.valueOf(d.getAttributes().getNamedItem("zT").getNodeValue());
							boolean isBossRoom = Byte.valueOf(d.getAttributes().getNamedItem("boss").getNodeValue()) > 0;

							if(!_rooms.containsKey(type))
							{
								_rooms.put(type, new FastMap<Byte, DimensionalRiftRoom>());
							}

							_rooms.get(type).put(room_id, new DimensionalRiftRoom(type, room_id, xMin, xMax, yMin, yMax, z1, z2, xT, yT, zT, isBossRoom));
						}
					}
				}
			}
		}
		catch(SAXException e)
		{
			_log.error("Can't load Dimension Rift zones.", e);
		}
		catch(IOException e)
		{
			_log.error("Can't load Dimension Rift zones.", e);
		}
		catch(ParserConfigurationException e)
		{
			_log.error("Can't load Dimension Rift zones.", e);
		}

		int typeSize = _rooms.keySet().size();
		int roomSize = 0;

		for(Byte b : _rooms.keySet())
		{
			roomSize += _rooms.get(b).keySet().size();
		}

		_log.info("DimensionalRiftManager: Loaded " + typeSize + " room types with " + roomSize + " rooms.");
	}

	public void loadSpawns()
	{
		int countGood = 0, countBad = 0;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File("./data/xml/dimensional_rift_spawns.xml");
			if(!file.exists())
			{
				throw new IOException();
			}

			Document doc = factory.newDocumentBuilder().parse(file);
			factory = null;
			file = null;

			NamedNodeMap attrs;
			byte type, roomId;
			int mobId, x, y, z, delay, count;
			L2Spawn spawnDat;
			L2NpcTemplate template;

			for(Node rift = doc.getFirstChild(); rift != null; rift = rift.getNextSibling())
			{
				if("rift".equalsIgnoreCase(rift.getNodeName()))
				{
					for(Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					{
						if("area".equalsIgnoreCase(area.getNodeName()))
						{
							attrs = area.getAttributes();
							type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());

							for(Node room = area.getFirstChild(); room != null; room = room.getNextSibling())
							{
								if("room".equalsIgnoreCase(room.getNodeName()))
								{
									attrs = room.getAttributes();
									roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());

									for(Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									{
										if("spawn".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
											delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
											count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());

											template = NpcTable.getInstance().getTemplate(mobId);
											if(template == null)
											{
												_log.warn("Template " + mobId + " not found!");
											}
											if(!_rooms.containsKey(type))
											{
												_log.warn("Type " + type + " not found!");
											}
											else if(!_rooms.get(type).containsKey(roomId))
											{
												_log.warn("Room " + roomId + " in Type " + type + " not found!");
											}

											for(int i = 0; i < count; i++)
											{
												DimensionalRiftRoom riftRoom = _rooms.get(type).get(roomId);
												x = riftRoom.getRandomX();
												y = riftRoom.getRandomY();
												z = riftRoom.getTeleportCoords()[2];
												riftRoom = null;

												if(template != null && _rooms.containsKey(type) && _rooms.get(type).containsKey(roomId))
												{
													spawnDat = new L2Spawn(template);
													spawnDat.setAmount(1);
													spawnDat.setLocx(x);
													spawnDat.setLocy(y);
													spawnDat.setLocz(z);
													spawnDat.setHeading(-1);
													spawnDat.setRespawnDelay(delay);
													SpawnTable.getInstance().addNewSpawn(spawnDat, false);
													_rooms.get(type).get(roomId).getSpawns().add(spawnDat);
													countGood++;
												}
												else
												{
													countBad++;
												}
											}
										}
									}
								}
							}
							attrs = null;
						}
					}
				}
			}
			spawnDat = null;
			template = null;
		}
		catch(Exception e0)
		{
			_log.error("Error on loading dimensional rift spawns", e0);
			e0.printStackTrace();
		}

		_log.info("DimensionalRiftManager: Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
	}

	public void reload()
	{
		for(Byte b : _rooms.keySet())
		{
			for(int i : _rooms.get(b).keySet())
			{
				_rooms.get(b).get(i).getSpawns().clear();
			}
			_rooms.get(b).clear();
		}
		_rooms.clear();
		loadRooms();
		loadSpawns();
	}

	public boolean checkIfInRiftZone(int x, int y, int z, boolean ignorePeaceZone)
	{
		if(ignorePeaceZone = true && _rooms != null)
		{
			return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z);
		}
		else
		{
			return _rooms.get((byte) 0).get((byte) 1).checkIfInZone(x, y, z) && _rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
		}
	}

	public boolean checkIfInPeaceZone(int x, int y, int z)
	{
		return _rooms.get((byte) 0).get((byte) 0).checkIfInZone(x, y, z);
	}

	public void teleportToWaitingRoom(L2PcInstance player)
	{
		int[] coords = getRoom((byte) 0, (byte) 0).getTeleportCoords();
		player.teleToLocation(coords[0], coords[1], coords[2]);
	}

	public void start(L2PcInstance player, byte type, L2Npc npc)
	{
		boolean canPass = true;
		if(!player.isInParty())
		{
			showHtmlFile(player, "data/html/sevensigns/rift/NoParty.htm", npc);
			return;
		}

		if(player.getParty().getPartyLeaderOID() != player.getObjectId())
		{
			showHtmlFile(player, "data/html/sevensigns/rift/NotPartyLeader.htm", npc);
			return;
		}

		if(player.getParty().isInDimensionalRift())
		{
			handleCheat(player, npc);
			return;
		}

		if(!isAreaAvailable(type))
		{
			player.sendMessage("This rift area is full. Try later.");
			return;
		}

		if(player.getParty().getMemberCount() < Config.RIFT_MIN_PARTY_SIZE)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/sevensigns/rift/SmallParty.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", new Integer(Config.RIFT_MIN_PARTY_SIZE).toString());
			player.sendPacket(html);
			return;
		}

		for(L2PcInstance p : player.getParty().getPartyMembers())
		{
			if(!checkIfInPeaceZone(p.getX(), p.getY(), p.getZ()))
			{
				canPass = false;
			}
		}

		if(!canPass)
		{
			showHtmlFile(player, "data/html/sevensigns/rift/NotInWaitingRoom.htm", npc);
			return;
		}

		L2ItemInstance i;
		for(L2PcInstance p : player.getParty().getPartyMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);

			if(i == null)
			{
				canPass = false;
				break;
			}

			if(i.getCount() > 0)
			{
				if(i.getCount() < getNeededItems(type))
				{
					canPass = false;
				}
			}
		}

		if(!canPass)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile("data/html/sevensigns/rift/NoFragments.htm");
			html.replace("%npc_name%", npc.getName());
			html.replace("%count%", new Integer(getNeededItems(type)).toString());
			player.sendPacket(html);
			html = null;
			return;
		}

		for(L2PcInstance p : player.getParty().getPartyMembers())
		{
			i = p.getInventory().getItemByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID);
			p.destroyItem("RiftEntrance", i.getObjectId(), getNeededItems(type), null, false);
		}

		i = null;

		byte room;
		do
		{
			room = (byte) Rnd.get(1, 9);
		}
		while(!isRoomAvailable(type, room));

		new DimensionalRift(player.getParty(), type, room);
	}

	public void killRift(DimensionalRift d)
	{
		if(d.getTeleportTimerTask() != null)
		{
			d.getTeleportTimerTask().cancel();
		}
		d.setTeleportTimerTask(null);

		if(d.getTeleportTimer() != null)
		{
			d.getTeleportTimer().cancel();
		}
		d.setTeleportTimer(null);

		if(d.getSpawnTimerTask() != null)
		{
			d.getSpawnTimerTask().cancel();
		}
		d.setSpawnTimerTask(null);

		if(d.getSpawnTimer() != null)
		{
			d.getSpawnTimer().cancel();
		}
		d.setSpawnTimer(null);
	}

	public class DimensionalRiftRoom
	{
		protected final byte _type;
		protected final byte _room;
		private final int _xMin;
		private final int _xMax;
		private final int _yMin;
		private final int _yMax;
		private final int _zMin;
		private final int _zMax;
		private final int[] _teleportCoords;
		private final Shape _s;
		private final boolean _isBossRoom;
		private final FastList<L2Spawn> _roomSpawns;
		protected final FastList<L2Npc> _roomMobs;
		private boolean _isUsed = false;

		public DimensionalRiftRoom(byte type, byte room, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax, int xT, int yT, int zT, boolean isBossRoom)
		{
			_type = type;
			_room = room;
			_xMin = xMin + 128;
			_xMax = xMax - 128;
			_yMin = yMin + 128;
			_yMax = yMax - 128;
			_zMin = zMin;
			_zMax = zMax;
			_teleportCoords = new int[]
			{
					xT, yT, zT
			};
			_isBossRoom = isBossRoom;
			_roomSpawns = new FastList<L2Spawn>();
			_roomMobs = new FastList<L2Npc>();
			_s = new Polygon(new int[]
			{
					xMin, xMax, xMax, xMin
			}, new int[]
			{
					yMin, yMin, yMax, yMax
			}, 4);
		}

		public int getRandomX()
		{
			return Rnd.get(_xMin, _xMax);
		}

		public int getRandomY()
		{
			return Rnd.get(_yMin, _yMax);
		}

		public int[] getTeleportCoords()
		{
			return _teleportCoords;
		}

		public boolean checkIfInZone(int x, int y, int z)
		{
			return _s.contains(x, y) && z >= _zMin && z <= _zMax;
		}

		public boolean isBossRoom()
		{
			return _isBossRoom;
		}

		public FastList<L2Spawn> getSpawns()
		{
			return _roomSpawns;
		}

		public void spawn()
		{
			for(L2Spawn spawn : _roomSpawns)
			{
				spawn.doSpawn();
				if(spawn.getNpcid() < 25333 && spawn.getNpcid() > 25338)
				{
					spawn.startRespawn();
				}
			}
		}

		public void unspawn()
		{
			for(L2Spawn spawn : _roomSpawns)
			{
				spawn.stopRespawn();
				if(spawn.getLastSpawn() != null)
				{
					spawn.getLastSpawn().deleteMe();
					spawn.decreaseCount(null);
				}
			}
			_isUsed = false;
		}

		public void setUsed()
		{
			_isUsed = true;
		}

		public boolean isUsed()
		{
			return _isUsed;
		}
	}

	private int getNeededItems(byte type)
	{
		switch(type)
		{
			case 1:
				return Config.RIFT_ENTER_COST_RECRUIT;
			case 2:
				return Config.RIFT_ENTER_COST_SOLDIER;
			case 3:
				return Config.RIFT_ENTER_COST_OFFICER;
			case 4:
				return Config.RIFT_ENTER_COST_CAPTAIN;
			case 5:
				return Config.RIFT_ENTER_COST_COMMANDER;
			case 6:
				return Config.RIFT_ENTER_COST_HERO;
			default:
				return 999999;
		}
	}

	public void showHtmlFile(L2PcInstance player, String file, L2Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(file);
		html.replace("%npc_name%", npc.getName());
		player.sendPacket(html);
		html = null;
	}

	public void handleCheat(L2PcInstance player, L2Npc npc)
	{
		showHtmlFile(player, "data/html/sevensigns/rift/Cheater.htm", npc);
		if(!player.isGM())
		{
			_log.warn("Player " + player.getName() + "(" + player.getObjectId() + ") was cheating in dimension rift area!");
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " tried to cheat in dimensional rift.", Config.DEFAULT_PUNISH);
		}
	}

}