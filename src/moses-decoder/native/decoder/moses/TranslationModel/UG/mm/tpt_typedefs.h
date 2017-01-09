// -*- mode: c++; indent-tabs-mode: nil; tab-width:2  -*-
// Basic type definitions for code related to tightly packed tries
// (c) 2006-2012 Ulrich Germann

#ifndef __tpt_typedefs_h
#define __tpt_typedefs_h
#include <stdint.h>
namespace tpt
{
  typedef uint32_t      id_type;
  typedef uint8_t   offset_type;
  typedef uint32_t   count_type;
  typedef uint64_t filepos_type;
  typedef unsigned char   uchar;

  // magic = ''.join(reversed(['%02x' % ord(c) for c in 'SaptIDX2']))
  const uint64_t INDEX_V2_MAGIC = 0x3258444974706153ULL; // magic number for encoding index file version. ASCII 'SaptIDX2'
}
#endif
