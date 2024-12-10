<%@ page language="java" contentType="text/html; charset=utf-8"
	pageEncoding="utf-8"
	 isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:set var="contextPath"  value="${pageContext.request.contextPath}"  />
<!DOCTYPE html >
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<c:if test='${not empty message }'>

<script type="text/javascript" src="${contextPath}/js/netfunnel.js" charset="UTF-8"></script>
<script type="text/javascript">
        function NF_Function() {
                NetFunnel_Action({'action_id':'nf_test'},"http://192.168.137.130:8080/bookShop/main/main.do");
                //NetFunnel_Action({'action_id':'nf_test'},"http://192.168.10.159:8080/bookShop/main/main.do");
                //NetFunnel_Action({'action_id':'action'},"/home/main");
        }
        </script>
<script>


window.onload=function()
{
  result();
}

function result(){
	alert("아이디나  비밀번호가 틀립니다. 다시 로그인해주세요");
}
</script>
</c:if>
</head>
<body>
	<H3>회원 로그인 창</H3>
	<DIV id="detail_table">
	<form id ="joinForm" action="${contextPath}/member/login.do" method="post">
		<TABLE>
			<TBODY>
				<TR class="dot_line">
					<TD class="fixed_join">아이디</TD>
					<TD><input name="member_id" type="text" size="20" /></TD>
				</TR>
				<TR class="solid_line">
					<TD class="fixed_join">비밀번호</TD>
					<TD><input name="member_pw" type="password" size="20" /></TD>
				</TR>
			</TBODY>
		</TABLE>
		<br><br>
		<INPUT	type="button" value="로그인(NetFunnel)" onClick="NF_Function()"> 
		<INPUT	type="submit" value="로그인"> 
		<INPUT type="button" value="초기화">
		
		


		
		<Br><br>
		   <a href="#">아이디 찾기</a>  | 
		   <a href="#">비밀번호 찾기</a> | 
		   <a href="${contextPath}/member/memberForm.do">회원가입</a>    | 
		   <a href="#">고객 센터</a>
					   
	</form>	
	
	<!-- netfunnel test  --> 

<button id="nf" onClick="NF_Function();"> netFunnel_test </button>	
</body>
</html>