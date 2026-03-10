#include "MergeSystem.h"
#include "Unit.h"
#include "UnitData.h"
#include "AndroidOut.h"

#include <cstdlib>
#include <vector>

SmartMergeResult MergeSystem::trySmartMerge(int tileIndex, Grid& grid, ObjectPool<Unit>& unitPool) {
    SmartMergeResult result;

    int row = tileIndex / Grid::COLS;
    int col = tileIndex % Grid::COLS;

    if (!grid.isValid(row, col)) return result;

    Unit* clickedUnit = grid.getUnit(row, col);
    if (!clickedUnit || !clickedUnit->active) return result;

    int family = clickedUnit->unitDefId % 5;
    int grade = clickedUnit->unitDefId / 5;

    // Can't merge if already at max grade
    if (grade >= MAX_GRADE) return result;

    // Find all units of same family+grade on the grid
    struct UnitPos { Unit* unit; int r; int c; };
    std::vector<UnitPos> sameUnits;

    for (int r = 0; r < Grid::ROWS; r++) {
        for (int c = 0; c < Grid::COLS; c++) {
            Unit* u = grid.getUnit(r, c);
            if (!u || !u->active) continue;
            int uFamily = u->unitDefId % 5;
            int uGrade = u->unitDefId / 5;
            if (uFamily == family && uGrade == grade) {
                sameUnits.push_back({u, r, c});
            }
        }
    }

    // Need at least 3 to merge
    if (sameUnits.size() < 3) return result;

    // Determine merge result grade
    int resultGrade = grade + 1;
    bool lucky = false;

    // 5% lucky chance: skip one grade
    float luckyRoll = static_cast<float>(std::rand()) / static_cast<float>(RAND_MAX) * 100.f;
    if (luckyRoll < 5.f && resultGrade + 1 <= MAX_GRADE) {
        resultGrade += 1;
        lucky = true;
    }

    int mergeResultId = resultGrade * 5 + family;
    if (mergeResultId < 0 || mergeResultId >= static_cast<int>(UNIT_TABLE_SIZE)) return result;

    aout << "Smart merge! " << getUnitDef(clickedUnit->unitDefId).name
         << " x3 -> " << getUnitDef(mergeResultId).name
         << (lucky ? " (LUCKY!)" : "") << std::endl;

    // Consume 3 units (prefer the clicked one to be the one that stays)
    // Remove clicked unit from consumed list — it will be transformed
    int consumed = 0;
    for (auto& up : sameUnits) {
        if (up.unit == clickedUnit) continue;  // skip clicked unit
        if (consumed >= 2) break;

        grid.removeUnit(up.r, up.c);
        up.unit->active = false;
        unitPool.release(up.unit);
        consumed++;
    }

    // Transform clicked unit into merge result
    Vec2 pos = grid.cellCenter(row, col);
    grid.removeUnit(row, col);
    clickedUnit->init(mergeResultId, pos);
    clickedUnit->gridRow = row;
    clickedUnit->gridCol = col;
    grid.placeUnit(row, col, clickedUnit);

    result.success = true;
    result.lucky = lucky;
    result.resultUnitId = mergeResultId;
    result.consumedCount = consumed + 1;  // including the transformed one

    return result;
}
