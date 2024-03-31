cmake_minimum_required(VERSION 3.1.0)
project(application LANGUAGES C)

include(CTest)
enable_testing()

set(PROJECT_SOURCE_DIR ./src)

# set all files in project_source_dir to be source files
file(GLOB SRC_FILES ${PROJECT_SOURCE_DIR}/*.c)
set(EXECUTABLE_OUTPUT_PATH ${PROJECT_BINARY_DIR}/../)

include_directories(./include)

find_package(Threads REQUIRED)
include(cmake/CPM.cmake)
CPMAddPackage(
    NAME NVTX
    GITHUB_REPOSITORY NVIDIA/NVTX
    GIT_TAG v3.1.0-c-cpp
    GIT_SHALLOW TRUE)

add_executable(${PROJECT_NAME} ${SRC_FILES})
target_link_libraries(${PROJECT_NAME} Threads::Threads json-c nvtx3-c m)


# # print all cmake-property
# get_cmake_property(_variableNames VARIABLES)
# list (SORT _variableNames)
# foreach (_variableName ${_variableNames})
# message(STATUS "${_variableName}=${${_variableName}}")
# endforeach()
