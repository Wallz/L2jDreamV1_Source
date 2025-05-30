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
package com.src.gameserver.templates.item;

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.skills.Env;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.skills.funcs.FuncTemplate;
import com.src.gameserver.templates.StatsSet;

public final class L2Armor extends L2Item
{
	private final int _avoidModifier;
	private final int _pDef;
	private final int _mDef;
	private final int _mpBonus;
	private final int _hpBonus;
	private L2Skill _itemSkill = null;

	public L2Armor(L2ArmorType type, StatsSet set)
	{
		super(type, set);
		_avoidModifier = set.getInteger("avoid_modify");
		_pDef = set.getInteger("p_def");
		_mDef = set.getInteger("m_def");
		_mpBonus = set.getInteger("mp_bonus", 0);
		_hpBonus = set.getInteger("hp_bonus", 0);

		int sId = set.getInteger("item_skill_id");
		int sLv = set.getInteger("item_skill_lvl");
		if(sId > 0 && sLv > 0)
		{
			_itemSkill = SkillTable.getInstance().getInfo(sId, sLv);
		}
	}

	@Override
	public L2ArmorType getItemType()
	{
		return (L2ArmorType) super._type;
	}

	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}

	public final int getMDef()
	{
		return _mDef;
	}

	public final int getPDef()
	{
		return _pDef;
	}

	public final int getAvoidModifier()
	{
		return _avoidModifier;
	}

	public final int getMpBonus()
	{
		return _mpBonus;
	}

	public final int getHpBonus()
	{
		return _hpBonus;
	}

	public L2Skill getSkill()
	{
		return _itemSkill;
	}

	@Override
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		List<Func> funcs = new FastList<Func>();
		if(_funcTemplates != null)
		{
			for(FuncTemplate t : _funcTemplates)
			{
				Env env = new Env();
				env.player = player;
				env.item = instance;
				Func f = t.getFunc(env, instance);
				if(f != null)
				{
					funcs.add(f);
				}
			}
		}
		return funcs.toArray(new Func[funcs.size()]);
	}

}