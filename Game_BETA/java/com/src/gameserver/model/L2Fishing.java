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
package com.src.gameserver.model;

import java.util.concurrent.Future;

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PenaltyMonsterInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExFishingHpRegen;
import com.src.gameserver.network.serverpackets.ExFishingStartCombat;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2Fishing implements Runnable
{
	private L2PcInstance _fisher;
	private int _time;
	private int _stop = 0;
	private int _goodUse = 0;
	private int _anim = 0;
	private int _mode = 0;
	private int _deceptiveMode = 0;
	private Future<?> _fishAiTask;
	private boolean _thinking;
	private int _fishId;
	private int _fishMaxHp;
	private int _fishCurHp;
	private double _regenHp;
	private boolean _isUpperGrade;
	private int _lureType;

	@Override
	public void run()
	{
		if(_fisher == null)
		{
			return;
		}

		if(_fishCurHp >= _fishMaxHp * 2)
		{
			_fisher.sendPacket(new SystemMessage(SystemMessageId.BAIT_STOLEN_BY_FISH));
			doDie(false);
		}
		else if(_time <= 0)
		{
			_fisher.sendPacket(new SystemMessage(SystemMessageId.FISH_SPIT_THE_HOOK));
			doDie(false);
		}
		else
		{
			aiTask();
		}
	}

	public L2Fishing(L2PcInstance Fisher, FishData fish, boolean isNoob, boolean isUpperGrade)
	{
		_fisher = Fisher;
		_fishMaxHp = fish.getHP();
		_fishCurHp = _fishMaxHp;
		_regenHp = fish.getHpRegen();
		_fishId = fish.getId();
		_time = fish.getCombatTime() / 1000;
		_isUpperGrade = isUpperGrade;

		if(isUpperGrade)
		{
			_deceptiveMode = Rnd.get(100) >= 90 ? 1 : 0;
			_lureType = 2;
		}
		else
		{
			_deceptiveMode = 0;
			_lureType = isNoob ? 0 : 1;
		}

		_mode = Rnd.get(100) >= 80 ? 1 : 0;

		ExFishingStartCombat efsc = new ExFishingStartCombat(_fisher, _time, _fishMaxHp, _mode, _lureType, _deceptiveMode);
		_fisher.broadcastPacket(efsc);

		_fisher.sendPacket(new PlaySound(1, "SF_S_01", 0, 0, 0, 0, 0));
		_fisher.sendPacket(new SystemMessage(SystemMessageId.GOT_A_BITE));

		if(_fishAiTask == null)
		{
			_fishAiTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(this, 1000, 1000);
		}

		efsc = null;
	}

	public void changeHp(int hp, int pen)
	{
		_fishCurHp -= hp;
		if(_fishCurHp < 0)
		{
			_fishCurHp = 0;
		}

		ExFishingHpRegen efhr = new ExFishingHpRegen(_fisher, _time, _fishCurHp, _mode, _goodUse, _anim, pen, _deceptiveMode);
		_fisher.broadcastPacket(efhr);
		_anim = 0;
		if(_fishCurHp > _fishMaxHp * 2)
		{
			_fishCurHp = _fishMaxHp * 2;
			doDie(false);
			return;
		}
		else if(_fishCurHp == 0)
		{
			doDie(true);
			return;
		}

		efhr = null;
	}

	public synchronized void doDie(boolean win)
	{
		if(_fishAiTask != null)
		{
			_fishAiTask.cancel(false);
			_fishAiTask = null;
		}

		if(_fisher == null)
		{
			return;
		}

		if(win)
		{
			int check = Rnd.get(100);
			if(check <= 5)
			{
				PenaltyMonster();
			}
			else
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.YOU_CAUGHT_SOMETHING));
				_fisher.addItem("Fishing", _fishId, 1, null, true);
			}
		}
		_fisher.EndFishing(win);
		_fisher = null;
	}

	protected void aiTask()
	{
		if(_thinking)
		{
			return;
		}

		_thinking = true;
		_time--;

		try
		{
			if(_mode == 1)
			{
				if(_deceptiveMode == 0)
				{
					_fishCurHp += (int) _regenHp;
				}
			}
			else
			{
				if(_deceptiveMode == 1)
				{
					_fishCurHp += (int) _regenHp;
				}
			}

			if(_stop == 0)
			{
				_stop = 1;
				int check = Rnd.get(100);

				if(check >= 70)
				{
					_mode = _mode == 0 ? 1 : 0;
				}
				if(_isUpperGrade)
				{
					check = Rnd.get(100);
					if(check >= 90)
					{
						_deceptiveMode = _deceptiveMode == 0 ? 1 : 0;
					}
				}
			}
			else
			{
				_stop--;
			}
		}
		finally
		{
			_thinking = false;
			ExFishingHpRegen efhr = new ExFishingHpRegen(_fisher, _time, _fishCurHp, _mode, 0, _anim, 0, _deceptiveMode);
			if(_anim != 0)
			{
				_fisher.broadcastPacket(efhr);
			}
			else
			{
				_fisher.sendPacket(efhr);
			}

			efhr = null;
		}
	}

	public void useRealing(int dmg, int pen)
	{
		_anim = 2;
		if(Rnd.get(100) > 90)
		{
			_fisher.sendPacket(new SystemMessage(SystemMessageId.FISH_RESISTED_ATTEMPT_TO_BRING_IT_IN));
			_goodUse = 0;
			changeHp(0, pen);
			return;
		}

		if(_fisher == null)
		{
			return;
		}

		if(_mode == 1)
		{
			if(_deceptiveMode == 0)
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.REELING_SUCCESFUL_S1_DAMAGE).addNumber(dmg));

				if(pen == 50)
				{
					_fisher.sendPacket(new SystemMessage(SystemMessageId.REELING_SUCCESSFUL_PENALTY_S1).addNumber(pen));
				}

				_goodUse = 1;
				changeHp(dmg, pen);
			}
			else
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.FISH_RESISTED_REELING_S1_HP_REGAINED).addNumber(dmg));
				_goodUse = 2;
				changeHp(-dmg, pen);
			}
		}
		else
		{
			if(_deceptiveMode == 0)
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.FISH_RESISTED_REELING_S1_HP_REGAINED).addNumber(dmg));
				_goodUse = 2;
				changeHp(-dmg, pen);
			}
			else
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.REELING_SUCCESFUL_S1_DAMAGE).addNumber(dmg));

				if(pen == 50)
				{
					_fisher.sendPacket(new SystemMessage(SystemMessageId.REELING_SUCCESSFUL_PENALTY_S1).addNumber(pen));
				}

				_goodUse = 1;
				changeHp(dmg, pen);
			}
		}
	}

	public void usePomping(int dmg, int pen)
	{
		_anim = 1;

		if(Rnd.get(100) > 90)
		{
			_fisher.sendPacket(new SystemMessage(SystemMessageId.FISH_RESISTED_ATTEMPT_TO_BRING_IT_IN));
			_goodUse = 0;
			changeHp(0, pen);
			return;
		}

		if(_fisher == null)
		{
			return;
		}

		if(_mode == 0)
		{
			if(_deceptiveMode == 0)
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.PUMPING_SUCCESFUL_S1_DAMAGE).addNumber(dmg));

				if(pen == 50)
				{
					_fisher.sendPacket(new SystemMessage(SystemMessageId.PUMPING_SUCCESSFUL_PENALTY_S1).addNumber(pen));
				}

				_goodUse = 1;
				changeHp(dmg, pen);
			}
			else
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.FISH_RESISTED_PUMPING_S1_HP_REGAINED).addNumber(dmg));
				_goodUse = 2;
				changeHp(-dmg, pen);
			}
		}
		else
		{
			if(_deceptiveMode == 0)
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.FISH_RESISTED_PUMPING_S1_HP_REGAINED).addNumber(dmg));
				_goodUse = 2;
				changeHp(-dmg, pen);
			}
			else
			{
				_fisher.sendPacket(new SystemMessage(SystemMessageId.PUMPING_SUCCESFUL_S1_DAMAGE).addNumber(dmg));

				if(pen == 50)
				{
					_fisher.sendPacket(new SystemMessage(SystemMessageId.PUMPING_SUCCESSFUL_PENALTY_S1).addNumber(pen));
				}

				_goodUse = 1;
				changeHp(dmg, pen);
			}
		}
	}

	private void PenaltyMonster()
	{
		int lvl = (int) Math.round(_fisher.getLevel() * 0.1);
		int npcid;

		_fisher.sendPacket(new SystemMessage(SystemMessageId.YOU_CAUGHT_SOMETHING_SMELLY_THROW_IT_BACK));
		switch(lvl)
		{
			case 0:
			case 1:
				npcid = 18319;
				break;
			case 2:
				npcid = 18320;
				break;
			case 3:
				npcid = 18321;
				break;
			case 4:
				npcid = 18322;
				break;
			case 5:
				npcid = 18323;
				break;
			case 6:
				npcid = 18324;
				break;
			case 7:
				npcid = 18325;
				break;
			case 8:
				npcid = 18326;
				break;
			default:
				npcid = 18319;
				break;
		}

		L2NpcTemplate temp;
		temp = NpcTable.getInstance().getTemplate(npcid);

		if(temp != null)
		{
			try
			{
				L2Spawn spawn = new L2Spawn(temp);
				spawn.setLocx(_fisher.GetFishx());
				spawn.setLocy(_fisher.GetFishy());
				spawn.setLocz(_fisher.GetFishz());
				spawn.setAmount(1);
				spawn.setHeading(_fisher.getHeading());
				spawn.stopRespawn();
				((L2PenaltyMonsterInstance) spawn.doSpawn()).setPlayerToKill(_fisher);
				spawn = null;
			}
			catch(Exception e)
			{
			}
		}

		temp = null;
	}

}