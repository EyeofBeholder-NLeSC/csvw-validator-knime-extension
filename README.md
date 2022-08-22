# csvw-validator-knime-extension
This is a demo of implementing a csvw validator as a knime node.

Note that it is still in the testing stage. This means that if anyone want to see how it works, he/she will have to open this project in Eclipse and run it as a KNIME extension. 

# Some notes

The development of this project is block at this stage, since a detailed documentation of KNIME extension development is missing (there is only a quick guidance on KNIME's website showing how to setup the development env and how to make a very simple extension work out). 

In the `lib` directory, there are all the Java libraries that will be used as dependencies of the extension in runtime. Note that this list of dependencies can be extended. The reason of adding them as JAR files is that KNIME extensions don't support runtime dependency management using Maven. That means if a library needs to be added as a dependency, it has to be downloaded as a JAR file, saved with the project, and added to the classpath manually (set that in the runtime file of the project).
