<%@taglib uri="/WEB-INF/tld/c.tld" prefix="c" %>
<%@taglib uri="/WEB-INF/tld/sso.tld" prefix="sso" %>
<html>
	<head>
		<title>A target service</title>
	</head>
	<body>
	<c:choose>
	<c:when test="${requestScope['SSO-USER'] != null && requestScope['SSO-USER'].loggedIn eq true}">
		<p>User found in request: ${requestScope['SSO-USER'].fullName}</p>
		
		<p><a href="<sso:logoutlink />">Sign out</a></p>
	</c:when>
	<c:otherwise>
		<a href="<sso:loginlink />">Sign in</a>
	</c:otherwise>
	</c:choose>
	
	</body>
</html>