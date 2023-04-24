-- 1. Init uber image DB
INSERT INTO sys_team
(team_id, create_time, team_name, update_time)
VALUES('default_team_id', '1672385485681', 'Default', '1672385485681');

INSERT INTO sys_user
(user_id, default_team_id, default_team_name, mail_address, role_id, role_name, user_name)
VALUES('user_id', 'default_team_id', 'Default', 'default@hydralab.com', 'user_role_id', 'USER', 'default@hydralab.com');

INSERT INTO user_team_relation
(mail_address, team_id, is_team_admin)
VALUES('default@hydralab.com', 'default_team_id', 0);

INSERT INTO sys_role 
(role_id,auth_level,create_time,role_name,update_time) 
VALUES ('user_role_id',100,'1672385485586','USER','1672385485586');

INSERT INTO agent_user
(id, capabilities, hostname, ip, mail_address, name, os, "role", secret, status, team_id, team_name, version_code, version_name)
VALUES('default_agent_id', 0, NULL, NULL, 'default@hydralab.com', 'TestAgent', 'Linux', NULL, 'default_agent_secret', 0, 'default_team_id', 'Default', NULL, NULL);


-- 2. Add package/add related DB record
INSERT INTO blob_file_info
(file_id, cdnurl, blob_container, blob_path, blob_url, create_time, file_len, file_name, file_parser, file_type, load_dir, load_type, md5, update_time)
VALUES('app_file_id', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_release.apk', 'pkgstore', '2023/04/23/record_release.apk', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_release.apk', '1682229902467', 261732, 'record_release.apk', '{"minSdkVersion":"21","appName":"HydraLab Client","pkgName":"com.microsoft.hydralab.android.client","targetSdkVersion":"28","version":"2.6.0"}', 'APP', NULL, NULL, '618aa2ac2817f2c9404d17b1e4d14c1b', '1682229902467');
INSERT INTO blob_file_info
(file_id, cdnurl, blob_container, blob_path, blob_url, create_time, file_len, file_name, file_parser, file_type, load_dir, load_type, md5, update_time)
VALUES('test_app_file_id', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_androidTest.apk', 'pkgstore', '2023/04/23/record_androidTest.apk', 'http://127.0.0.1:9886/api/storage/local/download/pkgstore/2023/04/23/record_androidTest.apk', '1682229903023', 527485, 'record_androidTest.apk', '{"minSdkVersion":"21","pkgName":"com.microsoft.hydralab.android.client.test","targetSdkVersion":"33"}', 'TEST_APP', NULL, NULL, '9efdd140688be18e580941cba768bb6f', '1682229903023');

INSERT INTO test_file_set
(id, app_name, build_type, commit_count, commit_id, commit_message, ingest_time, package_name, running_type, team_id, team_name, version)
VALUES('file_set_id', 'HydraLab Client', 'debug', '-1', NULL, 'null', '1682229902376', 'com.microsoft.hydralab.android.client', NULL, 'default_team_id', 'Default', '2.6.0');

INSERT INTO entity_file_relation
(entity_id, entity_type, file_id, file_order)
VALUES('file_set_id', 'FILE_SET', 'app_file_id', 1);
INSERT INTO entity_file_relation
(entity_id, entity_type, file_id, file_order)
VALUES('file_set_id', 'FILE_SET', 'test_app_file_id', 2);
