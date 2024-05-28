<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:useBean id="signatureSet" scope="request"
    type="org.fundaciobit.pluginsib.signatureweb.api.SignaturesSetWeb" />
<!DOCTYPE html>
<html>
<head>
<title>Pàgina de resultat</title>
</head>
<body>
    <h1>Pàgina de resultat</h1>

    <c:set var="status"
        value="${signatureSet.statusSignaturesSet.status}" />

    <span id="status">Status Code: <c:out value="${status}" /></span>
    <br/>
    <span id="status">Status Name:
     
    <c:choose>
         <c:when test="${status == 0}">
            STATUS_INITIALIZING
         </c:when>

         <c:when test="${status == 1}">
            STATUS_IN_PROGRESS
         </c:when>

         <c:when test="${status == 2}">
            STATUS_FINAL_OK
         </c:when>

         <c:when test="${status == -1}">
            STATUS_FINAL_ERROR
         </c:when>

         <c:when test="${status == -2}">
            STATUS_CANCELLED = -2
         </c:when>

         <c:otherwise>
            STATUS DESCONEGUT 
         </c:otherwise>
        </c:choose>
    </span>
    <br/>

    <c:if test="${status == 2}">

        <c:url value="/endSign" var="endSignUrl">
            <c:param name="ssid" value="${signatureSet.signaturesSetID}" />
        </c:url>

        <a id="endSignLink" href="${endSignUrl}">Descarregar document</a>
        <br />
    </c:if>


    <c:if test="${status != 2}">
        Missatge d'Error: <i><c:out value=" ${signatureSet.statusSignaturesSet.errorMsg}" /></i>
    <br />Excepció: <br/><textarea rows="4" cols="80">${signatureSet.statusSignaturesSet.errorException}</textarea>
    </c:if>

</body>
</html>