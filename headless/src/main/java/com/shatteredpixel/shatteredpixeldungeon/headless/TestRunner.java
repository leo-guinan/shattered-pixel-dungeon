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

import java.util.concurrent.*;

/**
 * Test runner with timeout support.
 * Ensures tests complete within a deterministic maximum time.
 */
public final class TestRunner {
	
	/**
	 * Runs a test with a timeout.
	 * 
	 * @param testName Name of the test (for logging)
	 * @param timeoutSeconds Maximum time allowed in seconds
	 * @param test The test to run
	 * @return true if test passed, false if failed or timed out
	 */
	public static boolean runWithTimeout(String testName, int timeoutSeconds, Runnable test) {
		System.out.println("=== Running: " + testName + " (timeout: " + timeoutSeconds + "s) ===");
		long startTime = System.currentTimeMillis();
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<?> future = executor.submit(() -> {
			try {
				test.run();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		try {
			future.get(timeoutSeconds, TimeUnit.SECONDS);
			long elapsed = System.currentTimeMillis() - startTime;
			System.out.println("✓ " + testName + " PASSED in " + elapsed + "ms");
			executor.shutdown();
			return true;
		} catch (TimeoutException e) {
			future.cancel(true);
			executor.shutdownNow();
			long elapsed = System.currentTimeMillis() - startTime;
			System.err.println("✗ " + testName + " TIMED OUT after " + elapsed + "ms (limit: " + timeoutSeconds + "s)");
			return false;
		} catch (Exception e) {
			long elapsed = System.currentTimeMillis() - startTime;
			System.err.println("✗ " + testName + " FAILED after " + elapsed + "ms");
			if (e.getCause() != null) {
				e.getCause().printStackTrace();
			} else {
				e.printStackTrace();
			}
			executor.shutdownNow();
			return false;
		}
	}
	
	/**
	 * Runs a test that returns a boolean result.
	 */
	public static boolean runWithTimeout(String testName, int timeoutSeconds, Callable<Boolean> test) {
		System.out.println("=== Running: " + testName + " (timeout: " + timeoutSeconds + "s) ===");
		long startTime = System.currentTimeMillis();
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(test);
		
		try {
			Boolean result = future.get(timeoutSeconds, TimeUnit.SECONDS);
			long elapsed = System.currentTimeMillis() - startTime;
			if (result) {
				System.out.println("✓ " + testName + " PASSED in " + elapsed + "ms");
			} else {
				System.err.println("✗ " + testName + " FAILED in " + elapsed + "ms");
			}
			executor.shutdown();
			return result;
		} catch (TimeoutException e) {
			future.cancel(true);
			executor.shutdownNow();
			long elapsed = System.currentTimeMillis() - startTime;
			System.err.println("✗ " + testName + " TIMED OUT after " + elapsed + "ms (limit: " + timeoutSeconds + "s)");
			return false;
		} catch (Exception e) {
			long elapsed = System.currentTimeMillis() - startTime;
			System.err.println("✗ " + testName + " FAILED after " + elapsed + "ms");
			if (e.getCause() != null) {
				e.getCause().printStackTrace();
			} else {
				e.printStackTrace();
			}
			executor.shutdownNow();
			return false;
		}
	}
}
