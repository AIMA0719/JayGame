#ifndef JAYGAME_GRAPHICSCONTEXT_H
#define JAYGAME_GRAPHICSCONTEXT_H

#include <EGL/egl.h>

struct android_app;
struct ANativeWindow;

class GraphicsContext {
public:
    GraphicsContext() = default;
    ~GraphicsContext();

    // Non-copyable
    GraphicsContext(const GraphicsContext &) = delete;
    GraphicsContext &operator=(const GraphicsContext &) = delete;

    bool init(android_app *app);
    void shutdown();

    // Destroy only the surface (keep context alive for fast re-init)
    void destroySurface();
    // Recreate surface from a new window
    bool recreateSurface(android_app *app);

    void swapBuffers();

    // Returns true if the render area changed since last call
    bool updateRenderArea();

    EGLDisplay getDisplay() const { return display_; }
    EGLSurface getSurface() const { return surface_; }
    EGLContext getContext() const { return context_; }
    int getWidth() const { return width_; }
    int getHeight() const { return height_; }
    bool isReady() const { return display_ != EGL_NO_DISPLAY && surface_ != EGL_NO_SURFACE; }
    bool hasContext() const { return display_ != EGL_NO_DISPLAY && context_ != EGL_NO_CONTEXT; }

private:
    EGLDisplay display_ = EGL_NO_DISPLAY;
    EGLSurface surface_ = EGL_NO_SURFACE;
    EGLContext context_ = EGL_NO_CONTEXT;
    EGLConfig config_ = nullptr;
    int width_ = 0;
    int height_ = 0;
};

#endif // JAYGAME_GRAPHICSCONTEXT_H
