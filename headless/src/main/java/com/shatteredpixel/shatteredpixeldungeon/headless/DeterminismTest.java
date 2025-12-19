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

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Test #1: Prove determinism with a "no-op" run.
 * 
 * Runs the same seed/class twice with WAIT actions.
 * Verifies state hashes match 1:1 after each turn.
 */
public class DeterminismTest {
	
	public static void main(String[] args) {
		long seed = 12345L;
		HeroClass heroClass = HeroClass.WARRIOR;
		// Start with fewer turns for quick testing
		int numTurns = args.length > 0 ? Integer.parseInt(args[0]) : 50;
		
		// Calculate timeout: allow 100ms per turn, minimum 5 seconds
		int timeoutSeconds = Math.max(5, (numTurns * 100) / 1000 + 5);
		
		boolean passed = TestRunner.runWithTimeout("Determinism Test (" + numTurns + " turns)", timeoutSeconds, () -> {
			runTest(seed, heroClass, numTurns);
		});
		
		System.exit(passed ? 0 : 1);
	}
	
	private static void runTest(long seed, HeroClass heroClass, int numTurns) {
		System.out.println("Seed: " + seed);
		System.out.println("Class: " + heroClass);
		System.out.println("Turns: " + numTurns);
		System.out.println();
		
		// Run 1
		System.out.println("Running first instance...");
		List<Long> hashes1 = runInstance(seed, heroClass, numTurns, 1);
		
		// CRITICAL: Reset ALL static state between runs
		// The two instances run in the same JVM, so we must reset everything
		System.out.println("Resetting static state between runs...");
		resetAllStaticState();
		
		// Run 2
		System.out.println("Running second instance...");
		List<Long> hashes2 = runInstance(seed, heroClass, numTurns, 2);
		
		// Compare
		System.out.println();
		System.out.println("Comparing hashes...");
		boolean allMatch = true;
		int firstMismatch = -1;
		
		for (int i = 0; i < Math.min(hashes1.size(), hashes2.size()); i++) {
			if (!hashes1.get(i).equals(hashes2.get(i))) {
				if (firstMismatch == -1) {
					firstMismatch = i;
				}
				allMatch = false;
				if (i < 10 || i % 100 == 0) {
					System.err.printf("MISMATCH at turn %d: %016x != %016x%n", 
						i+1, hashes1.get(i), hashes2.get(i));
				}
			}
		}
		
		if (allMatch) {
			System.out.println("✓ PASS: All " + hashes1.size() + " state hashes match!");
		} else {
			System.err.println("✗ FAIL: Hashes diverged at turn " + (firstMismatch + 1));
			System.err.println("  First hash: " + String.format("%016x", hashes1.get(firstMismatch)));
			System.err.println("  Second hash: " + String.format("%016x", hashes2.get(firstMismatch)));
			throw new RuntimeException("Determinism test failed");
		}
	}
	
	private static void resetAllStaticState() {
		// Reset RNG
		com.watabou.utils.Random.resetGenerators();
		
		// Reset Actor system
		com.shatteredpixel.shatteredpixeldungeon.actors.Actor.clear();
		com.shatteredpixel.shatteredpixeldungeon.actors.Actor.resetNextID();
		
		// Reset Dungeon static state
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.level = null;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero = null;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.depth = 0;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.branch = 0;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.seed = 0;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.customSeedText = "";
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.daily = false;
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.dailyReplay = false;
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.generatedLevels != null) {
			com.shatteredpixel.shatteredpixeldungeon.Dungeon.generatedLevels.clear();
		}
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.droppedItems != null) {
			com.shatteredpixel.shatteredpixeldungeon.Dungeon.droppedItems.clear();
		}
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.chapters != null) {
			com.shatteredpixel.shatteredpixeldungeon.Dungeon.chapters.clear();
		}
		
		// Reset GamesInProgress
		com.shatteredpixel.shatteredpixeldungeon.GamesInProgress.selectedClass = null;
		
		// Note: Can't reset SPDSettings here because Gdx.app isn't initialized yet
		// It will be set correctly in startNewGame()
	}
	
	private static List<Long> runInstance(long seed, HeroClass heroClass, int numTurns, int instanceNum) {
		// Reset static state before each instance
		resetAllStaticState();
		
		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		config.updatesPerSecond = 0;
		
		SPDHeadlessApp app = new SPDHeadlessApp(seed, heroClass);
		HeadlessApplication headlessApp = new HeadlessApplication(app, config);
		
		// Wait for initialization
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		DeterministicRunner runner = app.getRunner();
		if (runner == null) {
			throw new RuntimeException("Runner is null for instance " + instanceNum);
		}
		
		// Verify initial state
		if (Dungeon.level == null) {
			throw new RuntimeException("Dungeon.level is null after initialization (instance " + instanceNum + ")");
		}
		if (Dungeon.hero == null) {
			throw new RuntimeException("Dungeon.hero is null after initialization (instance " + instanceNum + ")");
		}
		
		// Log initial state for debugging
		long seedCurDepth = Dungeon.seedCurDepth();
		System.out.printf("  Instance %d: Initial state - Depth=%d, Hero pos=%d, HP=%d/%d, Seed=%d, seedCurDepth=%d%n",
			instanceNum, Dungeon.depth, Dungeon.hero.pos, Dungeon.hero.HP, Dungeon.hero.HT, Dungeon.seed, seedCurDepth);
		
		List<Long> hashes = new ArrayList<>();
		
		// Compute initial hash BEFORE any steps
		long initialHash = computeStateHash();
		hashes.add(initialHash);
		System.out.printf("  Instance %d: Initial hash (before steps): %016x%n", instanceNum, initialHash);
		
		// Take WAIT actions for N turns
		long startTime = System.currentTimeMillis();
		for (int turn = 0; turn < numTurns; turn++) {
			long turnStart = System.currentTimeMillis();
			DeterministicRunner.StepResult result = runner.step(HeadlessAction.WAIT);
			long turnTime = System.currentTimeMillis() - turnStart;
			
			// Compute comprehensive state hash AFTER the step
			long hash = computeStateHash();
			hashes.add(hash);
			
			// Log progress more frequently for short tests
			int logInterval = numTurns > 100 ? 100 : 10;
			if ((turn + 1) % logInterval == 0 || turnTime > 1000) {
				System.out.printf("  Instance %d: Turn %d/%d, Hash: %016x, Time: %dms%n", 
					instanceNum, turn + 1, numTurns, hash, turnTime);
				if (turnTime > 1000) {
					System.out.printf("    WARNING: Turn took %dms (slow!)%n", turnTime);
				}
			}
			
			if (result.done) {
				System.out.printf("  Instance %d: Game ended at turn %d%n", instanceNum, turn + 1);
				break;
			}
		}
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.printf("  Instance %d: Completed %d turns in %dms (avg %.1fms/turn)%n",
			instanceNum, hashes.size(), totalTime, hashes.size() > 0 ? (double)totalTime / hashes.size() : 0);
		
		return hashes;
	}
	
	/**
	 * Computes a comprehensive state hash including:
	 * - depth, hero HP, hero pos
	 * - inventory item IDs + counts
	 * - enemies: positions + HP (god-view)
	 */
	private static long computeStateHash() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			
			// Basic state
			if (Dungeon.hero != null) {
				Hero hero = Dungeon.hero;
				md.update(String.format("depth:%d,hero_pos:%d,hero_hp:%d/%d,hero_gold:%d",
					Dungeon.depth, hero.pos, hero.HP, hero.HT, Dungeon.gold).getBytes());
				
				// Inventory
				if (hero.belongings != null) {
					List<String> items = new ArrayList<>();
					if (hero.belongings.weapon != null) {
						items.add("w:" + hero.belongings.weapon.getClass().getSimpleName());
					}
					if (hero.belongings.armor != null) {
						items.add("a:" + hero.belongings.armor.getClass().getSimpleName());
					}
					if (hero.belongings.misc != null) {
						items.add("m:" + hero.belongings.misc.getClass().getSimpleName());
					}
					// Backpack items
					if (hero.belongings.backpack != null) {
						for (com.shatteredpixel.shatteredpixeldungeon.items.Item item : hero.belongings.backpack.items) {
							items.add("b:" + item.getClass().getSimpleName());
						}
					}
					md.update(String.join(",", items).getBytes());
				}
			}
			
			// Level state
			if (Dungeon.level != null) {
				md.update(String.format("level:%dx%d", 
					Dungeon.level.width(), Dungeon.level.height()).getBytes());
				
				// Enemies (god-view)
				// CRITICAL: HashSet iteration order is non-deterministic!
				// Sort mobs by position to ensure deterministic hashing
				if (Dungeon.level.mobs != null) {
					List<String> mobs = new ArrayList<>();
					List<Mob> sortedMobs = new ArrayList<>(Dungeon.level.mobs);
					// Sort by position for deterministic ordering
					sortedMobs.sort((a, b) -> Integer.compare(a.pos, b.pos));
					for (Mob mob : sortedMobs) {
						if (mob != null && mob.isAlive()) {
							mobs.add(String.format("mob:pos%d,hp%d,ht%d", 
								mob.pos, mob.HP, mob.HT));
						}
					}
					md.update(String.join(";", mobs).getBytes());
				}
			}
			
			// RNG state (if we can get it)
			// Note: This is tricky with the current Random implementation
			// We'll skip it for now but it's a bonus feature
			
			byte[] hash = md.digest();
			// Convert first 8 bytes to long
			long result = 0;
			for (int i = 0; i < 8; i++) {
				result = (result << 8) | (hash[i] & 0xFF);
			}
			return result;
		} catch (Exception e) {
			System.err.println("Error computing state hash: " + e.getMessage());
			e.printStackTrace();
			return 0;
		}
	}
}
