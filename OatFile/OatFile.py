#!/usr/bin/env python
#
# DTF Core Content
# Copyright 2013-2018 Jake Valletta (@jake_valletta)
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
"""Read and Parse OAT Files"""

import StringIO
import struct

from elftools.common.py3compat import  bytes2str
from elftools.elf.elffile import ELFFile
from elftools.elf.sections import SymbolTableSection
from elftools.elf.descriptions import describe_p_type

import dtf.logging as log

OAT_DATA_SYMBOL = "oatdata"
P_TYPE_PHDR = "PHDR"
MAX_FILE_LOCATION_DATA = 256

LIB_TAG = "OatFile"


class OatFileException(Exception):

    """Generic exception"""

    def __init__(self, message):

        """Generate new exception"""

        Exception.__init__(self, message)


class Value(object):

    """Store value with offset"""

    value = None
    offset = 0

    def __init__(self, value, offset):

        """Store value with offset"""

        self.value = value
        self.offset = offset

    def __repr__(self):

        """Should always return value"""

        return self.value

    def __str__(self):

        """Return value, let caller cast"""

        return self.value

    def __int__(self):

        """Return (int) value, let caller cast"""

        return self.value


class DexHeader(object):

    """Object representation of DEX header"""

    file_f = None

    start_offset = 0
    end_offset = 0
    file_location = ''
    dex_offset = 0
    dex_size = 0
    checksum = 0
    class_defs_size = None
    class_offset_size = None

    def __init__(self, file_f, offset):

        """Initialize dex header"""

        self.file_f = file_f
        self.start_offset = offset

    def read_dex_bytes(self):

        """Carve and return a DEX file"""

        if self.dex_size.value < 0:
            raise OatFileException("Unable to get DEX size.")

        dex_bytes_f = StringIO.StringIO()
        self.file_f.seek(self.dex_offset)
        read_bytes = 0

        while read_bytes < self.dex_size.value:
            if read_bytes + 1024 > self.dex_size.value:
                read_size = self.dex_size.value - read_bytes
            else:
                read_size = 1024

            write_bytes = self.file_f.read(read_size)

            dex_bytes_f.write(write_bytes)

            read_bytes += read_size

        return dex_bytes_f


class OatElf(object):

    """OAT ELF binary file class"""

    samsung_mode = False

    def __init__(self, file_f, samsung_mode=False):

        """Class initialization"""

        self.samsung_mode = samsung_mode
        self.file_f = file_f
        self.elffile = ELFFile(file_f)
        self.oat_data_offset = self.__get_oat_data_sym() - self.__get_base_offset()

        log.d(LIB_TAG, "OAT Data offset: %s" % self.oat_data_offset)

        self.magic = self.__read_string(self.oat_data_offset, 4)
        self.version = self.__parse_version()
        self.dex_file_count = self.__read_uint32(self.oat_data_offset + 20)

        log.d(LIB_TAG, "Magic: %s" % repr(self.magic))
        log.d(LIB_TAG, "Version: %d" % self.version)
        log.d(LIB_TAG, "DEX File Count: %d" % self.dex_file_count)

        # The rest of the OAT header depends on the version
        if self.version < 64:
            self.key_value_store_size = self.__read_uint32(self.oat_data_offset + 80)
            self.dex_header_start = (self.key_value_store_size
                                     + self.oat_data_offset + 84)
        else:
            self.key_value_store_size = self.__read_uint32(self.oat_data_offset + 68)
            self.dex_header_start = (self.key_value_store_size.value
                                     + self.oat_data_offset + 72)

        log.d(LIB_TAG, "Key/Value Size: %i" % self.key_value_store_size)
        log.d(LIB_TAG, "First DEX Header:  0x%4x" % self.dex_header_start)

        i = 0
        start = self.dex_header_start
        self.dex_headers = list()

        while i < self.dex_file_count.value:

            dex_header = self.parse_dex_header(start)
            if dex_header is None:
                log.e(LIB_TAG, "Unable to parse header for DEX file %d!" % i)
                raise Exception("DEX header parse failed")

            self.dex_headers.append(dex_header)
            start = dex_header.end_offset
            i += 1


    def __read_uint32(self, offset):

        """Read 32-bit int at offset"""

        self.file_f.seek(offset)
        uint32 = struct.unpack('I', self.file_f.read(4))[0]

        return Value(uint32, offset)

    def __read_string(self, offset, length):

        """Read string of length length at offset"""

        self.file_f.seek(offset)
        string = struct.unpack("%is" % length, self.file_f.read(length))[0]

        return Value(string, offset)

    def __get_base_offset(self):

        """Get the base offset"""

        if self.elffile.num_segments() == 0:
            log.e(LIB_TAG, "Unable to read program header!")
            raise BufferError

        for segment in self.elffile.iter_segments():
            if describe_p_type(segment['p_type']) == P_TYPE_PHDR:

                p_offset = segment['p_offset']
                p_vaddr = segment['p_vaddr']

                return  p_vaddr - p_offset

        log.e(LIB_TAG, "Unable to find base address!")
        raise BufferError

    def __get_oat_data_sym(self):

        """Get OAT data sym offset"""

        for section in self.elffile.iter_sections():
            if not isinstance(section, SymbolTableSection):
                continue

            if section['sh_entsize'] == 0:
                log.e(LIB_TAG, "Could not find any symbol table!")
                return -1

            for _, symbol in enumerate(section.iter_symbols()):

                name = bytes2str(symbol.name)
                if name == OAT_DATA_SYMBOL:
                    return symbol['st_value']

    def __parse_version(self):

        """Parse out the version"""

        raw_version = self.__read_string(self.oat_data_offset + 4, 4)

        return int(raw_version.value.strip('\0'))

    def parse_dex_header(self, off):

        """Parse a DEX header"""

        dex_header = DexHeader(self.file_f, off)

        log.d(LIB_TAG, "Parsing OatDexHeader @ 0x%4x" % off)

        dex_file_location_size = self.__read_uint32(off)
        off += 4

        log.d(LIB_TAG, "Length of location name string : %i"
              % dex_file_location_size)

        dex_header.file_location = self.__read_string(off, dex_file_location_size.value)

        off += dex_file_location_size.value

        # Check to make sure we're starting on the correct offset.
        if len(dex_header.file_location.value) > MAX_FILE_LOCATION_DATA:

            log.e(LIB_TAG, "Unsually large location name detected")
            return None

        log.d(LIB_TAG, "Dex file location string : %s" % dex_header.file_location)

        dex_header.checksum = self.__read_uint32(off)
        off += 4

        log.d(LIB_TAG, "Dex file location checksum : 0x%4x"
              % dex_header.checksum)

        dex_file_pointer = self.__read_uint32(off)
        off += 4

        log.d(LIB_TAG, "Dex file pointer : %i" % dex_file_pointer)

        # beginning of "oatdata" section + offset is the dex file.
        dex_header.dex_offset = dex_file_pointer.value + self.oat_data_offset
        dex_header.dex_size = self.__read_uint32(dex_header.dex_offset + 32)

        log.d(LIB_TAG, "DEX code located at 0x%4x, size 0x%4x"
              % (dex_header.dex_offset, dex_header.dex_size))

        # Samsung added methods_offsets_, which is uint32_t
        if self.samsung_mode:
            off += 4

        dex_header.class_defs_size = self.__read_uint32(dex_header.dex_offset + 96)
        dex_header.class_offset_size = (dex_header.class_defs_size.value * 4)
        off += dex_header.class_offset_size

        log.d(LIB_TAG, "Class defs size: %i" % dex_header.class_defs_size)
        log.d(LIB_TAG, "Class offset size : %i" % dex_header.class_offset_size)

        dex_header.end_offset = off

        return dex_header
