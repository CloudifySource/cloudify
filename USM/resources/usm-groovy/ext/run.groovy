
println "In run.groovy at " + new Date()
new File("marker.txt").write("MARKER")

sleep(5000)
println "End of run.groovy"