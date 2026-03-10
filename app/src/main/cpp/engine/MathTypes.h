#ifndef JAYGAME_MATHTYPES_H
#define JAYGAME_MATHTYPES_H

#include <cmath>
#include <cstring>

struct Vec2 {
    float x = 0.f, y = 0.f;

    Vec2() = default;
    constexpr Vec2(float x, float y) : x(x), y(y) {}

    Vec2 operator+(const Vec2 &o) const { return {x + o.x, y + o.y}; }
    Vec2 operator-(const Vec2 &o) const { return {x - o.x, y - o.y}; }
    Vec2 operator*(float s) const { return {x * s, y * s}; }
    Vec2 &operator+=(const Vec2 &o) { x += o.x; y += o.y; return *this; }
    Vec2 &operator-=(const Vec2 &o) { x -= o.x; y -= o.y; return *this; }
    Vec2 &operator*=(float s) { x *= s; y *= s; return *this; }

    float length() const { return std::sqrt(x * x + y * y); }
    float lengthSq() const { return x * x + y * y; }
    float dot(const Vec2 &o) const { return x * o.x + y * o.y; }

    Vec2 normalized() const {
        float len = length();
        if (len < 1e-6f) return {0.f, 0.f};
        return {x / len, y / len};
    }

    static Vec2 lerp(const Vec2 &a, const Vec2 &b, float t) {
        return {a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t};
    }
};

struct Vec3 {
    float x = 0.f, y = 0.f, z = 0.f;

    Vec3() = default;
    constexpr Vec3(float x, float y, float z) : x(x), y(y), z(z) {}

    Vec3 operator+(const Vec3 &o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator-(const Vec3 &o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator*(float s) const { return {x * s, y * s, z * s}; }
};

struct Vec4 {
    float x = 0.f, y = 0.f, z = 0.f, w = 1.f;

    Vec4() = default;
    constexpr Vec4(float x, float y, float z, float w) : x(x), y(y), z(z), w(w) {}
};

struct Rect {
    float x = 0.f, y = 0.f, w = 0.f, h = 0.f;

    Rect() = default;
    constexpr Rect(float x, float y, float w, float h) : x(x), y(y), w(w), h(h) {}

    bool contains(float px, float py) const {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    bool contains(const Vec2 &p) const { return contains(p.x, p.y); }

    bool intersects(const Rect &o) const {
        return x < o.x + o.w && x + w > o.x && y < o.y + o.h && y + h > o.y;
    }

    float centerX() const { return x + w * 0.5f; }
    float centerY() const { return y + h * 0.5f; }
    Vec2 center() const { return {centerX(), centerY()}; }
};

// Column-major 4x4 matrix
struct Mat4 {
    float m[16];

    Mat4() { identity(); }

    void identity() {
        std::memset(m, 0, sizeof(m));
        m[0] = m[5] = m[10] = m[15] = 1.f;
    }

    const float *data() const { return m; }
    float *data() { return m; }

    // Column-major access: m[col * 4 + row]
    float &at(int row, int col) { return m[col * 4 + row]; }
    float at(int row, int col) const { return m[col * 4 + row]; }

    Mat4 operator*(const Mat4 &o) const {
        Mat4 result;
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                result.m[col * 4 + row] =
                    m[0 * 4 + row] * o.m[col * 4 + 0] +
                    m[1 * 4 + row] * o.m[col * 4 + 1] +
                    m[2 * 4 + row] * o.m[col * 4 + 2] +
                    m[3 * 4 + row] * o.m[col * 4 + 3];
            }
        }
        return result;
    }

    Vec4 transform(const Vec4 &v) const {
        return {
            m[0] * v.x + m[4] * v.y + m[8]  * v.z + m[12] * v.w,
            m[1] * v.x + m[5] * v.y + m[9]  * v.z + m[13] * v.w,
            m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * v.w,
            m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * v.w
        };
    }

    // Orthographic projection: maps (left,bottom,near) to (-1,-1,-1) and (right,top,far) to (1,1,1)
    static Mat4 ortho(float left, float right, float bottom, float top, float near, float far) {
        Mat4 result;
        result.m[0]  = 2.f / (right - left);
        result.m[5]  = 2.f / (top - bottom);
        result.m[10] = -2.f / (far - near);
        result.m[12] = -(right + left) / (right - left);
        result.m[13] = -(top + bottom) / (top - bottom);
        result.m[14] = -(far + near) / (far - near);
        result.m[15] = 1.f;
        // zero out the rest
        result.m[1] = result.m[2] = result.m[3] = 0.f;
        result.m[4] = result.m[6] = result.m[7] = 0.f;
        result.m[8] = result.m[9] = result.m[11] = 0.f;
        return result;
    }

    static Mat4 translate(float x, float y, float z = 0.f) {
        Mat4 result;
        result.m[12] = x;
        result.m[13] = y;
        result.m[14] = z;
        return result;
    }

    static Mat4 scale(float sx, float sy, float sz = 1.f) {
        Mat4 result;
        result.m[0] = sx;
        result.m[5] = sy;
        result.m[10] = sz;
        return result;
    }

    static Mat4 rotateZ(float radians) {
        Mat4 result;
        float c = std::cos(radians);
        float s = std::sin(radians);
        result.m[0] = c;
        result.m[1] = s;
        result.m[4] = -s;
        result.m[5] = c;
        return result;
    }

    // Invert an affine orthographic matrix (for screen-to-world conversion)
    Mat4 inverse() const {
        // For ortho matrices: inv(M) has reciprocal scales and negated translations
        Mat4 inv;
        // Simple approach: general 4x4 inverse (cofactor method)
        float a[16];
        std::memcpy(a, m, sizeof(a));

        float det;
        float inv_m[16];

        inv_m[0] = a[5]*a[10]*a[15] - a[5]*a[11]*a[14] - a[9]*a[6]*a[15] + a[9]*a[7]*a[14] + a[13]*a[6]*a[11] - a[13]*a[7]*a[10];
        inv_m[4] = -a[4]*a[10]*a[15] + a[4]*a[11]*a[14] + a[8]*a[6]*a[15] - a[8]*a[7]*a[14] - a[12]*a[6]*a[11] + a[12]*a[7]*a[10];
        inv_m[8] = a[4]*a[9]*a[15] - a[4]*a[11]*a[13] - a[8]*a[5]*a[15] + a[8]*a[7]*a[13] + a[12]*a[5]*a[11] - a[12]*a[7]*a[9];
        inv_m[12] = -a[4]*a[9]*a[14] + a[4]*a[10]*a[13] + a[8]*a[5]*a[14] - a[8]*a[6]*a[13] - a[12]*a[5]*a[10] + a[12]*a[6]*a[9];
        inv_m[1] = -a[1]*a[10]*a[15] + a[1]*a[11]*a[14] + a[9]*a[2]*a[15] - a[9]*a[3]*a[14] - a[13]*a[2]*a[11] + a[13]*a[3]*a[10];
        inv_m[5] = a[0]*a[10]*a[15] - a[0]*a[11]*a[14] - a[8]*a[2]*a[15] + a[8]*a[3]*a[14] + a[12]*a[2]*a[11] - a[12]*a[3]*a[10];
        inv_m[9] = -a[0]*a[9]*a[15] + a[0]*a[11]*a[13] + a[8]*a[1]*a[15] - a[8]*a[3]*a[13] - a[12]*a[1]*a[11] + a[12]*a[3]*a[9];
        inv_m[13] = a[0]*a[9]*a[14] - a[0]*a[10]*a[13] - a[8]*a[1]*a[14] + a[8]*a[2]*a[13] + a[12]*a[1]*a[10] - a[12]*a[2]*a[9];
        inv_m[2] = a[1]*a[6]*a[15] - a[1]*a[7]*a[14] - a[5]*a[2]*a[15] + a[5]*a[3]*a[14] + a[13]*a[2]*a[7] - a[13]*a[3]*a[6];
        inv_m[6] = -a[0]*a[6]*a[15] + a[0]*a[7]*a[14] + a[4]*a[2]*a[15] - a[4]*a[3]*a[14] - a[12]*a[2]*a[7] + a[12]*a[3]*a[6];
        inv_m[10] = a[0]*a[5]*a[15] - a[0]*a[7]*a[13] - a[4]*a[1]*a[15] + a[4]*a[3]*a[13] + a[12]*a[1]*a[7] - a[12]*a[3]*a[5];
        inv_m[14] = -a[0]*a[5]*a[14] + a[0]*a[6]*a[13] + a[4]*a[1]*a[14] - a[4]*a[2]*a[13] - a[12]*a[1]*a[6] + a[12]*a[2]*a[5];
        inv_m[3] = -a[1]*a[6]*a[11] + a[1]*a[7]*a[10] + a[5]*a[2]*a[11] - a[5]*a[3]*a[10] - a[9]*a[2]*a[7] + a[9]*a[3]*a[6];
        inv_m[7] = a[0]*a[6]*a[11] - a[0]*a[7]*a[10] - a[4]*a[2]*a[11] + a[4]*a[3]*a[10] + a[8]*a[2]*a[7] - a[8]*a[3]*a[6];
        inv_m[11] = -a[0]*a[5]*a[11] + a[0]*a[7]*a[9] + a[4]*a[1]*a[11] - a[4]*a[3]*a[9] - a[8]*a[1]*a[7] + a[8]*a[3]*a[5];
        inv_m[15] = a[0]*a[5]*a[10] - a[0]*a[6]*a[9] - a[4]*a[1]*a[10] + a[4]*a[2]*a[9] + a[8]*a[1]*a[6] - a[8]*a[2]*a[5];

        det = a[0]*inv_m[0] + a[1]*inv_m[4] + a[2]*inv_m[8] + a[3]*inv_m[12];
        if (std::abs(det) < 1e-10f) return inv; // return identity on singular

        det = 1.f / det;
        for (int i = 0; i < 16; i++)
            inv.m[i] = inv_m[i] * det;

        return inv;
    }
};

#endif // JAYGAME_MATHTYPES_H
