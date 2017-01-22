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

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.List;

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

            /* Strings Table */
            sql = "CREATE TABLE strings " +
                         "(id INTEGER PRIMARY KEY NOT NULL," +
                         " name           TEXT    NOT NULL)";
            stmt.executeUpdate(sql);

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
            stmt.executeUpdate("DROP TABLE IF EXISTS strings");
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

    public int addStrings(List<String> stringValues) {

        int i = 0;
        int rtn = 0;
        String sql = "INSERT INTO strings (name) VALUES (?)";

        try {
            PreparedStatement pStmt = con.prepareStatement(sql);

            for (String stringValue : stringValues) {
                pStmt.setString(1, stringValue);
                pStmt.addBatch();
                i++;

                if (i % 1000 == 0 || i == stringValues.size()) {
                    pStmt.executeBatch(); // Execute every 1000 items.
                }
            }
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

    public int addStaticFields(ClassDef classDef, int classIdx) {

        int rtn = 0;
        String sql = "INSERT INTO static_fields (name, type, access_flags, class_id) " +
                     "VALUES (?, ?, ?, "+Integer.toString(classIdx)+")";

        try {
            PreparedStatement pStmt = con.prepareStatement(sql);

            for (Field field: classDef.getStaticFields()) {

                pStmt.setString(1, field.getName());
                pStmt.setString(2, field.getType());
                pStmt.setInt(3, field.getAccessFlags());

                pStmt.executeUpdate();

                con.commit();
            }

        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int addInstanceFields(ClassDef classDef, int classIdx) {

        int rtn = 0;
        String sql = "INSERT INTO instance_fields (name, type, access_flags, class_id) " +
                     "VALUES (?, ?, ?, "+Integer.toString(classIdx)+")";

        try {
            PreparedStatement pStmt = con.prepareStatement(sql);

            for (Field field: classDef.getInstanceFields()) {

                pStmt.setString(1, field.getName());
                pStmt.setString(2, field.getType());
                pStmt.setInt(3, field.getAccessFlags());

                pStmt.executeUpdate();

                con.commit();
            }

        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int addVirtualMethods(ClassDef classDef, int classIdx, int methodType) {

        int rtn = 0;
        int accessFlags = 0;
        String methodName = "";
        String methodDescriptor = "";
        String sql = "INSERT INTO methods (name, type, descriptor, access_flags, class_id) " +
                     "VALUES (?, ?, ?, ?, "+Integer.toString(classIdx)+")";

        StringBuilder sb = null;

        try {
            PreparedStatement pStmt = con.prepareStatement(sql);

            for (Method method: classDef.getVirtualMethods()) {

                sb = new StringBuilder("(");
                for (MethodParameter param: method.getParameters()) {

                    sb.append(param.getType());
                }
                sb.append(")");
                sb.append(method.getReturnType());

                methodDescriptor = sb.toString();

                pStmt.setString(1, method.getName());
                pStmt.setInt(2, methodType);
                pStmt.setString(3, methodDescriptor);
                pStmt.setInt(4, method.getAccessFlags());

                pStmt.executeUpdate();

                con.commit();
            }

        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }

    public int addDirectMethods(ClassDef classDef, int classIdx, int methodType) {

        int rtn = 0;
        int accessFlags = 0;
        String methodName = "";
        String methodDescriptor = "";
        String sql = "INSERT INTO methods (name, type, descriptor, access_flags, class_id) " +
                     "VALUES (?, ?, ?, ?, "+Integer.toString(classIdx)+")";

        StringBuilder sb = null;

        try {
            PreparedStatement pStmt = con.prepareStatement(sql);

            for (Method method: classDef.getDirectMethods()) {

                sb = new StringBuilder("(");
                for (MethodParameter param: method.getParameters()) {

                    sb.append(param.getType());
                }
                sb.append(")");
                sb.append(method.getReturnType());

                methodDescriptor = sb.toString();

                pStmt.setString(1, method.getName());
                pStmt.setInt(2, methodType);
                pStmt.setString(3, methodDescriptor);
                pStmt.setInt(4, method.getAccessFlags());

                pStmt.executeUpdate();

                con.commit();
            }

        } catch (SQLException e) {
            System.err.println(e);
            rtn = -1;
        }

        return rtn;
    }
}
