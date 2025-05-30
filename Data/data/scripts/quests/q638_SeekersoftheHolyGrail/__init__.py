import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q638_SeekersoftheHolyGrail"

#NPCs
INNOCENTIN = 31328

#MOBs
MOBS = [22194]+range(22136,22149)+range(22151,22176)+range(22188,22190)

#Items
DROP_CHANCE = 75
PAGAN_TOTEM = 8068
ADENA = 57
EWS_EAS = [959,960]
class Quest (JQuest) :
 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    totem = st.getQuestItemsCount(PAGAN_TOTEM)
    if event == "ok.htm" :
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
       st.set("cond","1")
    elif event == "nagrada" :
        if totem >= 2000 :
            if st.getRandom(100) < 50 : 
                st.takeItems(PAGAN_TOTEM,2000)
                st.giveItems(EWS_EAS[st.getRandom(len(EWS_EAS))],1)
                st.playSound("ItemSound.quest_itemget")
            else :
                st.takeItems(PAGAN_TOTEM,2000)
                st.giveItems(ADENA,3576000) 
                st.playSound("ItemSound.quest_itemget")
        else :
            htmltext = "nehvataet.htm"
    return htmltext

 def onTalk (self,npc,player):
    st = player.getQuestState(qn)
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>" 
    if not st: return htmltext
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    totem = st.getQuestItemsCount(PAGAN_TOTEM)
    if npcId == INNOCENTIN :
       if id == CREATED :
          if player.getLevel() >= 73 :
             htmltext = "privetstvie.htm"
          else :
             htmltext = "lvl.htm"
             st.exitQuest(1)
       elif cond == 1:
          if totem >= 2000 :
             htmltext = "prines.htm"
          else:
             htmltext = "nehvataet.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    partyMember = self.getRandomPartyMember(player,"1")
    if not partyMember : return
    st = partyMember.getQuestState(qn)
    if st :
        chance = DROP_CHANCE*Config.RATE_DROP_QUEST
        numItems, chance = divmod(chance,100)
        random = st.getRandom(100)
        if random <= chance:
           numItems += 1
        if int(numItems) != 0 :
           st.giveItems(PAGAN_TOTEM,int(numItems))
           st.playSound("ItemSound.quest_itemget")
    return  

QUEST = Quest(638,qn,"Seekers of the Holy Grail")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(INNOCENTIN)
QUEST.addTalkId(INNOCENTIN)

for m in MOBS:
   QUEST.addKillId(m)
