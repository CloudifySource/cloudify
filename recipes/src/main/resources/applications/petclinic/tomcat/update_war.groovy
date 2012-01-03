import groovy.util.ConfigSlurper

def config=new ConfigSlurper().parse(new File("tomcat.properties").toURL())

def ant = new AntBuilder();  

ant.get(src:config.travelWarUrl, dest:config.travelWar, skipexisting:false)
ant.copy(todir: "${catalinaHome}/webapps",  file:config.appWar, overwrite:true)
