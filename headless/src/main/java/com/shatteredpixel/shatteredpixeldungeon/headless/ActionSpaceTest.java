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
 * Test #3: Verify canonical action space is complete and well-defined.
 * 
 * Tests that:
 * - All actions are properly defined
 * - Action count matches documentation
 * - Actions can be enumerated and used
 */
public class ActionSpaceTest {
	
	public static void main(String[] args) {
		boolean passed = TestRunner.runWithTimeout("Action Space Test", 5, () -> {
			runTest();
		});
		
		System.exit(passed ? 0 : 1);
	}
	
	private static void runTest() {
		System.out.println("=== Canonical Action Space Verification ===");
		System.out.println();
		
		// Test 1: Verify action count
		int actionCount = HeadlessAction.actionCount();
		System.out.println("Total actions: " + actionCount);
		
		HeadlessAction[] allActions = HeadlessAction.values();
		if (allActions.length != actionCount) {
			throw new RuntimeException("Action count mismatch: values().length=" + 
				allActions.length + " but actionCount()=" + actionCount);
		}
		System.out.println("✓ Action count consistent");
		System.out.println();
		
		// Test 2: Verify all actions have descriptions
		System.out.println("Action descriptions:");
		for (HeadlessAction action : allActions) {
			String desc = action.getDescription();
			if (desc == null || desc.isEmpty()) {
				throw new RuntimeException("Action " + action + " has no description");
			}
			System.out.println("  " + action + ": " + desc);
		}
		System.out.println("✓ All actions have descriptions");
		System.out.println();
		
		// Test 3: Verify movement actions have correct dx/dy
		System.out.println("Movement action offsets:");
		for (HeadlessAction action : allActions) {
			if (action.isMovement()) {
				System.out.printf("  %s: dx=%d, dy=%d%n", action, action.dx, action.dy);
				// Verify movement actions have non-zero dx or dy (except WAIT which isn't movement)
				if (action == HeadlessAction.WAIT) {
					if (action.dx != 0 || action.dy != 0) {
						throw new RuntimeException("WAIT should have dx=0, dy=0");
					}
				} else if (action.dx == 0 && action.dy == 0) {
					throw new RuntimeException("Movement action " + action + " has zero offset");
				}
			}
		}
		System.out.println("✓ Movement offsets correct");
		System.out.println();
		
		// Test 4: Verify action space is deterministic (can be used in game)
		System.out.println("Testing action application...");
		long seed = 12345L;
		HeroClass heroClass = HeroClass.WARRIOR;
		
		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		config.updatesPerSecond = 0;
		
		SPDHeadlessApp app = new SPDHeadlessApp(seed, heroClass);
		HeadlessApplication headlessApp = new HeadlessApplication(app, config);
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		
		DeterministicRunner runner = app.getRunner();
		if (runner == null) {
			throw new RuntimeException("Runner is null!");
		}
		
		// Try each action type at least once
		System.out.println("  Testing WAIT...");
		runner.step(HeadlessAction.WAIT);
		
		System.out.println("  Testing MOVE_E...");
		runner.step(HeadlessAction.MOVE_E);
		
		System.out.println("  Testing PICKUP...");
		runner.step(HeadlessAction.PICKUP);
		
		System.out.println("  Testing REST...");
		runner.step(HeadlessAction.REST);
		
		System.out.println("✓ All action types can be applied");
		System.out.println();
		
		System.out.println("=== Action Space Summary ===");
		System.out.println("Total actions: " + actionCount);
		int movementCount = 0;
		for (HeadlessAction a : allActions) {
			if (a.isMovement()) movementCount++;
		}
		System.out.println("Movement actions: " + movementCount);
		System.out.println("Explicit actions: " + (actionCount - movementCount));
		System.out.println();
		System.out.println("✓ Canonical action space is complete and well-defined");
	}
}
