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

import java.util.logging.Logger;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.knownlist.NullKnownList;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.ShowTownMap;

public class L2StaticObjectInstance extends L2Object
{
	private static Logger _log = Logger.getLogger(L2StaticObjectInstance.class.getName());

	public static final int INTERACTION_DISTANCE = 150;

	private int _staticObjectId;
	private int _type = -1;
	private int _x;
	private int _y;
	private String _texture;

	public int getStaticObjectId()
	{
		return _staticObjectId;
	}

	public void setStaticObjectId(int StaticObjectId)
	{
		_staticObjectId = StaticObjectId;
	}

	public L2StaticObjectInstance(int objectId)
	{
		super(objectId);
		setKnownList(new NullKnownList(this));
	}

	public int getType()
	{
		return _type;
	}

	public void setType(int type)
	{
		_type = type;
	}

	public void setMap(String texture, int x, int y)
	{
		_texture = "town_map." + texture;
		_x = x;
		_y = y;
	}

	private int getMapX()
	{
		return _x;
	}

	private int getMapY()
	{
		return _y;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(_type < 0)
		{
			_log.info("L2StaticObjectInstance: StaticObject with invalid type! StaticObjectId: " + getStaticObjectId());
		}

		if(this != player.getTarget())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		}
		else
		{
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;

			if(!player.isInsideRadius(this, INTERACTION_DISTANCE, false, false))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
			{
				if(_type == 2)
				{
					String filename = "data/html/signboard.htm";
					String content = HtmCache.getInstance().getHtm(filename);
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

					if(content == null)
					{
						html.setHtml("<html><body>Signboard is missing:<br>" + filename + "</body></html>");
					}
					else
					{
						html.setHtml(content);
					}

					player.sendPacket(html);
					player.sendPacket(ActionFailed.STATIC_PACKET);
					html = null;
					filename = null;
					content = null;
				}
				else if(_type == 0)
				{
					player.sendPacket(new ShowTownMap(_texture, getMapX(), getMapY()));
				}

				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

}