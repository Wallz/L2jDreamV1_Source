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
package com.src.gameserver.skills.effects;

import javolution.util.FastList;

import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2EffectPointInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.MagicSkillLaunched;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.l2skills.L2SkillSignetCasttime;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.util.object.Point3D;

public final class EffectSignetMDam extends L2Effect
{
	private L2EffectPointInstance _actor;
	boolean isAffected = false;

	public EffectSignetMDam(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.SIGNET_GROUND;
	}

	@Override
	public void onStart()
	{
		L2NpcTemplate template;
		if(getSkill() instanceof L2SkillSignetCasttime)
		{
			template = NpcTable.getInstance().getTemplate(((L2SkillSignetCasttime) getSkill())._effectNpcId);
		}
		else
		{
			return;
		}

		L2EffectPointInstance effectPoint = new L2EffectPointInstance(IdFactory.getInstance().getNextId(), template, getEffector());
		effectPoint.getStatus().setCurrentHp(effectPoint.getMaxHp());
		effectPoint.getStatus().setCurrentMp(effectPoint.getMaxMp());
		L2World.getInstance();
		L2World.storeObject(effectPoint);

		int x = getEffector().getX();
		int y = getEffector().getY();
		int z = getEffector().getZ();

		if(getEffector() instanceof L2PcInstance && getSkill().getTargetType() == L2Skill.SkillTargetType.TARGET_GROUND)
		{
			Point3D wordPosition = ((L2PcInstance) getEffector()).getCurrentSkillWorldPosition();

			if(wordPosition != null)
			{
				x = wordPosition.getX();
				y = wordPosition.getY();
				z = wordPosition.getZ();
			}
		}
		effectPoint.setIsInvul(true);
		effectPoint.spawnMe(x, y, z);

		_actor = effectPoint;
	}

	@Override
	public boolean onActionTime()
	{
		if(getCount() >= getTotalCount() - 2)
		{
			return true;
		}
		int mpConsume = getSkill().getMpConsume();

		L2PcInstance caster = (L2PcInstance) getEffector();

		FastList<L2Character> targets = new FastList<L2Character>();

		boolean ss = false;
		boolean bss = false;

		for(L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if(cha == null || cha == caster)
			{
				continue;
			}

			if(cha instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) getEffector();
				L2PcInstance target = (L2PcInstance) cha;
				if(cha == player)
				{
					continue;
				}

				if(target.getPvpFlag() > 0)
				{
					if(player.getParty() != null)
					{
						if(!player.getParty().getPartyMembers().contains(target))
						{
							isAffected = true;
						}
						else if(player.getParty().getCommandChannel() != null)
						{
							if(!player.getParty().getCommandChannel().getMembers().contains(target))
							{
								isAffected = true;
							}
						}
					}

					if(player.getClan() != null && !player.isInsideZone(L2Character.ZONE_PVP))
					{
						if(!player.getClan().isMember(null))
						{
							isAffected = true;
						}

						if(player.getAllyId() > 0 && target.getAllyId() > 0)
						{
							if(player.getAllyId() != target.getAllyId())
							{
								isAffected = true;
							}
						}
					}

					if(target.getParty() == null)
					{
						isAffected = true;
					}
				}

				if(target.getPvpFlag() == 0)
				{
					if(player.getClan() != null && target.getClan() != null)
					{
						if(player.getClan().isAtWarWith(target.getClanId()))
						{
							isAffected = true;
						}
					}
				}
			}

			if(cha instanceof L2Attackable || cha instanceof L2Playable)
			{
				if(cha.isAlikeDead())
				{
					continue;
				}

				if(mpConsume > caster.getStatus().getCurrentMp())
				{
					caster.sendPacket(new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
					return false;
				}

				caster.reduceCurrentMp(mpConsume);

				if(cha instanceof L2Playable)
				{
					if(cha instanceof L2Summon && ((L2Summon) cha).getOwner() == caster)
					{}
					else
					{
						caster.updatePvPStatus(cha);
					}
				}

				targets.add(cha);
			}
		}

		if(!targets.isEmpty())
		{
			caster.broadcastPacket(new MagicSkillLaunched(caster, getSkill().getDisplayId(), getSkill().getLevel(), targets.toArray(new L2Character[targets.size()])));
			for(L2Character target : targets)
			{
				boolean mcrit = Formulas.calcMCrit(caster.getMCriticalHit(target, getSkill()));
				int mdam = (int) Formulas.calcMagicDam(caster, target, getSkill(), ss, bss, mcrit);

				if(target instanceof L2Summon)
				{
					target.broadcastStatusUpdate();
				}

				if(mdam > 0)
				{
					if(!target.isRaid() && Formulas.calcAtkBreak(target, mdam))
					{
						target.breakAttack();
						target.breakCast();
					}

					caster.sendDamageMessage(target, mdam, mcrit, false, false);
					target.reduceCurrentHp(mdam, caster);
				}

				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, caster);
			}
		}

		if(isAffected)
		{
			_actor.broadcastPacket(new MagicSkillLaunched(caster, getSkill().getId(), getSkill().getLevel(), targets.toArray(new L2Character[targets.size()])));
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if(_actor != null)
		{
			_actor.deleteMe();
		}
	}
}