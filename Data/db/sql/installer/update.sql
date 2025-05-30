ALTER TABLE characters DROP banchat_time;

ALTER TABLE characters change in_jail punish_level TINYINT UNSIGNED NOT NULL DEFAULT 0;
ALTER TABLE characters change jail_timer punish_timer INT UNSIGNED NOT NULL DEFAULT 0

ALTER TABLE `character_skills_save` ADD `systime` bigint( 20 ) AFTER `reuse_delay`;