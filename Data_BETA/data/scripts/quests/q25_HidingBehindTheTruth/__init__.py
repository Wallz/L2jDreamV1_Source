# ----------------------
#  Create by Skeleton!!!
# ----------------------
# fix by Bibigon for RT 04.12.07
# fixed by Pixar
import sys
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q25_HidingBehindTheTruth"

# ~~~~~ npcId list: ~~~~~
Benedict          = 31349
Agripel           = 31348
MysteriousWizard  = 31522
BrokenBookshelf   = 31534
MaidofLidia       = 31532
Tombstone         = 31531
Coffin            = 31536
# ~~~~~~~~~~~~~~~~~~~~~~~

# ~~~~~~ itemId list: ~~~~~~
GemstoneKey           = 7157
SuspiciousTotemDoll1  = 7151
SuspiciousTotemDoll2  = 7156
SuspiciousTotemDoll3  = 7158
MapForestofDeadman    = 7063
Contract              = 7066
LidiasDress           = 7155
EarringofBlessing     = 874
RingofBlessing        = 905
NecklaceofBlessing    = 936
# ~~~~~~~~~~~~~~~~~~~~~~~~~~

# ~~ Monster List: ~~
TriolsPawn    = 27218
# ~~~~~~~~~~~~~~~~~~~

class Quest (JQuest) :

    def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

    def onEvent (self,event,st):
        htmltext = event
        if event == "StartQuest":
            InhabitantsOfTheForestOfTheDead = st.getPlayer().getQuestState("q24_InhabitantsForestOfTheDead")
            if InhabitantsOfTheForestOfTheDead :
                if InhabitantsOfTheForestOfTheDead.get("onlyone") == "1":
                    if st.getQuestItemsCount(SuspiciousTotemDoll1) != 0:
                        htmltext = "31349-03.htm"
                    else:
                        htmltext = "31349-03a.htm"
                else:
                    htmltext = "31349-02.htm"
                    st.set("cond","1")
            else:
                htmltext = "31349-02.htm"
                st.set("cond","1")
        elif event == "31349-05.htm":
            st.set("cond","2")
            st.setState(STARTED)
        elif event == "31349-10.htm":
            st.set("cond","4")
            st.setState(STARTED)
        elif event == "31348-08.htm":
            if st.getInt("cond") == 4:
                st.set("cond","5")
                if st.getQuestItemsCount(SuspiciousTotemDoll1) != 0:
                    st.takeItems(SuspiciousTotemDoll1,-1)
                if st.getQuestItemsCount(SuspiciousTotemDoll2) != 0:
                    st.takeItems(SuspiciousTotemDoll2,-1)
                if st.getQuestItemsCount(GemstoneKey) == 0:
                    st.giveItems(GemstoneKey,1)
            elif st.getInt("cond") == 5:
                htmltext = "31348-08a.htm"
        elif event == "31522-04.htm":
            st.set("cond","6")
            if st.getQuestItemsCount(MapForestofDeadman) == 0:
                st.giveItems(MapForestofDeadman,1)
        elif event == "31534-07.htm":
            st.set("cond","7")
            st.addSpawn(TriolsPawn)
        elif event == "31534-11.htm":
            st.set("id","8")
            st.giveItems(Contract,1)
            st.takeItems(GemstoneKey,-1)
        elif event == "31532-07.htm":
            st.set("cond","11")
        elif event == "31532-02.htm":
            st.takeItems(Contract,-1)
        elif event == "31531-02.htm":
            st.set("cond","12")
            st.addSpawn(31536,60081,-35921,-697,120000)
            st.startQuestTimer("Coffin_Spawn",120000)
        elif event == "Coffin_Spawn":
            htmltext = " "
            if st.getInt("cond") == 12:
               st.set("cond","11")
        elif event == "Lidia_wait":
            st.set("id","14")
            htmltext = " "
        elif event == "31532-21.htm":
            st.set("cond","15")
        elif event == "31522-13.htm":
            st.set("cond","16")
        elif event == "31348-10.htm":
            st.takeItems(SuspiciousTotemDoll3,-1)
        elif event == "31348-16.htm":
            st.set("cond","17")
        elif event == "31348-17.htm":
            st.set("cond","18")
        elif event == "31348-14.htm":
            st.set("id","16")
        elif event == "End1":
            if st.getInt("cond") == 17:
                st.unset("id")
                st.set("onlyone","1")
                st.unset("cond")
                st.setState(COMPLETED)
                st.giveItems(RingofBlessing,2)
                st.giveItems(EarringofBlessing,1)
                st.takeItems(MapForestofDeadman,-1)
                st.addExpAndSp(1607062,0)
                htmltext = "31532-25.htm"
            else:
                htmltext = "31532-24.htm"
        elif event == "End2":
            if st.getInt("cond") == 18:
                st.unset("id")
                st.set("onlyone","1")
                st.unset("cond")
                st.setState(COMPLETED)
                st.giveItems(EarringofBlessing,1)
                st.giveItems(NecklaceofBlessing,1)
                st.takeItems(MapForestofDeadman,-1)
                st.addExpAndSp(1607062,0)
                htmltext = "31522-16.htm"
            else:
                htmltext = "31522-15a.htm"
        return htmltext

    def onTalk (self,npc,player):
        htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
        st = player.getQuestState(qn)
        if not st : return htmltext
        
        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        onlyone = st.getInt("onlyone")
        IntId = st.getInt("id")
        if npcId == Benedict:
            if cond == 0 and onlyone == 0:
                htmltext = "31349-01.htm"
            elif cond == 1 and onlyone == 0:
                htmltext = "31349-01.htm"
            elif cond == 2 and onlyone == 0:
                htmltext = "31349-03a.htm"
            elif cond == 3 and onlyone == 0:
                htmltext = "31349-03.htm"
            elif cond == 4 and onlyone == 0:
                htmltext = "31349-11.htm"
        elif npcId == MysteriousWizard:
            if cond == 2 and onlyone == 0:
                htmltext = "31522-01.htm"
                st.set("cond","3")
                st.giveItems(SuspiciousTotemDoll2,1)
            elif cond == 3 and onlyone == 0:
                htmltext = "31522-02.htm"
            elif cond == 5 and onlyone == 0:
                htmltext = "31522-03.htm"
            elif cond == 6 and onlyone == 0:
                htmltext = "31522-05.htm"
            elif cond == 8 and onlyone == 0:
                if IntId == 8:
                    htmltext = "31522-06.htm"
                    st.set("cond","9")
                else:
                    htmltext = "31522-05.htm"
            elif cond == 15 and onlyone == 0:
                htmltext = "31522-06a.htm"
            elif cond == 16 and onlyone == 0:
                htmltext = "31522-12.htm"
            elif cond == 18 and onlyone == 0:
                htmltext = "31522-15.htm"
                st.set("id","17")
        elif npcId == Agripel:
            if cond == 4 and onlyone == 0:
                htmltext = "31348-01.htm"
            elif cond == 5 and onlyone == 0:
                htmltext = "31348-03.htm"
            elif cond == 16 and onlyone == 0:
                if IntId != 16:
                    htmltext = "31348-09.htm"
                else:
                    htmltext = "31348-15.htm"
            elif cond == 17 and onlyone == 0:
                htmltext = "31348-16.htm"
            elif cond == 18 and onlyone == 0:
                htmltext = "31348-17.htm"
        elif npcId == BrokenBookshelf:
            if cond == 6 and onlyone == 0:
                htmltext = "31534-01.htm"
            elif cond == 7 and onlyone == 0:
                htmltext = "31534-08.htm"
            elif cond == 8 and onlyone == 0:
                if IntId != 8:
                    htmltext = "31534-10.htm"
                else:
                    htmltext = "31534-06.htm"
        elif npcId == MaidofLidia:
            if cond == 9 and onlyone == 0:
                if st.getQuestItemsCount(Contract) != 0:
                    htmltext = "31532-01.htm"
                else:
                    htmltext = "You have no Contract..."
            elif cond in range(11,12) and onlyone == 0:
                htmltext = "31532-08.htm"
            elif cond == 13 and onlyone == 0:
                if st.getQuestItemsCount(LidiasDress) != 0:
                    htmltext = "31532-09.htm"
                    st.set("cond","14")
                    st.startQuestTimer("Lidia_wait",60000)
                    st.takeItems(LidiasDress,-1)
                else:
                    htmltext = "31532-08.htm"
            elif cond == 14 and onlyone == 0:
                if IntId == 14:
                    htmltext = "31532-10.htm"
                else:
                    htmltext = "31532-09.htm"
            elif cond == 17 and onlyone == 0:
                htmltext = "31532-23.htm"
                st.set("id","17")
        elif npcId == Tombstone:
            if cond == 11 and onlyone == 0:
                htmltext = "31531-01.htm"
            elif cond == 12 and onlyone == 0:
                htmltext = "31531-02.htm"
            elif cond == 13 and onlyone == 0:
                htmltext = "31531-03.htm"
        elif npcId == Coffin:
            if cond == 12 and onlyone == 0:
                htmltext = "31536-01.htm"
                st.set("cond","13")
                st.giveItems(LidiasDress,1)
            elif cond == 13 and onlyone == 0:
                htmltext = "31531-03.htm"
        return htmltext

    def onKill (self,npc,player,isPet):
        st = player.getQuestState(qn)
        if not st : return 
        if st.getState() != STARTED : return

        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        if npcId == TriolsPawn:
            if cond == 7:
                st.set("cond","8")
                st.giveItems(SuspiciousTotemDoll3,1)
        return

QUEST     = Quest(25,qn,"Hiding Behind The Truth")
CREATED   = State('Start',     QUEST)
STARTED   = State('Started',   QUEST)
COMPLETED = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(Benedict)

QUEST.addTalkId(Benedict)
QUEST.addTalkId(Agripel)
QUEST.addTalkId(MysteriousWizard)
QUEST.addTalkId(BrokenBookshelf)
QUEST.addTalkId(MaidofLidia)
QUEST.addTalkId(Tombstone)
QUEST.addTalkId(Coffin)

QUEST.addKillId(TriolsPawn)

STARTED.addQuestDrop(TriolsPawn,SuspiciousTotemDoll3,1)