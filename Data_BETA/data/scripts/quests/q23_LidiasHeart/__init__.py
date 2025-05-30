# ~~~~ Made by ValianT ~~~~ #
# ~~~~~~~~~~~~~~~~~~~~~~~~~ #
# Modifed Bibigon for RT
# fixed by Pixar

import sys
from com.src.gameserver.model.quest        import State
from com.src.gameserver.model.quest        import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q23_LidiasHeart"

# ~~~~~~ npcId list: ~~~~~~
Innocentin          = 31328
BrokenBookshelf     = 31526
GhostofvonHellmann  = 31524
Tombstone           = 31523
Violet              = 31386
Box                 = 31530
# ~~~~~~~~~~~~~~~~~~~~~~~~~

# ~~~~~ itemId List ~~~~~
MapForestofDeadman = 7063
SilverKey          = 7149
LidiaHairPin       = 7148
LidiaDiary         = 7064
SilverSpear        = 7150
Adena              = 57
# ~~~~~~~~~~~~~~~~~~~~~~~

class Quest (JQuest) :

    def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

    def onEvent (self,event,st):
        htmltext = event
        if event == "31328-02.htm":
            st.giveItems(MapForestofDeadman,1)
            st.giveItems(SilverKey,1)
            st.set("cond","1")
            st.setState(STARTED)
        elif event == "31328-03.htm":
            st.set("cond","2")
        elif event == "31526-01.htm":
            st.set("cond","3")
        elif event == "31526-05.htm":
            st.giveItems(LidiaHairPin,1)
            if st.getQuestItemsCount(LidiaDiary) != 0:
                st.set("cond","4")
        elif event == "31526-11.htm":
            st.giveItems(LidiaDiary,1)
            if st.getQuestItemsCount(LidiaHairPin) != 0:
                st.set("cond","4")
        elif event == "31328-19.htm":
            st.set("cond","6")
        elif event == "31524-04.htm":
            st.set("cond","7")
            st.takeItems(LidiaDiary,-1)
        elif event == "31523-02.htm":
            st.addSpawn(GhostofvonHellmann,120000)
        elif event == "31523-05.htm":
            st.startQuestTimer("viwer_timer",10000)
        elif event == "viwer_timer":
            st.set("cond","8")
            htmltext = "31523-06.htm"
        elif event == "31530-02.htm":
            st.set("cond","10")
            st.takeItems(SilverKey,-1)
            st.giveItems(SilverSpear,1)
        elif event == "i7064-02.htm":
            htmltext = "i7064-02.htm"
        elif event == "31526-13.htm":
            st.startQuestTimer("read_book",120000)
        elif event == "read_book":
            htmltext = "i7064.htm"
        return htmltext

    def onTalk (self, npc, player) :
        st = player.getQuestState(qn)
        htmltext = "<html><body>I have nothing to say you...</body></html>"
        if not st : return htmltext
        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        onlyone = st.getInt("onlyone")
        if npcId == Innocentin:
            if cond == 0 and onlyone == 0:
                TragedyInVonHellmannForest=st.getPlayer().getQuestState("q22_TragedyInVonHellmannForest")
                if TragedyInVonHellmannForest :
                    htmltext = "31328-01.htm"
                else:
                    htmltext = "31328-00.htm"
            elif cond == 1 and onlyone == 0:
                htmltext = "31328-03.htm"
            elif cond == 2 and onlyone == 0:
                htmltext = "31328-07.htm"
            elif cond == 4 and onlyone == 0:
                htmltext = "31328-08.htm"
            elif cond == 6 and onlyone == 0:
                htmltext = "31328-19.htm"
        elif npcId == BrokenBookshelf:
            if cond == 2 and onlyone == 0:
                if st.getQuestItemsCount(SilverKey) != 0:
                    htmltext = "31526-00.htm"
            elif cond == 3 and onlyone == 0:
                if st.getQuestItemsCount(LidiaHairPin) == 0 and st.getQuestItemsCount(LidiaDiary) != 0:
                    htmltext = "31526-12.htm"
                elif st.getQuestItemsCount(LidiaHairPin) != 0 and st.getQuestItemsCount(LidiaDiary) == 0:
                    htmltext = "31526-06.htm"
                elif st.getQuestItemsCount(LidiaHairPin) == 0 and st.getQuestItemsCount(LidiaDiary) == 0:
                    htmltext = "31526-02.htm"
            elif cond == 4 and onlyone == 0:
                htmltext = "31526-13.htm"
        elif npcId == GhostofvonHellmann:
            if cond == 6 and onlyone == 0:
                htmltext = "31524-01.htm"
            elif cond == 7 and onlyone == 0:
                htmltext = "31524-05.htm"
        elif npcId == Tombstone:
            if cond == 6 and onlyone == 0:
                if st.getQuestTimer("spawn_timer") != None:
                    htmltext = "31523-03.htm"
                else:
                    htmltext = "31523-01.htm"
            if cond == 7 and onlyone == 0:
                htmltext = "31523-04.htm"
            elif cond == 8 and onlyone == 0:
                htmltext = "31523-06.htm"
        elif npcId == Violet:
            if cond == 8 and onlyone == 0:
                htmltext = "31386-01.htm"
                st.set("cond","9")
            elif cond == 9 and onlyone == 0:
                htmltext = "31386-02.htm"
            elif cond == 10 and onlyone == 0:
                if st.getQuestItemsCount(SilverSpear) != 0:
                    htmltext = "31386-03.htm"
                    st.takeItems(SilverSpear,-1)
                    st.giveItems(Adena,100000)
                    st.unset("cond")
                    st.set("onlyone","1")
                    st.setState(COMPLETED)
                else:
                    htmltext = "You have no Silver Spear..."
        elif npcId == Box:
            if cond == 9 and onlyone == 0:
                if st.getQuestItemsCount(SilverKey) != 0:
                    htmltext = "31530-01.htm"
                else:
                    htmltext = "You have no key..."
            elif cond == 10 and onlyone == 0:
                htmltext = "31386-03.htm"
        return htmltext

QUEST     = Quest(23,qn,"Lidia's Heart")
CREATED   = State('Start',     QUEST)
STARTING  = State('Starting',  QUEST)
STARTED   = State('Started',   QUEST)
COMPLETED = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(Innocentin)

QUEST.addTalkId(Innocentin)
QUEST.addTalkId(BrokenBookshelf)
QUEST.addTalkId(GhostofvonHellmann)
QUEST.addTalkId(Tombstone)
QUEST.addTalkId(Violet)
QUEST.addTalkId(Box)
