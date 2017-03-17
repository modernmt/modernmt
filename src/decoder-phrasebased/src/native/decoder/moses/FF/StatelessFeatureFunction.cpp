#include "StatelessFeatureFunction.h"

namespace Moses
{

std::vector<const StatelessFeatureFunction*> StatelessFeatureFunction::m_statelessFFs;

StatelessFeatureFunction
::StatelessFeatureFunction(const std::string &line)
  : FeatureFunction(line)
{
}

StatelessFeatureFunction
::StatelessFeatureFunction(size_t numScoreComponents, const std::string &line)
  : FeatureFunction(numScoreComponents, line)
{
}

}

