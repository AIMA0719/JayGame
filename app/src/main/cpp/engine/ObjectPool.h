#ifndef JAYGAME_OBJECTPOOL_H
#define JAYGAME_OBJECTPOOL_H

#include <vector>
#include <cassert>
#include <cstdint>

template<typename T>
class ObjectPool {
public:
    explicit ObjectPool(size_t capacity = 256) {
        pool_.resize(capacity);
        active_.resize(capacity, false);
        freeList_.reserve(capacity);
        for (int i = static_cast<int>(capacity) - 1; i >= 0; i--) {
            freeList_.push_back(static_cast<size_t>(i));
        }
    }

    T *acquire() {
        if (freeList_.empty()) {
            // Grow pool
            size_t oldSize = pool_.size();
            size_t newSize = oldSize * 2;
            pool_.resize(newSize);
            active_.resize(newSize, false);
            for (int i = static_cast<int>(newSize) - 1; i >= static_cast<int>(oldSize); i--) {
                freeList_.push_back(static_cast<size_t>(i));
            }
        }

        size_t idx = freeList_.back();
        freeList_.pop_back();
        active_[idx] = true;
        pool_[idx] = T{};
        acquiredCount_++;
        return &pool_[idx];
    }

    void release(T *obj) {
        size_t idx = obj - pool_.data();
        assert(idx < pool_.size() && active_[idx]);
        active_[idx] = false;
        freeList_.push_back(idx);
        acquiredCount_--;
    }

    // Iterate over all active objects
    template<typename Func>
    void forEach(Func func) {
        for (size_t i = 0; i < pool_.size(); i++) {
            if (active_[i]) {
                func(pool_[i]);
            }
        }
    }

    size_t activeCount() const { return acquiredCount_; }
    size_t capacity() const { return pool_.size(); }
    size_t freeCount() const { return freeList_.size(); }

    void clear() {
        freeList_.clear();
        for (size_t i = pool_.size(); i > 0; i--) {
            freeList_.push_back(i - 1);
            active_[i - 1] = false;
        }
        acquiredCount_ = 0;
    }

private:
    std::vector<T> pool_;
    std::vector<bool> active_;
    std::vector<size_t> freeList_;
    size_t acquiredCount_ = 0;
};

#endif // JAYGAME_OBJECTPOOL_H
