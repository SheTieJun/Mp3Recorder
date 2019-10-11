
cmake_minimum_required(VERSION 3.4.1)

#C 的编译选项是 CMAKE_C_FLAGS
# 指定编译参数，可选
SET(CMAKE_CXX_FLAGS "-Wno-error=format-security -Wno-error=pointer-sign")

#设置生成的so动态库最后输出的路径
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../libs/${ANDROID_ABI})

#ADD_SUBDIRECTORY(utility) //添加要编译的子目录

##当前../jni目录的所有.c .cpp源文件
AUX_SOURCE_DIRECTORY(../jni/libmp3lame_3.100 SRC_LIST)

#设置变量
#SET(LAME_LIBMP3_DIR  ../jni/libmp3lame_3.100)
#
#SET(APPEND  SRC_LIST ../jni/lame_util.c)

add_library( # Sets the name of the library.
             shetj_mp3lame

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
            ../jni/lame_util.c
            ${SRC_LIST})



find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )



target_link_libraries( # Specifies the target library.
                        shetj_mp3lame

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )