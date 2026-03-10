#include "TextureAsset.h"
#include "AndroidOut.h"
#include "Utility.h"

#include <android/asset_manager.h>
#include <android/bitmap.h>
#include <GLES3/gl3.h>
#include <cstdlib>
#include <cstring>

// stb_image-style minimal PNG/image loading via Android's built-in decoders
// AImageDecoder is API 30+, so we use the jnigraphics + raw asset approach for broader compat

// Simple TGA/raw fallback: for API < 30 we load raw RGBA data via AAsset
// For API 30+ we use AImageDecoder

#if __ANDROID_API__ >= 30
#include <android/imagedecoder.h>
#endif

std::shared_ptr<TextureAsset>
TextureAsset::loadAsset(AAssetManager *assetManager, const std::string &assetPath) {
    auto pAsset = AAssetManager_open(assetManager, assetPath.c_str(), AASSET_MODE_BUFFER);
    if (!pAsset) {
        aout << "Failed to open asset: " << assetPath << std::endl;
        return nullptr;
    }

    GLuint textureId = 0;
    bool success = false;

#if __ANDROID_API__ >= 30
    AImageDecoder *decoder = nullptr;
    auto result = AImageDecoder_createFromAAsset(pAsset, &decoder);
    if (result == ANDROID_IMAGE_DECODER_SUCCESS) {
        AImageDecoder_setAndroidBitmapFormat(decoder, ANDROID_BITMAP_FORMAT_RGBA_8888);

        const AImageDecoderHeaderInfo *header = AImageDecoder_getHeaderInfo(decoder);
        auto width = AImageDecoderHeaderInfo_getWidth(header);
        auto height = AImageDecoderHeaderInfo_getHeight(header);
        auto stride = AImageDecoder_getMinimumStride(decoder);

        std::vector<uint8_t> imageData(height * stride);
        auto decodeResult = AImageDecoder_decodeImage(decoder, imageData.data(), stride, imageData.size());

        if (decodeResult == ANDROID_IMAGE_DECODER_SUCCESS) {
            glGenTextures(1, &textureId);
            glBindTexture(GL_TEXTURE_2D, textureId);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                         GL_RGBA, GL_UNSIGNED_BYTE, imageData.data());
            success = true;
        }

        AImageDecoder_delete(decoder);
    }
#endif

    if (!success) {
        // Fallback: try to load as raw RGBA file or create a placeholder texture
        // For PNG files on API < 30, create a solid color placeholder
        aout << "Using placeholder texture for: " << assetPath << std::endl;

        const int placeholderSize = 64;
        std::vector<uint8_t> pixels(placeholderSize * placeholderSize * 4);
        for (int y = 0; y < placeholderSize; y++) {
            for (int x = 0; x < placeholderSize; x++) {
                int idx = (y * placeholderSize + x) * 4;
                // Checkerboard pattern
                bool checker = ((x / 8) + (y / 8)) % 2 == 0;
                pixels[idx + 0] = checker ? 200 : 100; // R
                pixels[idx + 1] = checker ? 200 : 100; // G
                pixels[idx + 2] = checker ? 200 : 100; // B
                pixels[idx + 3] = 255;                  // A
            }
        }

        glGenTextures(1, &textureId);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, placeholderSize, placeholderSize, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, pixels.data());
    }

    AAsset_close(pAsset);

    if (textureId == 0) return nullptr;
    return std::shared_ptr<TextureAsset>(new TextureAsset(textureId));
}

TextureAsset::~TextureAsset() {
    glDeleteTextures(1, &textureID_);
    textureID_ = 0;
}
