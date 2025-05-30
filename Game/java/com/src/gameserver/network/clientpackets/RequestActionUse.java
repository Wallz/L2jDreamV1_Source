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

import java.util.Map;
import java.util.logging.Logger;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2ManufactureList;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.src.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.src.gameserver.model.actor.instance.L2StaticObjectInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ChairSit;
import com.src.gameserver.network.serverpackets.RecipeShopManageList;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestActionUse extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestActionUse.class.getName());

	private static final String _C__45_REQUESTACTIONUSE = "[C] 45 RequestActionUse";

	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;

	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = readD() == 1;
		_shiftPressed = readC() == 1;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if(activeChar.isAlikeDead())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isOutOfControl())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isCastingNow())
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Summon pet = activeChar.getPet();
		L2Object target = activeChar.getTarget();

		switch(_actionId)
		{
			case 0:
				if(activeChar.getMountType() != 0)
				{
					break;
				}

				if(target != null && !activeChar.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && CastleManager.getInstance().getCastle(target) != null && activeChar.isInsideRadius(target, L2StaticObjectInstance.INTERACTION_DISTANCE, false, false))
				{
					ChairSit cs = new ChairSit(activeChar, ((L2StaticObjectInstance) target).getStaticObjectId());
					activeChar.sendPacket(cs);
					activeChar.sitDown();
					activeChar.broadcastPacket(cs);
					break;
				}

				if(activeChar.isSitting())
				{
					activeChar.standUp();
				}
				else if((activeChar.getAI().getIntention() == CtrlIntention.AI_INTENTION_MOVE_TO) || (activeChar.getAI().getIntention() == CtrlIntention.AI_INTENTION_CAST))
				{
					activeChar.getAI().setSitDownAfterAction(true);
				}
				else
				{
					activeChar.sitDown();
				}

				break;
			case 1:
				if(activeChar.isRunning())
				{
					activeChar.setWalking();
				}
				else
				{
					activeChar.setRunning();
				}

				break;
			case 15:
			case 21:
				if(pet != null && !pet.isMovementDisabled() && !activeChar.isBetrayed())
				{
					pet.setFollowStatus(!pet.getFollowStatus());
				}

				break;
			case 16:
			case 22:
				if(target != null && pet != null && pet != target && !pet.isAttackingDisabled() && !activeChar.isBetrayed())
				{
					if(activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
					{
						activeChar.sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}

					if(!activeChar.getAccessLevel().allowPeaceAttack() && L2Character.isInsidePeaceZone(pet, target))
					{
						if(!activeChar.isInFunEvent() || !target.isInFunEvent())
						{
							activeChar.sendMessage("You cant attack in peace zone.");
							return;
						}
					}

					if(target.isAutoAttackable(activeChar) || _ctrlPressed)
					{
						if(target instanceof L2DoorInstance)
						{
							if(((L2DoorInstance) target).isAttackable(activeChar) && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
							{
								pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
							}
						}
						else if(pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
						{
							pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
						}
					}
				}
				break;
			case 17:
			case 23:
				if(pet != null && !pet.isMovementDisabled() && !activeChar.isBetrayed())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
				}

				break;
			case 19:
				if(pet != null && !activeChar.isBetrayed())
				{
					if(pet.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED));
					}
					else if(pet.isAttackingNow() || pet.isRooted())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
					}
					else
					{
						if(pet instanceof L2PetInstance)
						{
							L2PetInstance petInst = (L2PetInstance) pet;

							if (petInst.isHungry()) 
								activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS)); 
						 	else 
						 		pet.unSummon(activeChar);
						}
					}
				}
				break;
			case 38:
				if(pet != null && pet.isMountable() && !activeChar.isMounted() && !activeChar.isBetrayed())
				{
					if(activeChar.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD));
					}
					else if(pet.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN));
					}
					else if(pet.isInCombat() || pet.isRooted())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN));
					}
					else if(activeChar.isInCombat() || activeChar.getPvpFlag() != 0)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
					}
					else if(activeChar.isInEvent())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
					}
					else if(activeChar.isSitting() || activeChar.isMoving())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING));
					}
					else if(activeChar.isFishing())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2));
					}
					else if(activeChar.isCursedWeaponEquiped())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
					}
					else if(!pet.isDead() && !activeChar.isMounted())
					{
						if(!activeChar.disarmWeapons())
						{
							return;
						}

						Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().npcId);
						activeChar.broadcastPacket(mount);
						activeChar.setMountType(mount.getMountType());
						activeChar.setMountObjectID(pet.getControlItemId());
						pet.unSummon(activeChar);

						if(activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) != null || activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND) != null)
						{
                            if(activeChar.isFlying())
                            {
                                activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
                                activeChar.sendSkillList();
                            }
                            
							if(activeChar.setMountType(0))
							{

								Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0);
								activeChar.broadcastPacket(dismount);
								activeChar.setMountObjectID(0);
							}
						}
					}
				}
				else if(activeChar.isRentedPet())
				{
					activeChar.stopRentPet();
				}
				else if(activeChar.isMounted())
				{
                    if(activeChar.isFlying())
                    {        
                        // Remove skill Wyvern Breath
                        activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
                        activeChar.sendSkillList();
                    }
                    
					if(activeChar.setMountType(0))
					{

						activeChar.broadcastPacket(new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0));
						activeChar.setMountObjectID(0);
					}
				}
				break;
			case 32:
				useSkill(4230);
				break;
			case 36:
				useSkill(4259);
				break;
			case 37:
				if(activeChar.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if(activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo();
				}

				if(activeChar.isSitting())
				{
					activeChar.standUp();
				}

				if(activeChar.getCreateList() == null)
				{
					activeChar.setCreateList(new L2ManufactureList());
				}

				activeChar.sendPacket(new RecipeShopManageList(activeChar, true));
				break;
			case 39:
				useSkill(4138);
				break;
			case 41:
				if(target != null && (target instanceof L2DoorInstance || target instanceof L2SiegeFlagInstance))
				{
					useSkill(4230);
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				break;
			case 42:
				useSkill(4378, activeChar);
				break;
			case 43:
				useSkill(4137);
				break;
			case 44:
				useSkill(4139);
				break;
			case 45:
				useSkill(4025, activeChar);
				break;
			case 46:
				useSkill(4261);
				break;
			case 47:
				useSkill(4260);
				break;
			case 48:
				useSkill(4068);
				break;
			case 51:
				if(activeChar.isAlikeDead())
				{
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if(activeChar.getPrivateStoreType() != 0)
				{
					activeChar.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
					activeChar.broadcastUserInfo();
				}

				if(activeChar.isSitting())
				{
					activeChar.standUp();
				}

				if(activeChar.getCreateList() == null)
				{
					activeChar.setCreateList(new L2ManufactureList());
				}

				activeChar.sendPacket(new RecipeShopManageList(activeChar, false));
				break;
			case 52:
				if (pet != null && pet instanceof L2SummonInstance)
				{
					if (pet.isDead())
						activeChar.sendPacket(new SystemMessage(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED));
					else if (pet.isBetrayed() || pet.isMovementDisabled())
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_REFUSING_ORDER));
					else if (pet.isAttackingNow() || pet.isInCombat())
						activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE));
					else
						pet.unSummon(activeChar);
				}
				break;
			case 53:
				if(target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isBetrayed())
				{
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 54:
				if (target != null && pet != null && pet != target && !pet.isMovementDisabled() && !pet.isBetrayed())
				{
					pet.setFollowStatus(false);
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				}
				break;
			case 96:
				_log.info("98 Accessed");
				break;
			case 97:
				_log.info("97 Accessed");
				break;
			case 1000:
				if(target instanceof L2DoorInstance)
					useSkill(4079);
				break;
			case 1001:
				break;
			case 1003:
				useSkill(4710);
				break;
			case 1004:
				useSkill(4711, activeChar);
				break;
			case 1005:
				useSkill(4712);
				break;
			case 1006:
				useSkill(4713, activeChar);
				break;
			case 1007:
				useSkill(4699, activeChar);
				break;
			case 1008:
				useSkill(4700, activeChar);
				break;
			case 1009:
				useSkill(4701);
				break;
			case 1010:
				useSkill(4702, activeChar);
				break;
			case 1011:
				useSkill(4703, activeChar);
				break;
			case 1012:
				useSkill(4704);
				break;
			case 1013:
				useSkill(4705);
				break;
			case 1014:
				useSkill(4706, activeChar);
				break;
			case 1015:
				useSkill(4707);
				break;
			case 1016:
				useSkill(4709);
				break;
			case 1017:
				useSkill(4708);
				break;
			case 1031:
				useSkill(5135);
				break;
			case 1032:
				useSkill(5136);
				break;
			case 1033:
				useSkill(5137);
				break;
			case 1034:
				useSkill(5138);
				break;
			case 1035:
				useSkill(5139);
				break;
			case 1036:
				useSkill(5142);
				break;
			case 1037:
				useSkill(5141);
				break;
			case 1038:
				useSkill(5140);
				break;
			case 1039:
				if(!(target instanceof L2DoorInstance))
					useSkill(5110);
				break;
			case 1040:
				if(!(target instanceof L2DoorInstance))
					useSkill(5111);
				break;
			default:
				_log.warning(activeChar.getName() + ": unhandled action type " + _actionId);
		}
	}

	private void useSkill(int skillId, L2Object target)
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
			return;
		
		final L2Summon activeSummon = activeChar.getPet();
		if (activeSummon != null && !activeSummon.isBetrayed())
		{
			if (activeSummon instanceof L2PetInstance)
			{
				if (activeSummon.getLevel() - activeChar.getLevel() > 20)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_TOO_HIGH_TO_CONTROL));
					return;
				}
			}
			
			Map<Integer, L2Skill> _skills = activeSummon.getTemplate().getSkills();
			if (_skills == null || _skills.size() == 0)
				return;
			
			L2Skill skill = _skills.get(skillId);
			if (skill == null)
			{
				_log.warning(activeSummon.getName() + " does not have the skill id " + skillId + " assigned.");
				return;
			}
			
			if (skill.isOffensive() && activeChar == target)
				return;
			
			activeSummon.setTarget(target);
			activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
	}
	
	/*
	 * Cast a skill for active pet/servitor. Target is retrieved from owner' target, then validated by overloaded method useSkill(int, L2Object).
	 */
	private void useSkill(int skillId)
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		useSkill(skillId, activeChar.getTarget());
	}

	@Override
	public String getType()
	{
		return _C__45_REQUESTACTIONUSE;
	}
}