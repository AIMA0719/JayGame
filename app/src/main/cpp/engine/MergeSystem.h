#ifndef JAYGAME_MERGESYSTEM_H
#define JAYGAME_MERGESYSTEM_H

#include "MathTypes.h"
#include "Grid.h"
#include "ObjectPool.h"
#include "UnitData.h"

class Unit;
class SpriteBatch;
class TextureAsset;
class SpriteAtlas;

class MergeSystem {
public:
    enum class DragState {
        Idle,
        Dragging
    };

    enum class MergeResult {
        None,
        Merged,       // same family + same grade x3 → next grade
        Swapped,      // different family or grade → swap positions
        Moved,        // dropped on empty cell
        Cancelled     // dropped outside grid
    };

    MergeSystem() = default;

    // Call from onInput with drag events
    void onDragBegin(const Vec2& worldPos, Grid& grid);
    void onDragMove(const Vec2& worldPos);
    MergeResult onDragEnd(const Vec2& worldPos, Grid& grid, ObjectPool<Unit>& unitPool);
    void onDragCancel(Grid& grid);

    // Render the ghost sprite and highlights
    void render(SpriteBatch& batch, const SpriteAtlas& atlas, const Grid& grid) const;

    DragState getState() const { return state_; }
    bool isDragging() const { return state_ == DragState::Dragging; }

    // The last merge result (for UI feedback)
    MergeResult lastResult() const { return lastResult_; }

private:
    DragState state_ = DragState::Idle;
    MergeResult lastResult_ = MergeResult::None;

    // Dragging state
    Unit* dragUnit_ = nullptr;
    int dragSourceRow_ = -1;
    int dragSourceCol_ = -1;
    Vec2 dragCurrentPos_;

    // Highlight
    int highlightRow_ = -1;
    int highlightCol_ = -1;

    static constexpr int MAX_GRADE = 4;  // Transcendent grade (unitDefId / 5)

    Unit* findThirdUnit(const Grid& grid, int family, int grade,
                        Unit* exclude1, Unit* exclude2) const;

    void renderGhost(SpriteBatch& batch, const TextureAsset& texture,
                     const SpriteFrame& wp) const;
    void renderHighlight(SpriteBatch& batch, const TextureAsset& texture,
                         const SpriteFrame& wp, const Grid& grid) const;
    void renderSourceOutline(SpriteBatch& batch, const TextureAsset& texture,
                             const SpriteFrame& wp, const Grid& grid) const;
};

#endif // JAYGAME_MERGESYSTEM_H
