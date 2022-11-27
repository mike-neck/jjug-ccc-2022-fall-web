create table authors (
    id int not null primary key ,
    `name` varchar(60) not null
) engine = innodb default charset = utf8mb4;

begin ;

insert into authors(id, `name`) values
    (1, '手柄治虫'),
    (2, '赤塚不三夫')
;

commit ;
