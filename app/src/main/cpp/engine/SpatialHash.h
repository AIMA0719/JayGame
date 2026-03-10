#ifndef JAYGAME_SPATIALHASH_H
#define JAYGAME_SPATIALHASH_H

#include <vector>
#include <unordered_map>
#include <cstdint>
#include "MathTypes.h"

template<typename T>
class SpatialHash {
public:
    explicit SpatialHash(float cellSize = 64.f)
        : cellSize_(cellSize), invCellSize_(1.f / cellSize) {}

    void clear() {
        cells_.clear();
        queryCount_ = 0;
    }

    void insert(T *obj, const Rect &bounds) {
        int x0 = toCell(bounds.x);
        int y0 = toCell(bounds.y);
        int x1 = toCell(bounds.x + bounds.w);
        int y1 = toCell(bounds.y + bounds.h);

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                cells_[cellKey(x, y)].push_back(obj);
            }
        }
    }

    // Query all objects that might overlap with the given rect
    std::vector<T *> query(const Rect &bounds) {
        queryCount_++;
        std::vector<T *> result;

        int x0 = toCell(bounds.x);
        int y0 = toCell(bounds.y);
        int x1 = toCell(bounds.x + bounds.w);
        int y1 = toCell(bounds.y + bounds.h);

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                auto it = cells_.find(cellKey(x, y));
                if (it != cells_.end()) {
                    for (T *obj : it->second) {
                        // Simple dedup: check if already in result
                        bool found = false;
                        for (T *existing : result) {
                            if (existing == obj) { found = true; break; }
                        }
                        if (!found) result.push_back(obj);
                    }
                }
            }
        }

        return result;
    }

    // Query objects near a point
    std::vector<T *> queryPoint(float x, float y) {
        return query({x, y, 0.f, 0.f});
    }

    size_t getCellCount() const { return cells_.size(); }
    size_t getQueryCount() const { return queryCount_; }
    void resetQueryCount() { queryCount_ = 0; }

private:
    int toCell(float v) const {
        return static_cast<int>(std::floor(v * invCellSize_));
    }

    static uint64_t cellKey(int x, int y) {
        // Pack two 32-bit ints into one 64-bit key
        uint32_t ux = static_cast<uint32_t>(x);
        uint32_t uy = static_cast<uint32_t>(y);
        return (static_cast<uint64_t>(ux) << 32) | static_cast<uint64_t>(uy);
    }

    float cellSize_;
    float invCellSize_;
    std::unordered_map<uint64_t, std::vector<T *>> cells_;
    size_t queryCount_ = 0;
};

#endif // JAYGAME_SPATIALHASH_H
