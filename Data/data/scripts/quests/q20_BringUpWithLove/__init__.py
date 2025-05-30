# Need TEST (Not for OFF)
#Modified by Bibigon for RT C5 v.0.3
# Edited by TARAN 24.12.07

import sys

from com.src import Config 
from com.src.gameserver.model.actor.instance import L2TamedBeastInstance
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q20_BringUpWithLove"

TUNATUN = 31537
TAMEDBEAST = range(16013,16019)
JEWEL_INNOCENCE = 7185
SKILL_GOLDEN_SPICE = 2188
SKILL_CRYSTAL_SPICE = 2189

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
   id = st.getState()
   id2 = st.getStateId()
   htmltext = event
   if id == CREATED or id2 == "Aborted" :
      st.set("cond","0")
      if event == "31537-1.htm" :
        return htmltext
      elif event == "31537-2.htm" :
        return htmltext
      elif event == "31537-3.htm" :
        return htmltext
      elif event == "31537-4.htm" :
        return htmltext
      elif event == "31537-5.htm" :
        return htmltext
      elif event == "31537-6.htm" :
        st.set("cond","1")
        st.setState(STARTED)
        st.playSound("ItemSound.quest_accept")
        return htmltext
   elif event == "31537-8.htm" :
     st.takeItems(JEWEL_INNOCENCE,-1)
     st.giveItems(57,68500)
     st.unset("cond")
     st.setState(COMPLETED)
     st.playSound("ItemSound.quest_finish")
   return htmltext

 def onTalk (self, npc, player) :
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   htmltext = "<html><head><body>I have nothing to say you</body></html>"
   if not st : return htmltext
   id = st.getState()
   id2 = st.getStateId()
   cond = st.getInt("cond")
   if npcId == TUNATUN :
     if cond == 0 :
       if id == COMPLETED :
         htmltext = "<html><body>This quest has already been completed.</body></html>"
       elif id == CREATED or id2 == "Aborted" :
         if st.getPlayer().getLevel() <= 64 :
           st.exitQuest(1)
           htmltext = "31537-00.htm"
         else:
           htmltext = "31537-0.htm"
     elif cond == 1 and st.getQuestItemsCount(JEWEL_INNOCENCE) == 0 :
       htmltext = "31537-6a.htm"
     elif cond == 2 and st.getQuestItemsCount(JEWEL_INNOCENCE) >= 1 :
       htmltext = "31537-7.htm"
   return htmltext

 def onSkillUse (self,npc,player,skill):
    st = player.getQuestState(qn)
    if not st : return
    if st.getState() != STARTED : return
    npcId = npc.getNpcId()
    skillId = skill.getId()
    if npcId not in TAMEDBEAST : return
    if skillId not in [SKILL_GOLDEN_SPICE,SKILL_CRYSTAL_SPICE] : return

    if st.getQuestItemsCount(JEWEL_INNOCENCE) == 0 :
       if npcId in TAMEDBEAST :
          if skillId in [SKILL_GOLDEN_SPICE,SKILL_CRYSTAL_SPICE] :
             if st.getRandom(100) <= 20 :
                st.giveItems(JEWEL_INNOCENCE,1)
                st.playSound("ItemSound.quest_middle")
                st.set("cond","2")
    else : return

    return


QUEST       = Quest(20,qn,"Bring Up With Love")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)
ABORTED     = State('Aborted',   QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(TUNATUN)

QUEST.addTalkId(TUNATUN)

for i in TAMEDBEAST :
  QUEST.addSkillUseId(i)
  STARTED.addQuestDrop(i,JEWEL_INNOCENCE,1)
