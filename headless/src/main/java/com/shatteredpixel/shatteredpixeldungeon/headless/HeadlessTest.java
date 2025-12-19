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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

/**
 * Simple test for the headless system.
 */
public class HeadlessTest {
	
	public static void main(String[] args) {
		System.out.println("=== SPD Headless Test ===");
		
		try {
			// Initialize headless application
			HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
			config.updatesPerSecond = 0;
			
			long seed = 12345L;
			HeroClass heroClass = HeroClass.WARRIOR;
			
			SPDHeadlessApp app = new SPDHeadlessApp(seed, heroClass);
			HeadlessApplication headlessApp = new HeadlessApplication(app, config);
			
			// Give it a moment to initialize
			Thread.sleep(100);
			
			DeterministicRunner runner = app.getRunner();
			if (runner == null) {
				System.err.println("ERROR: Runner is null!");
				System.exit(1);
			}
			
			System.out.println("✓ Headless app initialized");
			System.out.println("✓ Runner created");
			System.out.println("Seed: " + seed + ", Class: " + heroClass);
			System.out.println();
			
			// Take a few steps
			System.out.println("Taking 10 steps...");
			for (int turn = 0; turn < 10; turn++) {
				HeadlessAction action = HeadlessAction.MOVE_E; // Move east
				DeterministicRunner.StepResult result = runner.step(action);
				
				DeterministicRunner.GameState state = result.state;
				System.out.printf("Turn %2d: Action=%s, Pos=(%d,%d), HP=%d/%d, Depth=%d, Hash=%016x, Done=%s%n",
					turn + 1,
					action.name(),
					state.heroX,
					state.heroY,
					state.heroHP,
					state.heroMaxHP,
					state.depth,
					result.stateHash,
					result.done
				);
				
				if (result.done) {
					System.out.println("Game ended!");
					break;
				}
			}
			
			System.out.println();
			System.out.println("=== Test Complete ===");
			System.out.println("✓ All steps executed successfully");
			
			// Test determinism: run again with same seed and verify state hashes match
			System.out.println();
			System.out.println("Testing determinism...");
			SPDHeadlessApp app2 = new SPDHeadlessApp(seed, heroClass);
			HeadlessApplication headlessApp2 = new HeadlessApplication(app2, config);
			Thread.sleep(100);
			
			DeterministicRunner runner2 = app2.getRunner();
			long[] hashes1 = new long[5];
			long[] hashes2 = new long[5];
			
			for (int turn = 0; turn < 5; turn++) {
				DeterministicRunner.StepResult r1 = runner.step(HeadlessAction.MOVE_E);
				DeterministicRunner.StepResult r2 = runner2.step(HeadlessAction.MOVE_E);
				hashes1[turn] = r1.stateHash;
				hashes2[turn] = r2.stateHash;
			}
			
			boolean deterministic = true;
			for (int i = 0; i < 5; i++) {
				if (hashes1[i] != hashes2[i]) {
					System.err.printf("ERROR: Hash mismatch at turn %d: %016x != %016x%n", 
						i+1, hashes1[i], hashes2[i]);
					deterministic = false;
				}
			}
			
			if (deterministic) {
				System.out.println("✓ Determinism test passed: State hashes match!");
			} else {
				System.err.println("✗ Determinism test FAILED!");
				System.exit(1);
			}
			
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
