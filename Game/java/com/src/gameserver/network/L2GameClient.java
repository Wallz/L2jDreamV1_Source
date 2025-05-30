/* This program is free software; you can redistribute it and/or modify
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
package com.src.gameserver.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.communitybbs.Manager.RegionBBSManager;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.managers.FunEventsManager;
import com.src.gameserver.model.CharSelectInfoPackage;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.LeaveWorld;
import com.src.gameserver.network.serverpackets.ServerClose;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.thread.LoginServerThread;
import com.src.gameserver.thread.LoginServerThread.SessionKey;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.thread.daemons.AutoSave;
import com.src.gameserver.util.FloodProtector;
import com.src.mmocore.MMOClient;
import com.src.mmocore.MMOConnection;
import com.src.mmocore.ReceivablePacket;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.protection.nProtect;

public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>> implements Runnable
{
	private static final Log _log = LogFactory.getLog(L2GameClient.class);

	public static enum GameClientState
	{
		CONNECTED,
		AUTHED,
		IN_GAME
	};

	public GameClientState state;
	public String accountName;
	public SessionKey sessionId;
	public L2PcInstance activeChar;
	private ReentrantLock _activeCharLock = new ReentrantLock();

	private boolean _isAuthedGG;
	private long _connectionStartTime;
	private List<Integer> _charSlotMapping = new FastList<Integer>();

	private ScheduledFuture<?> _guardCheckTask = null;

	protected ScheduledFuture<?> _autoSaveInDB;
	protected ScheduledFuture<?> _cleanupTask = null;

	private ClientStats _stats;

	public GameCrypt crypt;

	public long packetsNextSendTick = 0;

	private int unknownPacketCount = 0;

	private boolean _closenow = true;
	private boolean _isDetached = false;

	private final ArrayBlockingQueue<ReceivablePacket<L2GameClient>> _packetQueue;
	private ReentrantLock _queueLock = new ReentrantLock();

	public L2GameClient(MMOConnection<L2GameClient> con)
	{
		super(con);
		state = GameClientState.CONNECTED;
		_connectionStartTime = System.currentTimeMillis();
		crypt = new GameCrypt();
		_stats = new ClientStats();
		_packetQueue = new ArrayBlockingQueue<ReceivablePacket<L2GameClient>>(com.src.mmocore.Config.CLIENT_PACKET_QUEUE_SIZE);
		if(Config.AUTOSAVE_INITIAL_TIME > 0)
		{
			_autoSaveInDB = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoSave(activeChar), Config.AUTOSAVE_INITIAL_TIME, Config.AUTOSAVE_DELAY_TIME);
		}
		_guardCheckTask = nProtect.getInstance().startTask(this);
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if(_closenow)
				{
					close(new LeaveWorld());
				}
			}
		}, 4000);
	}

	public byte[] enableCrypt()
	{
		byte[] key = BlowFishKeygen.getRandomKey();
		GameCrypt.setKey(key, crypt);
		return key;
	}

	public GameClientState getState()
	{
		return state;
	}

	public void setState(GameClientState pState)
	{
		if(state != pState)
		{
			state = pState;
			_packetQueue.clear();
		}
	}

	public ClientStats getStats()
	{
		return _stats;
	}

	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		_closenow = false;
		GameCrypt.decrypt(buf.array(), buf.position(), size, crypt);
		return true;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
		GameCrypt.encrypt(buf.array(), buf.position(), size, crypt);
		buf.position(buf.position() + size);
		return true;
	}

	public L2PcInstance getActiveChar()
	{
		return activeChar;
	}

	public void setActiveChar(L2PcInstance pActiveChar)
	{
		activeChar = pActiveChar;
		if(activeChar != null)
		{
			L2World.storeObject(getActiveChar());
		}
	}

	public ReentrantLock getActiveCharLock()
	{
		return _activeCharLock;
	}

	public boolean isAuthedGG()
	{
		return _isAuthedGG;
	}

	public void setGameGuardOk(boolean val)
	{
		_isAuthedGG = val;
	}

	public void setAccountName(String pAccountName)
	{
		accountName = pAccountName;
	}

	public String getAccountName()
	{
		return accountName;
	}

	public void setSessionId(SessionKey sk)
	{
		sessionId = sk;
	}

	public SessionKey getSessionId()
	{
		return sessionId;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
		if(_isDetached) 
		{ 
			return; 
		} 

		if(getConnection() != null)
		{
			getConnection().sendPacket(gsp);
		}

		gsp.runImpl();
	}

	public boolean isDetached()
	{
		return _isDetached;
	}

	public void setDetached(boolean b)
	{
		_isDetached = b;
	}

	public byte markToDeleteChar(int charslot)
	{
		int objid = getObjectIdForSlot(charslot);

		if(objid < 0)
		{
			return -1;
		}

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clanId FROM characters WHERE obj_Id = ?");
			statement.setInt(1, objid);
			ResultSet rs = statement.executeQuery();

			rs.next();

			int clanId = rs.getInt(1);

			byte answer = 0;

			if(clanId != 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);

				if(clan == null)
				{
					answer = 0;
				}
				else if(clan.getLeaderId() == objid)
				{
					answer = 2;
				}
				else
				{
					answer = 1;
				}

				clan = null;
			}

			if(answer == 0)
			{
				if(Config.DELETE_DAYS == 0)
				{
					deleteCharByObjId(objid);
				}
				else
				{
					statement = con.prepareStatement("UPDATE characters SET deletetime = ? WHERE obj_Id = ?");
					statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS * 86400000L);
					statement.setInt(2, objid);
					statement.execute();
					ResourceUtil.closeStatement(statement);
					rs.close();
				}
			}
			else
			{
				ResourceUtil.closeStatement(statement);
				rs.close();
			}

			return answer;
		}
		catch(Exception e)
		{
			_log.error("Data error on update delete time of char", e);
			return -1;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void markRestoredChar(int charslot) throws Exception
	{
		int objid = getObjectIdForSlot(charslot);

		if(objid < 0)
		{
			return;
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime = 0 WHERE obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("Data error on restoring char", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public static void deleteCharByObjId(int objid)
	{
		if(objid < 0)
		{
			return;
		}

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("DELETE FROM character_friends WHERE char_id = ? OR friend_id = ?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_macroses WHERE char_obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM heroes WHERE char_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE char_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM seven_signs WHERE char_obj_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id = ?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id IN (SELECT object_id FROM items WHERE items.owner_id = ?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("DELETE FROM characters WHERE obj_Id = ?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("Data error on deleting char", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public L2PcInstance loadCharFromDisk(int charslot)
	{
		final int objId = getObjectIdForSlot(charslot);
		if(objId < 0)
		{
			return null;
		}

		L2PcInstance character = L2World.getInstance().getPlayer(objId);
		if(character != null)
		{
			_log.warn("Attempt of double login: " + character.getName() + "(" + objId + ") " + getAccountName());
			if(character.getClient() != null)
			{
				character.getClient().closeNow();
			}
			else
			{
				character.deleteMe();
				try
				{
					character.store();
				}
				catch(Exception e2)
				{
				}
			}
		}

		character = L2PcInstance.load(objId);

		if(character != null)
		{
			character.setRunning();
			character.standUp();
			character.refreshOverloaded();
			character.refreshExpertisePenalty();
			character.sendPacket(new UserInfo(character));
			character.broadcastKarma();
			character.setOnlineStatus(true);
		}
		else
		{
			_log.error("could not restore in slot: " + charslot, new Exception());
		}

		return character;
	}

	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping.clear();

		for(CharSelectInfoPackage c : chars)
		{
			int objectId = c.getObjectId();

			_charSlotMapping.add(new Integer(objectId));
		}
	}

	public void close(L2GameServerPacket gsp)
	{
		if(getConnection()!=null)
		{
			getConnection().close(gsp);
		}
	}

	private int getObjectIdForSlot(int charslot)
	{
		if(charslot < 0 || charslot >= _charSlotMapping.size())
		{
			_log.warn(toString() + " tried to delete Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}

		Integer objectId = _charSlotMapping.get(charslot);

		return objectId.intValue();
	}

	@Override
	public void onForcedDisconnection()
	{
		_log.info("Client " + toString() + " disconnected abnormally.");
		L2PcInstance player = getActiveChar();
		if(player != null)
		{
			if(Olympiad.getInstance().isRegistered(player))
			{
				Olympiad.getInstance().unRegisterNoble(player);
			}

			if(player.isFlying())
			{
				player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			}
			
			if(player.isInParty())
			{
				player.getParty().removePartyMember(player);
			}
			
			player.deleteMe();

			try
			{
				player.store();
			}
			catch(Exception e2)
			{
			}
			FunEventsManager.getInstance().notifyPlayerLogout(player);
			L2World.getInstance().removeFromAllPlayers(player);
			setActiveChar(null);
			LoginServerThread.getInstance().sendLogout(getAccountName());
		}
		stopGuardTask();
		nProtect.getInstance().closeSession(this);
	}

	public void stopGuardTask()
	{
		if(_guardCheckTask != null)
		{
			_guardCheckTask.cancel(true);
			_guardCheckTask = null;
		}
	}

	@Override
	public void onDisconnection()
	{
		try
		{
			ThreadPoolManager.getInstance().executeTask(new DisconnectTask());

		}
		catch(RejectedExecutionException e)
		{
		}
	}

	public void closeNow()
	{
		close(ServerClose.STATIC_PACKET);
		synchronized(this)
		{
			if(_cleanupTask != null)
			{
				cancelCleanup();
			}

			_cleanupTask = ThreadPoolManager.getInstance().scheduleGeneral(new CleanupTask(), 0);
		}
	}


	@Override
	public String toString()
	{
		try
		{
			InetAddress address = getConnection().getInetAddress();
			String ip = "N/A";

			if(address == null)
			{
				ip = "disconnected";
			}
			else
			{
				ip = address.getHostAddress();
			}

			switch(getState())
			{
				case CONNECTED:
					return "[IP: " + ip + "]";
				case AUTHED:
					return "[Account: " + getAccountName() + " - IP: " + ip + "]";
				case IN_GAME:
					address = null;
					return "[Character: " + (getActiveChar() == null ? "disconnected" : getActiveChar().getName()) + " - Account: " + getAccountName() + " - IP: " + ip + "]";
				default:
					address = null;
					throw new IllegalStateException("Missing state on switch");
			}
		}
		catch(NullPointerException e)
		{
			return "[Character read failed due to disconnect]";
		}
	}

	class CleanupTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if(_autoSaveInDB != null)
				{
					_autoSaveInDB.cancel(true);
				}

				L2PcInstance player = L2GameClient.this.getActiveChar();
				if(player != null)
				{
					player.setClient(null);

					if(player.isOnline() == 1)
					{
						player.deleteMe();
					}
				}

				L2GameClient.this.setActiveChar(null);
			}
			catch(Exception e1)
			{
				_log.error("Error while cleanup client.", e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(L2GameClient.this.getAccountName());
			}
		}
	}

	class DisconnectTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				try
				{
					RegionBBSManager.getInstance().changeCommunityBoard();
				}
				catch(Exception e)
				{
					_log.error("", e);
				}

				if(_autoSaveInDB != null)
				{
					_autoSaveInDB.cancel(true);
				}

				L2PcInstance player = getActiveChar();
				if(player != null)
				{
					if(player.isFlying())
					{
						player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
					}

					if(player.isInParty())
					{
						player.getParty().removePartyMember(player);
					}

					if(Olympiad.getInstance().isRegistered(player))
					{
						Olympiad.getInstance().unRegisterNoble(player);
					}

					if(player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE || player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE)
					{
						player.setOffline(true);
						player.leaveParty();

						if(Config.OFFLINE_SLEEP_EFFECT)
						{
							player.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_SLEEP);
						}

						if(Config.OFFLINE_SET_NAME_COLOR)
						{
							player._originalNameColorOffline = player.getAppearance().getNameColor();
							player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
							player.broadcastUserInfo();
						}

						if(player.getOfflineStartTime() == 0)
						{
							player.setOfflineStartTime(System.currentTimeMillis());
						}

						return;
					}
					FunEventsManager.getInstance().notifyPlayerLogout(player);
					player.deleteMe();
					player.store();

					try
					{
						player.store();
					}
					catch(Exception e2)
					{
					}
				}

				setActiveChar(null);

				player = null;
			}
			catch(Exception e1)
			{
				_log.error("Error while disconnecting client", e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(getAccountName());
			}
		}
	}

	public boolean checkUnknownPackets()
	{
		if(getActiveChar() != null && !FloodProtector.getInstance().tryPerformAction(getActiveChar().getObjectId(), FloodProtector.PROTECTED_UNKNOWNPACKET))
		{
			unknownPacketCount++;

			if(unknownPacketCount >= Config.MAX_UNKNOWN_PACKETS)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			unknownPacketCount = 0;
			return false;
		}
	}

	private boolean cancelCleanup()
	{
		Future<?> task = _cleanupTask;
		if(task != null)
		{
			_cleanupTask = null;
			return task.cancel(true);
		}

		return false;
	}

	public boolean dropPacket()
	{
		if(_isDetached)
		{
			return true;
		}

		if(getStats().countPacket(_packetQueue.size()))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}

		return getStats().dropPacket();
	}

	public void onBufferUnderflow()
	{
		if(getStats().countUnderflowException())
		{
			_log.error("Client " + toString() + " - Disconnected: Too many buffer underflow exceptions.");
			closeNow();
			return;
		}

		if(state == GameClientState.CONNECTED)
		{
			closeNow();
		}
	}

	public void execute(ReceivablePacket<L2GameClient> packet)
	{
		if(getStats().countFloods())
		{
			_log.error("Client " + toString() + " - Disconnected, too many floods:" + getStats().longFloods + " long and " + getStats().shortFloods + " short.");
			closeNow();
			return;
		}

		if(!_packetQueue.offer(packet))
		{
			if(getStats().countQueueOverflow())
			{
				_log.error("Client " + toString() + " - Disconnected, too many queue overflows.");
				closeNow();
			}
			else
			{
				sendPacket(ActionFailed.STATIC_PACKET);
			}

			return;
		}

		if(_queueLock.isLocked())
		{
			return;
		}

		try
		{
			if(state == GameClientState.CONNECTED)
			{
				if(getStats().processedPackets > 3)
				{
					closeNow();
					return;
				}

				ThreadPoolManager.getInstance().executeIOPacket(this);
			}
			else
			{
				ThreadPoolManager.getInstance().executePacket(this);
			}
		}
		catch(RejectedExecutionException e)
		{
			if(!ThreadPoolManager.getInstance().isShutdown())
			{
				_log.error("Failed executing: " + packet.getClass().getSimpleName() + " for Client: " + toString());
			}
		}
	}

	@Override
	public void run()
	{
		if(!_queueLock.tryLock())
		{
			return;
		}

		try
		{
			int count = 0;
			while(true)
			{
				final ReceivablePacket<L2GameClient> packet = _packetQueue.poll();
				if(packet == null)
				{
					return;
				}

				if(_isDetached)
				{
					_packetQueue.clear();
					return;
				}

				try
				{
					packet.run();
				}
				catch(Exception e)
				{
					_log.error("Exception during execution " + packet.getClass().getSimpleName() + ", client: " + toString() + "," + e.getMessage());
				}

				count++;
				if(getStats().countBurst(count))
				{
					return;
				}
			}
		}
		finally
		{
			_queueLock.unlock();
		}
	}

	public void cleanMe(boolean fast)
	{
		try
		{
			synchronized(this)
			{
				if (_cleanupTask == null)
				{
					_cleanupTask = ThreadPoolManager.getInstance().scheduleGeneral(new CleanupTask(), fast ? 5 : 15000L);
				}
			}
		}
		catch (Exception e1)
		{
			_log.warn("Error during cleanup.", e1);                   
		}
	}
}