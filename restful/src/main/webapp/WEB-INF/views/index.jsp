<%@page import="net.jini.core.discovery.LookupLocator"%>
<%@page import="org.springframework.web.servlet.support.RequestContextUtils"%>
<%@page import="java.net.InetAddress"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page import="org.openspaces.admin.Admin"%>
<%@page import="java.util.Arrays"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<% 
   String host = InetAddress.getLocalHost().getHostAddress() + ":" + request.getLocalPort(); 
   String context = request.getContextPath();
   
   ApplicationContext applicationContext = RequestContextUtils.getWebApplicationContext(request);
   Admin admin = (Admin)applicationContext.getBean("admin");
   String groups = Arrays.toString((admin.getGroups()));
   String locators = Arrays.toString(admin.getLocators());
   
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>GigaSpaces REST API</title>
<link rel="icon" 
      type="image/x-icon" 
      href="<%=context%>/resources/favicon.ico">
</head>
<body>
	<center><img alt="logo" src="<%=context%>/resources/logo.png"></center><br/>
	
	<h1>REST Admin API</h1>
	<ul>
		<li> Locators
		<ul> 
			<li><%=locators%>					
		</ul>
		<li> Groups
		<ul> 
			<li><%=groups%>					
		</ul>
		<li> Machines
		<ul> 
			<li><a href="<%=context%>/admin/Machines">admin/Machines</a>					
		</ul>
		<li> Processing Units
		<ul> 
			<li><a href="<%=context%>/admin/ProcessingUnits">admin/ProcessingUnits</a>					
		</ul>

		<li> Containers
		<ul> 
			<li><a href="<%=context%>/admin/GridServiceContainers">admin/GridServiceContainers</a>
						
		</ul>
				
		<li> Spaces
		<ul>
			<li><a href="<%=context%>/admin/Spaces">admin/Spaces</a>
		</ul>
		
		<li> GSMs
		<ul> 
			<li><a href="<%=context%>/admin/GridServiceManagers">admin/GridServiceManagers</a>								
		</ul>
		
		<li> Elastic Service Managers
		<ul> 
			<li><a href="<%=context%>/admin/ElasticServiceManagers">admin/ElasticServiceManagers</a>									
		</ul>

				
		<li> Zones
		<ul>
			<li><a href="<%=context%>/admin/zones">admin/Zones</a>
		</ul>
		
				
		<li> JVMs
		<ul>
			<li><a href="<%=context%>/admin/VirtualMachines">admin/VirtualMachines</a>
		</ul>
		
		<li> Applications
		<ul>
			<li><a href="<%=context%>/admin/applications">admin/Applications</a>
		</ul>
				
	</ul>
	
	<h1>REST Service API</h1>
		<ul>
			<li><a href="<%=context%>/resources/restDoclet/restDoclet.html">REST API documentation</a>
		</ul>
</body>
</html>