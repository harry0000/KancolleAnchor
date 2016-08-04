drop table if exists admiral;
create table admiral (
  id bigint not null auto_increment,
  admiral_id varchar(255) not null,
  name varchar(32),
  created timestamp not null,
  primary key(id)
);

drop table if exists anchor;
create table anchor (
  prefecture tinyint not null,
  place tinyint not null,
  credits tinyint not null,
  page tinyint not null,
  number smallint not null,
  admiral_id bigint not null,
  anchored timestamp not null,
  weighed timestamp null,
  primary key(prefecture, place, credits, admiral_id, anchored),
  unique key(prefecture, place, credits, page, number, admiral_id)
);

drop table if exists weigh_anchor_spotting;
create table weigh_anchor_spotting (
  prefecture tinyint not null,
  place tinyint not null,
  credits tinyint not null,
  page tinyint not null,
  number smallint not null,
  spotter_id bigint not null,
  reported timestamp not null,
  primary key(prefecture, place, credits, page, number, spotter_id),
  unique key(spotter_id, reported)
);
