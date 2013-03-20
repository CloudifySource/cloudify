#! /usr/bin/env groovy

outputFile = "/tmp/debugOut${new Date().toString().split(" ")[3]}"
new File(outputFile).createNewFile()
System.out = new PrintStream(new FileOutputStream(outputFile))

println("Welcome to printContext.groovy\n")

import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.context.ServiceContext

ServiceContext context = ServiceContextFactory.getServiceContext()
println("\nBelow are the context properties for this instance:\n${context}")

println()
context.getProperties().each{println it}