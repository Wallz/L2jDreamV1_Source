package com.src.gameserver.managers;

import java.util.List;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.event.BW;
import com.src.gameserver.model.entity.event.CTF;
import com.src.gameserver.model.entity.event.DM;
import com.src.gameserver.model.entity.event.FunEvent;
import com.src.gameserver.model.entity.event.FunEvent.State;
import com.src.gameserver.model.entity.event.TvT;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;

public class FunEventsManager
{
	//private static final Logger _log = LogManager.getLogger(FunEventsManager.class);

	private static FastMap<String, FunEvent> _events = new FastMap<String, FunEvent>();
	private static List<FunEvent> _activeEvents = new FastList<FunEvent>();

	private FunEventsManager()
	{
		loadEvents();
	}

	public void loadEvents()
	{
		_events.clear();
		_events.put("TVT", new TvT());
		_events.put("CTF", new CTF());
		_events.put("BW", new BW());
		_events.put("DM", new DM());
		//_events.put("TW", new TW()); Maybye later, then i improve it
	}

	public FunEvent getEvent(String eventName)
	{
		return _events.get(eventName);
	}

	public FunEvent getEvent(int eventId)
	{
		for (FunEvent event : _events.values())
		{
			if (event.EVENT_ID == eventId)
				return event;
		}
		return null;
	}

	public boolean isFightingInEvent(L2PcInstance player)
	{
		if (player.getEventName().equals(""))
			return false;
		for (FunEvent event : _events.values())
		{
			if (player.getEventName().equals(event.EVENT_NAME) && event.getState() == State.FIGHTING)
				return true;
		}
		return false;
	}

	public void autoStartEvents()
	{
		for (FunEvent event : _events.values())
			event.autoStart();
	}

	public void abortEvents()
	{
		for (FunEvent event : _events.values())
			event.abortEvent();
	}

	public void abortEvent(String eventName)
	{
		FunEvent event = _events.get(eventName);
		if (event != null)
			event.requestAbortEvent();
	}

	public void startEvent(String eventName)
	{
		FunEvent event = _events.get(eventName);
		if (event != null)
		{
			event.startEvent();
			_activeEvents.add(event);
		}
	}

	public String getEventsInfo(String lang)
	{
		String info = "";
		for (FunEvent event : _events.values())
			info += event.getInfo(lang);
		return info;
	}

	public NpcHtmlMessage getChatWindow(L2PcInstance player, String eventName)
	{
		NpcHtmlMessage mes = null;
		FunEvent event = _events.get(eventName);
		if (event != null)
			mes = event.getChatWindow(player);
		return mes;
	}

	public void notifyJoinCursed(L2PcInstance player)
	{
		FunEvent event = _events.get(player.getEventName());
		if (event != null && event.getState() == State.PARTICIPATING && !event.EVENT_JOIN_CURSED)
		{
			event.removePlayer(player);
			// TODO: Message
		}
	}

	public void notifyLevelChanged(L2PcInstance player)
	{
		FunEvent event = _events.get(player.getEventName());
		if (event != null && event.getState() == State.PARTICIPATING && (player.getLevel() > event.EVENT_PLAYER_LEVEL_MAX || player.getLevel() < event.EVENT_PLAYER_LEVEL_MIN))
		{
			event.removePlayer(player);
			// TODO: Message
		}
	}

	public boolean notifyPlayerKilled(L2PcInstance player, L2Character killer)
	{
		FunEvent event = null;
		L2PcInstance killerInstance = null;
		if (killer instanceof L2PcInstance) 
		    killerInstance = (L2PcInstance)killer;
		   
		if (player.isFightingInEvent() && player.isInSameEvent(killerInstance) && !player.isInSameTeam(killerInstance))
		{
			event = _events.get(player.getEventName());
			if (event != null && event.getState() == State.FIGHTING)
				return event.onPlayerDie(player, killerInstance);
		}
		return true;
	}

	public void notifyPlayerLogout(L2PcInstance player)
	{
		FunEvent event = null;
		if (player.isFightingInEvent())
		{
			event = _events.get(player.getEventName());
			if (event != null && event.getState() != State.INACTIVE)
				event.onPlayerLogout(player);
		}
	}

	public void notifyPlayerLogin(L2PcInstance player)
	{
			for (FunEvent event : _events.values())
				event.onPlayerLogin(player);
	}

	public static FunEventsManager getInstance()
	{
		return SingletonHolder._instance;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final FunEventsManager _instance = new FunEventsManager();
	}
	
	public FastMap<String, FunEvent> getEvents()
	{
		return _events;
	}
	
	public List<FunEvent> getActiveEvents()
	{
		return _activeEvents;
	}
}