#ifndef JAYGAME_SCENEMANAGER_H
#define JAYGAME_SCENEMANAGER_H

#include <memory>
#include <vector>
#include "Scene.h"

class SceneManager {
public:
    void push(std::unique_ptr<Scene> scene);
    void pop();
    void replace(std::unique_ptr<Scene> scene);

    Scene *current() const;
    bool empty() const { return stack_.empty(); }

    // Process deferred operations (call once per frame before update)
    void processPending();

private:
    std::vector<std::unique_ptr<Scene>> stack_;

    enum class Op { Push, Pop, Replace };
    struct PendingOp {
        Op type;
        std::unique_ptr<Scene> scene;
    };
    std::vector<PendingOp> pending_;
};

#endif // JAYGAME_SCENEMANAGER_H
