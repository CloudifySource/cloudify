@Grab(group='org.hsqldb', module='hsqldb', version='2.2.4')
@GrabConfig(systemClassLoader=true)

sql = groovy.sql.Sql.newInstance("jdbc:hsqldb:hsql://localhost:9001", "SA", "", "org.hsqldb.jdbc.JDBCDriver")
sql.execute "SHUTDOWN"