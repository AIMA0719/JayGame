#include "GraphicsContext.h"

#include <cassert>
#include <GLES3/gl3.h>
#include <game-activity/native_app_glue/android_native_app_glue.h>
#include <swappy/swappyGL.h>
#include <swappy/swappyGL_extra.h>

#include "AndroidOut.h"

//! executes glGetString and outputs the result to logcat
#define PRINT_GL_STRING(s) {aout << #s": "<< glGetString(s) << std::endl;}

GraphicsContext::~GraphicsContext() {
    shutdown();
}

bool GraphicsContext::init(android_app *app) {
    // Choose render attributes
    constexpr EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_NONE
    };

    auto display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(display, nullptr, nullptr);

    EGLint numConfigs;
    eglChooseConfig(display, attribs, nullptr, 0, &numConfigs);

    std::unique_ptr<EGLConfig[]> supportedConfigs(new EGLConfig[numConfigs]);
    eglChooseConfig(display, attribs, supportedConfigs.get(), numConfigs, &numConfigs);

    auto config = *std::find_if(
        supportedConfigs.get(),
        supportedConfigs.get() + numConfigs,
        [&display](const EGLConfig &config) {
            EGLint red, green, blue, depth;
            if (eglGetConfigAttrib(display, config, EGL_RED_SIZE, &red)
                && eglGetConfigAttrib(display, config, EGL_GREEN_SIZE, &green)
                && eglGetConfigAttrib(display, config, EGL_BLUE_SIZE, &blue)
                && eglGetConfigAttrib(display, config, EGL_DEPTH_SIZE, &depth)) {
                return red == 8 && green == 8 && blue == 8 && depth == 24;
            }
            return false;
        });

    aout << "Found " << numConfigs << " configs" << std::endl;

    EGLSurface surface = eglCreateWindowSurface(display, config, app->window, nullptr);

    EGLint contextAttribs[] = {EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE};
    EGLContext context = eglCreateContext(display, config, nullptr, contextAttribs);

    auto madeCurrent = eglMakeCurrent(display, surface, surface, context);
    assert(madeCurrent);

    display_ = display;
    surface_ = surface;
    context_ = context;
    config_ = config;
    width_ = -1;
    height_ = -1;

    PRINT_GL_STRING(GL_VENDOR);
    PRINT_GL_STRING(GL_RENDERER);
    PRINT_GL_STRING(GL_VERSION);

    // Initialize Swappy for frame pacing
    // SwappyGL_init requires JNIEnv* — get it from the JavaVM
    JNIEnv *env = nullptr;
    app->activity->vm->AttachCurrentThread(&env, nullptr);
    SwappyGL_init(env, app->activity->javaGameActivity);
    SwappyGL_setSwapIntervalNS(SWAPPY_SWAP_60FPS);
    SwappyGL_setWindow(app->window);

    aout << "GraphicsContext initialized with Swappy frame pacing" << std::endl;

    return true;
}

void GraphicsContext::destroySurface() {
    if (display_ != EGL_NO_DISPLAY && surface_ != EGL_NO_SURFACE) {
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(display_, surface_);
        surface_ = EGL_NO_SURFACE;
        aout << "GraphicsContext: surface destroyed (context kept)" << std::endl;
    }
}

bool GraphicsContext::recreateSurface(android_app *app) {
    if (display_ == EGL_NO_DISPLAY || context_ == EGL_NO_CONTEXT || !app->window) {
        return false;
    }

    surface_ = eglCreateWindowSurface(display_, config_, app->window, nullptr);
    if (surface_ == EGL_NO_SURFACE) {
        aout << "GraphicsContext: failed to recreate surface" << std::endl;
        return false;
    }

    auto madeCurrent = eglMakeCurrent(display_, surface_, surface_, context_);
    if (!madeCurrent) {
        aout << "GraphicsContext: failed to make current after surface recreate" << std::endl;
        return false;
    }

    width_ = -1;
    height_ = -1;

    // Re-init Swappy with new window
    SwappyGL_setWindow(app->window);

    aout << "GraphicsContext: surface recreated" << std::endl;
    return true;
}

void GraphicsContext::shutdown() {
    SwappyGL_destroy();

    if (display_ != EGL_NO_DISPLAY) {
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (context_ != EGL_NO_CONTEXT) {
            eglDestroyContext(display_, context_);
            context_ = EGL_NO_CONTEXT;
        }
        if (surface_ != EGL_NO_SURFACE) {
            eglDestroySurface(display_, surface_);
            surface_ = EGL_NO_SURFACE;
        }
        eglTerminate(display_);
        display_ = EGL_NO_DISPLAY;
    }
}

void GraphicsContext::swapBuffers() {
    SwappyGL_swap(display_, surface_);
}

bool GraphicsContext::updateRenderArea() {
    EGLint width, height;
    eglQuerySurface(display_, surface_, EGL_WIDTH, &width);
    eglQuerySurface(display_, surface_, EGL_HEIGHT, &height);

    if (width != width_ || height != height_) {
        width_ = width;
        height_ = height;
        glViewport(0, 0, width, height);
        return true;
    }
    return false;
}
