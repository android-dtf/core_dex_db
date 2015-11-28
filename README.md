DexDumpSql
==========

Introduction
------------
Process all strings, classes, methods, and fields of a DEX file into a SQL database for fast querying.

Building
--------
Building requires Maven. To build:

    user@system$ mvn package

Output will be at `target/DexDumpSql-*.jar`.

Usage
-----
Create a DEX database for the APK com.example.apk:

```
analyst$ java -jar DexDumpSql-*.jar -a 22 -i com.example.apk -o com.example.db
```
