/*
 * Android DEX to Sqlite3 DB utility
 * Copyright 2013-2014 Jake Valletta (@jake_valletta)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jakev.dexdumpsql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class DexDbHelper {

    public Connection con = null; 
    public Statement stmt = null;

    public DexDbHelper(String outputFileName ) {

        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:"+outputFileName);
            con.setAutoCommit(false);
            stmt = con.createStatement();
            stmt.setQueryTimeout(30);

        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] SQLite Java bindings not found! Exiting!");
            System.exit(-1);
        } catch (SQLException e) {
            System.err.println("[ERROR] "+e.getClass().getName()+": "+e.getMessage());
            System.exit(-2);
        }
    }

    public int createTables() {

        int rtn = 0;
        String sql = "";        

        try {
            /* Classes Table */
            sql = "CREATE TABLE classes " +
                         "(id INTEGER PRIMARY KEY NOT NULL," +
                         " name           TEXT    NOT NULL," + 
                         " access_flags   INTEGER NOT NULL," + 
                         " superclass     TEXT    NOT NULL)";
            stmt.executeUpdate(sql);

            /* Static Fields Table */
            sql = "CREATE TABLE static_fields " +
                         "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                         " name           TEXT    NOT NULL," + 
                         " type           TEXT    NOT NULL," + 
                         " access_flags   INTEGER NOT NULL," + 
                         " class_id       INTEGER NOT NULL," +
                         " FOREIGN KEY(class_id) REFERENCES class(id))";
            stmt.executeUpdate(sql);

            /* Instance Fields Table */
            sql = "CREATE TABLE instance_fields " +
                         "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                         " name           TEXT    NOT NULL," + 
                         " type           TEXT    NOT NULL," + 
                         " access_flags   INTEGER NOT NULL," + 
                         " class_id       INTEGER NOT NULL," +
                         " FOREIGN KEY(class_id) REFERENCES class(id))";
            stmt.executeUpdate(sql);

            /* Methods Table */
            sql = "CREATE TABLE methods " +
                         "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                         " name          TEXT    NOT NULL," + 
                         " type          TEXT    NOT NULL," + 
                         " descriptor    TEXT    NOT NULL," + 
                         " access_flags  INTEGER NOT NULL," + 
                         " class_id      INTEGER NOT NULL," +
                         " FOREIGN KEY(class_id) REFERENCES class(id))";
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int dropTables() {

        int rtn = 0;

        try {
            stmt.executeUpdate("DROP TABLE IF EXISTS classes");
            stmt.executeUpdate("DROP TABLE IF EXISTS static_fields");
            stmt.executeUpdate("DROP TABLE IF EXISTS instance_fields");
            stmt.executeUpdate("DROP TABLE IF EXISTS methods");

        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int closeDatabase() {

        int rtn = 0;
        
        try {
            con.close();
        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int addClass(int classIdx, String classDescriptor,
                        int accessFlags, String superclassDescriptor) {

        int rtn = 0;

        try {
            String sql = "INSERT INTO classes (id, name, access_flags, superclass) "+
                         "VALUES ("+
                          Integer.toString(classIdx)+", '"+
                          classDescriptor+"', "+
                          Integer.toString(accessFlags)+", '"+
                          superclassDescriptor+"')";
            stmt.executeUpdate(sql);
            con.commit();
        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int addSField(String fieldName, String fieldType,
                         int accessFlags, int classIdx) {

        int rtn = 0;

        try {
            String sql = "INSERT INTO static_fields (name, type, access_flags, class_id) "+
                         "VALUES ('"+
                          fieldName+"', '"+
                          fieldType+"', "+
                          Integer.toString(accessFlags)+", "+
                          Integer.toString(classIdx)+")";
            stmt.executeUpdate(sql);
            con.commit();
        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int addIField(String fieldName, String fieldType,
                         int accessFlags, int classIdx) {

        int rtn = 0;

        try {
            String sql = "INSERT INTO instance_fields (name, type, access_flags, class_id) "+
                         "VALUES ('"+
                          fieldName+"', '"+
                          fieldType+"', "+
                          Integer.toString(accessFlags)+", "+
                          Integer.toString(classIdx)+")";
            stmt.executeUpdate(sql);
            con.commit();
        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }      

    public int addMethod(String methodName, int methodType,
                         String methodDescriptor, int accessFlags,
                         int classIdx) {

        int rtn = 0;

        try {
            String sql = "INSERT INTO methods (name, type, descriptor, access_flags, class_id)"+
                         "VALUES ('"+
                         methodName+"', "+
                         Integer.toString(methodType)+", '"+
                         methodDescriptor+"', "+
                         Integer.toString(accessFlags)+", "+
                         Integer.toString(classIdx)+")";
            stmt.executeUpdate(sql);
            con.commit();
        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }
}
