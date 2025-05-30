import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q639_GuardiansoftheHolyGrail"

#NPCs
DOMINIC = 31350
GREMORY = 32008
HOLY_GRAIL = 32028

#MOBs
MOBS = range(22122,22129)+range(22132,22135)

#Items
DROP_CHANCE = 75
SCRIPTURE = 8069
WATER_BOTTLE = 8070
WATER_GRAIL = 8071
ADENA = 57
EWS = 959
EAS = 960

class Quest (JQuest) :
 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    monk = st.getQuestItemsCount(SCRIPTURE)
    if event == "ok.htm" :
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
       st.set("cond","1")
    elif event == "nagradadominic" :
        if monk >= 1 :
            st.takeItems(SCRIPTURE,-1)
            num = monk*1000
            st.giveItems(ADENA,num)
            st.playSound("ItemSound.quest_itemget")
        else :
            htmltext = "nehvataetdominic.htm"
    elif event == "ews" :
        if monk >= 4000 :
            st.takeItems(SCRIPTURE,4000)
            st.giveItems(EWS,1)
            st.playSound("ItemSound.quest_itemget")
        else :
            htmltext = "nehvataetgremory.htm"
    elif event == "eas" :
        if monk >= 400 :
            st.takeItems(SCRIPTURE,400)
            st.giveItems(EAS,1)
            st.playSound("ItemSound.quest_itemget")
        else :
            htmltext = "nehvataetgremory.htm"          
    elif event == "okgremory.htm" :
        st.set("cond","2")
        st.giveItems(WATER_BOTTLE,1)
        st.playSound("ItemSound.quest_itemget")
    elif event == "voda.htm" :
        st.set("cond","3")
        st.takeItems(WATER_BOTTLE,1)
        st.giveItems(WATER_GRAIL,1)
        st.playSound("ItemSound.quest_itemget")
    elif event == "monks.htm" :
        st.set("cond","4")
        st.takeItems(WATER_GRAIL,1)
        st.playSound("ItemSound.quest_itemget") 
    return htmltext

 def onTalk (self,npc,player):
    st = player.getQuestState(qn)
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>" 
    if not st: return htmltext
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    monk = st.getQuestItemsCount(SCRIPTURE)
    if npcId == DOMINIC :
       if id == CREATED :
          if player.getLevel() >= 73 :
             htmltext = "privetstvie.htm"
          else :
             htmltext = "lvl.htm"
             st.exitQuest(1)
       elif id == STARTED :
          if monk >= 1 :
             htmltext = "prinesdominic.htm"
          else:
             htmltext = "nehvataetdominic.htm"
    elif npcId == GREMORY :
       if id == STARTED and cond == 1 :
          htmltext = "privetstviegremory.htm"
       elif id == STARTED and  cond == 2 :
          htmltext = "zavodoi.htm"        
       elif id == STARTED and  cond == 3 :
          htmltext = "prinesvodu.htm"
       elif id == STARTED and  cond == 4 :
          if monk >= 1 :
             htmltext = "prinesgremory.htm"
          else:
             htmltext = "nehvataetgremory.htm"        
    elif npcId == HOLY_GRAIL :
       if id == STARTED and cond == 2 :
          htmltext = "privetstvieholygrail.htm"          
    return htmltext
 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    cond = st.getInt("cond")
    npcId = npc.getNpcId()
    if st.getState() == STARTED :
        chance = DROP_CHANCE*Config.RATE_DROP_QUEST
        numItems, chance = divmod(chance,100)
        random = st.getRandom(100)
        if random <= chance:
            numItems += 1
        if int(numItems) != 0 :
            st.giveItems(SCRIPTURE,int(numItems))
            st.playSound("ItemSound.quest_itemget") 

QUEST = Quest(639,qn,"Guardians of the Holy Grail")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)

for npcId in MOBS:
   QUEST.addKillId(npcId)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(DOMINIC)
QUEST.addTalkId(DOMINIC)
QUEST.addTalkId(GREMORY)
QUEST.addTalkId(HOLY_GRAIL)
