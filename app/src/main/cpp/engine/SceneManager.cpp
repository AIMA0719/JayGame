#include "SceneManager.h"

void SceneManager::push(std::unique_ptr<Scene> scene) {
    pending_.push_back({Op::Push, std::move(scene)});
}

void SceneManager::pop() {
    pending_.push_back({Op::Pop, nullptr});
}

void SceneManager::replace(std::unique_ptr<Scene> scene) {
    pending_.push_back({Op::Replace, std::move(scene)});
}

Scene *SceneManager::current() const {
    return stack_.empty() ? nullptr : stack_.back().get();
}

void SceneManager::processPending() {
    for (auto &op : pending_) {
        switch (op.type) {
            case Op::Push:
                if (!stack_.empty()) {
                    // Optionally pause the current scene
                }
                stack_.push_back(std::move(op.scene));
                stack_.back()->onEnter();
                break;

            case Op::Pop:
                if (!stack_.empty()) {
                    stack_.back()->onExit();
                    stack_.pop_back();
                }
                break;

            case Op::Replace:
                if (!stack_.empty()) {
                    stack_.back()->onExit();
                    stack_.pop_back();
                }
                stack_.push_back(std::move(op.scene));
                stack_.back()->onEnter();
                break;
        }
    }
    pending_.clear();
}
