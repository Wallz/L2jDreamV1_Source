import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest

qn = "q111_ElrokianHuntersProof"

#NPCs
MARQUEZ = 32113
MUSHIKA = 32114
ASAMAH = 32115
KIRIKACHIN = 32116

#MOBs
VELOCIRAPTOR = range(22196,22198)+[22118,22223]
ORNITHOMIMUS = range(22200,22202)+[22219,22224]
DEINONYCHUS = range(22203,22205)+[22220,22225]
PACHYCAPHALOSAURUS = range(22208,22210)+[22221,22226]

#Items
DROP_CHANCE = 100
DIARY_FRAGMENT = 8768
ORNITHOMIMUS_CLAW = 8770
DEINONYCHUS_BONE = 8771
PACHYCAPHALOSAURUS_SKIN = 8772

#nagrada
ADENA = 57
TRAP_STONE = 8764
PRACTICE_ELROKIAN_TRAP = 8773
ELROKIAN_TRAP = 8763

class Quest (JQuest) :
 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "ok.htm" :
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
       st.set("cond","1")
    elif event == "nauchi.htm" :
       st.set("cond","2")
    elif event == "nauchiasamah.htm" :
       st.set("cond","3")
    elif event == "diarysoberi.htm" :
       st.set("cond","4")
    elif event == "kirikachin.htm" :
       st.takeItems(DIARY_FRAGMENT,-1)
       st.set("cond","6")
    elif event == "bloknotprines.htm" :
       st.set("cond","7")
    elif event == "pesnyaok.htm" :
       st.set("cond","8")    
    elif event == "pesnyapoem.htm" :
       st.set("cond","9")
    elif event == "lovdino.htm" :
       st.set("cond","10")
    elif event == "lovuwka.htm" :
       st.set("cond","12")
    elif event == "lovuwkaok.htm" :
       st.playSound("ItemSound.quest_finish")
       st.giveItems(ADENA,1022636)
       st.giveItems(ELROKIAN_TRAP,1)
       st.giveItems(TRAP_STONE,100)
       st.unset("cond")
       st.setState(COMPLETED)   
    return htmltext

 def onTalk (self,npc,player):
    st = player.getQuestState(qn)
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>" 
    if not st: return htmltext
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    diaryfragment = st.getQuestItemsCount(DIARY_FRAGMENT)
    if npcId == MARQUEZ :
       if id == CREATED :
          if player.getLevel() >= 75 :
             htmltext = "privetstvie.htm"
          else :
             htmltext = "lvl.htm"
             st.exitQuest(1)
       elif cond == 1:
          htmltext = "idikmushika.htm"
       elif cond == 3:
          htmltext = "druzba.htm"
       elif cond == 5:
          htmltext = "bloknot.htm"
    elif npcId == MUSHIKA :
       if cond == 1:
          htmltext = "privmushika.htm"
    elif npcId == ASAMAH :
       if cond == 2:
          htmltext = "privasamah.htm"
       elif cond == 8:
          htmltext = "pesnyaokkk.htm"
       elif cond == 9 :
          htmltext = "drug.htm"
       elif cond == 10 :
          htmltext = "matersoberi.htm"
       elif cond == 11 :
          htmltext = "materprines.htm"
          st.takeItems(ORNITHOMIMUS_CLAW,-1)
          st.takeItems(DEINONYCHUS_BONE,-1)
          st.takeItems(PACHYCAPHALOSAURUS_SKIN,-1)
          st.giveItems(PRACTICE_ELROKIAN_TRAP,3)
    elif npcId == KIRIKACHIN :
       if cond == 6:
          htmltext = "privkirikachin.htm"
       elif cond == 7:
          htmltext = "pesnya.htm"
       elif cond == 12:
          htmltext = "lovuwkaa.htm"
    return htmltext

 def onKill (self, npc, player,isPet):
    st = player.getQuestState(qn)
    if not st : return
    cond = st.getInt("cond")
    npcId = npc.getNpcId()
    if cond == 4 and npcId in VELOCIRAPTOR:
       chance = DROP_CHANCE*Config.RATE_DROP_QUEST
       random = st.getRandom(100)
       diaryfragment = st.getQuestItemsCount(DIARY_FRAGMENT)
       if diaryfragment == 50:
          st.playSound("ItemSound.quest_middle")
          st.set("cond","5")
       else:
          if random <= chance:
             st.playSound("ItemSound.quest_itemget")
             st.giveItems(DIARY_FRAGMENT,1)
    if cond == 10:
       chance = DROP_CHANCE*Config.RATE_DROP_QUEST
       random = st.getRandom(100)
       ornyclaw = st.getQuestItemsCount(ORNITHOMIMUS_CLAW)
       deinobone = st.getQuestItemsCount(DEINONYCHUS_BONE)
       pachyskin = st.getQuestItemsCount(PACHYCAPHALOSAURUS_SKIN)
       if ornyclaw == deinobone == pachyskin == 10:
          st.playSound("ItemSound.quest_middle")
          st.set("cond","11")
       elif npcId in ORNITHOMIMUS :
          if ornyclaw < 10:
             if random <= chance:
                st.giveItems(ORNITHOMIMUS_CLAW,1)
                st.playSound("ItemSound.quest_itemget")
       elif npcId in DEINONYCHUS :
          if deinobone < 10:
             if random <= chance:
                st.giveItems(DEINONYCHUS_BONE,1)
                st.playSound("ItemSound.quest_itemget")
       elif npcId in PACHYCAPHALOSAURUS :
          if pachyskin < 10:
             if random <= chance:
                st.giveItems(PACHYCAPHALOSAURUS_SKIN,1)
                st.playSound("ItemSound.quest_itemget")
    return

QUEST = Quest(111,qn,"Elrokian Hunter's Proof")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(MARQUEZ)
QUEST.addTalkId(MARQUEZ)
QUEST.addTalkId(MUSHIKA)
QUEST.addTalkId(ASAMAH)
QUEST.addTalkId(KIRIKACHIN)

for npcId in VELOCIRAPTOR:
   QUEST.addKillId(npcId)
   
for npcId in ORNITHOMIMUS:
   QUEST.addKillId(npcId)
   
for npcId in DEINONYCHUS:
   QUEST.addKillId(npcId)
   
for npcId in PACHYCAPHALOSAURUS:
   QUEST.addKillId(npcId)
