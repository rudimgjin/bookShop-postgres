<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" 
    isELIgnored="false"  %>
 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"  %>
<c:set var="contextPath"  value="${pageContext.request.contextPath}"  />

<%
  request.setCharacterEncoding("UTF-8");
%>    


<html>
<head>
<meta charset=UTF-8">
<title>회원 정보 출력창</title>

<script>

window.onload=function()
{
 result();
  
}

function result(){
	alert("회원 아이디와 비밀번호를 확인하세요. 탈퇴하기를 누르면 더 이상 서비스를 이용하실 수 없습니다. 그래도 탈퇴하시겠습니까??");
}


</script>
 <%-- method="post"   action="${contextPath}/member/removeMember.do" --%>

</head>
<body>



<form>
<table border="1"  align="center"  width="80%">
    <tr align="center"   bgcolor="lightgreen">
      <b><td>
	아아디 : <input name="member_id" type="text" size="20" value="${memberInfo.member_id }"  disabled/>			</td></b>
      <td><b>
      비밀 번호 : <input name="member_pw" type="password" size="20" value="${memberInfo.member_pw }"  disabled/>			</td></b>
      </b></td>
   </tr>
   
 
  
</table>
<a href="${contextPath}/member/removeMember.do?member_id=${memberInfo.member_id}"
onclick="alert('회원 탈퇴에 성공하셨습니다. 이용해주셔서 감사합니다.');">탈퇴하기</a>
</form>

</body>
</html>