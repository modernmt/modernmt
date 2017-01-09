set(MERT_SRC
        Util.cpp
        GzFileBuf.cpp
        FileStream.cpp
        Timer.cpp
        ScoreStats.cpp
        ScoreArray.cpp
        ScoreData.cpp
        ScoreDataIterator.cpp
        FeatureStats.cpp
        FeatureArray.cpp
        FeatureData.cpp
        FeatureDataIterator.cpp
        ForestRescore.cpp
        HopeFearDecoder.cpp
        Hypergraph.cpp
        MiraFeatureVector.cpp
        MiraWeightVector.cpp
        HypPackEnumerator.cpp
        Data.cpp
        BleuScorer.cpp
        BleuDocScorer.cpp
        SemposScorer.cpp
        SemposOverlapping.cpp
        InterpolatedScorer.cpp
        Point.cpp
        PerScorer.cpp
        HwcmScorer.cpp
        InternalTree.cpp
        Scorer.cpp
        ScorerFactory.cpp
        Optimizer.cpp
        OptimizerFactory.cpp
        TER/alignmentStruct.cpp
        TER/hashMap.cpp
        TER/hashMapStringInfos.cpp
        TER/stringHasher.cpp
        TER/terAlignment.cpp
        TER/terShift.cpp
        TER/hashMapInfos.cpp
        TER/infosHasher.cpp
        TER/stringInfosHasher.cpp
        TER/tercalc.cpp
        TER/tools.cpp
        TER/bestShiftStruct.cpp
        TerScorer.cpp
        CderScorer.cpp
        MeteorScorer.cpp
        Vocabulary.cpp
        PreProcessFilter.cpp
        SentenceLevelScorer.cpp
        Permutation.cpp
        PermutationScorer.cpp
        StatisticsBasedScorer.cpp)

set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost REQUIRED COMPONENTS
        system
        thread
        filesystem
        unit_test_framework
        program_options
        iostreams
        )
find_package(ZLIB REQUIRED)

include_directories(${Boost_INCLUDE_DIRS})

add_library(mertc SHARED ${MERT_SRC} ../moses/ThreadPool.cpp $<TARGET_OBJECTS:kenlm_util>)
target_link_libraries(mertc ${Boost_LIBRARIES} ${ZLIB_LIBRARIES} pthread)

set(MERT_EXECUTABLES evaluator extractor kbmira mert pro)

foreach (exe ${MERT_EXECUTABLES})
    add_executable(${exe} ${exe}.cpp)
    target_link_libraries(${exe} ${Boost_LIBRARIES} ${ZLIB_LIBRARIES} mertc)
endforeach (exe)