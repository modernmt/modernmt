/**
 * This file is part of moses.  Its use is licensed under the GNU Lesser General
 * Public License version 2.1 or, at your option, any later version.
 *
 * Random subset generation.
 *
 * Author: David Madl <git@abanbytes.eu>
 */

#ifndef FISHERYATES_H
#define FISHERYATES_H

#include <boost/random/mersenne_twister.hpp>
#include <boost/random/uniform_int_distribution.hpp>
#include <boost/unordered_map.hpp>
#include <vector>
#include <algorithm>

namespace map_helper {
  /** Map access with default element value */
  inline size_t get(boost::unordered_map<size_t, size_t>& m, size_t key) {
    size_t defaultVal = key;
    boost::unordered_map<size_t, size_t>::iterator it = m.find(key);
    if(it != m.end())
      return it->second;
    else
      return defaultVal;
  }
}

/**
* Generates k random indices from the range [0,n)
* Efficient for the case k << n (partial Fisher-Yates shuffle).
*
* Outputs indices in 'out' in sorted order.
*
* Kudos to Nick Johnson: http://stackoverflow.com/a/6978109
*/
template<class Engine>
void random_indices(size_t k, size_t n, Engine& randomEngine, std::vector<size_t>& out) {
  using map_helper::get;
  // shuffled array representation: missing elements are implicitly equal to their key/index
  boost::unordered_map<size_t, size_t> state;
  size_t swap_with, t;

  state.reserve(k);

  for(size_t i = 0; i < k; i++) {
    // uniform_int_distribution takes a closed range [a,b]
    boost::random::uniform_int_distribution<size_t> uniform(i, n-1);
    swap_with = uniform(randomEngine);

    // swap state[i] with a random element 'swap_with'
    t = get(state, i);
    state[i] = get(state, swap_with);
    state[swap_with] = t;
  }

  out.clear();
  for(size_t i = 0; i < k; i++)
    out.push_back(state[i]);

  // sort choice, for potential memory access benefits
  // * for later use in memory access, sequential access may be faster
  // * BitextSampler<Token>::uniform_collect() now relies on our output being sorted
  std::sort(out.begin(), out.end());
}


#endif // FISHERYATES_H
