# handle the QUIETLY and REQUIRED arguments and set OPENMP_FOUND to TRUE if
# all listed variables are TRUE
include(FindPackageHandleStandardArgs)

set(MyOpenMP_LIBRARY_DIR "/opt/local/lib/libomp/")
set(MyOpenMP_INCLUDE_DIR "/opt/local/include/libomp/")
set(MyOpenMP_CXX_FLAGS "-lomp")
set(MyOpenMP_EXE_LINKER_FLAGS "-lomp")

find_package_handle_standard_args(MyOpenMP REQUIRED_VARS MyOpenMP_INCLUDE_DIR)
find_package_handle_standard_args(MyOpenMP REQUIRED_VARS MyOpenMP_LIBRARY_DIR)

mark_as_advanced(MyOpenMP_INCLUDE_DIR MyOpenMP_LIBRARY_DIR MyOpenMP_EXE_LINKER_FLAGS MyOpenMP_CXX_FLAGS)
