import groovy.util.ConfigSlurper

def config=new ConfigSlurper().parse(new File("tomcat.properties").toURL())

def ant = new AntBuilder();  

ant.get(src:config.applicationWarUrl, dest:config.applicationWar, skipexisting:false)
ant.copy(todir: "${catalinaHome}/webapps",  file:config.applicationWar, overwrite:true)
