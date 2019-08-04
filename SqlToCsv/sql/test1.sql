select site_name "사이트명", product "제품명", eer_version "버전", filename "파일명", customize_name "커스트마이징 이름", requirements "요구사항", java_changes "java변경사항", db_changes "DB변경사항", changed_files "변경파일", '' "추가의견"
from customize_item
where (site_name, customize_name) in (
	select site_name, customize_name
	from customize_changed_file
	where f_get_component_type(filepath) = 'ticket'
	and file_ext != ''
)
order by site_name