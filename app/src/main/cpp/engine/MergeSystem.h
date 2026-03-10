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

struct SmartMergeResult {
    bool success = false;
    bool lucky = false;  // jackpot! skipped a grade
    int resultUnitId = -1;
    int consumedCount = 0;
};

class MergeSystem {
public:
    MergeSystem() = default;

    // Smart merge: find 3 units of same family+grade on grid, consume them, produce next grade
    // 5% lucky chance: skip one grade (e.g., LOW->HIGH instead of LOW->MEDIUM)
    SmartMergeResult trySmartMerge(int tileIndex, Grid& grid, ObjectPool<Unit>& unitPool);

private:
    static constexpr int MAX_GRADE = 4;  // Transcendent grade (unitDefId / 5)
};

#endif // JAYGAME_MERGESYSTEM_H
