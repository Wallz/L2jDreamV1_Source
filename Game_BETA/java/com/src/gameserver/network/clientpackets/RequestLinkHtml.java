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
package com.src.gameserver.network.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;

public final class RequestLinkHtml extends L2GameClientPacket
{
	 private static Log _log = LogFactory.getLog(RequestLinkHtml.class);

	private static final String REQUESTLINKHTML__C__20 = "[C] 20 RequestLinkHtml";

	private String _link;

	@Override
	protected void readImpl()
	{
		_link = readS();
	}

	@Override
	public void runImpl()
	{
		L2PcInstance actor = getClient().getActiveChar();
		if(actor == null)
		{
			return;
		}

		if(_link.contains("..") || !_link.contains(".htm"))
		{
			_log.warn("[RequestLinkHtml] hack? link contains prohibited characters: '" + this._link + "', skipped");
			return;
		}

		if(!actor.validateLink(_link))
		{
			return;
		}
		try
		{
			String filename = "data/html/" + this._link;
			NpcHtmlMessage msg = new NpcHtmlMessage(0);
			msg.setFile(filename);
			L2Npc npc = actor.getLastFolkNPC();
			if (npc != null)
				msg.replace("%objectId%", String.valueOf(npc.getObjectId()));
			sendPacket(msg);
		}
		catch (Exception e)
		{
			_log.fatal("Bad RequestLinkHtml: " + this._link, e);
		}
	}

	@Override
	public String getType()
	{
		return REQUESTLINKHTML__C__20;
	}

}