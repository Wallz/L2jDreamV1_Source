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
package com.src.gameserver.updaters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.itemcontainer.ItemContainer;
import com.src.gameserver.thread.ThreadPoolManager;

public class CastleUpdater implements Runnable
{
	private static Log _log = LogFactory.getLog(CastleUpdater.class);

	private L2Clan _clan;
	private int _runCount = 0;

	public CastleUpdater(L2Clan clan, int runCount)
	{
		_clan = clan;
		_runCount = runCount;
	}

	@Override
	public void run()
	{
		try
		{
			ItemContainer warehouse = _clan.getWarehouse();
			if(warehouse != null && _clan.getHasCastle() > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(_clan.getHasCastle());
				if(!Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					if(_runCount % Config.ALT_MANOR_SAVE_PERIOD_RATE == 0)
					{
						castle.saveSeedData();
						castle.saveCropData();
						_log.info("Manor System: all data for " + castle.getName() + " saved");
					}
				}

				_runCount++;
				CastleUpdater cu = new CastleUpdater(_clan, _runCount);
				ThreadPoolManager.getInstance().scheduleGeneral(cu, 3600000);
				warehouse = null;
			}
		}
		catch(Throwable e)
		{
			_log.error("", e);
		}
	}

}