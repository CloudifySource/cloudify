//Create a connection
connection = new java.net.URL('http://localhost:9200/_cluster/nodes/_local/_shutdown').openConnection()
connection.requestMethod = 'POST'
connection.setRequestProperty('Content-Type', 'application/x-www-form-urlencoded')
connection.setRequestProperty('Content-Length', '0')
connection.setRequestProperty('Content-Language', 'en-US')
connection.useCaches = false
connection.doInput = true
connection.doOutput = true 

//Send request
wr = new DataOutputStream( connection.getOutputStream ())
wr.writeBytes ''
wr.flush()
wr.close()

//Get Response
rd = new BufferedReader(new InputStreamReader(connection.getInputStream()))
while(rd.readLine() != null) {}
rd.close()

