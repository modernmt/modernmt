#include "StatefulFeatureFunction.h"

namespace Moses
{

std::vector<const StatefulFeatureFunction*>  StatefulFeatureFunction::m_statefulFFs;

StatefulFeatureFunction
::StatefulFeatureFunction(const std::string &line)
  : FeatureFunction(line)
{
}

StatefulFeatureFunction
::StatefulFeatureFunction(size_t numScoreComponents, const std::string &line)
  : FeatureFunction(numScoreComponents, line)
{
}

}

