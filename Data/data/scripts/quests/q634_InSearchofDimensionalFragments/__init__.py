import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q634_InSearchofDimensionalFragments"

DIMENSION_FRAGMENT_ID = 7079
DROP_CHANCE=75

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "2a.htm" :
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
      st.set("cond","1")
    elif event == "5.htm" :
      st.playSound("ItemSound.quest_finish")
      st.exitQuest(1)
    return htmltext

 def onTalk (self,npc,player):
   st = player.getQuestState(qn)
   if st :
        npcId = npc.getNpcId()
        htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
        id = st.getState()
        if id == CREATED :
            if player.getLevel() < 20 :
                st.exitQuest(1)
                htmltext="1.htm"
            else:
                htmltext="2.htm"
        elif id == STARTED :
            htmltext = "4.htm"
   return htmltext

 def onKill (self, npc, player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    if st :
        if st.getState() == STARTED :
            cond = st.getInt("cond")
            if cond == 1 :
                chance = DROP_CHANCE*Config.RATE_DROP_QUEST
                numItems, chance = divmod(chance,100)
                if st.getRandom(100) < chance : 
                    numItems += 1
                if numItems :
                    st.playSound("ItemSound.quest_itemget")
                    st.giveItems(DIMENSION_FRAGMENT_ID,int(numItems))
    return

QUEST       = Quest(634, qn, "In Search of Dimensional Fragments")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)

for npcId in range(31494,31508):
  QUEST.addTalkId(npcId)
  QUEST.addStartNpc(npcId)

for mobs in range(21208,21256):
  QUEST.addKillId(mobs)
