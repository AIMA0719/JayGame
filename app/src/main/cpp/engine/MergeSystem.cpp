#include "MergeSystem.h"
#include "Unit.h"
#include "UnitData.h"
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
    // 3-unit merge: same family + same grade → next grade
    {
        int dragFamily = dragUnit_->unitDefId % 5;
        int dragGrade  = dragUnit_->unitDefId / 5;
        int targetFamily = targetUnit->unitDefId % 5;
        int targetGrade  = targetUnit->unitDefId / 5;

        if (dragFamily == targetFamily &&
            dragGrade == targetGrade &&
            dragGrade < MAX_GRADE) {

            // Find a 3rd unit of same family+grade on the grid
            Unit* third = findThirdUnit(grid, dragFamily, dragGrade,
                                        dragUnit_, targetUnit);

            if (third) {
                int mergeResultId = dragUnit_->unitDefId + 5;
                if (mergeResultId >= 0 && mergeResultId < static_cast<int>(UNIT_TABLE_SIZE)) {
                    aout << "3-unit merge! " << getUnitDef(dragUnit_->unitDefId).name
                         << " x3 → " << getUnitDef(mergeResultId).name << std::endl;

                    // Remove source unit
                    grid.removeUnit(dragSourceRow_, dragSourceCol_);
                    dragUnit_->active = false;
                    unitPool.release(dragUnit_);

                    // Remove third unit from grid
                    int thirdRow, thirdCol;
                    if (grid.findUnit(third, thirdRow, thirdCol)) {
                        grid.removeUnit(thirdRow, thirdCol);
                    }
                    third->active = false;
                    unitPool.release(third);

                    // Transform target into merge result
                    Vec2 targetPos = grid.cellCenter(targetRow, targetCol);
                    grid.removeUnit(targetRow, targetCol);
                    targetUnit->init(mergeResultId, targetPos);
                    grid.placeUnit(targetRow, targetCol, targetUnit);

                    state_ = DragState::Idle;
                    dragUnit_ = nullptr;
                    lastResult_ = MergeResult::Merged;
                    return lastResult_;
                }
            }
        }
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

Unit* MergeSystem::findThirdUnit(const Grid& grid, int family, int grade,
                                  Unit* exclude1, Unit* exclude2) const {
    for (int r = 0; r < Grid::ROWS; r++) {
        for (int c = 0; c < Grid::COLS; c++) {
            Unit* u = grid.getUnit(r, c);
            if (!u || !u->active) continue;
            if (u == exclude1 || u == exclude2) continue;
            int uFamily = u->unitDefId % 5;
            int uGrade  = u->unitDefId / 5;
            if (uFamily == family && uGrade == grade) {
                return u;
            }
        }
    }
    return nullptr;
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
        int dragFamily = dragUnit_->unitDefId % 5;
        int dragGrade  = dragUnit_->unitDefId / 5;
        int targetFamily = targetUnit->unitDefId % 5;
        int targetGrade  = targetUnit->unitDefId / 5;

        if (dragFamily == targetFamily &&
            dragGrade == targetGrade &&
            dragGrade < MAX_GRADE) {
            // 3-unit merge possible (green)
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
