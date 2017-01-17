#pragma once

#include <string>

#include <boost/shared_ptr.hpp>
#include <boost/unordered_map.hpp>

namespace Moses
{

class FeatureFactory;
class FeatureFunction;

/**
 * Base class for feature setup functor. Run by FeatureFactory after creating feature objects.
 */
class FeatureSetup {
public:
  virtual void operator()(FeatureFunction *feature) = 0;
};

class FeatureRegistry {
public:
  /** Uses default feature setup functor (register with FeatureFunction, StaticData). */
  FeatureRegistry();

  /** Use specific setup functor. */
  FeatureRegistry(boost::shared_ptr<FeatureSetup> setup);

  ~FeatureRegistry();

  void Construct(const std::string &name, const std::string &line);
  void PrintFF() const;

private:
  void AddFactories(FeatureSetup& setup);

  void Add(const std::string &name, FeatureFactory *factory);

  typedef boost::unordered_map<std::string, boost::shared_ptr<FeatureFactory> > Map;

  Map registry_;
  boost::shared_ptr<FeatureSetup> featureSetup_;
};

} // namespace Moses
