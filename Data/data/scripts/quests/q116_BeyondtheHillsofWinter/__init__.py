import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q116_BeyondtheHillsofWinter"

FILAUR = 30535
OBI = 32052

BANDAGE = 1833
ENERGY_STONE = 5589
THIEF_KEY = 1661
FIGNA = 8098
ADENA = 57
SSD = 1463

class Quest (JQuest):
    
    def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)    
    
    def onEvent (self,event,st):
        htmltext = event
        if event == "prinesi.htm":
            st.set("cond","1")                        
            st.setState(STARTED)
            st.playSound("ItemSound.quest_accept")
        if event == "obi.htm":
            st.set("cond","2")
            st.takeItems(BANDAGE,20)
            st.takeItems(ENERGY_STONE,5)
            st.takeItems(THIEF_KEY,10)
            st.giveItems(FIGNA,1)
        elif event == "adena":
            st.takeItems(FIGNA,1)
            st.giveItems(ADENA,16500)
            st.unset("cond")
            htmltext = "nagrada.htm"
            st.unset("cond")
            st.setState(COMPLETED)          
        elif event == "ssd":
            st.takeItems(FIGNA,1)
            st.giveItems(SSD,1650)
            st.unset("cond")
            htmltext = "nagrada.htm"
            st.unset("cond")
            st.setState(COMPLETED)
        return htmltext


    def onTalk (self,npc,player):
        htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
        st = player.getQuestState(qn)
        if not st: return
        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        if npcId == FILAUR:
            id = st.getState()
            cond = st.getInt("cond")
            if id == CREATED:
                if player.getLevel() < 30:
                    htmltext = "lvl.htm"
                    st.exitQuest(1)
                else:
                    htmltext = "privetstvie.htm"
            elif id == STARTED and cond == 1:
                if st.getQuestItemsCount(BANDAGE) >=20 and st.getQuestItemsCount(ENERGY_STONE) >=5 and st.getQuestItemsCount(THIEF_KEY) >=10 :
                    htmltext = "prines.htm"
                else:
                    htmltext = "nehvataet.htm"
            elif cond == 2:
                htmltext = "obi.htm"    
        elif npcId == OBI and cond == 2 :
            htmltext = "privetstvieobi.htm"
        return htmltext
       
QUEST       = Quest(116, qn, "Beyond the Hills of Winter") 
CREATED     = State('Start',   QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)
   
QUEST.setInitialState(CREATED)
QUEST.addStartNpc(FILAUR)
QUEST.addTalkId(FILAUR)
QUEST.addTalkId(OBI)
