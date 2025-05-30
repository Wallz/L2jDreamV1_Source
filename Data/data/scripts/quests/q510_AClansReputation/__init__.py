
import sys
from com.src.gameserver.model.quest        import State
from com.src.gameserver.model.quest        import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest
from com.src.gameserver.network.serverpackets      import PledgeShowInfoUpdate
from com.src.gameserver.network.serverpackets      import RadarControl
from com.src.gameserver.network.serverpackets      import SystemMessage

qn="q510_AClansReputation"
qd="A Clans Reputation"

# Quest NPC
GRAND_MAGISTER_VALDIS = 31331

# Quest Items
TYRANNOSAURUS_CLAW 			= 8767

#Quest Raid Bosses
TYRANNOSAURUS		 		= [22215,22216,22217]

# Reward
CLAN_POINTS_REWARD = 50

# id:[RaidBossNpcId,questItemId]
REWARDS_LIST={
    1:[TYRANNOSAURUS,  TYRANNOSAURUS_CLAW]
    }

RADAR={
    1:[13839,-18617,-3108]
    }

class Quest (JQuest) :

 def __init__(self,id,name,descr) : JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player) :
  st = player.getQuestState(qn)
  if not st: return
  cond = st.getInt("cond")
  htmltext=event
  if event == "30868-0.htm" :
    if cond == 0 :
      st.set("cond","1")
      st.setState(STARTED)
  elif event.isdigit() :
    if int(event) in REWARDS_LIST.keys():
      st.set("raid",event)
      htmltext="30868-"+event+".htm"
      x,y,z=RADAR[int(event)]
      if x+y+z:
        player.sendPacket(RadarControl(2, 2, x, y, z))
        player.sendPacket(RadarControl(0, 1, x, y, z))
      st.playSound("ItemSound.quest_accept")
  elif event == "30868-7.htm" :
    st.playSound("ItemSound.quest_finish")
    st.exitQuest(1)
  return htmltext

 def onTalk (self,npc,player) :
  htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
  st = player.getQuestState(qn)
  if not st : return htmltext
  clan = player.getClan()
  npcId = npc.getNpcId()
  if player.getClan() == None or player.isClanLeader() == 0 :
     st.exitQuest(1)
     htmltext = "30868-0a.htm"
  elif player.getClan().getLevel() < 5 :
     st.exitQuest(1)
     htmltext =  "30868-0b.htm"
  else :
     cond = st.getInt("cond")
     raid = st.getInt("raid")
     id = st.getState()
     if id == CREATED and cond == 0 :
        htmltext =  "30868-0d.htm"
     elif id == STARTED and cond == 1 and raid in REWARDS_LIST.keys() :
        npc,item=REWARDS_LIST[raid]
        count = st.getQuestItemsCount(item)
        if not count :
           htmltext = "30868-"+str(raid)+"a.htm"
        elif count >= 1 :
           htmltext = "30868-"+str(raid)+"b.htm"
           st.takeItems(item,1*count)
           clan.addReputationScore(clan.getReputationScore()+CLAN_POINTS_REWARD*count,True)
           player.sendPacket(SystemMessage(1777).addNumber(CLAN_POINTS_REWARD*count))
           clan.broadcastToOnlineMembers(PledgeShowInfoUpdate(clan))
  return htmltext

 def onKill(self,npc,player,isPet):
  partyMember = self.getRandomPartyMember(player,"1")
  if not partyMember : return
  st = partyMember.getQuestState(qn)
  if st :
   if st.getState() == STARTED :
    npcId = npc.getNpcId()
    if npcId in TYRANNOSAURUS : 
     st.giveItems(TYRANNOSAURUS_CLAW,1)
     st.playSound("ItemSound.quest_itemget")
  return

# Quest class and state definition
QUEST       = Quest(510,qn,qd)
CREATED     = State('Start',QUEST)
STARTED     = State('Started',QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(GRAND_MAGISTER_VALDIS)
QUEST.addTalkId(GRAND_MAGISTER_VALDIS)

for i in TYRANNOSAURUS :
    QUEST.addKillId(i)

STARTED.addQuestDrop(8767,TYRANNOSAURUS_CLAW,1)
