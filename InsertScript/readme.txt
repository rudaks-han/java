# 실행방법
# db.properties를 열어서 안에 속성값에 맞는 정보를 입력한다.

url=jdbc:microsoft:sqlserver://127.0.0.1:1433;SelectMethod=cursor;DatabaseName=DB명
driver=com.microsoft.jdbc.sqlserver.SQLServerDriver
user=사용자명
pass=비밀번호
query=테이블명 혹은 쿼리
tablename=테이블명
filename=출력파일명

# insertScript.jar파일을 더블클릭한다.

# 더블클릭해도 실행이 안될때는 명령프롬프트창에서 java -jar insertScript.jar를 실행해보면 에러가 보일것이다.