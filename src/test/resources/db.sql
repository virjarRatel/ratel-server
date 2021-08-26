use ratel_server;
drop table if exists ratel_user;
drop table if exists ratel_certificate;
drop table if exists ratel_apk;
drop table if exists ratel_user_apk;
drop table if exists ratel_task;
drop table if exists ratel_engine_bin;
drop table if exists ratel_manager_apk;
drop table if exists ratel_hot_module;
create table ratel_user
(
  id          bigint(11) primary key auto_increment comment '自增主建',
  account     varchar(50) not null comment '账号',
  pwd         varchar(50) not null comment '密码',
  user_level  tinyint      default 0 comment '用户等级',
  balance     bigint(11)   default 0 comment '余额',
  login_token varchar(255) default null comment '用户登陆的token，api访问的凭证',
  last_active datetime     default null comment '用户最后活跃时间',
  is_admin    bool         default false comment '是否是管理员',
  nick_name   varchar(50) comment '用户昵称'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment '用户';

create table ratel_certificate
(
  id                   bigint(11) primary key auto_increment comment '自增主建',
  user_id              bigint(11)   not null comment '所属用户',
  content              text         not null comment '证书内容',
  licence_id           varchar(255) not null comment '证书id，来自content解码',
  licence_version_code int default 0 comment '证书版本号，来自content解码'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment '授权证书';

create table ratel_apk
(
  id               bigint(11) primary key auto_increment comment '自增主建',
  file_name        varchar(255) not null default 'unknown.apk' comment '文件名称，一般为用户上传的时候设定的',
  file_hash        varchar(255) comment '文件hash，用于唯一定位apk内容',
  app_package      varchar(255) not null comment 'apk的package',
  app_name         varchar(255) comment 'apk中提取的name label',
  app_version      varchar(32) comment 'apk中提取的版本号',
  app_version_code bigint(11) comment 'apk中提取的版本号的数字号码',
  oss_url          varchar(255) comment '本apk需要上传到oss，用以实现和服务器环境无关的序列化',
  upload_time      datetime comment '上传时间',
  last_used_time   datetime comment '最后访问时间',
  is_xposed_module bool                  default false comment '当前apk是否为xposed模块，在前端可以通过这个字段对apk分组'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment '上传的apk文件';

create table ratel_user_apk
(
  id            bigint(11) primary key auto_increment comment '自增主建',
  user_id       bigint(11)   not null comment '所属用户',
  apk_id        bigint(11)   not null comment 'apk文件id',
  apk_file_name varchar(255) not null comment '上传文件名称',
  alias         varchar(255) comment '别名'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment '某个用户下面的apk，因为apk可以被不同用户重复上传，所以用户见到的是一个文件映射';

create table ratel_task
(
  id                   bigint(11) primary key auto_increment comment '自增主建',
  user_id              bigint(11) not null comment '所属用户',
  origin_apk_id        bigint(11) not null comment '原始apk',
  xposed_module_apk_id bigint(11) comment 'xposed模块apk，非embed模式下，这个可以为空',
  add_debug_flag       bool        default false comment '是否在apk文件中增加debug的开关（不是所有引擎都支持，而且暂时都不支持）',
  ratel_engine         varchar(10) default null comment '使用的引擎，选择appendDex|rebuildDex|shell三个之一,默认使用rebuildDex',
  certificate_id       bigint(11) not null comment '使用的授权证书，如果没有证书，那么无法构建apk',
  log_oss_url          varchar(255) comment '任务构建日志将会上传到oss',
  output_oss_url       varchar(255) comment 'task输出产物是一个apk，将会上传到oss',
  add_time             datetime comment '任务创建时间',
  finish_time          datetime comment '任务完成时间',
  task_status          tinyint comment '当前任务状态',
  consumer             varchar(64) comment '任务消费主机，在多机部署的时候，考虑任务被不同机器消费。避免冲突',
  ext_param            varchar(2048) comment '打包的附加参数',
  app_name             varchar(255) comment 'apk中提取的name label,迁移自ratel_apk',
  app_version          varchar(32) comment 'apk中提取的版本号,迁移自ratel_apk',
  app_version_code     bigint(11) comment 'apk中提取的版本号的数字号码,迁移自ratel_apk',
  comment              varchar(255) comment '任务备注',
  app_package          varchar(255) comment 'apk的package,迁移自ratel_apk',
  ratel_version        varchar(25) comment '引擎版本'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment 'apk处理任务';

create table ratel_engine_bin
(
  id                  bigint(11) primary key auto_increment comment '自增主建',
  file_hash           varchar(255) comment '文件hash，发布包是一个zip包',
  engine_version      varchar(50) comment '引擎版本',
  engine_version_code bigint(11) comment '引擎版本号',
  enabled             bool default false comment '当前版本的引擎是否启用',
  oss_url             varchar(255) comment '上传到oss',
  dex_engine_url      varchar(255) comment '可以运行在Android上面的dex格式的engine'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment 'ratel构建引擎二进制发布包';

create table ratel_manager_apk
(
  id               bigint(11) primary key auto_increment comment '自增主建',
  file_hash        varchar(255) comment 'apk文件hash值',
  oss_url          varchar(255) comment '本apk需要上传到oss，用以实现和服务器环境无关的序列化',
  upload_time      datetime comment '上传时间',
  app_version      varchar(32) comment 'apk中提取的版本号',
  app_version_code bigint(11) comment 'apk中提取的版本号的数字号码'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment 'ratelManager的发布包';

create table ratel_hot_module
(
  id                  bigint(11) primary key auto_increment comment '自增主建',
  file_hash           varchar(255) comment 'apk文件hash值',
  oss_url             varchar(255) comment '本apk需要上传到oss，用以实现和服务器环境无关的序列化',
  upload_time         datetime comment '上传时间',
  module_pkg_name     varchar(127) not null comment '该模块对应的包名',
  module_version      varchar(32)  not null comment 'apk中提取的版本号',
  module_version_code bigint(11)   not null comment 'apk中提取的版本号的数字号码',
  certificate_id      varchar(64)  not null comment '该模块对应的的证书id',
  for_ratel_app       varchar(127) not null comment '该模块对应的app',
  ratel_group         varchar(64)  null comment '可选，对应的group，解析自模块AndroidManifest.xml',
  user_id             bigint(11)   not null comment '上传用户，ratel网站上面的操作用户',
  user_name           varchar(50) comment '上传用户名称，同步自用户表',
  enable              bool comment '是否生效',
  file_size           int comment '文件大小，单位KB'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 comment 'ratel热发模块';