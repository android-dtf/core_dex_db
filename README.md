DEX Database Utils
==================
A collection of `dtf` modules and helpers for manipulating DEX databases.

Contents
--------

### appdexdb
Utility for creating and processing DEX databases for applications.

### frameworkdexdb
Utility for creating and processing DEX databases for frameworks.

### classsearch
Utility for searching DEX databases for classes, strings, etc.

### dexdiff.py
Utility for comparing DEX databases.

### DexDumpSQL
Java library for processing strings, classes, methods, and fields of a DEX file into a SQL database for fast querying.

#### Building
Building requires Maven. To build:

    user@system$ mvn package

Output will be at `target/DexDumpSql-*.jar`.

#### Usage
Create a DEX database for the APK com.example.apk:

```
analyst$ java -jar DexDumpSql-*.jar -a 22 -i com.example.apk -o com.example.db
```
