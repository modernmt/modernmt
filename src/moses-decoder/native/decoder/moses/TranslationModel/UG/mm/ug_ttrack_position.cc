// -*- mode: c++; indent-tabs-mode: nil; tab-width:2  -*-
#include "ug_ttrack_position.h"
namespace sapt
{
  namespace ttrack
  {
    Position::Position() : sid(0), offset(0) {};
    Position::Position(tpt::id_type _sid, tpt::offset_type _off) : sid(_sid), offset(_off) {};

  }
}
