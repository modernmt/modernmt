
# documented as necessary for PhraseDictionaryTree, which MMT doesn't use (but who knows what else depends on these flags?)
add_definitions(-D_FILE_OFFSET_BITS=64 -D_LARGE_FILES)

set(MOSES_MOSES_SRC
AlignmentInfoCollection.cpp
AlignmentInfo.cpp
BaseManager.cpp
BitmapContainer.cpp
Bitmap.cpp
Bitmaps.cpp
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
FF/Factory.cpp

File.cpp
FloydWarshall.cpp
GenerationDictionary.cpp
Hypothesis.cpp
HypothesisStack.cpp
HypothesisStackCubePruning.cpp
HypothesisStackNormal.cpp
InputFileStream.cpp
InputPath.cpp
InputType.cpp
IOWrapper.cpp
LatticeMBR.cpp
LM/Base.cpp
LM/Implementation.cpp
LM/MMTInterpolatedLM.cpp
LM/SingleFactor.cpp
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
PrefixTreeMap.cpp
Range.cpp
ReorderingConstraint.cpp
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
TranslationModel/PhraseDictionarySADB.cpp
TranslationOptionCollectionConfusionNet.cpp
TranslationOptionCollection.cpp
TranslationOptionCollectionLattice.cpp
TranslationOptionCollectionText.cpp
TranslationOption.cpp
TranslationOptionList.cpp
TranslationTask.cpp
TreeInput.cpp
TrellisPathCollection.cpp
TrellisPath.cpp
Util.cpp
Word.cpp
WordLattice.cpp
XmlOption.cpp
)

execute_process(COMMAND git describe --dirty WORKING_DIRECTORY ${PROJECT_SOURCE_DIR} OUTPUT_VARIABLE MOSES_VERSION_ID OUTPUT_STRIP_TRAILING_WHITESPACE)

set_source_files_properties(Parameter.cpp PROPERTIES COMPILE_FLAGS -DMOSES_VERSION_ID=\\\"${MOSES_VERSION_ID}\\\")


# Group these objects together for later use. 
#
# Given add_library(foo OBJECT ${my_foo_sources}),
# refer to these objects as $<TARGET_OBJECTS:foo>
#
add_library(moses_moses OBJECT ${MOSES_MOSES_SRC})

add_subdirectory(decoder)
add_subdirectory(java)
