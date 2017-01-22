#!/usr/bin/env python
# Copyright 2013-2016 Jake Valletta (@jake_valletta)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
"""Limited API for interacting with DEX databases"""

import dtf.logging as log

import os.path
import sqlite3

LIB_TAG = "DexDb"

# Exceptions
# Exceptions
class DexDbException(Exception):

    """Exception related to DEX DB"""

    def __init__(self, message):

        """Initialize exception"""

        # Call the base class constructor with the parameters it needs
        Exception.__init__(self, message)

# Class Class
class Class(object):

    """Class representation in DEX DB"""

    _id = 0
    name = ''
    access_flags = 0
    superclass = ''

    def __init__(self, class_id, name, access_flags, superclass):

        """Initialize Class object"""

        self._id = class_id
        self.name = name
        self.access_flags = access_flags
        self.superclass = superclass

# TODO
class Method(object):

    """Method representation of DEX DB"""

class Field(object):

    """Field representation of DEX DB"""

    _id = 0
    name = ""
    access_flags = 0
    class_id = 0

    def __init__(self, field_id, name, access_flags, class_id):

        """Initialize Field object"""

        self._id = field_id
        self.name = name
        self.access_flags = access_flags
        self.class_id = class_id

# DB Class
class DexDb(object):

    """Class for querying DEX databases"""

    db_file_path = None
    db_conn = None

    def __init__(self, db_file_path, safe=False):

        """Init DB connection"""

        # Make sure the DB exists, don't create it.
        if safe and not os.path.isfile(db_file_path):
            raise DexDbException("Database file not found: %s" % db_file_path)

        self.db_file_path = db_file_path
        self.db_conn = sqlite3.connect(db_file_path)

        self.db_conn.text_factory = str

    def get_db_name(self):

        """Return the name of the DB file"""

        return os.path.basename(self.db_file_path)

    def get_strings(self):

        """Get a list of strings in the DB"""

        string_list = list()

        cursor = self.db_conn.cursor()
        sql = ('SELECT name '
               'FROM strings')

        try:
            for line in cursor.execute(sql):
                string_list.append(line[0])

        except sqlite3.OperationalError as inst:

            log.e(LIB_TAG, "Error getting strings: %s" % inst)
            return None

        return string_list

    # TODO
    def get_classes(self):

        """Get list of classes"""

        class_list = list()

        cursor = self.db_conn.cursor()
        sql = ('SELECT id, name, access_flags, superclass '
               'FROM classes')

        try:
            for line in cursor.execute(sql):

                class_id = int(line[0])
                name = line[1]
                access_flags = int(line[2])
                superclass = line[3]

                class_list.append(Class(class_id, name, access_flags,
                                         superclass))

        except sqlite3.OperationalError as inst:

            log.e(LIB_TAG, "Error getting classes: %s" % inst)
            return None

        return class_list

    # TODO
    def get_class_static_fields(self, clazz):

        """Get static fields of a class"""

        pass

    # TODO
    def get_class_instance_fields(self, clazz):

        """Get instance fields of a class"""

    # TODO
    def get_class_methods(self, clazz):

        """Get methods of a class"""

        pass
