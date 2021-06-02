<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="signatureSet" scope="request" type="org.fundaciobit.plugins.signatureweb.api.SignaturesSetWeb" />
<!DOCTYPE html>
<html>
<head>
    <title>Pàgina de resultat</title>
</head>
<body>    
    <h1>Pàgina de resultat</h1>

    <span id="status"><c:out value="${signatureSet.statusSignaturesSet.status}" /></span>
        
    <c:url value="/endSign" var="endSignUrl">   
        <c:param name="ssid" value="${signatureSet.signaturesSetID}" />
    </c:url>
    
    <a id="endSignLink" href="${endSignUrl}">Descarregar document</a>
    
</body>
</html>