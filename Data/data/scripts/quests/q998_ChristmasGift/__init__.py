# Elektra Christmas Quest

import sys
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q998_ChristmasGift"

BLACK_WOLF_PELT_ID = 1482
BW_GRADE_ID = 148
BA_GRADE_ID = 2381
DW_GRADE_ID = 225
DA_GRADE_ID = 396
CW_GRADE_ID = 303
CA_GRADE_ID = 356
METAL_HARD = 5231
DUAL_CRAFT = 5126
COAL_ID = 1870

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "1" :
      st.set("id","0")
      st.set("cond","1")
      st.setState(STARTED)
      st.playSound("ItemSound.quest_accept")
      htmltext = "12618-03.htm"
    return htmltext


 def onTalk (Self,npc,st):

   npcId = npc.getNpcId()
   htmltext = "<html><body>I have nothing to say you</body></html>"
   id = st.getState()
   if id == CREATED :
     st.setState(STARTING)
     st.set("cond","0")
     st.set("onlyone","0")
     st.set("id","0")
   if npcId == 12618 and int(st.get("cond"))==0 :
      if int(st.get("cond")) < 15 :
        if st.getPlayer().getLevel() < 19 :
          htmltext = "12618-01.htm"
          st.exitQuest(1)
        else:
          htmltext = "12618-02.htm"
          return htmltext
      else:
        htmltext = "12618-01.htm"
        st.exitQuest(1)
   elif npcId == 12618 and int(st.get("cond")) :
      if st.getPlayer().getLevel() < 19 :
        htmltext = "12618-04.htm"
      else:
        if int(st.get("id")) != 291 :
          st.set("id","291")
          htmltext = "12618-05.htm"
          st.set("cond","1")
          st.setState(COMPLETED)
          st.playSound("ItemSound.quest_finish")
          n = st.getRandom(100)
          if n <= 2 :
            st.giveItems(BW_GRADE_ID,1)
          elif n <= 4 :
            st.giveItems(BA_GRADE_ID,1)
          elif n <= 8 :
            st.giveItems(CW_GRADE_ID,1)
          elif n <= 13 :
            st.giveItems(CA_GRADE_ID,1)
          elif n <= 18 :
            st.giveItems(METAL_HARD,1)
          elif n <= 24 :
            st.giveItems(DUAL_CRAFT,1)
          elif n <= 30 :
            st.giveItems(DW_GRADE_ID,1)
          elif n <= 35 :
            st.giveItems(DA_GRADE_ID,1)
          else:
            st.giveItems(COAL_ID,50)

   return htmltext

 def onTalk (Self,npc,player):

   npcId = npc.getNpcId()
   st = player.getQuestState(qn)
   htmltext = "<html><body>I have nothing to say to you.</body></html>"
   st.set("cond","0")
   st.setState(STARTED)
   return "12618-01.htm"

QUEST       = Quest(998,qn,"Christmas Gift")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)

QUEST.addStartNpc(31864)
QUEST.addTalkId(31864)