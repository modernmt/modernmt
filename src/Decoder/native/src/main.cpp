//
// Created by Davide  Caroselli on 14/01/16.
//

#include <stdio.h>
#include <iostream>

#include "wrapper/MosesDecoder.h"

using namespace JNIWrapper;

int main(int argc, const char *argv[]) {
  if (argc != 2) {
    printf("USAGE: jnitest MOSES_INI\n");
    return 1;
  }

  const char *moses_ini = argv[1];
  std::cout << "Moses INI: " << moses_ini << "\n";

  MosesDecoder *decoder = MosesDecoder::createInstance(moses_ini);

  // Features
  std::vector<feature_t> features = decoder->getFeatures();
  for (int i = 0; i < features.size(); i++) {
    std::cout << "Feature: " << features[i].name << " ";
    std::vector<float> weights = decoder->getFeatureWeights(features[i]);

    for (int j = 0; j < weights.size(); j++)
      std::cout << weights[j] << " ";

    std::cout << "\n";
  }
  std::cout << "\n";

  // Translation
  std::string text = "just a simple text message";
  translation_t translation;

  std::map<std::string, float> ibm;;
  ibm["ibm"] = 1.f;
  std::map<std::string, float> europarl;
  europarl["europarl"] = 1.f;

  translation = decoder->translate(text, 0, NULL, 0);
  std::cout << "Translation: " << translation.text << "\n";
  std::cout << "Alignment: ";
  for(std::vector<std::pair<size_t, size_t> >::iterator it = translation.alignment.begin(); it != translation.alignment.end(); it++)
    std::cout << it->first << "-" << it->second << " ";
  std::cout << "\n";
  translation = decoder->translate(text, 0, &ibm, 0);
  std::cout << "Translation IBM: " << translation.text << "\n";
  translation = decoder->translate(text, 0, &europarl, 0);
  std::cout << "Translation EUR: " << translation.text << "\n";
  std::cout << "\n";

  int64_t ibmSession = decoder->openSession(ibm);
  int64_t europarlSession = decoder->openSession(europarl);

  std::cout << "Opened Translation Session IBM ID: " << ibmSession << "\n";
  std::cout << "Opened Translation Session EUR ID: " << europarlSession << "\n";

  translation = decoder->translate(text, ibmSession, NULL, 0);
  std::cout << "Translation Session IBM: " << translation.text << "\n";
  translation = decoder->translate(text, europarlSession, NULL, 0);
  std::cout << "Translation Session EUR: " << translation.text << "\n";
  std::cout << "\n";

  translation = decoder->translate(text, 0, NULL, 10);
  for (int i = 0; i < translation.hypotheses.size(); i++) {
    hypothesis_t hyp = translation.hypotheses[i];
    std::cout << "Translation HYP: " << hyp.text << " ||| " << hyp.fvals << " ||| " << hyp.score << "\n";
  }

  return 0;
}
