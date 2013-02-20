import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

this.setProperty("catalinaZip", "https://gigaspaces.blob.core.windows.net/packages/apache-tomcat-7.0.16.zip?&se=2015-06-02T00%3A00%3A00Z&sr=b&si=readforever&sig=nfR58qT%2FaYJKhgu4U1RpJ2uwB2996zeXhm1jpGy0Ezw%3D")
this.setProperty("catalinaHome", "install")
this.setProperty("tomcatPort","80")
this.setProperty("catalinaOverwrite","overwrite")
this.setProperty("travelWar","https://gigaspaces.blob.core.windows.net/packages/travel.war?&se=2015-06-20T00%3A00%3A00Z&sr=b&si=readforever&sig=xXjZ660%2Faj%2B90faxia23UKQp6s8gmfAif8Twua7i6Mw%3D")


def ant = new AntBuilder()   // create an antbuilder

def zipName = "apache-tomcat-7.0.16"

if (!(new File("apache-tomcat-7.0.16.zip")).exists()) {
  //download apache tomcat
  ant.get (src:catalinaZip, dest:zipName+".zip")
}

ant.sequential {

  unzip(  src:zipName+".zip",
          dest:catalinaHome,
          overwrite:"true" )

  move(todir: catalinaHome, overwrite:true) {
          fileset(dir:catalinaHome + "/" + zipName, includes:"**/*")
  }

  delete(dir:"${catalinaHome}/${zipName}") ;

  // overwrite default tomcat files
  move(todir:catalinaHome, overwrite:true) {
    fileset(dir:catalinaOverwrite,includes:"**/*")
  }
}

if ((new File("travel.war")).exists()) {
  ant.move(todir: catalinaHome+"/webapps", file:"travel.war", overwrite:true)
}
else {
  //download the war file to webapps
  ant.get (src:travelWar, dest:catalinaHome + "/webapps")
}