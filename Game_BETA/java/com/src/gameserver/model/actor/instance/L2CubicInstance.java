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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package com.src.gameserver.model.actor.instance;

import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2CubicInstance
{
	protected static final Logger _log = Logger.getLogger(L2CubicInstance.class.getName());

	public static final int STORM_CUBIC = 1;
	public static final int VAMPIRIC_CUBIC = 2;
	public static final int LIFE_CUBIC = 3;
	public static final int VIPER_CUBIC = 4;
	public static final int POLTERGEIST_CUBIC = 5;
	public static final int BINDING_CUBIC = 6;
	public static final int AQUA_CUBIC = 7;
	public static final int SPARK_CUBIC = 8;
	public static final int ATTRACT_CUBIC = 9;

	protected L2PcInstance _owner;
	protected L2Character _target;

	protected int _id;
	protected int _level = 1;

	protected List<Integer> _skills = new FastList<Integer>();

	private Future<?> _disappearTask;
	private Future<?> _actionTask;

	public L2CubicInstance(L2PcInstance owner, int id, int level)
	{
		_owner = owner;
		_id = id;
		_level = level;

		switch(_id)
		{
			case STORM_CUBIC:
				_skills.add(4049);
				break;
			case VAMPIRIC_CUBIC:
				_skills.add(4050);
				break;
			case LIFE_CUBIC:
				_skills.add(4051);
				_disappearTask = ThreadPoolManager.getInstance().scheduleGeneral(new Disappear(), 3600000);
				doAction(_owner);
				break;
			case VIPER_CUBIC:
				_skills.add(4052);
				break;
			case POLTERGEIST_CUBIC:
				_skills.add(4053);
				_skills.add(4054);
				_skills.add(4055);
				break;
			case BINDING_CUBIC:
				_skills.add(4164);
				break;
			case AQUA_CUBIC:
				_skills.add(4165);
				break;
			case SPARK_CUBIC:
				_skills.add(4166);
				break;
			case ATTRACT_CUBIC:
				_skills.add(5115);
				_skills.add(5116);
				break;
		}
		if(_disappearTask == null)
		{
			_disappearTask = ThreadPoolManager.getInstance().scheduleGeneral(new Disappear(), 1200000); // disappear in 20 mins
		}
	}

	public void doAction(L2Character target)
	{
		if(_target == target)
		{
			return;
		}
		stopAction();
		_target = target;
		switch(_id)
		{
			case STORM_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(12), 0, 10000);
				break;
			case VAMPIRIC_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(8), 0, 15000);
				break;
			case VIPER_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(30), 0, 20000);
				break;
			case POLTERGEIST_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(30), 0, 8000);
				break;
			case BINDING_CUBIC:
			case AQUA_CUBIC:
			case SPARK_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(30), 0, 8000);
				break;
			case LIFE_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(50), 0, 30000);
				break;
			case ATTRACT_CUBIC:
				_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(30), 0, 8000);
				break;
		}
	}

	public int getId()
	{
		return _id;
	}

	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	public void setLevel(int level)
	{
		_level = level;
	}

	public void stopAction()
	{
		_target = null;
		if (_actionTask != null)
		{
			if(!_actionTask.isCancelled())
				_actionTask.cancel(true);
			_actionTask = null;
		}
	}

	public void cancelDisappear()
	{
		if(_disappearTask != null)
		{
			_disappearTask.cancel(true);
			_disappearTask = null;
		}
	}

	private class Action implements Runnable
	{
		private int _chance;

		Action(int chance)
		{
			_chance = chance;
		}

		@Override
		public void run()
		{
			final L2PcInstance owner = _owner;
			final L2Character target = _target;

			if(owner != null && (owner.isDead() || owner.getTarget() != target || owner.isOffline() || target == null || target.isDead() || ( target instanceof L2PcInstance && ((L2PcInstance)target).isOffline())))
			{
				stopAction();
				if(owner.isDead())
				{
					owner.delCubic(_id);
					owner.broadcastUserInfo();
					cancelDisappear();
				}
				return;
			}
			if(!AttackStanceTaskManager.getInstance().getAttackStanceTask(owner))
			{
				stopAction();
				return;
			}
			if(target != null)
			{
				try
				{
					if(Rnd.get(1, 100) < _chance)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(_skills.get(Rnd.get(_skills.size())), _level);

						if(skill != null)
						{
							L2Character[] targets =
							{
								target
							};
							ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());

							int x, y, z;
							int range = target.getTemplate().collisionRadius + 400;

							x = owner.getX() - target.getX();
							y = owner.getY() - target.getY();
							z = owner.getZ() - target.getZ();
							if(x * x + y * y + z * z <= range * range)
							{
								if(handler != null)
								{
									handler.useSkill(owner, skill, targets);
								}
								else
								{
									skill.useSkill(owner, targets);
								}

								MagicSkillUser msu = new MagicSkillUser(owner, _target, skill.getId(), _level, 0, 0);
								owner.broadcastPacket(msu);
							}
						}
					}
				}
				catch(Exception e)
				{
					_log.log(Level.SEVERE, "", e);
				}
			}
		}
	}

	private class Heal implements Runnable
	{
		private int _chance;

		Heal(int chance)
		{
			_chance = chance;
		}

		@Override
		public void run()
		{
			if(_owner.isDead())
			{
				stopAction();
				_owner.delCubic(_id);
				_owner.broadcastUserInfo();
				cancelDisappear();
				return;
			}
			try
			{
				if(Rnd.get(1, 100) < _chance)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(_skills.get(Rnd.get(_skills.size())), _level);

					if(skill != null)
					{
						L2Character target, caster;
						target = null;
						if(_owner.isInParty())
						{
							caster = _owner;
							L2PcInstance player = _owner;
							L2Party party = player.getParty();
							double percentleft = 100.0;
							if(party != null)
							{
								List<L2PcInstance> partyList = party.getPartyMembers();
								L2Character partyMember = null;
								int x, y, z;
								int range = 400;
								for(int i = 0; i < partyList.size(); i++)
								{
									partyMember = partyList.get(i);
									if(!partyMember.isDead())
									{
										x = caster.getX() - partyMember.getX();
										y = caster.getY() - partyMember.getY();
										z = caster.getZ() - partyMember.getZ();
										if(x * x + y * y + z * z > range * range)
										{
											continue;
										}

										if(partyMember.getCurrentHp() < partyMember.getMaxHp())
										{
											if(percentleft > partyMember.getCurrentHp() / partyMember.getMaxHp())
											{
												percentleft = partyMember.getCurrentHp() / partyMember.getMaxHp();
												target = partyMember;
											}
										}
									}
								}
								party = null;
							}
							player = null;
						}
						else
						{
							if(_owner.getCurrentHp() < _owner.getMaxHp())
							{
								target = _owner;
							}
						}
						if(target != null)
						{
							L2Character[] targets =
							{
								target
							};
							ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
							if(handler != null)
							{
								handler.useSkill(_owner, skill, targets);
							}
							else
							{
								skill.useSkill(_owner, targets);
							}
							MagicSkillUser msu = new MagicSkillUser(_owner, target, skill.getId(), _level, 0, 0);
							_owner.broadcastPacket(msu);
						}
					}
				}
			}
			catch(Exception e)
			{
			}
		}
	}

	private class Disappear implements Runnable
	{
		Disappear()
		{
		}

		@Override
		public void run()
		{
			stopAction();
			_owner.delCubic(_id);
			_owner.broadcastUserInfo();
		}
	}
}