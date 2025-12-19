/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.headless;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;

import java.security.MessageDigest;
import java.util.ArrayList;

/**
 * Deterministic runner for headless game execution.
 * Provides step(action) interface that advances exactly one turn.
 */
public final class DeterministicRunner {
	
	private final RunConfig config;
	private int turnCount = 0;
	private long lastStateHash = 0;
	
	public DeterministicRunner(RunConfig config) {
		this.config = config;
	}
	
	/**
	 * Starts a new game with the configured seed and hero class.
	 * This bypasses all UI and directly initializes the game state.
	 */
	public void startNewGame() {
		// Reset RNG generators first to ensure clean state
		Random.resetGenerators();
		
		// Set hero class FIRST (before any initialization)
		GamesInProgress.selectedClass = config.heroClass;
		
		// Set challenges
		SPDSettings.challenges(config.challenges);
		Dungeon.mobsToChampion = config.mobsToChampion;
		
		// Set seed-related flags BEFORE initSeed()
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		
		// CRITICAL: Set custom seed in SPDSettings BEFORE initSeed()
		// initSeed() checks SPDSettings.customSeed() and if empty, generates a RANDOM seed!
		// We need to set it so initSeed() uses our deterministic seed
		String seedText = com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed.convertToCode(config.seed);
		SPDSettings.customSeed(seedText);
		
		// Now initSeed() will use our seed
		Dungeon.initSeed();
		
		// Verify seed is correct (should match our config)
		if (Dungeon.seed != config.seed) {
			System.err.println("WARNING: Seed mismatch after initSeed(). Expected " + config.seed + " but got " + Dungeon.seed);
			// Force it to our seed
			Dungeon.seed = config.seed;
		}
		
		// Initialize the game (this will seed RNG with seed+1 for initialization)
		Dungeon.init();
		
		// After init, reset generators and push the actual game seed
		// This ensures all game logic uses the deterministic seed
		Random.resetGenerators();
		Random.pushGenerator(config.seed);
		
		// CRITICAL: Verify seed is set before level generation
		// seedCurDepth() uses Dungeon.seed, so it must be correct
		if (Dungeon.seed != config.seed) {
			System.err.println("WARNING: Dungeon.seed mismatch before level generation. Expected " + 
				config.seed + " but got " + Dungeon.seed);
			Dungeon.seed = config.seed;
		}
		
		// Generate first level (this will use seedCurDepth() which depends on Dungeon.seed)
		Level level = Dungeon.newLevel();
		if (level == null) {
			throw new RuntimeException("Failed to generate first level");
		}
		
		// Call switchLevel but catch any exceptions from observe() or saveAll()
		// These might fail in headless mode but we can continue
		// We'll manually do what switchLevel does, skipping UI-dependent parts
		try {
			// Manually do switchLevel logic, skipping observe() and saveAll()
			int pos = -1;
			if (pos < 0 || pos >= level.length() || level.invalidHeroPos(pos)) {
				pos = level.getTransition(null).cell();
			}
			
			com.watabou.utils.PathFinder.setMapSize(level.width(), level.height());
			Dungeon.level = level;
			if (Dungeon.hero != null) {
				Dungeon.hero.pos = pos;
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob.restoreAllies(level, pos);
				com.shatteredpixel.shatteredpixeldungeon.actors.Actor.init();
				level.addRespawner();
				
				// Displace mobs if needed
				for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : level.mobs) {
					if (m.pos == Dungeon.hero.pos && !com.shatteredpixel.shatteredpixeldungeon.actors.Char.hasProp(m, com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.IMMOVABLE)) {
						for (int i : com.watabou.utils.PathFinder.NEIGHBOURS8) {
							if (com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(m.pos + i) == null && level.passable[m.pos + i]) {
								m.pos += i;
								break;
							}
						}
					}
				}
				
				// Update view distance
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light light = Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light.class);
				Dungeon.hero.viewDistance = light == null ? level.viewDistance : Math.max(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light.DISTANCE, level.viewDistance);
				Dungeon.hero.curAction = Dungeon.hero.lastAction = null;
				
				// Skip observe() and saveAll() - they need UI
			}
		} catch (Exception e) {
			// Fallback: just set level and hero position
			System.err.println("Warning: switchLevel setup threw exception: " + e.getMessage());
			e.printStackTrace();
			Dungeon.level = level;
			if (Dungeon.hero != null && level != null) {
				try {
					int pos = level.getTransition(null).cell();
					Dungeon.hero.pos = pos;
					com.shatteredpixel.shatteredpixeldungeon.actors.Actor.init();
				} catch (Exception e2) {
					throw new RuntimeException("Failed to initialize level", e2);
				}
			}
		}
		
		// Verify level was set
		if (Dungeon.level == null) {
			throw new RuntimeException("Dungeon.level is null after switchLevel");
		}
		
		// Ensure hero is ready for input
		Hero hero = Dungeon.hero;
		if (hero == null) {
			throw new RuntimeException("Dungeon.hero is null after initialization");
		}
		
		hero.ready = false;
		hero.curAction = null;
		
		// Disable audio in headless mode
		com.watabou.noosa.audio.Music.INSTANCE.enable(false);
		com.watabou.noosa.audio.Sample.INSTANCE.enable(false);
		
		// Process initial turn to get hero ready
		advanceUntilNextPlayerTurn();
		
		// Final verification
		if (Dungeon.level == null) {
			throw new RuntimeException("Dungeon.level became null after advanceUntilNextPlayerTurn");
		}
	}
	
	/**
	 * Steps the game forward by one player turn.
	 * 
	 * Key behavior: Advances until the NEXT hero-input moment, not "one tick".
	 * This makes RL and replay playback clean.
	 * 
	 * Process:
	 * 1. Apply action for hero
	 * 2. Tick world until hero can act again OR episode ends
	 * 3. Return observation + reward + done + info
	 * 
	 * @param action The action to take
	 * @return StepResult containing observation, reward, done flag, state hash, and info
	 */
	public StepResult step(HeadlessAction action) {
		// Check if episode already ended
		if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
			GameState state = extractState();
			return new StepResult(state, 0, true, computeStateHash(), createInfo(true, "Hero already dead"));
		}
		
		// Verify hero is ready for input (should be true at start of step)
		Hero hero = Dungeon.hero;
		if (!hero.ready && hero.curAction == null) {
			// Hero should be ready, but isn't. Force it.
			hero.ready = true;
		}
		
		// Store pre-step state for reward calculation
		int preDepth = Dungeon.depth;
		int preHP = hero.HP;
		int preGold = Dungeon.gold;
		
		// Apply player action
		applyPlayerAction(action);
		
		// Advance game until next player turn or game ends
		// This processes all actors (enemies, buffs, etc.) until hero is ready again
		advanceUntilNextPlayerTurn();
		
		// Check if episode ended
		boolean done = (Dungeon.hero == null || !Dungeon.hero.isAlive());
		String doneReason = null;
		if (done) {
			if (Dungeon.hero == null) {
				doneReason = "hero_null";
			} else if (!Dungeon.hero.isAlive()) {
				doneReason = "hero_died";
			}
		}
		
		// Extract state (observation)
		GameState state = extractState();
		
		// Calculate reward (simple: depth progress + survival bonus)
		float reward = calculateReward(preDepth, preHP, preGold, done);
		
		// Compute state hash for integrity checking
		long stateHash = computeStateHash();
		
		// Create info dict
		Info info = createInfo(done, doneReason);
		
		turnCount++;
		
		// Log state hash every N turns for integrity checking
		if (turnCount % 100 == 0) {
			System.out.println("Turn " + turnCount + " state hash: " + stateHash);
		}
		
		return new StepResult(state, reward, done, stateHash, info);
	}
	
	/**
	 * Calculates reward for the step.
	 * Simple implementation: depth progress + survival bonus.
	 * Can be extended for more sophisticated reward shaping.
	 */
	private float calculateReward(int preDepth, int preHP, int preGold, boolean done) {
		float reward = 0.0f;
		
		// Depth progress (positive reward for going deeper)
		if (Dungeon.hero != null && Dungeon.hero.isAlive()) {
			int depthProgress = Dungeon.depth - preDepth;
			reward += depthProgress * 10.0f; // +10 per depth level
			
			// Gold collected
			int goldGained = Dungeon.gold - preGold;
			reward += goldGained * 0.1f; // +0.1 per gold
			
			// Survival bonus (small positive for staying alive)
			reward += 0.1f;
			
			// HP loss penalty (negative reward for taking damage)
			int hpLost = preHP - Dungeon.hero.HP;
			if (hpLost > 0) {
				reward -= hpLost * 0.5f; // -0.5 per HP lost
			}
		}
		
		// Death penalty
		if (done) {
			reward -= 100.0f; // Large negative for dying
		}
		
		return reward;
	}
	
	/**
	 * Creates info dict with episode metadata.
	 */
	private Info createInfo(boolean done, String doneReason) {
		Info info = new Info();
		info.turn = turnCount;
		info.done = done;
		info.doneReason = doneReason;
		
		if (Dungeon.hero != null) {
			info.heroAlive = Dungeon.hero.isAlive();
			info.depth = Dungeon.depth;
			info.heroHP = Dungeon.hero.HP;
			info.heroMaxHP = Dungeon.hero.HT;
		}
		
		return info;
	}
	
	/**
	 * Applies a player action to the hero.
	 * 
	 * This method translates HeadlessAction (canonical action space) into
	 * HeroAction (internal game actions). Movement actions use Hero.handle()
	 * for context-aware action selection (attacks, pickups, door opening, etc.).
	 */
	private void applyPlayerAction(HeadlessAction action) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) {
			return;
		}
		
		if (Dungeon.level == null) {
			throw new RuntimeException("Cannot apply action: Dungeon.level is null");
		}
		
		hero.ready = false;
		
		if (action.isMovement()) {
			// Calculate destination cell
			int dst = hero.pos;
			if (action.dx != 0 || action.dy != 0) {
				// Convert relative movement to absolute position
				int width = Dungeon.level.width();
				int height = Dungeon.level.height();
				int newX = (hero.pos % width) + action.dx;
				int newY = (hero.pos / width) + action.dy;
				if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
					dst = newY * width + newX;
				} else {
					// Invalid move (out of bounds), just wait
					hero.curAction = null;
					return;
				}
			}
			
			// Use Hero.handle() for context-aware action selection
			// This automatically handles:
			// - Attacking enemies
			// - Picking up items
			// - Opening chests
			// - Unlocking doors
			// - Level transitions
			// - Mining walls
			// - Alchemy pots
			// - And more...
			boolean handled = hero.handle(dst);
			if (!handled) {
				// If handle() returns false, the action is invalid (e.g., blocked cell)
				// Set to null to effectively wait
				hero.curAction = null;
			}
			// If handled is true, hero.curAction is already set by handle()
			
		} else if (action == HeadlessAction.PICKUP) {
			// Explicit pickup at current position
			hero.curAction = new HeroAction.PickUp(hero.pos);
			
		} else if (action == HeadlessAction.WAIT) {
			// Wait: skip turn without moving
			hero.curAction = null;
			hero.resting = false;
			
		} else if (action == HeadlessAction.REST) {
			// Rest: rest until fully healed or interrupted
			hero.curAction = null;
			hero.resting = true;
			
		} else {
			// Unknown action, treat as wait
			hero.curAction = null;
		}
	}
	
	/**
	 * Advances the game until the next player turn or game ends.
	 * 
	 * Key behavior: Processes all actors (enemies, buffs, etc.) until:
	 * - Hero is ready for next input (hero.ready == true && hero.curAction == null)
	 * - OR episode ends (hero dies)
	 * 
	 * This ensures step() advances to the NEXT decision point, not just one tick.
	 * Uses synchronous processing (no threads) for determinism.
	 */
	private void advanceUntilNextPlayerTurn() {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) {
			return;
		}
		
		// Process actors synchronously until hero is ready or game ends
		// This is a simplified version of Actor.process() that runs synchronously
		int maxIterations = 500; // Safety limit (reduced for performance - should be < 100 per turn normally)
		int iterations = 0;
		int heroActCount = 0; // Track how many times hero acts
		
		try {
			// Use reflection to access private Actor.current field
			java.lang.reflect.Field currentField = Actor.class.getDeclaredField("current");
			currentField.setAccessible(true);
			java.lang.reflect.Field nowField = Actor.class.getDeclaredField("now");
			nowField.setAccessible(true);
			
			while (iterations < maxIterations) {
				// Check if hero is ready and no action is pending
				Actor current = (Actor) currentField.get(null);
				
				// DECISION POINT: Hero is ready for next input when:
				// - hero.ready == true (hero has completed its turn)
				// - hero.curAction == null (no action pending)
				// - current == null (no actor currently processing)
				if (hero.ready && hero.curAction == null && current == null) {
					// Hero is ready for next input - this is the decision point
					break;
				}
				
				// Safety: if hero has acted multiple times without becoming ready, force it
				// This handles edge cases where ready() wasn't called properly
				if (heroActCount > 5 && !hero.ready && hero.curAction == null) {
					hero.ready = true;
					break;
				}
				
				// Check if hero died
				if (hero == null || !hero.isAlive()) {
					break;
				}
				
				// Find next actor to process
				Actor next = null;
				float earliest = Float.MAX_VALUE;
				
			// Use reflection to access private/protected Actor fields
			java.lang.reflect.Field timeField = Actor.class.getDeclaredField("time");
			timeField.setAccessible(true);
			java.lang.reflect.Field priorityField = Actor.class.getDeclaredField("actPriority");
			priorityField.setAccessible(true);
			java.lang.reflect.Method actMethod = Actor.class.getDeclaredMethod("act");
			actMethod.setAccessible(true);
			
			for (Actor actor : Actor.all()) {
				// In headless mode, sprites don't exist, so we skip sprite movement checks
				float actorTime = timeField.getFloat(actor);
				int actorPriority = priorityField.getInt(actor);
				
				float nextTime = (next != null) ? timeField.getFloat(next) : Float.MAX_VALUE;
				int nextPriority = (next != null) ? priorityField.getInt(next) : Integer.MIN_VALUE;
				
				if (actorTime < earliest ||
						(actorTime == earliest && 
						 (next == null || actorPriority > nextPriority))) {
					earliest = actorTime;
					next = actor;
				}
			}
			
				if (next != null) {
					// Process this actor
					float nextTime = timeField.getFloat(next);
					currentField.set(null, next);
					nowField.setFloat(null, nextTime);
					
					boolean doNext = false;
					boolean isHero = (next instanceof Hero);
					if (isHero) heroActCount++;
					
					try {
						doNext = (Boolean) actMethod.invoke(next);
					} catch (Exception e) {
						// Handle sprite-related exceptions in headless mode
						Throwable cause = e.getCause();
						if (cause instanceof NullPointerException && 
							cause.getMessage() != null && 
							cause.getMessage().contains("sprite")) {
							// Hero.ready() or similar tried to access sprite
							// Manually set hero ready state
							if (isHero) {
								Hero h = (Hero) next;
								h.ready = true;
								h.curAction = null;
								h.damageInterrupt = true;
								h.waitOrPickup = false;
								// canSelfTrample is private, skip it
								doNext = false; // Hero is ready, stop processing
							} else {
								// For other actors, just continue
								doNext = false;
							}
						} else {
							// Re-throw other exceptions
							throw new RuntimeException("Error processing actor", e);
						}
					}
					
					if (!doNext) {
						currentField.set(null, null);
					}
					
					// Check if hero died
					if (hero == null || !hero.isAlive()) {
						currentField.set(null, null);
						break;
					}
				} else {
					// No more actors to process
					// Hero should be ready now
					if (hero != null && !hero.ready) {
						hero.ready = true;
						hero.curAction = null;
					}
					break;
				}
				
				iterations++;
				
				// Safety check
				if (hero == null || !hero.isAlive()) {
					break;
				}
			}
			
			if (iterations >= maxIterations) {
				System.err.println("WARNING: Max iterations reached in advanceUntilNextPlayerTurn");
				// Force hero to be ready to break the loop
				if (hero != null && hero.isAlive()) {
					hero.ready = true;
					hero.curAction = null;
				}
			}
			
			// Ensure hero is ready if still alive and no action pending
			if (hero != null && hero.isAlive() && !hero.ready && hero.curAction == null) {
				// In headless mode, we can't call ready() because it needs sprite
				// So we manually set the ready state
				hero.ready = true;
				hero.curAction = null;
				hero.damageInterrupt = true;
				hero.waitOrPickup = false;
				// canSelfTrample is private, skip it
			}
		} catch (Exception e) {
			System.err.println("Error in advanceUntilNextPlayerTurn: " + e.getMessage());
			e.printStackTrace();
			// Fallback: just mark hero as ready
			if (hero != null && hero.isAlive()) {
				hero.ready = true;
				hero.curAction = null;
			}
		}
	}
	
	/**
	 * Extracts the current game state for observation.
	 */
	private GameState extractState() {
		if (Dungeon.hero == null) {
			return new GameState(0, 0, 0, 0, 0, 0, new int[0], false);
		}
		
		Hero hero = Dungeon.hero;
		Level level = Dungeon.level;
		
		// Extract basic state
		int heroX = hero.pos % level.width();
		int heroY = hero.pos / level.width();
		int heroHP = hero.HP;
		int heroMaxHP = hero.HT;
		int depth = Dungeon.depth;
		int gold = Dungeon.gold;
		
		// Extract level map (simplified - just passable/not passable)
		int[] map = new int[level.length()];
		for (int i = 0; i < level.length(); i++) {
			map[i] = level.passable[i] ? 1 : 0;
		}
		
		boolean alive = hero.isAlive();
		
		return new GameState(heroX, heroY, heroHP, heroMaxHP, depth, gold, map, alive);
	}
	
	/**
	 * Computes a hash of the current game state for integrity checking.
	 */
	private long computeStateHash() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			
			if (Dungeon.hero != null) {
				Hero hero = Dungeon.hero;
				md.update(String.format("pos:%d,hp:%d/%d,depth:%d,gold:%d",
					hero.pos, hero.HP, hero.HT, Dungeon.depth, Dungeon.gold).getBytes());
			}
			
			if (Dungeon.level != null) {
				Level level = Dungeon.level;
				md.update(String.format("level:%dx%d", level.width(), level.height()).getBytes());
			}
			
			byte[] hash = md.digest();
			// Convert first 8 bytes to long
			long result = 0;
			for (int i = 0; i < 8; i++) {
				result = (result << 8) | (hash[i] & 0xFF);
			}
			return result;
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * Result of a step operation.
	 * Contains observation, reward, done flag, state hash, and info dict.
	 */
	public static class StepResult {
		public final GameState state;      // Observation (game state)
		public final float reward;         // Reward for this step
		public final boolean done;         // Episode ended?
		public final long stateHash;       // State hash for integrity checking
		public final Info info;            // Additional info (turn, depth, etc.)
		
		public StepResult(GameState state, float reward, boolean done, long stateHash, Info info) {
			this.state = state;
			this.reward = reward;
			this.done = done;
			this.stateHash = stateHash;
			this.info = info;
		}
		
		// Backward compatibility constructor
		public StepResult(GameState state, float reward, boolean done, long stateHash) {
			this(state, reward, done, stateHash, new Info());
		}
	}
	
	/**
	 * Info dict containing episode metadata.
	 */
	public static class Info {
		public int turn = 0;
		public boolean done = false;
		public String doneReason = null;
		public boolean heroAlive = true;
		public int depth = 0;
		public int heroHP = 0;
		public int heroMaxHP = 0;
	}
	
	/**
	 * Simplified game state for observation.
	 */
	public static class GameState {
		public final int heroX, heroY;
		public final int heroHP, heroMaxHP;
		public final int depth;
		public final int gold;
		public final int[] map; // Simplified map representation
		public final boolean alive;
		
		public GameState(int heroX, int heroY, int heroHP, int heroMaxHP, 
		                 int depth, int gold, int[] map, boolean alive) {
			this.heroX = heroX;
			this.heroY = heroY;
			this.heroHP = heroHP;
			this.heroMaxHP = heroMaxHP;
			this.depth = depth;
			this.gold = gold;
			this.map = map;
			this.alive = alive;
		}
	}
}
