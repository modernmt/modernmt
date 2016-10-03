
set(TEST_SOURCES
        util/NGramTable.cpp util/NGramTable.h)

file(GLOB textcases *.cpp)
foreach (testcase ${textcases})
    get_filename_component(exe ${testcase} NAME_WE)
    add_executable(${exe} ${testcase} ${TEST_SOURCES})
    target_link_libraries(${exe} ${PROJECT_NAME})
endforeach ()