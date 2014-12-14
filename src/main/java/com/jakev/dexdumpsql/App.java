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

import com.jakev.dexdumpsql.DexDbHelper;

import java.io.File;
import java.io.IOException;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.DexFileFactory;

public class App 
{

    private static final String gProgramName = "DtfDumpSql";
    private static final String gCmdName = "Dtfdumpsql";
    private static final String gProgramVersion = "1.0";

    private static DexFile gDexFile = null;
    private static DexDbHelper gDexDb = null;

    private static final int METHOD_TYPE_DIRECT = 0;
    private static final int METHOD_TYPE_VIRTUAL = 1;


    /* Set to true for debugging. */
    private static final boolean DEBUG = true;

    private static void usage() {

        System.err.println(gProgramName+" v"+gProgramVersion+" Command Line Utility");
        System.err.println("Usage: "+gCmdName+" <input_dex> <output_db> <api_level>");
        System.err.println("");
    }

    /* From AOSP : dalvik/tools/dexdeps/src/com/android/dexdeps/Output.java */
    static String primitiveTypeLabel(char typeChar) {
        /* primitive type; substitute human-readable name in */
        switch (typeChar) {
            case 'B':   return "byte";
            case 'C':   return "char";
            case 'D':   return "double";
            case 'F':   return "float";
            case 'I':   return "int";
            case 'J':   return "long";
            case 'S':   return "short";
            case 'V':   return "void";
            case 'Z':   return "boolean";
            default:
                /* huh? */
                System.err.println("Unexpected class char " + typeChar);
                assert false;
                return "UNKNOWN";
        }
    }

    static String descriptorToDot(String descr) {
        int targetLen = descr.length();
        int offset = 0;
        int arrayDepth = 0;

        if (descr == null) {
            return null;
        }

        /* strip leading [s; will be added to end */
        while (targetLen > 1 && descr.charAt(offset) == '[') {
            offset++;
            targetLen--;
        }
        arrayDepth = offset;

        if (targetLen == 1) {
            descr = primitiveTypeLabel(descr.charAt(offset));
            offset = 0;
            targetLen = descr.length();
        } else {
            /* account for leading 'L' and trailing ';' */
            if (targetLen >= 2 && descr.charAt(offset) == 'L' &&
                descr.charAt(offset+targetLen-1) == ';')
            {
                targetLen -= 2;     /* two fewer chars to copy */
                offset++;           /* skip the 'L' */
            }
        }

        char[] buf = new char[targetLen + arrayDepth * 2];

        /* copy class name over */
        int i;
        for (i = 0; i < targetLen; i++) {
            char ch = descr.charAt(offset + i);
            buf[i] = (ch == '/' || ch == '$') ? '.' : ch;
        }

        /* add the appopriate number of brackets for arrays */
        while (arrayDepth-- > 0) {
            buf[i++] = '[';
            buf[i++] = ']';
        }
        assert i == buf.length;

        return new String(buf);
    }
    /* End from AOSP */

    private static boolean isFile(String filePathString) {

        File f = new File(filePathString);
        if(f.exists() && !f.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    private static int processDex() {

        int rtn = 0;
        int i = 0;

        /* Process each class */
        for (ClassDef classDef: gDexFile.getClasses()) {

            rtn |= processClass(i, classDef);
            i++;
        }

        return rtn;
    }

    private static int processClass(int classIdx, ClassDef classDef) {

        String superclassDescriptor = "";
        String classDescriptor = "";
        String superclassName = "";
        int accessFlags = 0;
        int rtn = 0;

        superclassName = classDef.getSuperclass();

        if (superclassName == null) {
            superclassDescriptor = "None";
        } 
        else {
            superclassDescriptor = descriptorToDot(superclassName);
        }

        classDescriptor = descriptorToDot(classDef.getType());
        accessFlags = classDef.getAccessFlags();

        if (DEBUG) {
            System.out.println("Adding class "+classDescriptor);
        }
        /* Add this class */
        rtn = gDexDb.addClass(classIdx, classDescriptor, accessFlags,
                                superclassDescriptor);
        if (rtn != 0) {
            
            System.err.println("[ERROR] Unable to add class '"+
                                classDescriptor+"' ("+
                                Integer.toString(rtn)+
                                ")");
            return rtn;
        }

        /* Process the static fields */
        for (Field field: classDef.getStaticFields()) {
            rtn |= processStaticField(classIdx, field);
        }

        /* Process the instance fields */
        for (Field field: classDef.getInstanceFields()) {
            rtn |= processInstanceField(classIdx, field);
        }

        /* Process virtual methods */
        for (Method method: classDef.getVirtualMethods()) {
            rtn |= processMethod(classIdx, METHOD_TYPE_VIRTUAL, method);
        }

        /* Process direct methods */
        for (Method method: classDef.getDirectMethods()) {
            rtn |= processMethod(classIdx, METHOD_TYPE_DIRECT, method);
        }

        return rtn;
    }

    public static int processStaticField(int classIdx, Field field) {

        int rtn = 0;
        int accessFlags = 0;
        String fieldName = "";
        String fieldType = "";

        fieldName = field.getName();
        fieldType = field.getType();
        accessFlags = field.getAccessFlags();

        rtn = gDexDb.addSField(fieldName, fieldType, accessFlags, classIdx);
        if (rtn != 0) {
            
            System.err.println("[ERROR] Unable to add static field '"+
                                fieldName+"' ("+
                                Integer.toString(rtn)+
                                ")");
        }
        return rtn;
    }

    public static int processInstanceField(int classIdx, Field field) {

        int rtn = 0;
        int accessFlags = 0;
        String fieldName = "";
        String fieldType = "";

        fieldName = field.getName();
        fieldType = field.getType();
        accessFlags = field.getAccessFlags();

        rtn = gDexDb.addIField(fieldName, fieldType, accessFlags, classIdx);
        if (rtn != 0) {
            
            System.err.println("[ERROR] Unable to add instance field '"+
                                fieldName+"' ("+
                                Integer.toString(rtn)+
                                ")");
        }
        return rtn;
    }

    public static int processMethod(int classIdx, int methodType, Method method) {

        int rtn = 0;
        int accessFlags = 0;

        String methodName = "";
        String methodDescriptor = "";       
        StringBuilder sb = null;

        methodName = method.getName();
        accessFlags = method.getAccessFlags();

        sb = new StringBuilder("(");
        for (MethodParameter param: method.getParameters()) {

            sb.append(param.getType());
        }
        sb.append(")");
        sb.append(method.getReturnType());

        methodDescriptor = sb.toString();

        rtn = gDexDb.addMethod(methodName, methodType, methodDescriptor, accessFlags, classIdx);
        if (rtn != 0) {

            System.err.println("[ERROR] Unable to add method '"+
                                methodName+"' ("+
                                Integer.toString(rtn)+
                                ")");
        }
        return rtn;

    }

    public static void main(String[] args) {

        int rtn = 0;
    
        String dexFileName = "";
        String dexDbName = "";
        int sdkVersion = 0;

        if (args.length != 3) {
            usage();
            System.exit(-2);
        }

        dexFileName = args[0];
        dexDbName = args[1];
        sdkVersion = Integer.parseInt(args[2]);

        if (!isFile(dexFileName)) {
            System.err.println("[ERROR] File '"+dexFileName+"' does not exist!");
            System.exit(-3);
        }

        if (DEBUG) { System.out.println("Loading DEX into object."); }
        try {
            gDexFile = DexFileFactory.loadDexFile(dexFileName, sdkVersion);
        } catch (IOException e){
            System.err.println("[ERROR] Unable to load DEX file!");
            System.exit(-4);
        }

        if (DEBUG) { System.out.println("Creating DexDbHelper."); }
        gDexDb = new DexDbHelper(dexDbName);

        if (DEBUG) { System.out.println("Droping data from DB (if exists)."); }
        rtn = gDexDb.dropTables();
        if (rtn != 0) {
            System.err.println("[ERROR] Error dropping tables!");
            System.exit(rtn);
        }
      
        if (DEBUG) { System.out.println("About to create tables..."); }
        rtn = gDexDb.createTables();
        if (rtn != 0) {
            System.err.println("[ERROR] Error creating tables!");
            System.exit(rtn);
        }

        if (DEBUG) { System.out.println("About to process DEX..."); }
        rtn = processDex();
        if (rtn != 0) {
            System.err.println("[ERROR] Error processing dex!");    
        }

        /* Close it down. */
        if (DEBUG) { System.out.println("Closing database."); }
        rtn = gDexDb.closeDatabase();
        if (rtn != 0) {
            System.err.println("[ERROR] Could not close database!");
            System.exit(rtn);
        }
        System.exit(rtn);
    }
}
