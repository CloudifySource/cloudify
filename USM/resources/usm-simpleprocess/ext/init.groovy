println "init fired"

println "init fired2"

if(args == null) {
	throw new Exception("Expected three command line argument, got null")
}
if(args.length != 3) {
	throw new Exception("Expected three command line argument, got " + args.length)
}
println args[0]
println args[1]
println args[2]


