import groovy.util.ConfigSlurper


def env=System.getenv()
def config=new ConfigSlurper().parse(new File('service.properties').toURL())


//install postgresql
def builder = new ProcessBuilder()

//builder.command(["yum","-y","install","postgresql","postgresql-server"] as String[])
//def benv=builder.environment()
//benv.putAll(System.getenv())

//Process p = builder.start()
//p.consumeProcessErrorStream(System.err)
//p.consumeProcessOutputStream(System.out)


//p.waitFor()

new AntBuilder().sequential {
	mkdir(dir:"install")
	copy(file:"${config.platform.binLoc}/${config.platform.binName}", toDir:"install")
	unzip(src:"install/${config.platform.binName}", dest:"install", overwrite:true)
   delete(file:"install/${config.platform.binName}")
}


p = new ProcessBuilder("install/pgsql/bin/postgres.exe","-D","install/pgsql/data").start();
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)

Thread.sleep(5000)

//create schema and users
builder.command(["install/pgsql/bin/createdb.exe","petclinic"])
p = builder.start()
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)
p.waitFor()


builder.command(["install/pgsql/bin/psql","-d","petclinic","-c","create user test with password 'test'"])
p = builder.start()
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)
p.waitFor()

builder.command(["install/pgsql/bin/psql","-d","petclinic","-f","schema.txt"])
p = builder.start()
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)
p.waitFor()

builder.command(["install/pgsql/bin/psql","-d","petclinic","-c","grant all on database petclinic to test"])
p = builder.start()
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)
p.waitFor()

//populate demo data
builder.command(["install/pgsql/bin/psql","-d","petclinic","-f","populateDB.txt"])
p = builder.start()
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)
p.waitFor()


//stop db
builder.command(["install/pgsql/bin/pg_ctl","-D","install/pgsql/data","stop"] as String[])
Process p = builder.start()
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)
p.waitFor()

//copy config
//new AntBuilder().sequential{
	//copy(todir: "/var/lib/pgsql/data", file:"templates/pg_hba.conf",overwrite:true)
//}

