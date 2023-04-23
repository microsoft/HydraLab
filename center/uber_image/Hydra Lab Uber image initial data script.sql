-- 1. Init uber image DB
INSERT INTO sys_user
(user_id, default_team_id, default_team_name, mail_address, role_id, role_name, user_name)
VALUES('4cf741a3-ea49-41f6-b8e2-b5d1002dc070', '827e2a89-ef11-4234-a37f-61cd6ba80d0f', 'Default', 'default@hydralab.com', '716f6463-5ef0-485a-abc2-a51820d10c83', 'USER', 'default@hydralab.com');

INSERT INTO sys_team
(team_id, create_time, team_name, update_time)
VALUES('827e2a89-ef11-4234-a37f-61cd6ba80d0f', '1672385485681', 'Default', '1672385485681');

INSERT INTO user_team_relation
(mail_address, team_id, is_team_admin)
VALUES('default@hydralab.com', '827e2a89-ef11-4234-a37f-61cd6ba80d0f', 0);

INSERT INTO sys_role 
(role_id,auth_level,create_time,role_name,update_time) 
VALUES ('716f6463-5ef0-485a-abc2-a51820d10c83',100,'1672385485586','USER','1672385485586');

INSERT INTO agent_user
(id, capabilities, hostname, ip, mail_address, name, os, "role", secret, status, team_id, team_name, version_code, version_name)
VALUES('d3111900-22ac-4e80-bb39-b6120aa23738', 0, NULL, NULL, 'default@hydralab.com', 'TestAgent', 'Linux', NULL, '2a10lJ0ujxo8SOO8vnq56vyp8uoHy1GUGrIb8kGdcvp8pHks1wwwBDke', 0, '827e2a89-ef11-4234-a37f-61cd6ba80d0f', 'Default', NULL, NULL);


-- 2. Add package/add related DB record
INSERT INTO blob_file_info
(file_id, cdnurl, blob_container, blob_path, blob_url, create_time, file_len, file_name, file_parser, file_type, load_dir, load_type, md5, update_time)
VALUES('0fc14a87-a3c6-40cb-9a56-59b25f57f8f5', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_release_2378_.apk', 'pkgstore', '2023/04/23/record_release_2378_.apk', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_release_2378_.apk', '1682229902467', 261732, 'record_release_2378_.apk', '{"minSdkVersion":"21","appName":"HydraLab Client","pkgName":"com.microsoft.hydralab.android.client","targetSdkVersion":"28","version":"2.6.0"}', 'APP', NULL, NULL, '618aa2ac2817f2c9404d17b1e4d14c1b', '1682229902467');
INSERT INTO blob_file_info
(file_id, cdnurl, blob_container, blob_path, blob_url, create_time, file_len, file_name, file_parser, file_type, load_dir, load_type, md5, update_time)
VALUES('a7e7c9b5-41a9-4af7-b74c-f7b29318dfa4', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_androidTest_3000_.apk', 'pkgstore', '2023/04/23/record_androidTest_3000_.apk', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_androidTest_3000_.apk', '1682229903023', 527485, 'record_androidTest_3000_.apk', '{"minSdkVersion":"21","pkgName":"com.microsoft.hydralab.android.client.test","targetSdkVersion":"33"}', 'TEST_APP', NULL, NULL, '9efdd140688be18e580941cba768bb6f', '1682229903023');

INSERT INTO test_file_set
(id, app_name, build_type, commit_count, commit_id, commit_message, ingest_time, package_name, running_type, team_id, team_name, version)
VALUES('b64bd3a1-abea-4615-a736-c0a4ae28349f', 'HydraLab Client', 'debug', '-1', NULL, 'null', '1682229902376', 'com.microsoft.hydralab.android.client', NULL, '827e2a89-ef11-4234-a37f-61cd6ba80d0f', 'Default', '2.6.0');

INSERT INTO entity_file_relation
(entity_id, entity_type, file_id, file_order)
VALUES('b64bd3a1-abea-4615-a736-c0a4ae28349f', 'FILE_SET', '0fc14a87-a3c6-40cb-9a56-59b25f57f8f5', 1);
INSERT INTO entity_file_relation
(entity_id, entity_type, file_id, file_order)
VALUES('b64bd3a1-abea-4615-a736-c0a4ae28349f', 'FILE_SET', 'a7e7c9b5-41a9-4af7-b74c-f7b29318dfa4', 2);
