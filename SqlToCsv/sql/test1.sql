-- [ui만 변경된 요구사항 목록]
select a.site_name, a.customize_name, b.requirements, b.java_changes, b.changed_files
from (
	select site_name, customize_name,
			(
				select coalesce(max('Y'), 'N')
				from customize_changed_file where site_name = a.site_name and customize_name = a.customize_name
				and file_ext in ('java')
			) java_changed,
			(
				select coalesce(max('Y'), 'N')
				from customize_changed_file where site_name = a.site_name and customize_name = a.customize_name
				and file_ext in ('js', 'jsp', 'css')
			) ui_changed
	from (
		select site_name, customize_name, count(*)
		from customize_changed_file
		where file_ext != ''
		group by site_name, customize_name
	) a
) a, customize_item b
where a.site_name = b.site_name
and a.customize_name = b.customize_name
and java_changed = 'N' and ui_changed = 'Y';