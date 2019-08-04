create or replace FUNCTION f_get_module_type(filepath varchar(20000)) RETURNS Varchar
as $BODY$
 declare v_module_type varchar(100);
 BEGIN
 select case when filepath like '%supertalk-00-external%' then
 			'supertalk-00-external'
 		when position('spectra/base/' in filepath) > 0
 				or filepath like '%01-framework%' then
			'supertalk-01-framework'
 		when filepath like '%01-healthcheck%' then
			'supertalk-01-healthcheck'
		when position('spectra/ee/health' in filepath) > 0 then
			'supertalk-01-healthcheck'
		when position('spectra/ee/commons' in filepath) > 0
				or filepath like '%supertalk-02-commons%'
				or filepath like '%commons-engine%'
				or filepath like '%commons-common%'
				or filepath like '%ext/commons%' then
			'supertalk-02-commons'
		when position('spectra/ee/api' in filepath) > 0
				or filepath like '%supertalk-03-interface%'
				or filepath like '%interface-ecc%' then
			'supertalk-03-interface'
		when position('spectra/ee/gateway' in filepath) > 0
				or filepath like '%supertalk-04-gateway%'
				or filepath like '%gateway-server%'
				or filepath like '%gateway/WEB-INF%' then
			'supertalk-04-gateway'
		when position('spectra/ee/legw' in filepath) > 0
				or filepath like '%legacy-gateway%'
				or filepath like '%legw/WEB-INF/jsp%' then
			'supertalk-04-legacy-gateway'
		when position('spectra/ee/monitoring' in filepath) > 0
				or filepath like '%04-monitoring%'
				or filepath like '%rtis/%' then
			'supertalk-04-monitoring'
		when position('spectra/ee/router' in filepath) > 0
				or filepath like '%supertalk-04-router%'
				or filepath like '%router-server%' then
			'supertalk-04-router'
		when position('spectra/ee/interpreter' in filepath) > 0
				or filepath like '%04-scenario%' then
			'supertalk-04-scenario-interpreter'
		when position('spectra/ee/thirdparty' in filepath) > 0
				or filepath like '%supertalk-04-thirdparty%' then
			'supertalk-04-thirdparty'
		when filepath like '%supertalk-04-link%' then
			'supertalk-04-link'
		when position('spectra/ee/engine' in filepath) > 0
				or position('spectra/ext/engine' in filepath) > 0
				or filepath like '%engine%'
				or filepath like '%WEB-INF/http/%'
				or filepath like '%WEB-INF/rmi-%.xml%'
				or filepath like '%.jar'
				or filepath like '%remote-client-rmi.xml'
				or filepath like '%ibatis%common/%' then
			'supertalk-05-engine'
		when filepath like '%06-transport/%' then
			'supertalk-06-transport'
		when position('spectra/ee/client' in filepath) > 0
				or position('spectra/ext/client' in filepath) > 0 then
			'supertalk-07-client'
		when filepath like '%commandline%' then
			'supertalk-07-commandline'
		when filepath like '%07-client/%' then
			'supertalk-07-client'
		when position('spectra/ee/scheduler' in filepath) > 0
				or position('spectra/ext/scheduler' in filepath) > 0
				or filepath like '%/scheduler/%'
				or filepath like '%10-scheduler%'
				or filepath like 'scheduler%'
				then
			'supertalk-10-scheduler'
		when position('spectra/ee/ui/apps' in filepath) > 0
				or position('spectra/ext/ui/apps' in filepath) > 0
				or position('spectra/ee/ui/base' in filepath) > 0
				or position('spectra/ext/ui/base' in filepath) > 0
				or filepath like '%11-ui-framework%' then
			'supertalk-11-ui-framework'
		when position('spectra/ee/ui/webapps' in filepath) > 0
				or position('spectra/ext/ui/webapps' in filepath) > 0
				or filepath like '%js/webapps%'
				or filepath like '%webapps/WEB-INF/jsp%'
				or filepath like '%13-ui-webapps%'
				or filepath like '%webapps/webapps/WEB-INF%'
				or filepath like '%webapps%webapp/WEB-INF%'
				or filepath like '%WEB-INF/jsp/monitoring%'
				or filepath like '%WEB-INF/jsp/managing%'
				or filepath like '%webapps/images/themes%'
				or filepath like '%webapps/js/%'
				or filepath like '%webapp/js/%'
				or filepath like '%WEB-INF/jsp%'
				or filepath like '%WEB-INF/%-context%'
				or filepath like '%js/lib/spectra/%'
				or filepath like '%core/themes/%'
				or filepath like '%webapps/src%'
				or filepath like '%webapps/jsp%'
				or filepath like '%web.xml%'
				or filepath like '%WEB-INF/%servlet%'
				or filepath like 'webapps%'
				or filepath like '%spectra/ext/webapps%'
				or filepath like '%webapps/webapps%' then
			'supertalk-13-ui-webapps'
		when position('spectra/ee/restapi' in filepath) > 0
				or position('spectra/ext/restapi' in filepath) > 0
				or filepath like '%supertalk-20-restapi%'
				or filepath like '%rest-api%'
				or filepath like '%sql-map-config-rest%'
				or filepath like '%talk/server/command%'
				or filepath like '%web/api%'
				or filepath like '%restapi%' then
			'supertalk-20-restapi'
		when position('spectra/ee/proxy' in filepath) > 0
				or filepath like '%supertalk-21-ui-proxy%'
				or filepath like '%webroot/%'
				or filepath like '%/front/%'
				or filepath like '%jsp/talk/%'
				or filepath like '%jsp/common/%'
				or filepath like '%js/api/%'
				or filepath like '%js/spectra/%'
				or filepath like '%js/lang/%'
				or filepath like '%js/talk%'
				or filepath like 'webapp/%'
				or filepath like '%css%'
				or filepath like '%images/%.gif'
				or filepath like '%customerCenter%'
				or filepath like '%/talk/%.jsp'
				or filepath like '%/WebContent/%'
				or filepath like '%/proxy/%'
				or filepath like '%webapp%/js/%' then
			'supertalk-21-proxy'
		when filepath like '%30-notification%' then
			'supertalk-30-notification'
		when filepath like '%92-test%' then
			'supertalk-92-test'
		when filepath like '%93-dummy%' then
			'supertalk-93-dummy-chatbot'
		when filepath like '%99-release%' then
			'supertalk-99-release'
		when filepath like '%setup%'
				or filepath like '%HOME/conf%'
				or filepath like '%ENOMIX_HOME%'
				or filepath like '%home/conf%'
				or filepath like '%enomix/conf%'
				or filepath like '%m2repository%'
				or filepath like '%conf/%properties%'
				or filepath like '%sql-map-config.xml%'
				or filepath like '%sql-map-config-scheduler.xml%'
				or filepath like '%pom.xml'
				or filepath like '%bin/%.sh'
				or filepath like '%bin/%.bat'
				or filepath like '%apps/%.sh'
				or filepath like '%/home/%' then
			'home'
		when filepath like '%/solr/%' then
			'solr'
		else
			''
		end into v_module_type;

		return v_module_type;
END;
$BODY$ LANGUAGE plpgsql stable;

create or replace FUNCTION f_get_component_type(filepath varchar(20000)) RETURNS Varchar
as $BODY$
 declare v_component_type varchar(100);
 BEGIN
 select case
 		when filepath like '%/account/%' then
 			'account'
 		when filepath like '%/apps/blackconsumer%' then
 			'blackconsumer'
 		when filepath like '%/apps/channel%' then
 			'channel'
 		when filepath like '%/code/%' then
 			'code'
 		when filepath like '%/customerlead/%' then
 			'customerlead'
 		when filepath like '%/goal/%' then
 			'goal'
 		when filepath like '%/greeting/%' then
 			'greeting'
 		when filepath like '%/interfaces/%' then
 			'interfaces'
 		when filepath like '%/manager/%' then
 			'manager'
 		when filepath like '%/notice/%' then
 			'notice'
 		when filepath like '%/ping/%' then
 			'ping'
 		when filepath like '%/pushurl/%' then
 			'pushurl'
 		when filepath like '%/recommend/%' then
 			'recommend'
 		when filepath like '%/resource/%' then
 			'resource'
 		when filepath like '%/restrictedword/%' then
 			'restrictedword'
 		when filepath like '%/script/%' then
 			'script'
 		when filepath like '%/stepguide/%' then
 			'stepguide'
 		when filepath like '%/survey/%' then
 			'survey'
 		when filepath like '%/template/%' then
 			'template'
 		when filepath like '%/worktime/%' then
 			'worktime'
 		when filepath like '%/call/%' then
 			'call'
 		when filepath like '%/category/%' then
 			'category'
 		when filepath like '%/cm/%' then
 			'cm'
 		when filepath like '%/common/%' then
 			'common'
 		when filepath like '%/crm/%' then
 			'crm'
 		when filepath like '%/env/%' then
 			'env'
 		when filepath like '%/health/%' then
 			'health'
 		when filepath like '%/helper/%' then
 			'helper'
 		when filepath like '%/interfaces/%' then
 			'interfaces'
 		when filepath like '%/kb/%' then
 			'kb'
 		when filepath like '%/km/%' then
 			'km'
 		when filepath like '%/lb/%' then
 			'lb'
 		when filepath like '%/mo/%' then
 			'mo'
 		when filepath like '%/monitoring/%' and filepath like '%.java' then
 			'monitoring'
 		when filepath like '%/notification/%' then
 			'notification'
 		when filepath like '%/qna/%'
 				or filepath like '%/mail/%' then
 			'qna'
 		when filepath like '%/report/%'
 				or filepath like '%/statistics/%' then
 			'report'
 		when filepath like '%/routing/%' then
 			'routing'
 		when filepath like '%/rule/%' then
 			'rule'
 		when filepath like '%/rvts/%' then
 			'rvts'
 		when filepath like '%/scenario/%' then
 			'scenario'
 		when filepath like '%/scheduler/%' then
 			'scheduler'
 		when filepath like '%/search/%' then
 			'search'
 		when filepath like '%/security/%' then
 			'security'
 		when filepath like '%/sms/%' then
 			'sms'
 		when filepath like '%/spam/%' then
 			'spam'
 		when filepath like '%/startup/%' then
 			'startup'
 		when filepath like '%/switching/%' then
 			'switching'
 		when filepath like '%/system/%' then
 			'system'
 		when filepath like '%/talk/%'
 				or filepath like '%/cmd/%' then
 			'talk'
 		when filepath like '%/targeting/%' then
 			'targeting'
 		when filepath like '%thirdparty%' then
 			'thirdparty'
 		when filepath like '%/ticket/%'
 			or (filepath like '%/monitoring/%' and (filepath like 'jsp%' or filepath like 'js%'))
 			or (filepath like '%/jsp/monitoring/%')
 			or (filepath like '%/webapps/monitoring%')
 			then
 			'ticket'
 		when filepath like '%/trackRecord/%' then
 			'trackRecord'
 		when filepath like '%/push/%' then
 			'push'
 		when filepath like '%/legw/%'
 				or filepath like '%/legacy/%' then
 			'legw'
 		when filepath like '%/kbhelper/%' then
 			'kbhelper'
 		when filepath like '%/login/%' then
 			'login'
 		when filepath like '%/mci/%' then
 			'mci'
 		when filepath like '%/auth/%' then
 			'auth'
 		when filepath like '%chatbot%' then
 			'chatbot'
 		when filepath like '%/faq/%' then
 			'faq'
 		when filepath like '%/esb/%' then
 			'esb'
 		when filepath like '%/voccategory/%' then
 			'voccategory'
 		when filepath like '%/uq/%' then
 			'uq'
 		when filepath like '%/cti/%' then
 			'cti'
 		when filepath like '%/eai/%' then
 			'eai'
 		when filepath like '%/sso/%' then
 			'sso'
 		when filepath like '%/gateway/%' then
 			'gateway'
 		when filepath like '%/proxy/%'
 				or filepath like '%proxy%/front/%'
 		then
 			'proxy'
 		when filepath like '%router%'
 				or filepath like '%/route%' then
 			'router'
 		when filepath like '%/scheduler/%' then
 			'scheduler'
 		when filepath like '%restapi%' then
 			'restapi'
 		when filepath like '%/licensemanager/%' then
 			'licensemanager'
 		when filepath like '%/kms/%' then
 			'kms'
 		when filepath like '%blackconsumer%' then
 			'blackconsumer'
 		when filepath like '%/callcenter/%' then
 			'callcenter'
 		when filepath like '%/migration/%' then
 			'migration'
 		/*
 		when filepath like '%spectra%base%' then
 			'spectra/base'
 		when filepath like '%spectra/ee/commons%' then
 			'spectra/ee/commons'
 			*/
		else
			''
		end into v_component_type;

		return v_component_type;
END;
$BODY$ LANGUAGE plpgsql stable;


create or replace FUNCTION f_ratio(cnt numeric, total numeric) RETURNS Varchar
as $BODY$
 declare v_result varchar(100);
 BEGIN
 select case when total <= 0
 			then '-'
		when total > 0 then
			concat(round(cnt * 100 / total, 2), '%')
		end into v_result;
 return v_result;
 END;
$BODY$ LANGUAGE plpgsql stable;

create or replace FUNCTION f_site_count() RETURNS numeric
as $BODY$
 declare v_result numeric;
 BEGIN
	 select count(*) into v_result from (select site_name from customize_changed_file group by site_name) a;
	 return v_result;
 END;
$BODY$ LANGUAGE plpgsql immutable;

create or replace FUNCTION f_customize_item_count() RETURNS numeric
as $BODY$
 declare v_result numeric;
 BEGIN
	 select count(*) into v_result from (select site_name, customize_name from customize_item where customize_name != '' group by site_name, customize_name) a;
	-- select count(*) from (select site_name, customize_name from t_customize_changed_file group by site_name, customize_name) a;
	 return v_result;
 END;
$BODY$ LANGUAGE plpgsql immutable;

create or replace FUNCTION f_changed_file_count() RETURNS numeric
as $BODY$
 declare v_result numeric;
 BEGIN
	 select count(*) into v_result from customize_changed_file where file_ext != '' ;
	-- select count(*) from (select site_name, customize_name from t_customize_changed_file group by site_name, customize_name) a;
	 return v_result;
 END;
$BODY$ LANGUAGE plpgsql immutable;

update customize_changed_file set file_ext = ''
where file_ext in ('rosaloves', 'IRoutePriorityService', 'core', 'leevi', 'xsd', 'groupId', 'state', 'split', 'center', 'version', 'json', 'netty', 'Final', 'org', 'BaseUrlFilenameViewController', 'AjaxUploadPreviewController'
        'BzCheckManager', 'CustRoutePriorityDaoImpl', 'DummyRoutingClientServiceImpl', 'ReportSurveyResponseDaoImpl', 'RoutePriorityServiceImpl', 'RoutingResultListenerImpl', 'RoutingStateAspect',
        'a', 'j', 'BzCheckManager', 'AjaxUploadPreviewController');

