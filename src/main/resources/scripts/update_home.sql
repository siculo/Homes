UPDATE `homes` 
    SET `x`=?, `y`=?, `z`=?, `yaw`=?, `pitch`=?, `world_id`=(SELECT `id` FROM `homes_worlds` WHERE `world_name`=? LIMIT 1)
    WHERE `uuid_id`=(SELECT `id` FROM `homes_uuids` WHERE `uuid`=? LIMIT 1) AND `home_name`=?;