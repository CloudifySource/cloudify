
try {
	def context = org.cloudifysource.utilitydomain.context.ServiceContextFactory.getServiceContext()
	Thread.sleep(2000)
	println " context is:  " + context
} catch(Exception e) {
	println "EXCEPTION"
	println e
	e.printStackTrace()
}
System.exit(0)
