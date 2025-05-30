# Created by Gigiikun
# Quest: Defeat the Elrokian Raiders
import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q688_DefeatTheElrokianRaiders"

#Settings: drop chance in %
DROP_CHANCE = 50

ADENA = 57
DINOSAUR_FANG_NECKLACE = 8785


class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    count = st.getQuestItemsCount(DINOSAUR_FANG_NECKLACE)
    if event == "32105-03.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
    elif event == "32105-08.htm" :
       if count > 0 :
          st.takeItems(DINOSAUR_FANG_NECKLACE,-1)
          st.giveItems(ADENA,count*3000)
       st.playSound("ItemSound.quest_finish")
       st.exitQuest(1)
    elif event == "32105-06.htm" :
       st.takeItems(DINOSAUR_FANG_NECKLACE,-1)
       st.giveItems(ADENA,count*3000)
    elif event == "32105-07.htm" :
       if count >= 100 :
          st.takeItems(DINOSAUR_FANG_NECKLACE,100)
          st.giveItems(ADENA,450000)
       else :
          htmltext = "32105-04.htm"
    elif event == "None" :
       htmltext = None
    return htmltext

 def onTalk (self, npc, player):
    st = player.getQuestState(qn)
    htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
    if st :
       npcId = npc.getNpcId()
       cond = st.getInt("cond")
       count = st.getQuestItemsCount(DINOSAUR_FANG_NECKLACE)
       if cond == 0 :
          if player.getLevel() >= 75 :
             htmltext = "32105-01.htm"
          else :
             htmltext = "32105-00.htm"
             st.exitQuest(1)
       elif st.getState() == STARTED :
          if count == 0 :
             htmltext = "32105-04.htm"
          else :
             htmltext = "32105-05.htm"
    return htmltext

 def onKill (self, npc, player,isPet):
    partyMember = self.getRandomPartyMember(player,"1")
    if not partyMember: return
    st = partyMember.getQuestState(qn)
    if st :
       if st.getState() == STARTED :
          npcId = npc.getNpcId()
          cond = st.getInt("cond")
          count = st.getQuestItemsCount(DINOSAUR_FANG_NECKLACE)
          if cond == 1 :
             chance = DROP_CHANCE*Config.RATE_DROP_QUEST
             numItems, chance = divmod(chance,100)
             if st.getRandom(100) < chance : 
                numItems += 1
             if numItems :
                if count + numItems >= 100 :
                   st.playSound("ItemSound.quest_middle")
                else :
                   st.playSound("ItemSound.quest_itemget")
                st.giveItems(DINOSAUR_FANG_NECKLACE,int(numItems))
    return

QUEST       = Quest(688,qn,"Defeat The Elrokian Raiders")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(32105)

QUEST.addTalkId(32105)

QUEST.addKillId(22214)

STARTED.addQuestDrop(32105,DINOSAUR_FANG_NECKLACE,1)
