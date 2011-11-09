<%@page import="java.util.*"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html"/>
<title>HTTP Session Content</title>
</head>
<body>
<h3>Session Contents</h3>
<% 	Date created = new Date(session.getCreationTime());
	Date accessed = new Date(session.getLastAccessedTime());
	String hostName = request.getServerName(); 
    String sessionId = session.getId();
%>
<br/>
<table>
	<tr>
		<td>Session creation time:</td>
		<td><%= created %></td>
	</tr>
	<tr>
		<td>Session Last access time:</td>
		<td><%= accessed %></td>
	</tr>
	<tr>
		<td>Servlet container host name:</td>
		<td><%= hostName %></td>
	</tr>
	<tr>
		<td>Session id:</td>
		<td><%= sessionId %></td>
	</tr>
</table>
<h3>Session Contents:</h3>
<%	Enumeration names = session.getAttributeNames();
	while (names.hasMoreElements()) {
		String name = (String) names.nextElement();
		String value = (String)session.getAttribute(name);
%> 
<%= name %> = <%= value %><br/>
<%} %>
<p/>

<form action="<%= response.encodeURL("UpdateSessionServlet") %>" method="post">
Name of Session Attribute:
<input type="text" size="20" name="dataname"/>
<br/>
Value of Session Attribute:
<input type="text" size="20" name="datavalue"/>
<br/>
<input type="submit" value="Update Session"/>
</form>
</body>
</html>