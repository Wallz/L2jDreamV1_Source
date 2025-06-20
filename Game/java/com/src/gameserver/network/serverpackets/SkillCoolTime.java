/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.network.serverpackets;

import java.util.Collection;
import java.util.Iterator;

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance.TimeStamp;

public class SkillCoolTime extends L2GameServerPacket
{
    public Collection<TimeStamp> _reuseTimeStamps;
    
    public SkillCoolTime(L2PcInstance cha)
    {
    	_reuseTimeStamps = cha.getReuseTimeStamps();
		Iterator<TimeStamp> iter = _reuseTimeStamps.iterator();
		while (iter.hasNext())
		{
			if (!iter.next().hasNotPassed())
				iter.remove();
		}
    }
    
    @Override
    public String getType()
    {
        return "[S] C7 SkillCoolTime";
    }
    
    @Override
    protected void writeImpl()
    {
        writeC(0xc7);
        writeD(_reuseTimeStamps.size()); // list size
        for (TimeStamp ts : _reuseTimeStamps)
        {
            writeD(ts.getSkill());
            writeD(0x00);
            writeD((int) ts.getReuse() / 1000);
            writeD((int) ts.getRemaining() / 1000);
        }
    }
    
}
