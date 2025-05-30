-- 
-- Table structure for table `noble_teleport`
-- 
DROP TABLE IF EXISTS noble_teleport;
CREATE TABLE noble_teleport (
  Description varchar(75) default NULL,
  id decimal(11,0) NOT NULL default '0',
  loc_x decimal(9,0) default NULL,
  loc_y decimal(9,0) default NULL,
  loc_z decimal(9,0) default NULL,
  price decimal(6,0) default NULL,
  fornoble int(1) NOT NULL default '0',
  PRIMARY KEY  (id)
) ENGINE=MyISAM;

-- 
-- Dumping data for table `noble_teleport`
-- 
-- Noblesse TP po gorodam
INSERT INTO noble_teleport VALUES
-- Noble Gate Pass
('Town of Gludio',77701,-12672,122776,-3116,1000,1),
('Dion',77702,15670,142983,-2705,1,1),
('Town of Giran',77703,83400,147943,-3404,1,1),
('Oren Town',77704,82956,53162,-1495,1,1),
('Hunter Village',77705,116819,76994,-2714,1,1),
('Aden Town',77706,146331,25762,-2018,1,1),
('Goddard',77707,147928,-55273,-2734,1,1),
('Rune',77708,43799,-47727,-798,1,1),
('Schuttgart',77709,87386,-143246,-1293,1,1),
('Heine',77710,111409,219364,-3545,1,1),
('Giran Harbor',77711,47942,186764,-3485,1,1),
('Orc village',77712,-44836,-112524,-235,1,1),
('Dwarven village',77713,115113,-178212,-901,1,1),
('TI',77714,-84318,244579,-3730,1,1),
('Elven Village',77715,46934,51467,-2977,1,1),
('Dark Elven Village',77716,9745,15606,-4574,1,1),
-- 1000 adena
('Town of Gludio',77717,-12672,122776,-3116,1000,0),
('Dion',77718,15670,142983,-2705,1000,0),
('Town of Giran',77719,83400,147943,-3404,1000,0),
('Oren Town',77720,82956,53162,-1495,1000,0),
('Hunter Village',77721,116819,76994,-2714,1000,0),
('Aden Town',77722,146331,25762,-2018,1000,0),
('Goddard',77723,147928,-55273,-2734,1000,0),
('Rune',77724,43799,-47727,-798,1000,0),
('Schuttgart',77725,87386,-143246,-1293,1000,0),
('Heine',77726,111409,219364,-3545,1000,0),
('Giran Harbor',77727,47942,186764,-3485,1000,0),
('Orc village',77728,-44836,-112524,-235,1000,0),
('Dwarven village',77729,115113,-178212,-901,1000,0),
('TI',77730,-84318,244579,-3730,1000,0),
('Elven Village',77731,46934,51467,-2977,1000,0),
('Dark Elven Village',77732,9745,15606,-4574,1000,0);

-- Noblesse TP Seven Signes
INSERT INTO noble_teleport VALUES
-- Noble Gate Pass
('Necropolis of Sacrifice',77733,-41127,205913,-3358,1,1),
('Heretics Catacomb',77734,39271,144261,-3653,1,1),
('Pilgrims Necropolis',77735,45583,126972,-3685,1,1),
('Catacomb of the Branded',77736,43131,170643,-3252,1,1),
('Worshipers Necropolis',77737,107506,174339,-3711,1,1),
('Catacomb of the Apostate',77738,73972,78721,-3423,1,1),
('Martyrs Necropolis',77739,114581,132478,-3102,1,1),
('Catacomb of the Witch',77740,136692,80000,-3701,1,1),
('Ascetics Necropolis',77741,-55355,78787,-3012,1,1),
('Disciples Necropolis',77742,168586,-17930,-3172,1,1),
('Saints Necropolis',77743,79376,208901,-3710,1,1),
('Catacomb of the Dark Omens',77744,-22571,13826,-3173,1,1),
('Catacomb of the Forbidden Path',77745,110849,84230,-4839,1,1),
-- 1000 adena
('Necropolis of Sacrifice',77746,-41127,205913,-3358,1000,0),
('Heretics Catacomb',77747,39271,144261,-3653,1000,0),
('Pilgrims Necropolis',77748,45583,126972,-3685,1000,0),
('Catacomb of the Branded',77749,43131,170643,-3252,1000,0),
('Worshipers Necropolis',77750,107506,174339,-3711,1000,0),
('Catacomb of the Apostate',77751,73972,78721,-3423,1000,0),
('Martyrs Necropolis',77752,114581,132478,-3102,1000,0),
('Catacomb of the Witch',77753,136692,80000,-3701,1000,0),
('Ascetics Necropolis',77754,-55355,78787,-3012,1000,0),
('Disciples Necropolis',77755,168586,-17930,-3172,1000,0),
('Saints Necropolis',77756,79376,208901,-3710,1000,0),
('Catacomb of the Dark Omens',77757,-22571,13826,-3173,1000,0),
('Catacomb of the Forbidden Path',77758,110849,84230,-4839,1000,0);

