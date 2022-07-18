#关闭ssl 不支持rtmps  rtmp   加密  传递一变量  进制 加密验证
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO"  )
file(GLOB rtmp_source  *.c)
add_library(rtmp
        STATIC
        ${rtmp_source})

