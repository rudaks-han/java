select (
	select count(*) from (select site_name from customize_item group by site_name) a
) as "사이트 개수(customize_item 기준)",
(
	select count(*) from (select site_name from customize_changed_file group by site_name) a
) as "사이트 개수(customize_changed_file 기준)",
(
	select count(*) from (select site_name, filename from customize_item group by site_name, filename) a
) as "사이트 개수(커스트마이징 내역서 파일 개수 기준)",
(
	select count(*) from (select site_name, customize_name from customize_item where customize_name != '' group by site_name, customize_name) a
) as "커스트마이징 개수",
(
	select cnt/all_site
	from (
		select count(*) cnt, (
			select count(*) from (select site_name from customize_changed_file group by site_name) a
		) all_site
		from (
			select site_name, customize_name from customize_item where customize_name != '' group by site_name, customize_name
		) a
	) a
) as "사이트 당 커스트마이징 개수",
(
	select count(*) from (
		select file_ext from customize_changed_file where file_ext != '' group by file_ext
	) a
) as "파일 확장자 개수"