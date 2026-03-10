#include "MergeSystem.h"
#include "Unit.h"
#include "UnitData.h"
#include "CombinationTable.h"
#include "SpriteBatch.h"
#include "TextureAsset.h"
#include "SpriteAtlas.h"
#include "AndroidOut.h"

#include <cmath>

void MergeSystem::onDragBegin(const Vec2& worldPos, Grid& grid) {
    if (state_ != DragState::Idle) return;

    int row, col;
    if (!grid.getCellAt(worldPos, row, col)) return;

    Unit* unit = grid.getUnit(row, col);
    if (!unit || !unit->active) return;

    // Start dragging
    state_ = DragState::Dragging;
    dragUnit_ = unit;
    dragSourceRow_ = row;
    dragSourceCol_ = col;
    dragCurrentPos_ = worldPos;
    highlightRow_ = -1;
    highlightCol_ = -1;

    aout << "Drag started: unit " << unit->unitDefId << " lv" << unit->level
         << " from (" << row << "," << col << ")" << std::endl;
}

void MergeSystem::onDragMove(const Vec2& worldPos) {
    if (state_ != DragState::Dragging) return;
    dragCurrentPos_ = worldPos;

    // Update highlight (which cell the finger is over)
    int row, col;
    Grid tempGrid; // just for coordinate conversion
    if (tempGrid.getCellAt(worldPos, row, col)) {
        highlightRow_ = row;
        highlightCol_ = col;
    } else {
        highlightRow_ = -1;
        highlightCol_ = -1;
    }
}

MergeSystem::MergeResult MergeSystem::onDragEnd(const Vec2& worldPos,
                                                  Grid& grid,
                                                  ObjectPool<Unit>& unitPool) {
    if (state_ != DragState::Dragging || !dragUnit_) {
        state_ = DragState::Idle;
        lastResult_ = MergeResult::Cancelled;
        return lastResult_;
    }

    int targetRow, targetCol;
    if (!grid.getCellAt(worldPos, targetRow, targetCol)) {
        // Dropped outside grid — cancel
        state_ = DragState::Idle;
        dragUnit_ = nullptr;
        lastResult_ = MergeResult::Cancelled;
        aout << "Drag cancelled: dropped outside grid" << std::endl;
        return lastResult_;
    }

    // Same cell — cancel
    if (targetRow == dragSourceRow_ && targetCol == dragSourceCol_) {
        state_ = DragState::Idle;
        dragUnit_ = nullptr;
        lastResult_ = MergeResult::Cancelled;
        return lastResult_;
    }

    Unit* targetUnit = grid.getUnit(targetRow, targetCol);

    if (!targetUnit) {
        // Empty cell — move
        grid.removeUnit(dragSourceRow_, dragSourceCol_);
        Vec2 newPos = grid.cellCenter(targetRow, targetCol);
        dragUnit_->position = newPos;
        dragUnit_->entity.transform.position = newPos;
        dragUnit_->entity.transform.syncPrevious();
        grid.placeUnit(targetRow, targetCol, dragUnit_);

        aout << "Moved unit to (" << targetRow << "," << targetCol << ")" << std::endl;
        state_ = DragState::Idle;
        dragUnit_ = nullptr;
        lastResult_ = MergeResult::Moved;
        return lastResult_;
    }

    // Target cell has a unit
    // Check hidden combination first
    const CombinationRecipe* recipe = findCombination(
        dragUnit_->unitDefId, targetUnit->unitDefId,
        dragUnit_->level, targetUnit->level);

    if (recipe) {
        // Hidden combination!
        aout << "Hidden combination! " << getUnitDef(dragUnit_->unitDefId).name
             << " + " << getUnitDef(targetUnit->unitDefId).name
             << " = " << getUnitDef(recipe->resultUnitId).name << std::endl;

        // Remove source unit from grid and pool
        grid.removeUnit(dragSourceRow_, dragSourceCol_);
        dragUnit_->active = false;
        unitPool.release(dragUnit_);

        // Replace target unit with combination result
        Vec2 targetPos = grid.cellCenter(targetRow, targetCol);
        grid.removeUnit(targetRow, targetCol);
        targetUnit->init(recipe->resultUnitId, targetPos);
        targetUnit->level = recipe->resultLevel;
        grid.placeUnit(targetRow, targetCol, targetUnit);

        state_ = DragState::Idle;
        dragUnit_ = nullptr;
        lastResult_ = MergeResult::Combined;
        return lastResult_;
    }

    // Standard merge: same type + same level
    if (dragUnit_->unitDefId == targetUnit->unitDefId &&
        dragUnit_->level == targetUnit->level &&
        targetUnit->level < MAX_UNIT_LEVEL) {

        aout << "Merge! " << getUnitDef(dragUnit_->unitDefId).name
             << " lv" << dragUnit_->level << " → lv" << (dragUnit_->level + 1)
             << std::endl;

        // Remove source from grid and pool
        grid.removeUnit(dragSourceRow_, dragSourceCol_);
        dragUnit_->active = false;
        unitPool.release(dragUnit_);

        // Level up target
        targetUnit->level++;

        state_ = DragState::Idle;
        dragUnit_ = nullptr;
        lastResult_ = MergeResult::Merged;
        return lastResult_;
    }

    // Can't merge (different type, different level, or max level) — swap
    aout << "Swap: (" << dragSourceRow_ << "," << dragSourceCol_
         << ") <-> (" << targetRow << "," << targetCol << ")" << std::endl;

    grid.removeUnit(dragSourceRow_, dragSourceCol_);
    grid.removeUnit(targetRow, targetCol);

    // Swap positions
    Vec2 srcPos = grid.cellCenter(dragSourceRow_, dragSourceCol_);
    Vec2 tgtPos = grid.cellCenter(targetRow, targetCol);

    dragUnit_->position = tgtPos;
    dragUnit_->entity.transform.position = tgtPos;
    dragUnit_->entity.transform.syncPrevious();

    targetUnit->position = srcPos;
    targetUnit->entity.transform.position = srcPos;
    targetUnit->entity.transform.syncPrevious();

    grid.placeUnit(dragSourceRow_, dragSourceCol_, targetUnit);
    grid.placeUnit(targetRow, targetCol, dragUnit_);

    state_ = DragState::Idle;
    dragUnit_ = nullptr;
    lastResult_ = MergeResult::Swapped;
    return lastResult_;
}

void MergeSystem::onDragCancel(Grid& grid) {
    state_ = DragState::Idle;
    dragUnit_ = nullptr;
    lastResult_ = MergeResult::Cancelled;
}

void MergeSystem::render(SpriteBatch& batch, const SpriteAtlas& atlas,
                          const Grid& grid) const {
    if (state_ != DragState::Dragging || !dragUnit_) return;

    const auto& texture = *atlas.getTexture();
    const auto& wp = atlas.getWhitePixel();

    renderSourceOutline(batch, texture, wp, grid);
    renderHighlight(batch, texture, wp, grid);
    renderGhost(batch, texture, wp);
}

void MergeSystem::renderGhost(SpriteBatch& batch, const TextureAsset& texture,
                               const SpriteFrame& wp) const {
    if (!dragUnit_) return;

    Vec2 sz = dragUnit_->entity.sprite.size;
    Vec4 color = dragUnit_->entity.sprite.color;
    color.w = 0.6f; // semi-transparent ghost

    batch.draw(texture,
               dragCurrentPos_, sz,
               {0.f, 0.f, 1.f, 1.f},
               color,
               0.f,
               {0.5f, 0.5f});

    // Level indicator dot
    float levelSize = 8.f + dragUnit_->level * 2.f;
    batch.draw(texture,
               dragCurrentPos_.x - levelSize * 0.5f,
               dragCurrentPos_.y - sz.y * 0.5f - levelSize - 4.f,
               levelSize, levelSize,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               1.f, 1.f, 0.f, 0.8f);
}

void MergeSystem::renderHighlight(SpriteBatch& batch, const TextureAsset& texture,
                                   const SpriteFrame& wp, const Grid& grid) const {
    if (highlightRow_ < 0 || highlightCol_ < 0) return;
    if (highlightRow_ == dragSourceRow_ && highlightCol_ == dragSourceCol_) return;

    Vec2 cellTopLeft = grid.worldToCell(highlightRow_, highlightCol_);

    // Determine highlight color based on what would happen
    Vec4 hlColor;
    Unit* targetUnit = grid.getUnit(highlightRow_, highlightCol_);

    if (!targetUnit) {
        // Empty cell — move (blue)
        hlColor = {0.3f, 0.5f, 1.0f, 0.3f};
    } else if (dragUnit_ && targetUnit) {
        // Check combination
        const CombinationRecipe* recipe = findCombination(
            dragUnit_->unitDefId, targetUnit->unitDefId,
            dragUnit_->level, targetUnit->level);

        if (recipe) {
            // Hidden combination (gold)
            hlColor = {1.0f, 0.85f, 0.0f, 0.4f};
        } else if (dragUnit_->unitDefId == targetUnit->unitDefId &&
                   dragUnit_->level == targetUnit->level &&
                   targetUnit->level < MAX_UNIT_LEVEL) {
            // Merge possible (green)
            hlColor = {0.2f, 1.0f, 0.3f, 0.4f};
        } else {
            // Swap (orange)
            hlColor = {1.0f, 0.6f, 0.2f, 0.3f};
        }
    } else {
        hlColor = {0.5f, 0.5f, 0.5f, 0.2f};
    }

    batch.draw(texture,
               cellTopLeft.x, cellTopLeft.y,
               Grid::CELL_W, Grid::CELL_H,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               hlColor.x, hlColor.y, hlColor.z, hlColor.w);
}

void MergeSystem::renderSourceOutline(SpriteBatch& batch, const TextureAsset& texture,
                                       const SpriteFrame& wp, const Grid& grid) const {
    if (dragSourceRow_ < 0 || dragSourceCol_ < 0) return;

    Vec2 cellTopLeft = grid.worldToCell(dragSourceRow_, dragSourceCol_);
    constexpr float LINE_W = 3.f;

    // Faded outline of original cell
    batch.draw(texture,
               cellTopLeft.x, cellTopLeft.y,
               Grid::CELL_W, Grid::CELL_H,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
               0.5f, 0.5f, 0.5f, 0.15f);

    // Border
    batch.draw(texture, cellTopLeft.x, cellTopLeft.y, Grid::CELL_W, LINE_W,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h, 1.f, 1.f, 1.f, 0.3f);
    batch.draw(texture, cellTopLeft.x, cellTopLeft.y + Grid::CELL_H - LINE_W,
               Grid::CELL_W, LINE_W,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h, 1.f, 1.f, 1.f, 0.3f);
    batch.draw(texture, cellTopLeft.x, cellTopLeft.y, LINE_W, Grid::CELL_H,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h, 1.f, 1.f, 1.f, 0.3f);
    batch.draw(texture, cellTopLeft.x + Grid::CELL_W - LINE_W, cellTopLeft.y,
               LINE_W, Grid::CELL_H,
               wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h, 1.f, 1.f, 1.f, 0.3f);
}
