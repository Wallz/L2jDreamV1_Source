import sys
from com.src.gameserver.model.actor.instance import L2PcInstance
from java.util import Iterator
from com.src.gameserver.datatables import SkillTable
from com.src.util.database import L2DatabaseFactory
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "9999_NPCBuffer"

NPC=[70013]
ADENA_ID=57
QuestId     = 9999
QuestName   = "NPCBuffer"
QuestDesc   = "custom"
InitialHtml = "1.htm"

class Quest (JQuest) :

	def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)


	def onEvent(self,event,st):
		htmltext = event
		count=st.getQuestItemsCount(ADENA_ID)
		if count < 1000  or st.getPlayer().getLevel() < 0 :
			htmltext = "<html><head><body>You dont have enought Adena.</body></html>"
		else:
			st.takeItems(ADENA_ID,0)
			st.getPlayer().setTarget(st.getPlayer())

			#Wind Walk
			if event == "1":
				st.takeItems
				SkillTable.getInstance().getInfo(1204,2).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Acument
			if event == "2":
				st.takeItems
				SkillTable.getInstance().getInfo(1085,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Agility
			if event == "3":
				st.takeItems
				SkillTable.getInstance().getInfo(4355,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Berserker Spirit
			if event == "4":
				st.takeItems
				SkillTable.getInstance().getInfo(1062,2).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Bless Shield
			if event == "5":
				st.takeItems
				SkillTable.getInstance().getInfo(1243,6).getEffects(st.getPlayer(),st.getPlayer())				
				return "6.htm"
				st.setState(State.COMPLETED)

			#Blessed Body
			if event == "6":
				st.takeItems
				SkillTable.getInstance().getInfo(1045,6).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Blessed Soul
		        if event == "7":
				st.takeItems
				SkillTable.getInstance().getInfo(1048,6).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)		
				st.setState(COMPLETED)

			#Concentration
			if event == "8":
				st.takeItems
				SkillTable.getInstance().getInfo(1078,6).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Death Whisper
			if event == "9":
				st.takeItems
				SkillTable.getInstance().getInfo(1242,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Empower
			if event == "10":
				st.takeItems
				SkillTable.getInstance().getInfo(1059,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Focus
			if event == "11":
				st.takeItems
				SkillTable.getInstance().getInfo(1077,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Guidance
			if event == "12":
				st.takeItems
				SkillTable.getInstance().getInfo(1240,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Haste
			if event == "13":
				st.takeItems
				SkillTable.getInstance().getInfo(1086,2).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Magic Barrier
			if event == "14":
				st.takeItems
				SkillTable.getInstance().getInfo(4349,2).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Mental Shield
			if event == "15":
				st.takeItems
				SkillTable.getInstance().getInfo(1035,4).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Might
			if event == "16":
				st.takeItems
				SkillTable.getInstance().getInfo(1068,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Resist Shock
			if event == "17":
				st.takeItems
				SkillTable.getInstance().getInfo(1259,4).getEffects(st.getPlayer(),st.getPlayer())				
				return "6.htm"
				st.setState(State.COMPLETED)

			#Shield
			if event == "18":
				st.takeItems
				SkillTable.getInstance().getInfo(1040,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Vampiric Rage
			if event == "19":
				st.takeItems
				SkillTable.getInstance().getInfo(1268,4).getEffects(st.getPlayer(),st.getPlayer())				
				return "6.htm"
				st.setState(State.COMPLETED)

			#Dance of Aqua Guard
			if event == "30":
				st.takeItems
				SkillTable.getInstance().getInfo(307,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)


			#Dance of Concentration
			if event == "31":
				st.takeItems
				SkillTable.getInstance().getInfo(276,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of Earth Guard
			if event == "32":
				st.takeItems
				SkillTable.getInstance().getInfo(309,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of Fire
			if event == "33":
				st.takeItems
				SkillTable.getInstance().getInfo(274,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of Fury
			if event == "34":
				st.takeItems
				SkillTable.getInstance().getInfo(275,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of Inspiration
			if event == "35":
				st.takeItems
				SkillTable.getInstance().getInfo(272,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of Light
			if event == "36":
				st.takeItems
				SkillTable.getInstance().getInfo(277,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of the Mystic
			if event == "37":
				st.takeItems
				SkillTable.getInstance().getInfo(273,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of Protection
			if event == "38":
				st.takeItems
				SkillTable.getInstance().getInfo(311,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Siren's Dance
			if event == "39":
				st.takeItems
				SkillTable.getInstance().getInfo(365,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of the Vampire
			if event == "40":
				st.takeItems
				SkillTable.getInstance().getInfo(310,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Dance of the Warrior
			if event == "41":
				st.takeItems
				SkillTable.getInstance().getInfo(271,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "3.htm"
				st.setState(State.COMPLETED)

			#Song of Champion
			if event == "50":
				st.takeItems
				SkillTable.getInstance().getInfo(364,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Earth
			if event == "51":
				st.takeItems
				SkillTable.getInstance().getInfo(264,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Flame Guard
			if event == "52":
				st.takeItems
				SkillTable.getInstance().getInfo(306,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Hunter
			if event == "53":
				st.takeItems
				SkillTable.getInstance().getInfo(269,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Invocation
			if event == "54":
				st.takeItems
				SkillTable.getInstance().getInfo(270,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Life
			if event == "55":
				st.takeItems
				SkillTable.getInstance().getInfo(265,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Meditation
			if event == "56":
				st.takeItems
				SkillTable.getInstance().getInfo(363,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Renewal
			if event == "57":
				st.takeItems
				SkillTable.getInstance().getInfo(349,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Storm Guard
			if event == "58":
				st.takeItems
				SkillTable.getInstance().getInfo(308,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Vengeance
			if event == "59":
				st.takeItems
				SkillTable.getInstance().getInfo(305,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Vitality
			if event == "60":
				st.takeItems
				SkillTable.getInstance().getInfo(304,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Warding
			if event == "61":
				st.takeItems
				SkillTable.getInstance().getInfo(267,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Water
			if event == "62":
				st.takeItems
				SkillTable.getInstance().getInfo(266,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Song of Wind
			if event == "63":
				st.takeItems
				SkillTable.getInstance().getInfo(268,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "4.htm"
				st.setState(State.COMPLETED)

			#Greater Might
			if event == "64":
				st.takeItems
				SkillTable.getInstance().getInfo(1388,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Greater Shield
			if event == "65":
				st.takeItems
				SkillTable.getInstance().getInfo(1389,3).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Chant of Victory
			if event == "66":
				st.takeItems
				SkillTable.getInstance().getInfo(1363,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Prophecy of Fire
			if event == "67":
				st.takeItems
				SkillTable.getInstance().getInfo(1356,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Prophecy of Water
			if event == "68":
				st.takeItems
				SkillTable.getInstance().getInfo(1355,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Prophecy of Wind
			if event == "69":
				st.takeItems
				SkillTable.getInstance().getInfo(1357,1).getEffects(st.getPlayer(),st.getPlayer())	
				return "5.htm"
				st.setState(State.COMPLETED)

			#Cancellation
			if event == "70":
				st.takeItems
				SkillTable.getInstance().getInfo(4094,12).getEffects(st.getPlayer(),st.getPlayer())
				st.getPlayer().stopAllEffects()
				return "1.htm"
				st.setState(State.COMPLETED)

			#MP-HP
			if event == "71":
				st.takeItems
				st.getPlayer().setCurrentCp(st.getPlayer().getMaxCp())
				return "1.htm"				
				st.setState(State.COMPLETED)
	
        	#Chant of Magnus
			if event == "74":
				st.takeItems
				SkillTable.getInstance().getInfo(1413,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

       		#Wild Magic
			if event == "75":
				st.takeItems
				SkillTable.getInstance().getInfo(1303,1).getEffects(st.getPlayer(),st.getPlayer())				
				return "2.htm"
				st.setState(State.COMPLETED)

			#Blesing Of Queen
			if event == "78":
				st.takeItems
				SkillTable.getInstance().getInfo(4699,13).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Gift Of Queen
			if event == "79":
				st.takeItems
				SkillTable.getInstance().getInfo(4700,13).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Blessing Of Seraphim
			if event == "80":
				st.takeItems
				SkillTable.getInstance().getInfo(4702,13).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Gift Of Seraphim
			if event == "81":
				st.takeItems
				SkillTable.getInstance().getInfo(4703,13).getEffects(st.getPlayer(),st.getPlayer())				
				return "5.htm"
				st.setState(State.COMPLETED)

			#Fighter Buffs
			if event == "76":
				st.takeItems
				SkillTable.getInstance().getInfo(1040,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1036,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1062,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1045,6).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1204,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1068,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1086,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1389,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1077,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1242,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1352,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1353,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(268,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(267,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(264,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(304,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(269,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(364,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(349,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(271,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(274,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(275,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1363,1).getEffects(st.getPlayer(),st.getPlayer())
                                return "1.htm"
				st.setState(State.COMPLETED)

			#Mage Buffs
			if event == "77":
				st.takeItems
				SkillTable.getInstance().getInfo(1085,3).getEffects(st.getPlayer(),st.getPlayer())	
				SkillTable.getInstance().getInfo(1040,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1036,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1062,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1045,6).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1204,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1389,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1059,3).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1078,6).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1303,2).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1352,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1353,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(268,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(267,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(264,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(304,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(349,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(363,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(365,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(276,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(273,1).getEffects(st.getPlayer(),st.getPlayer())
				SkillTable.getInstance().getInfo(1363,1).getEffects(st.getPlayer(),st.getPlayer())
                                return "1.htm"
				st.setState(State.COMPLETED)

			#Magic Barrier
			if event == "100":
				st.takeItems
				SkillTable.getInstance().getInfo(1036,2).getEffects(st.getPlayer(),st.getPlayer())
                                return "2.htm"
				st.setState(State.COMPLETED)

			#Holy Weapon
			if event == "101":
				st.takeItems
				SkillTable.getInstance().getInfo(1043,1).getEffects(st.getPlayer(),st.getPlayer())
                                return "2.htm"
				st.setState(State.COMPLETED)

			#Elemental Protection
			if event == "102":
				st.takeItems
				SkillTable.getInstance().getInfo(1352,1).getEffects(st.getPlayer(),st.getPlayer())
                                return "6.htm"
				st.setState(State.COMPLETED)

			#Arcane Protection
			if event == "103":
				st.takeItems
				SkillTable.getInstance().getInfo(1354,1).getEffects(st.getPlayer(),st.getPlayer())
                                return "6.htm"
				st.setState(State.COMPLETED)

			#Divine Protection
			if event == "104":
				st.takeItems
				SkillTable.getInstance().getInfo(1353,1).getEffects(st.getPlayer(),st.getPlayer())
                                return "6.htm"
				st.setState(State.COMPLETED)

			#Advanced Block
			if event == "105":
				st.takeItems
				SkillTable.getInstance().getInfo(1304,3).getEffects(st.getPlayer(),st.getPlayer())
                                return "6.htm"
				st.setState(State.COMPLETED)
				
			#Noblesse
			if event == "106":
				st.takeItems
				SkillTable.getInstance().getInfo(1323,1).getEffects(st.getPlayer(),st.getPlayer())
                                return "1.htm"
				st.setState(State.COMPLETED)

			if htmltext != event:
				st.setState(COMPLETED)
				st.exitQuest(1)
		return htmltext


	def onTalk (self,npc,player):
	   st = player.getQuestState(qn)
	   htmltext = "<html><head><body>I have nothing to say to you</body></html>"
	   st.setState(STARTED)
	   return InitialHtml

QUEST       = Quest(QuestId,str(QuestId) + "_" + QuestName,QuestDesc)
CREATED=State('Start',QUEST)
STARTED=State('Started',QUEST)
COMPLETED=State('Completed',QUEST)

QUEST.setInitialState(CREATED)

for npcId in NPC:
 QUEST.addStartNpc(npcId)
 QUEST.addTalkId(npcId)