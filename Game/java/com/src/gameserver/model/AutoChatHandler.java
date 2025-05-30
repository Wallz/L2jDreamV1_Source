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
package com.src.gameserver.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.src.Config;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.model.spawn.SpawnListener;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class AutoChatHandler implements SpawnListener
{
	private final static Log _log = LogFactory.getLog(AutoChatHandler.class);

	private static AutoChatHandler _instance;

	private static final int DEFAULT_CHAT_DELAY = 30000;

	protected Map<Integer, AutoChatInstance> _registeredChats;

	protected AutoChatHandler()
	{
		_registeredChats = new FastMap<Integer, AutoChatInstance>();
		restoreChatData();
		L2Spawn.addSpawnListener(this);
	}

	private void restoreChatData()
	{
		int numLoaded = 0;
		int numLoaded1 = 0;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File f = new File(Config.DATAPACK_ROOT + "/data/xml/auto_chat.xml");
		if(!f.exists())
		{
			_log.warn("autochat.xml could not be loaded: file not found");
			return;
		}
		try
		{
		InputSource in = new InputSource(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);

			String[] messages = {"","","","","","","",""};
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if(n.getNodeName().equalsIgnoreCase("list"))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if(d.getNodeName().equalsIgnoreCase("autochat"))
						{
							int npcId = Integer.valueOf(d.getAttributes().getNamedItem("npcId").getNodeValue());
							int chatDelay = Integer.valueOf(d.getAttributes().getNamedItem("chatDelay").getNodeValue());
							numLoaded++;
							for(Node t = d.getFirstChild(); t != null; t = t.getNextSibling())
							{
								int i = 0;
								if(t.getNodeName().equalsIgnoreCase("chatText"))
								{
									if(t.getNodeValue() != null)
										messages[i] = t.getNodeValue();
									i++;
									numLoaded1++;
								}
							}
							registerGlobalChat(npcId, messages, chatDelay);
						}
					}
				}
			}
		}
		catch(SAXException e)
		{
			_log.error("Error while creating table", e);
		}
		catch(IOException e)
		{
			_log.error("Error while creating table", e);
		}
		catch(ParserConfigurationException e)
		{
			_log.error("Error while creating table", e);
		}

		_log.info("AutoChatHandler: Loaded " + numLoaded + " chat groups.");
		_log.info("AutoChatHandler: Loaded " + numLoaded1 + " chat texts.");
	}

	public static AutoChatHandler getInstance()
	{
		if(_instance == null)
		{
			_instance = new AutoChatHandler();
		}

		return _instance;
	}

	public int size()
	{
		return _registeredChats.size();
	}

	public AutoChatInstance registerGlobalChat(int npcId, String[] chatTexts, int chatDelay)
	{
		return registerChat(npcId, null, chatTexts, chatDelay);
	}

	public AutoChatInstance registerChat(L2Npc npcInst, String[] chatTexts, int chatDelay)
	{
		return registerChat(npcInst.getNpcId(), npcInst, chatTexts, chatDelay);
	}

	private final AutoChatInstance registerChat(int npcId, L2Npc npcInst, String[] chatTexts, int chatDelay)
	{
		AutoChatInstance chatInst = null;

		if(chatDelay < 0)
		{
			chatDelay = DEFAULT_CHAT_DELAY;
		}

		if(_registeredChats.containsKey(npcId))
		{
			chatInst = _registeredChats.get(npcId);
		}
		else
		{
			chatInst = new AutoChatInstance(npcId, chatTexts, chatDelay, (npcInst == null));
		}

		if(npcInst != null)
		{
			chatInst.addChatDefinition(npcInst);
		}

		_registeredChats.put(npcId, chatInst);

		return chatInst;
	}

	public boolean removeChat(int npcId)
	{
		AutoChatInstance chatInst = _registeredChats.get(npcId);

		return removeChat(chatInst);
	}

	public boolean removeChat(AutoChatInstance chatInst)
	{
		if(chatInst == null)
		{
			return false;
		}

		_registeredChats.remove(chatInst);
		chatInst.setActive(false);

		return true;
	}

	public AutoChatInstance getAutoChatInstance(int id, boolean byObjectId)
	{
		if(!byObjectId)
		{
			return _registeredChats.get(id);
		}
		else
		{
			for(AutoChatInstance chatInst : _registeredChats.values())
			{
				if(chatInst.getChatDefinition(id) != null)
				{
					return chatInst;
				}
			}
		}

		return null;
	}

	public void setAutoChatActive(boolean isActive)
	{
		for(AutoChatInstance chatInst : _registeredChats.values())
		{
			chatInst.setActive(isActive);
		}
	}

	/** 
	 * Sets the active state of all auto chat instances of specified NPC ID 
	 *  
	 * @param npcId 
	 * @param isActive 
	 */ 
	public void setAutoChatActive(int npcId, boolean isActive) 
	{ 
		for (AutoChatInstance chatInst : _registeredChats.values()) 
			if (chatInst.getNPCId() == npcId) chatInst.setActive(isActive); 
	} 
	
	@Override
	public void npcSpawned(L2Npc npc)
	{
		synchronized (_registeredChats)
		{
			if(npc == null)
			{
				return;
			}

			int npcId = npc.getNpcId();

			if(_registeredChats.containsKey(npcId))
			{
				AutoChatInstance chatInst = _registeredChats.get(npcId);

				if(chatInst != null && chatInst.isGlobal())
				{
					chatInst.addChatDefinition(npc);
				}
			}
		}
	}

	public class AutoChatInstance
	{
		protected int _npcId;
		private long _defaultDelay = DEFAULT_CHAT_DELAY;
		private String[] _defaultTexts;
		private boolean _defaultRandom = false;

		private boolean _globalChat = false;
		private boolean _isActive;

		private Map<Integer, AutoChatDefinition> _chatDefinitions = new FastMap<Integer, AutoChatDefinition>();
		protected ScheduledFuture<?> _chatTask;

		protected AutoChatInstance(int npcId, String[] chatTexts, long chatDelay, boolean isGlobal)
		{
			_defaultTexts = chatTexts;
			_npcId = npcId;
			_defaultDelay = chatDelay;
			_globalChat = isGlobal;

			setActive(true);
		}

		protected AutoChatDefinition getChatDefinition(int objectId)
		{
			return _chatDefinitions.get(objectId);
		}

		protected AutoChatDefinition[] getChatDefinitions()
		{
			return _chatDefinitions.values().toArray(new AutoChatDefinition[_chatDefinitions.values().size()]);
		}

		public int addChatDefinition(L2Npc npcInst)
		{
			return addChatDefinition(npcInst, null, 0);
		}

		public int addChatDefinition(L2Npc npcInst, String[] chatTexts, long chatDelay)
		{
			int objectId = npcInst.getObjectId();

			AutoChatDefinition chatDef = new AutoChatDefinition(this, npcInst, chatTexts, chatDelay);

			if(npcInst instanceof L2SiegeGuardInstance)
			{
				chatDef.setRandomChat(true);
			}

			_chatDefinitions.put(objectId, chatDef);

			chatDef = null;

			return objectId;
		}

		public boolean removeChatDefinition(int objectId)
		{
			if(!_chatDefinitions.containsKey(objectId))
			{
				return false;
			}

			AutoChatDefinition chatDefinition = _chatDefinitions.get(objectId);
			chatDefinition.setActive(false);

			_chatDefinitions.remove(objectId);

			chatDefinition = null;

			return true;
		}

		public boolean isActive()
		{
			return _isActive;
		}

		public boolean isGlobal()
		{
			return _globalChat;
		}

		public boolean isDefaultRandom()
		{
			return _defaultRandom;
		}

		public boolean isRandomChat(int objectId)
		{
			if(!_chatDefinitions.containsKey(objectId))
			{
				return false;
			}

			return _chatDefinitions.get(objectId).isRandomChat();
		}

		public int getNPCId()
		{
			return _npcId;
		}

		public int getDefinitionCount()
		{
			return _chatDefinitions.size();
		}

		public L2Npc[] getNPCInstanceList()
		{
			List<L2Npc> npcInsts = new FastList<L2Npc>();

			for(AutoChatDefinition chatDefinition : _chatDefinitions.values())
			{
				npcInsts.add(chatDefinition._npcInstance);
			}

			return npcInsts.toArray(new L2Npc[npcInsts.size()]);
		}

		public long getDefaultDelay()
		{
			return _defaultDelay;
		}

		public String[] getDefaultTexts()
		{
			return _defaultTexts;
		}

		public void setDefaultChatDelay(long delayValue)
		{
			_defaultDelay = delayValue;
		}

		public void setDefaultChatTexts(String[] textsValue)
		{
			_defaultTexts = textsValue;
		}

		public void setDefaultRandom(boolean randValue)
		{
			_defaultRandom = randValue;
		}

		public void setChatDelay(int objectId, long delayValue)
		{
			AutoChatDefinition chatDef = getChatDefinition(objectId);

			if(chatDef != null)
			{
				chatDef.setChatDelay(delayValue);
			}

			chatDef = null;
		}

		public void setChatTexts(int objectId, String[] textsValue)
		{
			AutoChatDefinition chatDef = getChatDefinition(objectId);

			if(chatDef != null)
			{
				chatDef.setChatTexts(textsValue);
			}

			chatDef = null;
		}

		public void setRandomChat(int objectId, boolean randValue)
		{
			AutoChatDefinition chatDef = getChatDefinition(objectId);

			if(chatDef != null)
			{
				chatDef.setRandomChat(randValue);
			}

			chatDef = null;
		}

		public void setActive(boolean activeValue)
		{
			if(_isActive == activeValue)
			{
				return;
			}

			_isActive = activeValue;

			if(!isGlobal())
			{
				for(AutoChatDefinition chatDefinition : _chatDefinitions.values())
				{
					chatDefinition.setActive(activeValue);
				}

				return;
			}

			if(isActive())
			{
				AutoChatRunner acr = new AutoChatRunner(_npcId, -1);
				_chatTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(acr, _defaultDelay, _defaultDelay);
				acr = null;
			}
			else
			{
				_chatTask.cancel(false);
			}
		}

		private class AutoChatDefinition
		{
			protected int _chatIndex = 0;
			protected L2Npc _npcInstance;

			protected AutoChatInstance _chatInstance;

			private long _chatDelay = 0;
			private String[] _chatTexts = null;
			private boolean _isActiveDefinition;
			private boolean _randomChat;

			protected AutoChatDefinition(AutoChatInstance chatInst, L2Npc npcInst, String[] chatTexts, long chatDelay)
			{
				_npcInstance = npcInst;

				_chatInstance = chatInst;
				_randomChat = chatInst.isDefaultRandom();

				_chatDelay = chatDelay;
				_chatTexts = chatTexts;

				if(!chatInst.isGlobal())
				{
					setActive(true);
				}
			}

			protected String[] getChatTexts()
			{
				if(_chatTexts != null)
				{
					return _chatTexts;
				}
				else
				{
					return _chatInstance.getDefaultTexts();
				}
			}

			private long getChatDelay()
			{
				if(_chatDelay > 0)
				{
					return _chatDelay;
				}
				else
				{
					return _chatInstance.getDefaultDelay();
				}
			}

			private boolean isActive()
			{
				return _isActiveDefinition;
			}

			boolean isRandomChat()
			{
				return _randomChat;
			}

			void setRandomChat(boolean randValue)
			{
				_randomChat = randValue;
			}

			void setChatDelay(long delayValue)
			{
				_chatDelay = delayValue;
			}

			void setChatTexts(String[] textsValue)
			{
				_chatTexts = textsValue;
			}

			void setActive(boolean activeValue)
			{
				if(isActive() == activeValue)
				{
					return;
				}

				if(activeValue)
				{
					AutoChatRunner acr = new AutoChatRunner(_npcId, _npcInstance.getObjectId());

					if(getChatDelay() == 0)
					{
						_chatTask = ThreadPoolManager.getInstance().scheduleGeneral(acr, 5);
					}
					else
					{
						_chatTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(acr, getChatDelay(), getChatDelay());
					}

					acr = null;
				}
				else
				{
					_chatTask.cancel(false);
				}

				_isActiveDefinition = activeValue;
			}
		}

		private class AutoChatRunner implements Runnable
		{
			private int _runnerNpcId;
			private int _objectId;

			protected AutoChatRunner(int pNpcId, int pObjectId)
			{
				_runnerNpcId = pNpcId;
				_objectId = pObjectId;
			}

			@Override
			public synchronized void run()
			{
				AutoChatInstance chatInst = _registeredChats.get(_runnerNpcId);
				AutoChatDefinition[] chatDefinitions;

				if(chatInst.isGlobal())
				{
					chatDefinitions = chatInst.getChatDefinitions();
				}
				else
				{
					AutoChatDefinition chatDef = chatInst.getChatDefinition(_objectId);

					if(chatDef == null)
					{
						_log.warn("AutoChatHandler: Auto chat definition is NULL for NPC ID " + _npcId + ".");
						return;
					}

					chatDefinitions = new AutoChatDefinition[]
					{
						chatDef
					};
					chatDef = null;
				}

				for(AutoChatDefinition chatDef : chatDefinitions)
				{
					try
					{
						L2Npc chatNpc = chatDef._npcInstance;
						List<L2PcInstance> nearbyPlayers = new FastList<L2PcInstance>();

						for(L2Character player : chatNpc.getKnownList().getKnownCharactersInRadius(1500))
						{
							if(!(player instanceof L2PcInstance))
								continue;

							nearbyPlayers.add((L2PcInstance) player);
						}

						int maxIndex = chatDef.getChatTexts().length;
						int lastIndex = Rnd.nextInt(maxIndex);

						String creatureName = chatNpc.getName();
						String text;

						if(!chatDef.isRandomChat())
						{
							lastIndex = chatDef._chatIndex;
							lastIndex++;

							if(lastIndex == maxIndex)
							{
								lastIndex = 0;
							}

							chatDef._chatIndex = lastIndex;
						}

						text = chatDef.getChatTexts()[lastIndex];

						if(text == null)
						{
							return;
						}

						if(!nearbyPlayers.isEmpty())
						{
							L2PcInstance randomPlayer; 
							for(;;)
							{
								randomPlayer = nearbyPlayers.get(Rnd.nextInt(nearbyPlayers.size()));
								if(!randomPlayer.isGM())
									break;
							}

							final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
							int losingCabal = SevenSigns.CABAL_NULL;

							if(winningCabal == SevenSigns.CABAL_DAWN)
							{
								losingCabal = SevenSigns.CABAL_DUSK;
							}
							else if(winningCabal == SevenSigns.CABAL_DUSK)
							{
								losingCabal = SevenSigns.CABAL_DAWN;
							}

							if(text.indexOf("%player_random%") > -1)
							{
								text = text.replaceAll("%player_random%", randomPlayer.getName());
							}

							if(text.indexOf("%player_cabal_winner%") > -1)
							{
								for(L2PcInstance nearbyPlayer : nearbyPlayers)
								{
									if(SevenSigns.getInstance().getPlayerCabal(nearbyPlayer) == winningCabal)
									{
										text = text.replaceAll("%player_cabal_winner%", nearbyPlayer.getName());
										break;
									}
								}
							}

							if(text.indexOf("%player_cabal_loser%") > -1)
							{
								for(L2PcInstance nearbyPlayer : nearbyPlayers)
								{
									if(SevenSigns.getInstance().getPlayerCabal(nearbyPlayer) == losingCabal)
									{
										text = text.replaceAll("%player_cabal_loser%", nearbyPlayer.getName());
										break;
									}
								}
							}
							randomPlayer = null;
						}

						if(text == null)
						{
							return;
						}

						if(text.contains("%player_cabal_loser%") || text.contains("%player_cabal_winner%") || text.contains("%player_random%"))
						{
							return;
						}

						CreatureSay cs = new CreatureSay(chatNpc.getObjectId(), 0, creatureName, text);

						for(L2PcInstance nearbyPlayer : nearbyPlayers)
						{
							nearbyPlayer.sendPacket(cs);
						}
					}
					catch(Exception e)
					{
						_log.error("", e);
						return;
					}
				}
			}
		}
	}
}