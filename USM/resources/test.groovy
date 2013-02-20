def ant = new AntBuilder()
ant.sequential ({
  echo("inside sequential")
  mkdir(dir:"install")
  gunzip(  src:"apache-activemq-5.5.0-bin.tar.gz", dest:"install/apache-activemq-5.5.0-bin.tar")
  untar(src:"install/apache-activemq-5.5.0-bin.tar", dest:"install")
})
