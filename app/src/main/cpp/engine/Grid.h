#ifndef JAYGAME_GRID_H
#define JAYGAME_GRID_H

#include <cstdlib>
#include "MathTypes.h"
#include "SpriteBatch.h"
#include "SpriteAtlas.h"

// Forward declaration — Unit is defined in Unit.h
class Unit;

class Grid {
public:
    static constexpr int COLS = 5;
    static constexpr int ROWS = 4;
    static constexpr int TOTAL_CELLS = COLS * ROWS;

    // Grid area in logical coordinates (left side of 1280x720 screen)
    static constexpr float GRID_X = 40.f;
    static constexpr float GRID_Y = 60.f;
    static constexpr float GRID_W = 600.f;
    static constexpr float GRID_H = 600.f;
    static constexpr float CELL_W = GRID_W / COLS;   // 120
    static constexpr float CELL_H = GRID_H / ROWS;   // 150

    Grid() { clear(); }

    void clear() {
        for (int i = 0; i < TOTAL_CELLS; i++)
            cells_[i] = nullptr;
    }

    // --------------- Coordinate helpers ---------------

    // Returns the top-left world position of cell (row, col)
    Vec2 worldToCell(int row, int col) const {
        return {GRID_X + col * CELL_W, GRID_Y + row * CELL_H};
    }

    // Returns the center world position of cell (row, col)
    Vec2 cellCenter(int row, int col) const {
        return {GRID_X + col * CELL_W + CELL_W * 0.5f,
                GRID_Y + row * CELL_H + CELL_H * 0.5f};
    }

    // Hit-test: given a world position, return true and fill outRow/outCol if inside a cell
    bool getCellAt(const Vec2& worldPos, int& outRow, int& outCol) const {
        float lx = worldPos.x - GRID_X;
        float ly = worldPos.y - GRID_Y;
        if (lx < 0.f || ly < 0.f || lx >= GRID_W || ly >= GRID_H)
            return false;
        outCol = static_cast<int>(lx / CELL_W);
        outRow = static_cast<int>(ly / CELL_H);
        if (outCol >= COLS) outCol = COLS - 1;
        if (outRow >= ROWS) outRow = ROWS - 1;
        return true;
    }

    // --------------- Cell state ---------------

    bool isValid(int row, int col) const {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    bool isEmpty(int row, int col) const {
        if (!isValid(row, col)) return false;
        return cells_[row * COLS + col] == nullptr;
    }

    Unit* getUnit(int row, int col) const {
        if (!isValid(row, col)) return nullptr;
        return cells_[row * COLS + col];
    }

    bool placeUnit(int row, int col, Unit* unit) {
        if (!isValid(row, col) || !isEmpty(row, col)) return false;
        cells_[row * COLS + col] = unit;
        return true;
    }

    Unit* removeUnit(int row, int col) {
        if (!isValid(row, col)) return nullptr;
        Unit* u = cells_[row * COLS + col];
        cells_[row * COLS + col] = nullptr;
        return u;
    }

    // Find a specific unit's position on the grid
    bool findUnit(Unit* unit, int& outRow, int& outCol) const {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (cells_[r * COLS + c] == unit) {
                    outRow = r;
                    outCol = c;
                    return true;
                }
            }
        }
        return false;
    }

    int getEmptyCellCount() const {
        int count = 0;
        for (int i = 0; i < TOTAL_CELLS; i++)
            if (cells_[i] == nullptr) count++;
        return count;
    }

    // Returns true and fills outRow/outCol with a random empty cell.
    // Returns false if the grid is full.
    bool getRandomEmptyCell(int& outRow, int& outCol) const {
        int emptyCount = getEmptyCellCount();
        if (emptyCount == 0) return false;

        int pick = std::rand() % emptyCount;
        int seen = 0;
        for (int i = 0; i < TOTAL_CELLS; i++) {
            if (cells_[i] == nullptr) {
                if (seen == pick) {
                    outRow = i / COLS;
                    outCol = i % COLS;
                    return true;
                }
                seen++;
            }
        }
        return false; // should not reach here
    }

    // --------------- Rendering ---------------

    // Draw grid cell borders as thin colored rectangles.
    // Uses a 1x1 white pixel texture for solid color drawing.
    void render(SpriteBatch& batch, const SpriteAtlas& atlas) const {
        const auto& tex = *atlas.getTexture();
        const auto& cellBg = atlas.getTile("grid_cell");
        const auto& wp = atlas.getWhitePixel();

        // Draw cell backgrounds — dark navy tint to match neon theme
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                float x = GRID_X + c * CELL_W;
                float y = GRID_Y + r * CELL_H;
                batch.draw(tex, x, y, CELL_W, CELL_H,
                           cellBg.uvRect.x, cellBg.uvRect.y,
                           cellBg.uvRect.w, cellBg.uvRect.h,
                           0.06f, 0.07f, 0.14f, 0.4f);
            }
        }

        // Draw grid lines — subtle cyan-tinted borders
        constexpr float LINE_W = 1.f;
        for (int c = 0; c <= COLS; c++) {
            float x = GRID_X + c * CELL_W - LINE_W * 0.5f;
            batch.draw(tex, x, GRID_Y, LINE_W, GRID_H,
                       wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                       0.2f, 0.35f, 0.5f, 0.35f);
        }
        for (int r = 0; r <= ROWS; r++) {
            float y = GRID_Y + r * CELL_H - LINE_W * 0.5f;
            batch.draw(tex, GRID_X, y, GRID_W, LINE_W,
                       wp.uvRect.x, wp.uvRect.y, wp.uvRect.w, wp.uvRect.h,
                       0.2f, 0.35f, 0.5f, 0.35f);
        }
    }

private:
    Unit* cells_[TOTAL_CELLS]{};
};

#endif // JAYGAME_GRID_H
