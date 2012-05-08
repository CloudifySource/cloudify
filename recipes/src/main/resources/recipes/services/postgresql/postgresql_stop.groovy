import groovy.util.ConfigSlurper



def builder = new ProcessBuilder()
builder.command(["install/pgsql/bin/pg_ctl","-D","install/pgsql/data","stop"] as String[])

Process p = builder.start()
p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)


p.waitFor()


