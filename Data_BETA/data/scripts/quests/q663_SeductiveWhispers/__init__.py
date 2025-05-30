# @author: RusTeam
import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q663_SeductiveWhispers"

WILBERT = 30846

DROP_CHANCE = 75

ADENA = 57

EWD = 955
EWC = 951
EWB = 947   
EAB = 948
EWA = 729
EAA = 730

WEAPONREC = range(5000,5008)
WEAPONKEY = range(4114,4121)

SPIRIT_BEAD = 8766

MONSTERS = [20996,20997,20998,20999,20678,20956,20955,20954,20959,20958,20957,20963,20962,20961,20960,21002,21006,21007,21008,21009,21010,20674,21001,20974,20975,20976]

class Quest (JQuest):
    
    def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)    
    
    def onEvent (self,event,st):
        htmltext = event
        if event == "accept" :
            st.set("cond","1")                        
            st.setState(STARTED)
            st.playSound("ItemSound.quest_accept")
            htmltext = "opisanie2.htm"
        if event == "quit":
            st.takeItems(SPIRIT_BEAD, -1)
            st.playSound("ItemSound.quest_finish")
            htmltext = "31388-10.htm"
            st.exitQuest(1)
        elif event == "test":
            if st.getInt("cond") == 1:
                if st.getQuestItemsCount(SPIRIT_BEAD) >=1:
                    st.takeItems(SPIRIT_BEAD,1)
                    htmltext = "igra-test.htm"
                else :
                    htmltext = "nehvataet.htm"  
        elif event == "play":
            if st.getInt("cond") == 1:
                if st.getQuestItemsCount(SPIRIT_BEAD) >=50:
                    st.takeItems(SPIRIT_BEAD,50)                
                    htmltext = "igra-0.htm"
                else :
                    htmltext = "nehvataet.htm"
        elif event == "roundone":
            if st.getRandom(100) < 80 :
                htmltext = "igra-1.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "roundtwo":
            if st.getRandom(100) < 70 : 
                htmltext = "igra-2.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "roundthree":
            if st.getRandom(100) < 60 : 
                htmltext = "igra-3.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "roundfour":
            if st.getRandom(100) < 50 : 
                htmltext = "igra-4.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "roundfive":
            if st.getRandom(100) < 40 : 
                htmltext = "igra-5.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "roundsix":
            if st.getRandom(100) < 30 : 
                htmltext = "igra-6.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "roundseven":
            if st.getRandom(100) < 20 : 
                htmltext = "igra-7.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "roundeight":
            if st.getRandom(100) < 10 : 
                htmltext = "igra-8.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testone":
            if st.getRandom(100) < 80 :
                htmltext = "test-1.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testtwo":
            if st.getRandom(100) < 70 : 
                htmltext = "test-2.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testthree":
            if st.getRandom(100) < 60 : 
                htmltext = "test-3.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testfour":
            if st.getRandom(100) < 50 : 
                htmltext = "test-4.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testfive":
            if st.getRandom(100) < 40 : 
                htmltext = "test-5.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testsix":
            if st.getRandom(100) < 30 : 
                htmltext = "test-6.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testseven":
            if st.getRandom(100) < 20 : 
                htmltext = "test-7.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "testeight":
            if st.getRandom(100) < 10 : 
                htmltext = "test-8.htm"
            else :
                htmltext = "neudacha.htm"
        elif event == "rewardone":
            st.giveItems(ADENA,40000)   
        elif event == "rewardtwo":
            st.giveItems(ADENA,80000)
        elif event == "rewardthree":
            st.giveItems(ADENA,110000)
            st.giveItems(EWD,1)
        elif event == "rewardfour":
            st.giveItems(ADENA,199000)
            st.giveItems(EWC,1)
        elif event == "rewardfive":
            st.giveItems(ADENA,388000)
            st.giveItems(WEAPONREC[st.getRandom(len(WEAPONREC))],1)
        elif event == "rewardsix":
            st.giveItems(ADENA,675000)
            st.giveItems(WEAPONKEY[st.getRandom(len(WEAPONKEY))],1)
        elif event == "rewardseven":
            st.giveItems(ADENA,1284000)
            st.giveItems(EWB,2)
            st.giveItems(EAB,2)
        elif event == "rewardeight":
            st.giveItems(ADENA,2384000)
            st.giveItems(EWA,2)
            st.giveItems(EAA,2)
        return htmltext


    def onTalk (self,npc,player):
        htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
        st = player.getQuestState(qn)
        if not st: return
        npcId = npc.getNpcId()
        if npcId == WILBERT:
            id = st.getState()
            cond = st.getInt("cond")
            if id == CREATED:
                if player.getLevel() >= 50:
                    htmltext = "31388-01.htm"
                else:
                    htmltext = "31388-03.htm"
                    st.exitQuest(1)
            elif cond == 1:
                htmltext = "31388-06.htm"
        return htmltext

    def onKill (self, npc, player,isPet):
        partyMember = self.getRandomPartyMember(player,"1")
        if not partyMember: return
        st = partyMember.getQuestState(qn)
        if st :
            if st.getState() == STARTED :
                npcId = npc.getNpcId()
                cond = st.getInt("cond")
                count = st.getQuestItemsCount(SPIRIT_BEAD)
                if cond == 1 :
                    chance = DROP_CHANCE*Config.RATE_DROP_QUEST
                    numItems, chance = divmod(chance,100)
                    if st.getRandom(100) < chance : 
                       numItems += 1
                    if numItems :
                       if count + numItems >= 1000000 :
                          st.playSound("ItemSound.quest_middle")
                       else :
                          st.playSound("ItemSound.quest_itemget")
                       st.giveItems(SPIRIT_BEAD,int(numItems))
        return

        
QUEST       = Quest(663, qn, "Seductive Whispers") 
CREATED     = State('Start',   QUEST)
STARTED     = State('Started', QUEST)

for npcId in MONSTERS:
    QUEST.addKillId(npcId)

    
QUEST.setInitialState(CREATED)
QUEST.addStartNpc(WILBERT)
QUEST.addTalkId(WILBERT)
STARTED.addQuestDrop(npcId,SPIRIT_BEAD,1)
