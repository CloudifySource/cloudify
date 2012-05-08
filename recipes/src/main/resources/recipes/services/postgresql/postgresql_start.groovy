
def Process p = new ProcessBuilder("install/pgsql/bin/postgres.exe","-D","install/pgsql/data").start();

p.consumeProcessErrorStream(System.err)
p.consumeProcessOutputStream(System.out)


p.waitFor()


