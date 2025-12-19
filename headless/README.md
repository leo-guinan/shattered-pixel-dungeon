# SPD Headless Module

This module provides a deterministic, headless (no rendering) execution environment for Shattered Pixel Dungeon. It enables:

- **Fully seedable and consistent RNG**: Runs are reproducible from seed + class + action sequence
- **No rendering**: Game loop runs without graphics
- **Step-by-step control**: `step(action)` interface advances exactly one turn
- **No time/input/UI side-effects**: All non-deterministic sources are disabled

## Building

```bash
./gradlew :headless:build
```

## Usage

### Basic Example

```java
import com.shatteredpixel.shatteredpixeldungeon.headless.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

// Create configuration
long seed = 12345L;
HeroClass heroClass = HeroClass.WARRIOR;
RunConfig config = new RunConfig(seed, heroClass);

// Create runner
DeterministicRunner runner = new DeterministicRunner(config);
runner.startNewGame();

// Step through the game
for (int turn = 0; turn < 100; turn++) {
    HeadlessAction action = HeadlessAction.MOVE_E; // Move east
    DeterministicRunner.StepResult result = runner.step(action);
    
    System.out.println("Turn " + turn + ": Hero at (" + 
        result.state.heroX + ", " + result.state.heroY + ")");
    
    if (result.done) {
        System.out.println("Game ended!");
        break;
    }
}
```

### Command Line

```bash
SPD_SEED=12345 SPD_CLASS=WARRIOR java -cp build/libs/headless.jar com.shatteredpixel.shatteredpixeldungeon.headless.HeadlessMain
```

## Determinism

A run is reproducible from:
- **Git commit** (or build ID)
- **Seed** (long integer)
- **Starting class** (HeroClass enum)
- **Action sequence** (sequence of HeadlessAction)

Replaying the exact same tuple produces identical outcomes (position, drops, damage rolls, etc.).

## State Integrity

The system computes a state hash every turn (logged every 100 turns). This can be used to verify determinism:

```java
DeterministicRunner.StepResult result = runner.step(action);
long stateHash = result.stateHash; // Use this to verify replay consistency
```

## Replay Logging

Use `ReplayLogger` to log actions and states for later analysis:

```java
ReplayLogger logger = new ReplayLogger(config);

for (int turn = 0; turn < maxTurns; turn++) {
    HeadlessAction action = chooseAction();
    DeterministicRunner.StepResult result = runner.step(action);
    logger.logTurn(turn, action, result);
}

logger.printReplay(); // Outputs to stdout
```

## Available Actions

- `MOVE_N`, `MOVE_NE`, `MOVE_E`, `MOVE_SE`, `MOVE_S`, `MOVE_SW`, `MOVE_W`, `MOVE_NW`: Movement in 8 directions
- `WAIT`: Wait one turn
- `PICKUP`: Pick up item at current position
- `REST`: Rest (regenerates HP over time)

## Implementation Details

### RNG Seeding

- Seed is set before game initialization
- All RNG calls use the centralized `Random` class
- No `Math.random()` or `new java.util.Random()` calls
- Seed is pushed to RNG stack at game start

### Non-Deterministic Sources Disabled

- **Audio**: Music and sound effects disabled
- **Animations**: Sprite movement checks bypassed
- **Time-based logic**: No delta time calculations
- **UI callbacks**: No scene rendering or UI updates
- **Thread synchronization**: Actor processing runs synchronously

### Actor Processing

The normal game uses a separate thread for actor processing. In headless mode:
- Actors are processed synchronously
- No thread wait/notify
- Processing continues until hero is ready for next input

## Limitations

- **Sprite movement**: Headless mode doesn't wait for sprite animations (sprites don't exist)
- **UI interactions**: Complex UI interactions (inventory, shops) not yet supported via HeadlessAction
- **Some game features**: Features that depend on rendering may not work correctly

## Future Enhancements

- More action types (attack, use item, etc.)
- Full state observation (enemies, items, etc.)
- Reward calculation for RL training
- Save/load game state for resuming runs
