cmake_minimum_required(VERSION 3.22.1)

project("fearai_llama_jni")

# Configure C++20 for llama.cpp
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    fearai_llama_jni
    SHARED
    llama-jni.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    fearai_llama_jni
    ${log-lib}
)
