[기능]
1. Jira의 조건을 검색한 다음 해당 revision에 해당하는 소스를 다운로드 받아서 특정 경로로 받아준다.

[조건]
1. 실행환경에 svn client가 설치되어 있어야 한다. (예: tortoiseSvn)
2. EER svn에 접근 가능해야 한다. (https://211.63.24.57/svn/SuperTalk)
3. java 실행환경이어야 한다.
4. 윈도우에서만 실행가능

[결과파일]
1. 엑셀파일 (조건에 해당되는 Jira의 내용)
2. 소스파일 (해당 Jira 아이디에 해당되는 폴더목록)
3. diff 파일 (비교버전과의 diff 파일)
4. svn history 내역

[실행방법]
1. run.bat 을 실행
2. 실행폴더의 output폴더에 파일이 생성

