<%@taglib uri="/WEB-INF/tld/c.tld" prefix="c" %>
<html>
	<head>
		<title>A target service</title>
	</head>
	<body>
	<c:set var="servicelogin" value=""/>
	<c:forEach items="${pageContext.request.cookies}" var="c">
		<c:if test="${c.name eq 'SSO-BlogBuilder'}">
			<c:set var="servicelogin" value="${c.value}"/>
		</c:if>
	</c:forEach>
	
	<c:choose>
	<c:when test="${servicelogin ne ''}">
		Logged in with ticket ${servicelogin}
	</c:when>
	<c:otherwise>
		<a href="http://moleman.warwick.ac.uk/origin/hs?shire=http%3A%2F%2Fmoleman.warwick.ac.uk%2Fsso-client%2Fshire&target=http%3A%2F%2Fmoleman.warwick.ac.uk%2Fsso-client%2Ftarget.jsp&time=1110798123&providerId=urn%3Amoleman.warwick.ac.uk%3Ablogbuilder%3Aservice">Sign in</a>
	</c:otherwise>
	</c:choose>
	
	</body>
</html>