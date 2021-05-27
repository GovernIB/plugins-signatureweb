<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Això era i no era una pàgina per signar</title>
</head>
<body>    
    <h1>Això era i no era una pàgina per signar</h1>
    <c:url value="/startSign" var="formAction" />
    <form id="signForm" method="post" action="${formAction}" enctype="multipart/form-data">
        <p>
            <label id="fileLabel" for="fitxer">Fitxer:</label>
            <input type="file" name="fitxer">
        </p>
        <p>
            <label id="pluginNameLabel" for="pluginName">Plugin:</label>
            <select name="pluginName">
                <c:forEach items="${pluginMapBean.pluginNames}" var="pluginName">
                    <option label="${pluginName}" value="${pluginName}">
                </c:forEach>
            </select>
        </p>
        
        <input type="submit" value="Enviar">        
    </form>
</body>
</html>