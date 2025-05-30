
import sys
from com.src.gameserver.model.quest        import State
from com.src.gameserver.model.quest        import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest
from com.src.gameserver.network.serverpackets      import PledgeShowInfoUpdate
from com.src.gameserver.network.serverpackets      import RadarControl
from com.src.gameserver.network.serverpackets      import SystemMessage

qn="q509_TheClansPrestige"
qd="The Clans Prestige"

# Quest NPC
GRAND_MAGISTER_VALDIS = 31331

# Quest Items
DAIMONS_EYES 				= 8489
HESTIAS_FAIRY_STONE       	= 8490
NUCLEUS_OF_LESSER_GOLEM		= 8491
FALSTONS_FANG       	    = 8492
SHAIDS_TALON                = 8493

#Quest Raid Bosses
DAIMON_THE_WHITE_EYED 		= 25290
HESTIA               		= 25293
PLAGUE_GOLEM			    = 25523
DEMONS_AGENT_FALSTON        = 25322
QUEEN_SHYEED                = 25514 

# Reward
CLAN_POINTS_REWARD = 1000 # 1000 Point Per Boss

# id:[RaidBossNpcId,questItemId]
REWARDS_LIST={
    1:[DAIMON_THE_WHITE_EYED,  DAIMONS_EYES],
    2:[HESTIA,       	HESTIAS_FAIRY_STONE],
    3:[PLAGUE_GOLEM,  NUCLEUS_OF_LESSER_GOLEM],
    4:[DEMONS_AGENT_FALSTON, FALSTONS_FANG],
    5:[QUEEN_SHYEED,          SHAIDS_TALON]
    }

RADAR={
    1:[186304,-43744,-3193],
    2:[134672,-115600,-1216],
    3:[170000,-60000,-3500],
    4:[93296,-75104,-1824],
    5:[79635,-55434,-6135]
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
  elif player.getClan().getLevel() < 6 :
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
        elif count == 1 :
           htmltext = "30868-"+str(raid)+"b.htm"
           st.takeItems(item,1)
           clan.setReputationScore(clan.getReputationScore()+CLAN_POINTS_REWARD,True)
           player.sendPacket(SystemMessage(1777).addNumber(CLAN_POINTS_REWARD))
           clan.broadcastToOnlineMembers(PledgeShowInfoUpdate(clan))
  return htmltext

 def onKill(self,npc,player,isPet) :
  st = 0
  if player.isClanLeader() :
   st = player.getQuestState(qn)
  else:
   clan = player.getClan()
   if clan:
    leader=clan.getLeader()
    if leader :
     pleader= leader.getPlayerInstance()
     if pleader :
      if npc.isInsideRadius(pleader, 1600, 1, 0) :
       st = pleader.getQuestState(qn)
  if not st : return
  option=st.getInt("raid")
  if st.getInt("cond") == 1 and st.getState() == STARTED and option in REWARDS_LIST.keys():
   raid,item = REWARDS_LIST[option]
   npcId=npc.getNpcId()
   if npcId == raid and not st.getQuestItemsCount(item) :
      st.giveItems(item,1)
      st.playSound("ItemSound.quest_middle")
  return


# Quest class and state definition
QUEST       = Quest(509,qn,qd)
CREATED     = State('Start',QUEST)
STARTED     = State('Started',QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(GRAND_MAGISTER_VALDIS)
QUEST.addTalkId(GRAND_MAGISTER_VALDIS)

for npc,item in REWARDS_LIST.values():
    QUEST.addKillId(npc)
    STARTED.addQuestDrop(npc,item,1)