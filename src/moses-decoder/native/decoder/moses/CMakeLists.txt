
# documented as necessary for PhraseDictionaryTree, which MMT doesn't use (but who knows what else depends on these flags?)
add_definitions(-D_FILE_OFFSET_BITS=64 -D_LARGE_FILES)

set(MOSES_MOSES_SRC
AlignmentInfoCollection.cpp
AlignmentInfo.cpp
BaseManager.cpp
BitmapContainer.cpp
Bitmap.cpp
Bitmaps.cpp
ChartCellCollection.cpp
ChartCell.cpp
ChartHypothesisCollection.cpp
ChartHypothesis.cpp
ChartKBestExtractor.cpp
ChartManager.cpp
ChartParser.cpp
ChartRuleLookupManager.cpp
ChartTranslationOption.cpp
ChartTranslationOptionList.cpp
ChartTranslationOptions.cpp
ConfusionNet.cpp
DecodeGraph.cpp
DecodeStep.cpp
DecodeStepGeneration.cpp
DecodeStepTranslation.cpp
ExportInterface.cpp
FactorCollection.cpp
Factor.cpp
FactorTypeSet.cpp
FeatureVector.cpp

#FF/BleuScoreFeature.cpp
#FF/ConstrainedDecoding.cpp
#FF/ControlRecombination.cpp
#FF/CountNonTerms.cpp
#FF/CoveredReferenceFeature.cpp
FF/DecodeFeature.cpp
#FF/DeleteRules.cpp
FF/DistortionScoreProducer.cpp
FF/DynamicCacheBasedLanguageModel.cpp
##FF/Factory.cpp # MMT specific overrides in top level CMakeLists.txt
FF/FeatureFunction.cpp
FF/FFState.cpp
#FF/GlobalLexicalModel.cpp
#FF/GlobalLexicalModelUnlimited.cpp
#FF/HyperParameterAsWeight.cpp
FF/InputFeature.cpp
FF/InternalTree.cpp
FF/LexicalReordering/LexicalReordering.cpp
FF/LexicalReordering/LexicalReorderingState.cpp
FF/LexicalReordering/LexicalReorderingTable.cpp
FF/LexicalReordering/ReorderingStack.cpp
FF/LexicalReordering/SparseReordering.cpp
#FF/MaxSpanFreeNonTermSource.cpp
#FF/Model1Feature.cpp
#FF/NieceTerminal.cpp
#FF/OSM-Feature/KenOSM.cpp
#FF/OSM-Feature/OpSequenceModel.cpp
#FF/OSM-Feature/osmHyp.cpp
#FF/PhraseBoundaryFeature.cpp
#FF/PhraseLengthFeature.cpp
#FF/PhraseOrientationFeature.cpp
#FF/PhrasePairFeature.cpp
FF/PhrasePenalty.cpp
#FF/ReferenceComparison.cpp
#FF/RulePairUnlexicalizedSource.cpp
#FF/RuleScope.cpp
#FF/SetSourcePhrase.cpp
FF/SkeletonStatefulFF.cpp
FF/SkeletonStatelessFF.cpp
#FF/SoftMatchingFeature.cpp
#FF/SoftSourceSyntacticConstraintsFeature.cpp
#FF/SourceGHKMTreeInputMatchFeature.cpp
#FF/SourceWordDeletionFeature.cpp
#FF/SpanLength.cpp
#FF/SparseHieroReorderingFeature.cpp
FF/StatefulFeatureFunction.cpp
FF/StatelessFeatureFunction.cpp
#FF/SyntaxRHS.cpp
#FF/TargetBigramFeature.cpp
#FF/TargetConstituentAdjacencyFeature.cpp
#FF/TargetNgramFeature.cpp
#FF/TargetPreferencesFeature.cpp
#FF/TargetWordInsertionFeature.cpp
#FF/TreeStructureFeature.cpp
#FF/UnalignedWordCountFeature.cpp
FF/UnknownWordPenaltyProducer.cpp
FF/WordPenaltyProducer.cpp
#FF/WordTranslationFeature.cpp

File.cpp
FloydWarshall.cpp
GenerationDictionary.cpp
HypergraphOutput.cpp
Hypothesis.cpp
HypothesisStack.cpp
HypothesisStackCubePruning.cpp
HypothesisStackNormal.cpp
Incremental.cpp
InputFileStream.cpp
InputPath.cpp
InputType.cpp
LatticeMBR.cpp
LM/Backward.cpp
LM/BackwardLMState.cpp
LM/Base.cpp
LM/BilingualLM.cpp
LM/Implementation.cpp
#### LM/IRST.cpp
        LM/MMTInterpolatedLM.cpp
LM/Ken.cpp
LM/MultiFactor.cpp
LM/Multiplexer.cpp
LM/Remote.cpp
LM/SingleFactor.cpp
LM/SkeletonLM.cpp
LVoc.cpp
Manager.cpp
mbr.cpp
NonTerminal.cpp
OutputFileStream.cpp
Parameter.cpp
parameters/AllOptions.cpp
parameters/BookkeepingOptions.cpp
parameters/ContextParameters.cpp
parameters/CubePruningOptions.cpp
parameters/InputOptions.cpp
parameters/LMBR_Options.cpp
parameters/MBR_Options.cpp
parameters/NBestOptions.cpp
parameters/OOVHandlingOptions.cpp
parameters/OptionsBaseClass.cpp
parameters/ReorderingOptions.cpp
parameters/ReportingOptions.cpp
parameters/SearchOptions.cpp
parameters/ServerOptions.cpp
parameters/SyntaxOptions.cpp
PartialTranslOptColl.cpp
PCNTools.cpp
Phrase.cpp
PP/CountsPhraseProperty.cpp
PP/Factory.cpp
PP/NonTermContextProperty.cpp
PP/OrientationPhraseProperty.cpp
PP/PhraseProperty.cpp
PP/SourceLabelsPhraseProperty.cpp
PP/SpanLengthPhraseProperty.cpp
PP/TargetConstituentBoundariesLeftPhraseProperty.cpp
PP/TargetConstituentBoundariesRightAdjacentPhraseProperty.cpp
PP/TargetPreferencesPhraseProperty.cpp
PrefixTreeMap.cpp
Range.cpp
ReorderingConstraint.cpp
RuleCube.cpp
RuleCubeItem.cpp
RuleCubeQueue.cpp
ScoreComponentCollection.cpp
Search.cpp
SearchCubePruning.cpp
SearchNormal.cpp
Sentence.cpp
SentenceStats.cpp
SquareMatrix.cpp
StaticData.cpp
TabbedSentence.cpp
TargetPhraseCollection.cpp
TargetPhrase.cpp
ThreadPool.cpp
Timer.cpp
TranslationAnalysis.cpp
TranslationModel/PhraseDictionary.cpp
TranslationModel/PhraseDictionaryDynamicCacheBased.cpp
TranslationModel/PhraseDictionaryTree.cpp
TranslationModel/PhraseDictionaryTreeAdaptor.cpp
TranslationModel/PhraseDictionarySADB.cpp
PDTAimp.cpp
TranslationOptionCollectionConfusionNet.cpp
TranslationOptionCollection.cpp
TranslationOptionCollectionLattice.cpp
TranslationOptionCollectionText.cpp
TranslationOption.cpp
TranslationOptionList.cpp
TreeInput.cpp
TrellisPathCollection.cpp
TrellisPath.cpp
Util.cpp
Word.cpp
WordLattice.cpp
XmlOption.cpp
)

set(MOSES_TM_UG_SRC
TranslationModel/UG/generic/file_io/ug_stream.cpp
TranslationModel/UG/generic/program_options/ug_get_options.cpp
TranslationModel/UG/generic/program_options/ug_splice_arglist.cc
TranslationModel/UG/generic/threading/ug_thread_pool.cc
TranslationModel/UG/generic/threading/ug_thread_safe_counter.cc
TranslationModel/UG/mm/num_read_write.cc
TranslationModel/UG/mm/tpt_pickler.cc
TranslationModel/UG/mm/tpt_tightindex.cc
TranslationModel/UG/mm/tpt_tokenindex.cc
TranslationModel/UG/mm/ug_bitext.cc
TranslationModel/UG/mm/ug_bitext_jstats.cc
TranslationModel/UG/mm/ug_bitext_pstats.cc
TranslationModel/UG/mm/ug_conll_record.cc
TranslationModel/UG/mm/ug_corpus_token.cc
TranslationModel/UG/mm/ug_deptree.cc
TranslationModel/UG/mm/ug_http_client.cc
TranslationModel/UG/mm/ug_im_bitext.cc
TranslationModel/UG/mm/ug_lexical_reordering.cc
TranslationModel/UG/mm/ug_load_primer.cc
TranslationModel/UG/mm/ug_phrasepair.cc
TranslationModel/UG/mm/ug_tsa_array_entry.cc
TranslationModel/UG/mm/ug_ttrack_base.cc
TranslationModel/UG/mm/ug_ttrack_position.cc
TranslationModel/UG/TargetPhraseCollectionCache.cc
)

set(MOSES_TM_UG_MMSAPT_SRC
TranslationModel/UG/mmsapt.cpp
TranslationModel/UG/mm/ug_bitext_sampler.cc
TranslationModel/UG/mm/ug_sampling_bias.cc
)

execute_process(COMMAND git describe --dirty WORKING_DIRECTORY ${PROJECT_SOURCE_DIR} OUTPUT_VARIABLE MOSES_VERSION_ID OUTPUT_STRIP_TRAILING_WHITESPACE)

set_source_files_properties(Parameter.cpp PROPERTIES COMPILE_FLAGS -DMOSES_VERSION_ID=\\\"${MOSES_VERSION_ID}\\\")


# Group these objects together for later use. 
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#
add_library(moses_moses OBJECT ${MOSES_MOSES_SRC} ${MOSES_TM_UG_MMSAPT_SRC})

add_library(moses_tm_ug OBJECT ${MOSES_TM_UG_SRC})

add_subdirectory(decoder)
add_subdirectory(java)

# mtt-build, mmlex-build, symal2mam
# need boost, UG PT.

# also need
# * util::Exception from util/exception.cc
# * util::WallTime() from util/usage.cc
# * util::ToString() from util/integer_to_string.cc

add_library(moses_basic_utils OBJECT ../util/exception.cc ../util/integer_to_string.cc ../util/usage.cc)

set(MTT_EXECUTABLES mtt-build mmlex-build symal2mam)

foreach(exe ${MTT_EXECUTABLES})
  add_executable(${exe} TranslationModel/UG/mm/${exe}.cc $<TARGET_OBJECTS:moses_basic_utils> $<TARGET_OBJECTS:moses_tm_ug>)
  target_link_libraries(${exe} ${Boost_LIBRARIES} ${ZLIB_LIBRARIES} pthread)
endforeach(exe)
