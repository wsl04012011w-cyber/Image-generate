cmake_minimum_required(VERSION 3.22.1)
project(imagegen LANGUAGES C CXX)

option(IMAGEGEN_WITH_SD "Fetch and link stable-diffusion.cpp" OFF)
option(IMAGEGEN_ENABLE_VULKAN "Build stable-diffusion.cpp with Vulkan backend" OFF)

add_library(imagegen SHARED native-lib.cpp)
target_compile_features(imagegen PRIVATE cxx_std_17)

if(IMAGEGEN_WITH_SD)
    include(FetchContent)
    set(SD_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(SD_WEBP OFF CACHE BOOL "" FORCE)
    set(SD_WEBM OFF CACHE BOOL "" FORCE)
    set(SD_VULKAN ${IMAGEGEN_ENABLE_VULKAN} CACHE BOOL "" FORCE)
    set(SD_OPENCL OFF CACHE BOOL "" FORCE)
    set(SD_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
    FetchContent_Declare(stable_diffusion
        GIT_REPOSITORY https://github.com/leejet/stable-diffusion.cpp.git
        GIT_TAG master
        GIT_SHALLOW TRUE
        GIT_SUBMODULES_RECURSE TRUE
    )
    FetchContent_MakeAvailable(stable_diffusion)
    target_compile_definitions(imagegen PRIVATE IMAGEGEN_WITH_SD=1)
    target_include_directories(imagegen PRIVATE ${stable_diffusion_SOURCE_DIR}/include)
    target_link_libraries(imagegen PRIVATE stable-diffusion)
endif()

find_library(log-lib log)
target_link_libraries(imagegen PRIVATE ${log-lib})
