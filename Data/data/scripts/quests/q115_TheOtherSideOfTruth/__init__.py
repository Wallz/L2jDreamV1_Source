import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q115_TheOtherSideOfTruth"

RAFFORTY = 32020
MISSA = 32018
ICE_SCULPTURE = 32021
KIERRE = 32022

MISAS_LETTER = 8079
RAFFORTYS_LETTER = 8080
PIECE_OF_TABLET = 8081
REPORT_PIECE = 8082 
ADENA = 57

class Quest (JQuest):
    
    def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)    
    
    def onEvent (self,event,st):
        htmltext = event
        cond=st.getInt("cond") 
        if event == "31521-1.htm":
            st.set("cond","1")
            st.set("dostup","0")
            st.setState(STARTED)
            st.playSound("ItemSound.quest_accept")
        elif event == "31547-1.htm" :
            st.giveItems(MISAS_LETTER,1)
            st.set("cond","2")
        elif event == "nechital.htm" :
            st.takeItems(MISAS_LETTER,1)
            st.set("cond","3")
        elif event == "condfour.htm" :
            st.set("cond","4")
        elif event == "chitai" :
            st.set("cond","5")
            #
        elif event == "31545-1.htm" :
            st.giveItems(RAFFORTYS_LETTER,1)
            st.set("cond","6")           
        elif event == "31544-1.htm" :
            st.takeItems(RAFFORTYS_LETTER,1) 
            st.set("cond","7")
        elif event == "31543-1.htm" :
            st.set("cond","8")
        elif event == "31542-1.htm" :
            st.giveItems(REPORT_PIECE,1)
            st.set("cond","9")
        elif event == "31521-3.htm" :
            st.takeItems(REPORT_PIECE,1)
            st.set("cond","10")
        elif event == "tablfragok.htm" :
            st.set("cond","11")
        elif event == "tfumenja.htm" :
            st.giveItems(PIECE_OF_TABLET,1)
            st.set("cond","12")
        elif event == "theend.htm" :
            st.takeItems(PIECE_OF_TABLET,1)
            st.giveItems(ADENA,60040)
            st.setState(COMPLETED)
            st.set("dostup","1")
            st.unset("cond")
            st.playSound("ItemSound.quest_finish")    
        return htmltext


    def onTalk (self,npc,player):
        htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
        st = player.getQuestState(qn)
        if not st: return
        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        if npcId == RAFFORTY:
            id = st.getState()
            cond = st.getInt("cond")
            if id == CREATED:
                if player.getLevel() < 53:
                    htmltext = "lvl.htm"
                    st.exitQuest(1)
                else:
                    htmltext = "31521-0.htm"
            elif cond == 1 :
                htmltext = "kmisse.htm"
            elif cond == 2 :
                htmltext = "31546-0.htm"
            elif cond == 3 :
                htmltext = "nechital.htm"
            elif cond == 4 :
                htmltext = "condfour.htm"
            elif cond == 5 :
                htmltext = "condfive.htm"
            elif cond == 6 :
                htmltext = "kmisse.htm"
            elif cond == 9 :
                htmltext = "31521-2.htm"
            elif cond == 10 :
                htmltext = "tablfrag.htm"
            elif cond == 11 :
                htmltext = "tablfragokok.htm"
            elif cond == 12 :
                htmltext = "tfprines.htm"
        if npcId == MISSA:
            if cond == 1 :
                htmltext = "31547-0.htm"
            elif cond == 2 :
                htmltext = "pismootmissi.htm"
            elif cond == 6 :
                htmltext = "31544-0.htm"
            elif cond == 7 :
                htmltext = "gosculptur.htm"
        if npcId == ICE_SCULPTURE:
            if cond == 7 :
                htmltext = "31543-0.htm"
            elif cond == 11 :
                htmltext = "tfnawel.htm"
        elif npcId == KIERRE:
            htmltext = "31542-0.htm"                
        return htmltext
       
QUEST       = Quest(115, qn, "The Other Side Of Truth") 
CREATED     = State('Start',   QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)
   
QUEST.setInitialState(CREATED)
QUEST.addStartNpc(32020)
QUEST.addTalkId(32020)
QUEST.addTalkId(32018)
QUEST.addTalkId(32021)
QUEST.addTalkId(32022)
