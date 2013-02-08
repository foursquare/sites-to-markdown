# Convert Google Sites to markdown

## Pre Requisits

Liberate your sites from google using [Google Sites Liberation](http://code.google.com/p/google-sites-liberation/)

Here's what it looks like for a google apps domain:

![Foursquare Liberation](http://cl.ly/image/1o3M3u2F3N3x/Image%202013.02.08%203:25:56%20PM.png)

**note: if you have two-factor authentication, you'll probably have to generate a password for this**

## Requirements

* java 1.7
* maven

## Usage

1. Make sure you're environment's jvm is 1.7
        # verify the version
        java -version
        # set the JAVA_HOME to the appropriate path (like this on osx)
        export JAVA_HOME=/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home
1. Run the conversion
        export MAVEN_OPTS=-Dfile.encoding=UTF-8
        mvn exec:java -Dexec.mainClass="jon.Convert" -Dexec.args="/path/to/exported/sites/ /path/to/markdown/destination"
1. Use [gollum](https://github.com/github/gollum) to host your new wiki!