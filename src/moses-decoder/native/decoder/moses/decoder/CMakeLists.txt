set(DECODER_SRC
        JNITranslationRequest.cpp
        JNITranslationRequest.h
        JNITranslator.cpp
        JNITranslator.h
        MosesDecoder.cpp
        )
add_library(moses_decoder OBJECT ${DECODER_SRC})
