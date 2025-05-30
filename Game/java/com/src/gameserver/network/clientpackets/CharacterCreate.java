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

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.CharNameTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.xml.CharTemplateTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.model.L2ShortCut;
import com.src.gameserver.model.L2SkillLearn;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.serverpackets.CharCreateFail;
import com.src.gameserver.network.serverpackets.CharCreateOk;
import com.src.gameserver.network.serverpackets.CharSelectInfo;
import com.src.gameserver.templates.chars.L2PcTemplate;
import com.src.gameserver.templates.chars.L2PcTemplate.PcTemplateItem;
import com.src.gameserver.util.Util;

@SuppressWarnings("unused")
public final class CharacterCreate extends L2GameClientPacket
{
	private static final String _C__0B_CHARACTERCREATE = "[C] 0B CharacterCreate";
	private static Logger _log = Logger.getLogger(CharacterCreate.class.getName());
	private static final Object CREATION_LOCK = new Object();

	private String _name;
	private int _race;
	private byte _sex;
	private int _classId;
	private int _int;
	private int _str;
	private int _con;
	private int _men;
	private int _dex;
	private int _wit;
	private byte _hairStyle;
	private byte _hairColor;
	private byte _face;

	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		_int = readD();
		_str = readD();
		_con = readD();
		_men = readD();
		_dex = readD();
		_wit = readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}

	@Override
	protected void runImpl()
	{
		synchronized (CREATION_LOCK)
		{
			if(CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= 7)
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS);
				sendPacket(ccf);
				return;
			}
			else if(CharNameTable.getInstance().doesCharNameExist(_name))
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS);
				sendPacket(ccf);
				return;
			}
			else if(_name.length() < 3 || _name.length() > 16 || !Util.isAlphaNumeric(_name) || !isValidName(_name))
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS);
				sendPacket(ccf);
				return;
			}

			if(Config.FORBIDDEN_NAMES.length > 1)
			{
				for(String st : Config.FORBIDDEN_NAMES)
				{
					if(_name.toLowerCase().contains(st.toLowerCase()))
					{
						CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_INCORRECT_NAME);
						sendPacket(ccf);
						return;
					}
				}
			}
			
			L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(_classId);

			if(template == null || template.classBaseLevel > 1)
			{
				CharCreateFail ccf = new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED);
				sendPacket(ccf);
				return;
			}

			int objectId = IdFactory.getInstance().getNextId();

			L2PcInstance newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
			newChar.setCurrentHp(newChar.getMaxHp());
			newChar.setCurrentCp(0);
			newChar.setCurrentMp(newChar.getMaxMp());

			CharCreateOk cco = new CharCreateOk();
			sendPacket(cco);

			initNewChar(getClient(), newChar);
		}
	}

	private boolean isValidName(String text)
	{
		boolean result = true;

		String test = text;
		Pattern pattern;

		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch(PatternSyntaxException e)
		{
			_log.warning("ERROR : Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}

		Matcher regexp = pattern.matcher(test);
		if(!regexp.matches())
		{
			result = false;
		}

		return result;
	}

	@SuppressWarnings("static-access")
	private void initNewChar(L2GameClient client, L2PcInstance newChar)
	{
		L2World.getInstance().storeObject(newChar);

		L2PcTemplate template = newChar.getTemplate();

		if(Config.STARTING_ADENA > 0)
		{
			newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		}

		if(Config.STARTING_AA > 0)
		{
			newChar.addAncientAdena("Init", Config.STARTING_AA, null, false);
		}

		if(Config.CUSTOM_STARTER_ITEMS_ENABLED)
		{
			if(newChar.isMageClass())
			{
				for(int[] reward : Config.STARTING_CUSTOM_ITEMS_M)
				{
					if(ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
					{
						newChar.getInventory().addItem("Starter Items Mage", reward[0], reward[1], newChar, null);
					}
					else
					{
						for(int i = 0; i < reward[1]; ++i)
						{
							newChar.getInventory().addItem("Starter Items Mage", reward[0], 1, newChar, null);
						}
					}
				}
			}
			else
			{
				for(int[] reward : Config.STARTING_CUSTOM_ITEMS_F)
				{
					if(ItemTable.getInstance().createDummyItem(reward[0]).isStackable())
					{
						newChar.getInventory().addItem("Starter Items Fighter", reward[0], reward[1], newChar, null);
					}
					else
					{
						for(int i = 0; i < reward[1]; ++i)
						{
							newChar.getInventory().addItem("Starter Items Fighter", reward[0], 1, newChar, null);
						}
					}
				}
			}
		}
		
		if(Config.SPAWN_CHAR)
		{
			newChar.setXYZInvisible(Config.SPAWN_X, Config.SPAWN_Y, Config.SPAWN_Z);
		}
		else
		{
			newChar.setXYZInvisible(template.spawnX, template.spawnY, template.spawnZ);
		}

		if(Config.ALLOW_CREATE_LVL)
		{
			newChar.getStat().addExp(Experience.getExp(Config.CHAR_CREATE_LVL));
		}

		if(Config.CHAR_TITLE)
		{
			newChar.setTitle(Config.ADD_CHAR_TITLE);
		}
		else
		{
			newChar.setTitle("");
		}

		L2ShortCut shortcut;

		shortcut = new L2ShortCut(0, 0, 3, 2, -1, 1);
		newChar.registerShortCut(shortcut);

		shortcut = new L2ShortCut(3, 0, 3, 5, -1, 1);
		newChar.registerShortCut(shortcut);

		shortcut = new L2ShortCut(10, 0, 3, 0, -1, 1);
		newChar.registerShortCut(shortcut);

		for (PcTemplateItem ia : template.getItems())
		{
			L2ItemInstance item = newChar.getInventory().addItem("Init", ia.getItemId(), ia.getAmount(), newChar, null);

			if (item.getItemId() == 5588)
			{
				shortcut = new L2ShortCut(11, 0, 1, item.getObjectId(), 0, 1);
				newChar.registerShortCut(shortcut);
			}

			if (item.isEquipable() && ia.isEquipped())
				newChar.getInventory().equipItemAndRecord(item);
		}

		L2SkillLearn[] startSkills = SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId());

		for(L2SkillLearn startSkill : startSkills)
		{
			newChar.addSkill(SkillTable.getInstance().getInfo(startSkill.getId(), startSkill.getLevel()), true);

			if(startSkill.getId() == 1001 || startSkill.getId() == 1177)
			{
				shortcut = new L2ShortCut(1, 0, 2, startSkill.getId(), 1, 1);
				newChar.registerShortCut(shortcut);
			}

			if(startSkill.getId() == 1216)
			{
				shortcut = new L2ShortCut(10, 0, 2, startSkill.getId(), 1, 1);
				newChar.registerShortCut(shortcut);
			}
		}

		startTutorialQuest(newChar);
		newChar.store();
		newChar.deleteMe();

		CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
		client.getConnection().sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}

	public void startTutorialQuest(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("Q255_Tutorial");

		Quest q = null;

		if(qs == null)
		{
			q = QuestManager.getInstance().getQuest("Q255_Tutorial");
		}

		if(q != null)
		{
			q.newQuestState(player);
		}
	}

	@Override
	public String getType()
	{
		return _C__0B_CHARACTERCREATE;
	}

}